package com.xy.lucky.meet;

import com.xy.lucky.spring.XSpringApplication;
import com.xy.lucky.spring.annotations.SpringApplication;
import com.xy.lucky.spring.annotations.aop.EnableAop;

@EnableAop
@SpringApplication
public class ImMeetApplication {

    public static void main(String[] args) {

        XSpringApplication.run(ImMeetApplication.class, args);

    }

}
