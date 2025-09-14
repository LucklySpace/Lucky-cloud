package com.xy.meet;

import com.xy.meet.netty.service.IMeetChatServer;
import com.xy.spring.XSpringApplication;
import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.aop.EnableAop;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.PostConstruct;

@EnableAop
@SpringApplication
public class ImMeetApplication {

    public static void main(String[] args) {

        XSpringApplication.run(ImMeetApplication.class, args);

    }

}
