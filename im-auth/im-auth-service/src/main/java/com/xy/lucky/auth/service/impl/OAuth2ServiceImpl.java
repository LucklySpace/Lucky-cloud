package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.domain.AuthTokenPair;
import com.xy.lucky.auth.domain.OAuth2AuthorizationCode;
import com.xy.lucky.auth.domain.OAuth2AuthorizeResult;
import com.xy.lucky.auth.domain.OAuth2TokenResult;
import com.xy.lucky.auth.security.config.OAuth2Properties;
import com.xy.lucky.auth.security.helper.PkceUtils;
import com.xy.lucky.auth.service.AuthTokenService;
import com.xy.lucky.auth.service.OAuth2Service;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.auth.utils.RequestContextUtil;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.exception.AuthenticationFailException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OAuth2 授权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private static final String AUTH_CODE_KEY_PREFIX = "auth:oauth2:code:";

    private final OAuth2Properties oAuth2Properties;
    private final RedisCache redisCache;
    private final AuthTokenService authTokenService;


    /**
     * 授权端点逻辑实现，支持 PKCE 校验
     */
    @Override
    public OAuth2AuthorizeResult authorize(HttpServletRequest request,
                                           String responseType,
                                           String clientId,
                                           String redirectUri,
                                           String codeChallenge,
                                           String codeChallengeMethod,
                                           String state,
                                           String scope) {
        if (!"code".equalsIgnoreCase(responseType)) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
        OAuth2Properties.Client client = resolveClient(clientId);
        validateRedirectUri(client, redirectUri);
        validateScope(client, scope);

        enforceAuthorizeRateLimit(request, clientId);

        // 验证 PKCE 参数是否符合规范
        boolean requirePkce = oAuth2Properties.isPkceRequired() || client.isRequirePkce();
        if (requirePkce) {
            validatePkce(codeChallenge, codeChallengeMethod);
        }

        // 必须在登录状态下才能进行授权
        String accessToken = authTokenService
                .resolveAccessToken(request.getHeader(IMConstant.AUTH_TOKEN_HEADER),
                        request.getParameter(IMConstant.ACCESS_TOKEN_PARAM))
                .orElse(null);

        if (!StringUtils.hasText(accessToken) || !JwtUtil.validate(accessToken)) {
            throw new AuthenticationFailException(ResultCode.UNAUTHORIZED);
        }

        String userId = JwtUtil.getUsername(accessToken);
        if (!StringUtils.hasText(userId)) {
            throw new AuthenticationFailException(ResultCode.UNAUTHORIZED);
        }

        // 生成一次性授权码
        String code = UUID.randomUUID().toString().replace("-", "");
        long issuedAt = System.currentTimeMillis();
        long expiresAt = issuedAt + TimeUnit.SECONDS.toMillis(oAuth2Properties.getAuthorizationCodeTtlSeconds());

        OAuth2AuthorizationCode record = new OAuth2AuthorizationCode()
                .setCode(code)
                .setClientId(clientId)
                .setUserId(userId)
                .setRedirectUri(redirectUri)
                .setCodeChallenge(codeChallenge)
                .setCodeChallengeMethod(codeChallengeMethod)
                .setScopes(parseScope(scope))
                .setIssuedAt(issuedAt)
                .setExpiresAt(expiresAt);

        redisCache.set(AUTH_CODE_KEY_PREFIX + code, record, oAuth2Properties.getAuthorizationCodeTtlSeconds(), TimeUnit.SECONDS);

        return new OAuth2AuthorizeResult()
                .setCode(code)
                .setState(state)
                .setRedirectUri(redirectUri)
                .setExpiresIn(oAuth2Properties.getAuthorizationCodeTtlSeconds());
    }

    /**
     * 令牌端点逻辑实现，验证授权码及 PKCE verifier
     */
    @Override
    public OAuth2TokenResult token(HttpServletRequest request,
                                   String grantType,
                                   String code,
                                   String redirectUri,
                                   String codeVerifier,
                                   String clientId) {
        String clientIp = RequestContextUtil.resolveClientIp(request);
        String deviceId = RequestContextUtil.resolveDeviceId(request, clientIp);
        String userAgent = request.getHeader("User-Agent");
        try {
            if (!"authorization_code".equalsIgnoreCase(grantType)) {
                throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
            }

            enforceTokenRateLimit(request, clientId);

            OAuth2AuthorizationCode record = redisCache.get(AUTH_CODE_KEY_PREFIX + code);
            if (record == null) {
                throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
            }

            OAuth2Properties.Client client = resolveClient(clientId);
            validateRedirectUri(client, redirectUri);

            if (!clientId.equals(record.getClientId())
                    || !redirectUri.equals(record.getRedirectUri())) {
                throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
            }

            boolean requirePkce = oAuth2Properties.isPkceRequired() || client.isRequirePkce();
            if (requirePkce) {
                validateCodeVerifier(codeVerifier);
                if (!PkceUtils.verifyChallenge(codeVerifier, record.getCodeChallenge(), record.getCodeChallengeMethod())) {
                    log.warn("PKCE 校验失败: clientId={}, code={}", clientId, code);
                    throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
                }
            }

            redisCache.del(AUTH_CODE_KEY_PREFIX + code);

            AuthTokenPair pair = authTokenService.issueTokens(record.getUserId(), deviceId, clientIp);

            return new OAuth2TokenResult()
                    .setAccessToken(pair.getAccessToken())
                    .setRefreshToken(pair.getRefreshToken())
                    .setExpiresIn(pair.getAccessExpiresIn())
                    .setScope(record.getScopes());
        } catch (Exception ex) {
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }

    private void enforceAuthorizeRateLimit(HttpServletRequest request, String clientId) {
        String clientIp = RequestContextUtil.resolveClientIp(request);
        String iKey = "IM:AUTH:RL:OA:AUTH:I:" + Optional.ofNullable(clientIp).orElse("");
        String cKey = "IM:AUTH:RL:OA:AUTH:C:" + Optional.ofNullable(clientId).orElse("");
        int limit = 10;
        long windowSec = TimeUnit.MINUTES.toSeconds(5);
        long ic = redisCache.incr(iKey, 1);
        if (ic == 1L) redisCache.expire(iKey, windowSec);
        long cc = redisCache.incr(cKey, 1);
        if (cc == 1L) redisCache.expire(cKey, windowSec);
        if (ic > limit || cc > limit) {
            throw new AuthenticationFailException(ResultCode.TOO_MANY_REQUESTS);
        }
    }

    private void enforceTokenRateLimit(HttpServletRequest request, String clientId) {
        String clientIp = RequestContextUtil.resolveClientIp(request);
        String iKey = "IM:AUTH:RL:OA:TOKEN:I:" + Optional.ofNullable(clientIp).orElse("");
        String cKey = "IM:AUTH:RL:OA:TOKEN:C:" + Optional.ofNullable(clientId).orElse("");
        int limit = 10;
        long windowSec = TimeUnit.MINUTES.toSeconds(5);
        long ic = redisCache.incr(iKey, 1);
        if (ic == 1L) redisCache.expire(iKey, windowSec);
        long cc = redisCache.incr(cKey, 1);
        if (cc == 1L) redisCache.expire(cKey, windowSec);
        if (ic > limit || cc > limit) {
            throw new AuthenticationFailException(ResultCode.TOO_MANY_REQUESTS);
        }
    }

    private OAuth2Properties.Client resolveClient(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
        return oAuth2Properties.getClients()
                .stream()
                .filter(c -> clientId.equals(c.getClientId()))
                .findFirst()
                .orElseThrow(() -> new AuthenticationFailException(ResultCode.NO_PERMISSION));
    }

    private void validateRedirectUri(OAuth2Properties.Client client, String redirectUri) {
        if (!StringUtils.hasText(redirectUri) || client.getRedirectUris() == null) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
        if (client.getRedirectUris().stream().noneMatch(uri -> uri.equalsIgnoreCase(redirectUri))) {
            throw new AuthenticationFailException(ResultCode.NO_PERMISSION);
        }
    }

    private void validateScope(OAuth2Properties.Client client, String scope) {
        List<String> requested = parseScope(scope);
        if (requested.isEmpty()) {
            return;
        }
        List<String> allowed = Optional.ofNullable(client.getScopes()).orElse(List.of());
        boolean allAllowed = requested.stream().allMatch(allowed::contains);
        if (!allAllowed) {
            throw new AuthenticationFailException(ResultCode.NO_PERMISSION);
        }
    }

    private List<String> parseScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return List.of();
        }
        return Arrays.stream(scope.split("[ ,]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private void validatePkce(String codeChallenge, String codeChallengeMethod) {
        if (!StringUtils.hasText(codeChallenge) || !StringUtils.hasText(codeChallengeMethod)) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
        if (!"S256".equalsIgnoreCase(codeChallengeMethod)) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
        int len = codeChallenge.length();
        if (len < 43 || len > 128) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
    }

    private void validateCodeVerifier(String codeVerifier) {
        if (!StringUtils.hasText(codeVerifier)) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
        int len = codeVerifier.length();
        if (len < 43 || len > 128) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }
    }
}

