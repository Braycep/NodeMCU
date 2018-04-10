package top.braycep.nodemcu.utils;

import java.io.PrintStream;

/**
 * Created by Braycep on 2018/4/10.
 */

public class KeepAlive implements Runnable{
    private PrintStream ps;
    private boolean alive = true;

    private static KeepAlive keepAlive = null;

    public void setPs(PrintStream ps) {
        this.ps = ps;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    private KeepAlive() {

    }

    private KeepAlive(PrintStream ps, boolean alive) {
        this.ps = ps;
        this.alive = alive;
    }

    public static KeepAlive GetInstance() {
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
                System.out.println("Send to Server: {\"M\":\"b\"}");
            }
        } catch (Exception e) {
            System.err.println("Keep Alive Failed...: "+e.getMessage());
        }
    }
}
