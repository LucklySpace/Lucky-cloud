package com.xy.lucky.live.core.sfu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SDP (Session Description Protocol) 工具类
 * <p>
 * 用于解析和生成 SDP 消息，提取 WebRTC 连接所需的参数。
 * SDP 是 WebRTC 中用于协商媒体格式、传输地址等信息的协议。
 *
 * <h2>SDP 结构</h2>
 * <pre>
 * v=0                          # 版本
 * o=- 123456789 2 IN IP4 127.0.0.1  # 会话标识
 * s=-                          # 会话名称
 * t=0 0                        # 时间
 * m=video 9 UDP/TLS/RTP/SAVPF 96  # 媒体描述
 * c=IN IP4 0.0.0.0            # 连接信息
 * a=rtpmap:96 H264/90000      # RTP 映射
 * a=ice-ufrag:xxxx            # ICE 用户名片段
 * a=ice-pwd:xxxx              # ICE 密码
 * a=fingerprint:sha-256 xx    # DTLS 指纹
 * a=setup:actpass             # DTLS 角色
 * </pre>
 *
 * <h2>关键参数</h2>
 * <ul>
 *   <li><b>ice-ufrag</b>: ICE 用户名片段，用于 STUN 绑定请求认证</li>
 *   <li><b>ice-pwd</b>: ICE 密码，用于 STUN 消息完整性验证</li>
 *   <li><b>fingerprint</b>: DTLS 证书指纹，用于 DTLS 握手验证</li>
 *   <li><b>setup</b>: DTLS 角色（actpass/active/passive）</li>
 *   <li><b>candidate</b>: ICE 候选地址信息</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 * @see <a href="https://tools.ietf.org/html/rfc4566">RFC 4566 - SDP</a>
 * @see <a href="https://tools.ietf.org/html/rfc5245">RFC 5245 - ICE</a>
 */
public class SdpUtils {

    private static final Logger log = LoggerFactory.getLogger(SdpUtils.class);

    /**
     * 提取 ICE 用户名片段的正则表达式
     */
    private static final Pattern ICE_UFRAG_PATTERN = Pattern.compile("a=ice-ufrag:([^\\r\\n]+)");

    /**
     * 提取 ICE 密码的正则表达式
     */
    private static final Pattern ICE_PWD_PATTERN = Pattern.compile("a=ice-pwd:([^\\r\\n]+)");

    /**
     * 提取 DTLS 指纹的正则表达式
     */
    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("a=fingerprint:(\\S+)\\s+([A-F0-9:]+)");

    /**
     * 提取 DTLS setup 角色的正则表达式
     */
    private static final Pattern SETUP_PATTERN = Pattern.compile("a=setup:(\\S+)");

    /**
     * 提取媒体类型的正则表达式
     */
    private static final Pattern MEDIA_PATTERN = Pattern.compile("m=(\\w+)\\s+(\\d+)\\s+(\\S+)\\s+(.+)");

    /**
     * 提取连接信息的正则表达式
     */
    private static final Pattern CONNECTION_PATTERN = Pattern.compile("c=IN\\s+IP(\\d)\\s+(\\S+)");

