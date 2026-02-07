package com.xy.lucky.auth.service;

import com.xy.lucky.auth.domain.OAuth2AuthorizeResult;
import com.xy.lucky.auth.domain.OAuth2TokenResult;
import jakarta.servlet.http.HttpServletRequest;

/**
 * OAuth2.1 规范核心服务接口，支持 PKCE 扩展
 */
public interface OAuth2Service {

    /**
     * 处理 OAuth2 授权请求，生成授权码
     *
     * @param request             当前请求上下文
     * @param responseType        响应类型（固定为 code）
     * @param clientId            客户端 ID
     * @param redirectUri         重定向回调地址
     * @param codeChallenge       PKCE 挑战码
     * @param codeChallengeMethod PKCE 挑战方法（推荐 S256）
     * @param state               CSRF 防护状态值
     * @param scope               授权范围
     * @return 授权结果
     */
    OAuth2AuthorizeResult authorize(HttpServletRequest request,
                                    String responseType,
                                    String clientId,
                                    String redirectUri,
                                    String codeChallenge,
                                    String codeChallengeMethod,
                                    String state,
                                    String scope);

    /**
     * 使用授权码和 PKCE 校验码换取令牌
     *
     * @param request      当前请求上下文
     * @param grantType    授权类型（固定为 authorization_code）
     * @param code         有效的授权码
     * @param redirectUri  重定向地址（需与授权请求一致）
     * @param codeVerifier PKCE 原始校验码
     * @param clientId     客户端 ID
     * @return 令牌结果
     */
    OAuth2TokenResult token(HttpServletRequest request,
                            String grantType,
                            String code,
                            String redirectUri,
                            String codeVerifier,
                            String clientId);
}

