package com.xy.leaf;

import org.junit.jupiter.api.Test;

//@SpringBootTest
class ImLeafApplicationTests {

//    @Resource
//    private IDGen redisSegmentIDGenImpl;


    @Test
    void contextLoads() {
//        Object im = redisSegmentIDGenImpl.get("im");
//        System.out.println("获取id"+ im.toString());


        MyThread t1 = new MyThread("001");
        MyThread t2 = new MyThread("002");
        MyThread t3 = new MyThread("003");

        t1.start();
        t2.start();
        t3.start();

    }

}