    /**
     * 从 SDP 中提取 ICE 用户名片段
     *
     * @param sdp SDP 字符串
     * @return ICE 用户名片段，如果未找到返回 null
     */
    public static String extractIceUfrag(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return null;
        }
        Matcher matcher = ICE_UFRAG_PATTERN.matcher(sdp);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 从 SDP 中提取 ICE 密码
     *
     * @param sdp SDP 字符串
     * @return ICE 密码，如果未找到返回 null
     */
    public static String extractIcePwd(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return null;
        }
        Matcher matcher = ICE_PWD_PATTERN.matcher(sdp);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 从 SDP 中提取 DTLS 指纹
     *
     * @param sdp SDP 字符串
     * @return DTLS 指纹信息（算法和值），如果未找到返回 null
     */
    public static Map<String, String> extractFingerprint(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return null;
        }
        Matcher matcher = FINGERPRINT_PATTERN.matcher(sdp);
        if (matcher.find()) {
            Map<String, String> result = new HashMap<>();
            result.put("algorithm", matcher.group(1).trim());
            result.put("value", matcher.group(2).trim());
            return result;
        }
        return null;
    }

    /**
     * 从 SDP 中提取 DTLS setup 角色
     *
     * @param sdp SDP 字符串
     * @return setup 角色（actpass/active/passive），如果未找到返回 null
     */
    public static String extractSetup(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return null;
        }
        Matcher matcher = SETUP_PATTERN.matcher(sdp);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 从 SDP 中提取所有媒体行
     *
     * @param sdp SDP 字符串
     * @return 媒体行列表，每个元素包含 [type, port, protocol, formats]
     */
    public static List<Map<String, String>> extractMedia(String sdp) {
        List<Map<String, String>> mediaList = new ArrayList<>();
        if (sdp == null || sdp.isEmpty()) {
            return mediaList;
        }

        Matcher matcher = MEDIA_PATTERN.matcher(sdp);
        while (matcher.find()) {
            Map<String, String> media = new HashMap<>();
            media.put("type", matcher.group(1));        // audio/video
            media.put("port", matcher.group(2));        // 端口
            media.put("protocol", matcher.group(3));    // UDP/TLS/RTP/SAVPF
            media.put("formats", matcher.group(4));      // 负载类型列表
            mediaList.add(media);
        }

        return mediaList;
    }

    /**
     * 从 SDP 中提取连接信息
     *
     * @param sdp SDP 字符串
     * @return 连接信息（IP 版本和地址），如果未找到返回 null
     */
    public static Map<String, String> extractConnection(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return null;
        }
        Matcher matcher = CONNECTION_PATTERN.matcher(sdp);
        if (matcher.find()) {
            Map<String, String> result = new HashMap<>();
            result.put("version", matcher.group(1));  // 4 或 6
            result.put("address", matcher.group(2));
            return result;
        }
        return null;
    }

    /**
     * 修改 SDP 中的 ICE 候选地址
     * <p>
     * 将客户端发送的候选地址替换为服务器配置的地址
     *
     * @param sdp           原始 SDP
     * @param candidateIp   服务器候选 IP 地址
     * @param candidatePort 服务器候选端口
     * @return 修改后的 SDP
     */
    public static String modifyCandidateIp(String sdp, String candidateIp, int candidatePort) {
        if (sdp == null || candidateIp == null) {
            return sdp;
        }

        // 替换连接信息中的 IP
        sdp = sdp.replaceAll("c=IN IP\\d \\S+", "c=IN IP4 " + candidateIp);

        // 替换候选地址中的 IP（如果存在）
        // a=candidate:... 192.168.1.100 50000 ...
        sdp = sdp.replaceAll("(a=candidate:[^\\s]+\\s+\\d+\\s+\\S+\\s+)(\\S+)(\\s+\\d+)",
                "$1" + candidateIp + "$3");

        return sdp;
    }

    /**
     * 生成 Answer SDP
     * <p>
     * 基于 Offer SDP 生成 Answer SDP，用于 SFU 模式下的媒体协商
     *
     * @param offerSdp      Offer SDP
     * @param candidateIp   服务器候选 IP
     * @param candidatePort 服务器候选端口
     * @param iceUfrag      服务器 ICE 用户名片段
     * @param icePwd        服务器 ICE 密码
     * @return Answer SDP
     */
    public static String generateAnswer(String offerSdp, String candidateIp, int candidatePort,
                                        String iceUfrag, String icePwd) {
        if (offerSdp == null) {
            return null;
        }

        StringBuilder answer = new StringBuilder();

        // 解析 Offer SDP
        String[] lines = offerSdp.split("\\r?\\n");

        // 生成会话级描述
        answer.append("v=0\r\n");
        answer.append("o=- ").append(System.currentTimeMillis()).append(" 2 IN IP4 ").append(candidateIp).append("\r\n");
        answer.append("s=-\r\n");
        answer.append("t=0 0\r\n");
        answer.append("c=IN IP4 ").append(candidateIp).append("\r\n");

        // 处理媒体描述
        boolean inMedia = false;
        for (String line : lines) {
            if (line.startsWith("m=")) {
                inMedia = true;
                // 保持媒体类型和格式，但修改端口和协议
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    answer.append("m=").append(parts[1]).append(" ")
                            .append(candidatePort).append(" UDP/TLS/RTP/SAVPF");
                    // 保留负载类型
                    for (int i = 3; i < parts.length; i++) {
                        answer.append(" ").append(parts[i]);
                    }
                    answer.append("\r\n");
                }
            } else if (line.startsWith("a=")) {
                if (inMedia) {
                    // 处理媒体级属性
                    if (line.startsWith("a=ice-ufrag:")) {
                        answer.append("a=ice-ufrag:").append(iceUfrag).append("\r\n");
                    } else if (line.startsWith("a=ice-pwd:")) {
                        answer.append("a=ice-pwd:").append(icePwd).append("\r\n");
                    } else if (line.startsWith("a=setup:")) {
                        // 确定 DTLS 角色：如果 Offer 是 actpass，Answer 应该是 active
                        String setup = extractSetup(offerSdp);
                        if ("actpass".equals(setup)) {
                            answer.append("a=setup:active\r\n");
                        } else {
                            answer.append(line).append("\r\n");
                        }
                    } else if (line.startsWith("a=candidate:")) {
                        // 替换候选地址
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 5) {
                            answer.append("a=candidate:").append(parts[1]).append(" 1 UDP 2130706431 ")
                                    .append(candidateIp).append(" ").append(candidatePort)
                                    .append(" typ host\r\n");
                        }
                    } else {
                        // 保留其他属性
                        answer.append(line).append("\r\n");
                    }
                } else {
                    // 会话级属性
                    if (line.startsWith("a=ice-ufrag:")) {
                        answer.append("a=ice-ufrag:").append(iceUfrag).append("\r\n");
                    } else if (line.startsWith("a=ice-pwd:")) {
                        answer.append("a=ice-pwd:").append(icePwd).append("\r\n");
                    } else {
                        answer.append(line).append("\r\n");
                    }
                }
            } else if (line.startsWith("c=")) {
                // 连接信息已在会话级处理，跳过
            } else {
                // 保留其他行
                answer.append(line).append("\r\n");
            }
        }

        return answer.toString();
    }

    /**
     * 验证 SDP 格式
     *
     * @param sdp SDP 字符串
     * @return true 如果格式有效
     */
    public static boolean isValidSdp(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return false;
        }

        // 检查基本字段
        return sdp.contains("v=") &&
                sdp.contains("o=") &&
                sdp.contains("s=") &&
                sdp.contains("t=");
    }

    /**
     * 从 SDP 中提取所有 ICE 候选地址
     *
     * @param sdp SDP 字符串
     * @return ICE 候选地址列表
     */
    public static List<Map<String, String>> extractCandidates(String sdp) {
        List<Map<String, String>> candidates = new ArrayList<>();
        if (sdp == null || sdp.isEmpty()) {
            return candidates;
        }

        // a=candidate:foundation 1 UDP 2130706431 192.168.1.100 50000 typ host
        Pattern pattern = Pattern.compile("a=candidate:(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+typ\\s+(\\S+)");
        Matcher matcher = pattern.matcher(sdp);

        while (matcher.find()) {
            Map<String, String> candidate = new HashMap<>();
            candidate.put("foundation", matcher.group(1));
            candidate.put("component", matcher.group(2));
            candidate.put("protocol", matcher.group(3));
            candidate.put("priority", matcher.group(4));
            candidate.put("ip", matcher.group(5));
            candidate.put("port", matcher.group(6));
            candidate.put("type", matcher.group(7));
            candidates.add(candidate);
        }

        return candidates;
    }
}

