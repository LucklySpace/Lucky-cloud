package com.xy.leaf;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyThread extends Thread {

    private static int allTicket = 1000;
    private static int curTicket = 0;

    private static Lock lock = new ReentrantLock();

    public MyThread(String name) {
        super(name);
    }

    @Override
    public void run() {

        while (curTicket < allTicket) {

            lock.lock();//手动上锁
            try {
                if (curTicket < allTicket) {
                    curTicket++;
                    System.out.println("窗口" + Thread.currentThread().getName() + "正在销售第" + curTicket + "张票");
                }
                if (curTicket >= allTicket) {
                    System.out.println("窗口" + Thread.currentThread().getName() + "票已经售完");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();//手动解锁
            }
        }
    }
}
