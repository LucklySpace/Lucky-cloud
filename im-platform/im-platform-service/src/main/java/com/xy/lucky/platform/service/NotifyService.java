package com.xy.lucky.platform.service;

import com.xy.lucky.platform.config.EmailProperties;
import com.xy.lucky.platform.config.SmsProperties;
import com.xy.lucky.platform.domain.enums.NotifyStatus;
import com.xy.lucky.platform.domain.enums.NotifyType;
import com.xy.lucky.platform.domain.po.NotifyRecordPo;
import com.xy.lucky.platform.domain.vo.EmailVo;
import com.xy.lucky.platform.domain.vo.SmsVo;
import com.xy.lucky.platform.exception.NotifyException;
import com.xy.lucky.platform.repository.NotifyRecordRepository;
import com.xy.lucky.platform.service.notify.EmailSender;
import com.xy.lucky.platform.service.notify.SmsSender;
import com.xy.lucky.platform.service.notify.provider.SmtpEmailSender;
import com.xy.lucky.platform.service.notify.provider.ZhenziSmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 通知服务
 */
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final EmailProperties emailProperties;
    private final SmsProperties smsProperties;
    private final NotifyRecordRepository notifyRecordRepository;
    private EmailSender emailSender;
    private SmsSender smsSender;

    /**
     * 发送邮件
     *
     * @param email 邮件参数
     * @return 邮件发送结果
     */
    public Boolean sendEmail(EmailVo email) {
        if (!emailProperties.isEnabled()) {
            throw new NotifyException("邮件服务未启用");
        }
        if (!StringUtils.hasText(emailProperties.getDefaultProvider())) {
            throw new NotifyException("邮件供应商未配置");
        }
        emailSender = resolveEmailSender();
        Boolean result = emailSender.send(email);
        saveRecord(NotifyType.EMAIL,
                emailProperties.getDefaultProvider(),
                email.getTo(),
                email.getSubject(),
                email.getContent(),
                result ? NotifyStatus.SUCCESS : NotifyStatus.FAILED,
                "");
        return result;
    }

    /**
     * 发送短信
     *
     * @param sms 短信参数
     * @return 短信发送结果
     */
    public Boolean sendSms(SmsVo sms) {
        if (!smsProperties.isEnabled()) {
            throw new NotifyException("短信服务未启用");
        }
        if (!StringUtils.hasText(smsProperties.getDefaultProvider())) {
            throw new NotifyException("短信供应商未配置");
        }
        smsSender = resolveSmsSender();
        Boolean result = smsSender.send(sms);

        String content = sms.getTemplateParams() != null ? sms.getTemplateParams().toString() : null;
        saveRecord(NotifyType.SMS,
                smsProperties.getDefaultProvider(),
                sms.getPhone(),
                sms.getTemplateId(),
                content,
                result ? NotifyStatus.SUCCESS : NotifyStatus.FAILED,
                "");
        return result;
    }

    /**
     * 根据配置选择邮件供应商
     *
     * @return 邮件供应商
     */
    private EmailSender resolveEmailSender() {
        String provider = emailProperties.getDefaultProvider();
        if ("smtp".equalsIgnoreCase(provider)) {
            return emailSender == null ? new SmtpEmailSender(emailProperties.getSmtp()) : emailSender;
        }
        throw new NotifyException("不支持的邮件供应商: " + provider);
    }

    /**
     * 根据配置选择短信供应商
     *
     * @return 短信供应商
     */
    private SmsSender resolveSmsSender() {
        String provider = smsProperties.getDefaultProvider();
        if ("zhenzi".equalsIgnoreCase(provider)) {
            return smsSender == null ? new ZhenziSmsSender(smsProperties.getZhenzi()) : smsSender;
        }
        throw new NotifyException("不支持的短信供应商: " + provider);
    }

    /**
     * 保存通知记录
     *
     * @param type     通知类型
     * @param provider 供应商
     * @param target   目标
     * @param title    标题
     * @param content  内容
     * @param status   状态
     * @param error    错误信息
     */
    private void saveRecord(NotifyType type,
                            String provider,
                            String target,
                            String title,
                            String content,
                            NotifyStatus status,
                            String error) {
        NotifyRecordPo po = NotifyRecordPo.builder()
                .type(type.name())
                .provider(provider)
                .target(target)
                .title(title)
                .content(content)
                .status(status.code())
                .error(error)
                .build();
        if (notifyRecordRepository != null) {
            notifyRecordRepository.save(po);
        }
    }

}
