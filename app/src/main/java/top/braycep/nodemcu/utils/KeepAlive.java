package top.braycep.nodemcu.utils;

import java.io.PrintStream;

/**
 * Created by Braycep on 2018/4/10.
 * Runnable的实现类
 */

public class KeepAlive implements Runnable{
    private PrintStream ps;
    private boolean alive = true;

    private static KeepAlive keepAlive = null;

    void setPs(PrintStream ps) {
        this.ps = ps;
    }

    void setAlive(boolean alive) {
        this.alive = alive;
    }

    //单例模式
    private KeepAlive() {

    }

    //单例模式
    static KeepAlive GetInstance() {
        if(keepAlive == null) {
            return new KeepAlive();
        }else {
            return keepAlive;
        }
    }

    @Override
    public void run() {
        try {
            while(alive) {
                Thread.sleep(45000);
                ps.println("{\"M\":\"b\"}");
            }
        } catch (Exception e) {
            System.err.println("Keep Alive Failed...: "+e.getMessage());
        }
    }
}
