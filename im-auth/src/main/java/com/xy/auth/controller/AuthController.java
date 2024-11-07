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
     * 生成二维码
     * @param qrcode 随机字符串
     * @return
     */
    @GetMapping("/qrcode")
    public Map<String, String> qrcode(@RequestParam("qrcode") String qrcode) {
        Map<String, String> map = new HashMap<>();
        String codeToBase64 = codeService.createCodeToBase64(qrcode);

        // 将二维码状态信息存储为一个Map
        Map<String, Object> qrCodeInfo = new HashMap<>();
        qrCodeInfo.put("status", QRCODE_PENDING);
        qrCodeInfo.put("createdAt", System.currentTimeMillis());

        // 设置3分钟二维码有效期，状态为“待扫描”
        redisUtil.set(QRCODE_PREFIX + qrcode, qrCodeInfo, 3, TimeUnit.MINUTES);

        map.put("qrcode", codeToBase64);

        return map;
    }

    /**
     * 扫码
     * @param map 用户信息
     * @return
     */
    @PostMapping("/qrcode/scan")
    public ResponseEntity<?> handleScan(@RequestBody Map<String, Object> map) {

        String qrcode = (String) map.get("qrcode");
        String userId = (String) map.get("userId");

        String redisKey = QRCODE_PREFIX + qrcode;

        if (!redisUtil.hasKey(redisKey)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("二维码无效或已过期");
        }

        // 更新二维码状态信息
        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);

        qrCodeInfo.put("status", QRCODE_SCANNED);
        qrCodeInfo.put("userId", userId);
        qrCodeInfo.put("scannedAt", System.currentTimeMillis());

        // 更新状态为已扫描，过期时间为20秒
        redisUtil.set(redisKey, qrCodeInfo, 30, TimeUnit.SECONDS);

        return ResponseEntity.ok("二维码已扫描");
    }

    /**
     * 检查二维码状态
     * @param qrcode 随机字符串
     * @return
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
     * 获取用户信息
     * @param user_id
     * @return
     */
    @GetMapping("/info")
    public UserVo info(@RequestParam("user_id") String user_id) {
        return imUserService.info(user_id);
    }

    /***
     * 验证手机号码 并且发送验证码
     * @param phone 手机号
     * @return
     * @throws Exception
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
     * @param user_id
     * @return
     */
    @GetMapping("/online")
    public boolean isOnline(@RequestParam("user_id") String user_id) {
        return imUserService.isOnline(user_id);
    }


}
