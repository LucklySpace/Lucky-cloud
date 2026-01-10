package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.config.SmsCodeProperties;
import com.xy.lucky.auth.service.SmsCodeService;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.exception.AuthenticationFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String FIELD_HASH = "h";
    private static final String FIELD_DEVICE = "d";
    private static final String FIELD_IP = "ip";
    private static final String FIELD_RETRY = "rc";

    private static final DefaultRedisScript<Long> VERIFY_AND_CONSUME_SCRIPT = new DefaultRedisScript<>(
            """
                    local key = KEYS[1]
                    local expectHash = ARGV[1]
                    local deviceId = ARGV[2]
                    local clientIp = ARGV[3]
                    local maxRetry = tonumber(ARGV[4])
                    
                    if redis.call('EXISTS', key) == 0 then
                      return 0
                    end
                    
                    local retry = tonumber(redis.call('HGET', key, 'rc') or '0')
                    if retry >= maxRetry then
                      redis.call('DEL', key)
                      return -4
                    end
                    
                    local storedHash = redis.call('HGET', key, 'h')
                    if storedHash ~= expectHash then
                      retry = redis.call('HINCRBY', key, 'rc', 1)
                      if retry >= maxRetry then
                        redis.call('DEL', key)
                        return -4
                      end
                      return -1
                    end
                    
                    local storedDevice = redis.call('HGET', key, 'd') or ''
                    if storedDevice ~= '' and deviceId ~= '' and storedDevice ~= deviceId then
                      return -2
                    end
                    
                    local storedIp = redis.call('HGET', key, 'ip') or ''
                    if storedIp ~= '' and clientIp ~= '' and storedIp ~= clientIp then
                      return -3
                    end
                    
                    redis.call('DEL', key)
                    return 1
                    """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final CryptoProperties cryptoProperties;
    private final SmsCodeProperties smsCodeProperties;

    private static String hmacSha256Hex(String secret, String message) {
        try {
            if (!StringUtils.hasText(secret)) {
                throw new IllegalStateException("HMAC secret is blank");
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC_SHA256 failed", e);
        }
    }

    @Override
    public String generateAndStore(String phone, String clientIp, String deviceId) {
        if (!StringUtils.hasText(phone)) {
            throw new AuthenticationFailException(ResultCode.BAD_REQUEST);
        }

        String code = generateCode();
        String key = buildKey(phone);
        String codeHash = hmacSha256Hex(cryptoProperties.getSign().getSecret(), code + ":" + phone);

        String normalizedDevice = StringUtils.hasText(deviceId) ? deviceId.trim() : "";
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp.trim() : "";

        Map<String, String> record = Map.of(
                FIELD_HASH, codeHash,
                FIELD_DEVICE, normalizedDevice,
                FIELD_IP, normalizedIp,
                FIELD_RETRY, "0"
        );

        Duration ttl = smsCodeProperties.getTtl();
        stringRedisTemplate.opsForHash().putAll(key, record);
        if (!ttl.isZero() && !ttl.isNegative()) {
            stringRedisTemplate.expire(key, ttl);
        }
        return code;
    }

    @Override
    public VerifyResult verifyAndConsume(String phone, String plainCode, String deviceId, String clientIp) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(plainCode)) {
            return VerifyResult.NOT_FOUND;
        }

        String key = buildKey(phone);
        String expectedHash = hmacSha256Hex(cryptoProperties.getSign().getSecret(), plainCode + ":" + phone);

        String normalizedDevice = StringUtils.hasText(deviceId) ? deviceId.trim() : "";
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp.trim() : "";

        Long result = stringRedisTemplate.execute(
                VERIFY_AND_CONSUME_SCRIPT,
                List.of(key),
                expectedHash,
                normalizedDevice,
                normalizedIp,
                String.valueOf(smsCodeProperties.getMaxRetry())
        );

        long code = Objects.requireNonNullElse(result, 0L);
        if (code == 1L) return VerifyResult.OK;
        if (code == 0L) return VerifyResult.NOT_FOUND;
        if (code == -1L) return VerifyResult.WRONG_CODE;
        if (code == -2L) return VerifyResult.DEVICE_MISMATCH;
        if (code == -3L) return VerifyResult.IP_MISMATCH;
        if (code == -4L) return VerifyResult.LOCKED;
        return VerifyResult.NOT_FOUND;
    }

    @Override
    public void deleteCode(String phone) {
        if (!StringUtils.hasText(phone)) return;
        stringRedisTemplate.delete(buildKey(phone));
    }

    private String buildKey(String phone) {
        return IMConstant.SMS_KEY_PREFIX + DigestUtils.sha256Hex(phone);
    }

    private String generateCode() {
        int number = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
