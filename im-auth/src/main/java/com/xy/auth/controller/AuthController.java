package com.xy.auth.controller;


import com.xy.auth.domain.vo.UserVo;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.service.ImUserService;
import com.xy.auth.service.QrCodeService;
import com.xy.auth.service.SmsService;
import com.xy.auth.utils.RSAUtil;
import com.xy.auth.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xy.auth.constant.Qrcode.*;

@RestController
@RequestMapping("/user")
public class AuthController {

    //rsa密匙类
    @Resource
    private RSAKeyProperties rsaKeyProp;

    @Resource
    private SmsService smsService;

    @Resource
    private ImUserService imUserService;

    @Resource
    private QrCodeService codeService;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 生成用于认证的二维码。
     *
     * @param qrcode 要编码到二维码中的随机字符串
     * @return 包含base64编码的二维码图片的Map
     */
    @GetMapping("/qrcode")
    public Map<String, String> qrcode(@RequestParam("qrcode") String qrcode) {
        Map<String, String> map = new HashMap<>();

        String code = QRCODE_PREFIX + qrcode;

        // 生成 base64 图片二维码
        String codeToBase64 = codeService.createCodeToBase64(code);

        // 将二维码状态信息存储为一个Map
        Map<String, Object> qrCodeInfo = new HashMap<>();
        qrCodeInfo.put("status", QRCODE_PENDING);
        qrCodeInfo.put("createdAt", System.currentTimeMillis());

        // 设置3分钟二维码有效期，状态为“待扫描”
        redisUtil.set(code, qrCodeInfo, 3, TimeUnit.MINUTES);

        map.put("qrcode", codeToBase64);

        return map;
    }

    /**
     * 处理二维码扫描过程。
     *
     * @param map 包含二维码和用户信息的Map
     * @return 包含扫描结果的ResponseEntity
     */
    @PostMapping("/qrcode/scan")
    public ResponseEntity<Map<String, Object>> handleScan(@RequestBody Map<String, Object> map) {
        String qrcode = (String) map.get("qrcode");
        String userId = (String) map.get("userId");

        String redisKey = QRCODE_PREFIX + qrcode;

        Map<String, Object> response = new HashMap<>();

        if (!redisUtil.hasKey(redisKey)) {
            response.put("status", "error");
            response.put("message", "Invalid or expired QR code.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Update QR code status information
        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);
        qrCodeInfo.put("status", QRCODE_SCANNED);
        qrCodeInfo.put("userId", userId);
        qrCodeInfo.put("scannedAt", System.currentTimeMillis());

        // Set the updated status to scanned with an expiration time of 30 seconds
        redisUtil.set(redisKey, qrCodeInfo, 30, TimeUnit.SECONDS);

        response.put("status", "success");
        response.put("message", "QR code scanned successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * 检查二维码的登录状态。
     *
     * @param qrcode 要检查的二维码字符串
     * @return 包含二维码当前状态的ResponseEntity
     */
    @GetMapping("/qrcode/status")
    public ResponseEntity<?> checkLoginStatus(@RequestParam("qrcode") String qrcode) {
        String redisKey = QRCODE_PREFIX + qrcode;
        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);
        if (qrCodeInfo == null) {
            return ResponseEntity.ok(QRCODE_EXPIRED);
        } else if (QRCODE_SCANNED.equals(qrCodeInfo.get("status"))) {
            // 更新状态为已登录，过期时间为20秒
            qrCodeInfo.put("status", "LOGGED_IN");
            // 生成临时密码，让前端使用临时密码登录
            qrCodeInfo.put("password", String.valueOf((int) ((Math.random() * 9 + 1) * 100000)));
            qrCodeInfo.put("loggedInAt", System.currentTimeMillis());
            redisUtil.set(redisKey, qrCodeInfo, 20, TimeUnit.SECONDS);
            return ResponseEntity.ok(qrCodeInfo);
        } else {
            return ResponseEntity.ok(qrCodeInfo);
        }
    }


    /**
     * 获取用户信息。
     *
     * @param userId 用户ID
     * @return 包含用户信息的UserVo对象
     */
    @GetMapping("/info")
    public UserVo info(@RequestParam("userId") String userId) {
        return imUserService.info(userId);
    }


    /**
     * 验证手机号码并发送验证码。
     *
     * @param phone 要验证并发送验证码的手机号码
     * @return 发送结果的字符串响应
     * @throws Exception 如果发送过程中出现错误
     */
    @GetMapping(value = "/sms")
    public String sms(@RequestParam("phone") String phone) throws Exception {
        return smsService.sendMessage(phone);
    }

    /**
     * 获取公钥
     *
     * @return
     */
    @GetMapping(value = "/publickey")
    public Map<String, String> getLoginPublicKey() {
        Map<String, String> map = new HashMap<>();
        map.put("publicKey", rsaKeyProp.getPublicKeyStr());
        return map;
    }

    /**
     * 密码加密
     *
     * @param password 密码
     * @return 加密后的密文
     * @throws Exception
     */
    @PostMapping("/password")
    public String passwordEncode(@RequestParam("password") String password) throws Exception {
        return RSAUtil.encrypt(password, rsaKeyProp.getPublicKeyStr());
    }

    /**
     * 用户是否在线
     *
     * @param userId
     * @return
     */
    @GetMapping("/online")
    public boolean isOnline(@RequestParam("userId") String userId) {
        return imUserService.isOnline(userId);
    }


}
