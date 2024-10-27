package com.xy.auth.controller;


import com.xy.auth.domain.vo.UserVo;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.service.ImUserService;
import com.xy.auth.service.SmsService;
import com.xy.auth.utils.RSAUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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


    @GetMapping("/qrcode")
    public void qrcode() {

    }

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
     * @param user_id
     * @return
     */
    @GetMapping("/online")
    public boolean isOnline(@RequestParam("user_id") String user_id) {
        return imUserService.isOnline(user_id);
    }


}
