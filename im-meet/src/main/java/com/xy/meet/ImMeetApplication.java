package com.xy.meet;

import com.xy.spring.XSpringApplication;
import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.aop.EnableAop;

@EnableAop
@SpringApplication
public class ImMeetApplication {

    public static void main(String[] args) {

        XSpringApplication.run(ImMeetApplication.class, args);

    }

}
