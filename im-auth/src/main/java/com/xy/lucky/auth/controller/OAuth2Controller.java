package com.xy.lucky.auth.controller;

import com.xy.lucky.auth.domain.OAuth2AuthorizeResult;
import com.xy.lucky.auth.domain.OAuth2TokenResult;
import com.xy.lucky.auth.service.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth2.1 认证接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/oauth2", "/api/{version}/oauth2"})
@Tag(name = "OAuth2", description = "OAuth2.1 认证中心")
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    @GetMapping("/authorize")
    @Operation(summary = "授权码请求", description = "遵循 OAuth2.1 规范，支持 PKCE 挑战。成功后将重定向至指定 URI 并携带 code。")
    public OAuth2AuthorizeResult authorize(HttpServletRequest request,
                                           @Parameter(description = "响应类型，固定为 code", required = true)
                                           @RequestParam("response_type") String responseType,
                                           @Parameter(description = "客户端标识", required = true)
                                           @RequestParam("client_id") String clientId,
                                           @Parameter(description = "回调地址", required = true)
                                           @RequestParam("redirect_uri") String redirectUri,
                                           @Parameter(description = "PKCE 挑战码", required = true)
                                           @RequestParam("code_challenge") String codeChallenge,
                                           @Parameter(description = "挑战方法 (S256)", required = true)
                                           @RequestParam("code_challenge_method") String codeChallengeMethod,
                                           @Parameter(description = "透传状态值")
                                           @RequestParam(value = "state", required = false) String state,
                                           @Parameter(description = "授权范围")
                                           @RequestParam(value = "scope", required = false) String scope) {
        return oauth2Service.authorize(request, responseType, clientId, redirectUri,
                codeChallenge, codeChallengeMethod, state, scope);
    }

    @PostMapping("/token")
    @Operation(summary = "换取访问令牌", description = "使用授权码及 PKCE 校验码换取正式的 AccessToken 和 RefreshToken。")
    public OAuth2TokenResult token(HttpServletRequest request,
                                   @Parameter(description = "授权类型，固定为 authorization_code", required = true)
                                   @RequestParam("grant_type") String grantType,
                                   @Parameter(description = "授权码", required = true)
                                   @RequestParam("code") String code,
                                   @Parameter(description = "回调地址（须一致）", required = true)
                                   @RequestParam("redirect_uri") String redirectUri,
                                   @Parameter(description = "PKCE 校验码", required = true)
                                   @RequestParam("code_verifier") String codeVerifier,
                                   @Parameter(description = "客户端标识", required = true)
                                   @RequestParam("client_id") String clientId) {
        return oauth2Service.token(request, grantType, code, redirectUri, codeVerifier, clientId);
    }
}

