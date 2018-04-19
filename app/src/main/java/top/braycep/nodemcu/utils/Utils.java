package top.braycep.nodemcu.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Created by Braycep on 2018/4/10.
 * 这个类用来控制网络数据的传输
 */

public class Utils {
    private static Socket socket = null;
    private static BufferedReader br = null;
    private static PrintStream ps = null;

    private static int id;
    private static String key;
    private static int tid;

    private static String line = null;

    /**
     * 初始化连接，并进行登陆
     *
     * @return 返回是否登陆成功
     */
    public static boolean init() {
        String ip = "121.42.180.30";
        int port = 8282;

        try {
            //开启socket通信
            socket = new Socket(ip, port);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
            ps = new PrintStream(socket.getOutputStream());
            if (br.readLine().toUpperCase().contains("WELCOME")) {
                System.out.println("连接成功！");
            }

            //登陆设备
            ps.println("{\"M\":\"checkin\",\"ID\":\"" + id + "\",\"K\":\"" + key + "\"}");
            if (br.readLine().toLowerCase().contains("checkinok")) {
                System.err.println("登录成功！");
            }
            return true;
        } catch (IOException e) {
            System.err.println("连接到服务器失败...: " + e.getMessage());
        }
        return false;
    }

    /**
     * 查询目标设备是否在线，使用tid
     *
     * @return 返回查询结果
     */
    public static boolean isOL() {
        try {
            if (!socket.isClosed()) {
                ps.println("{\"M\":\"isOL\",\"ID\":\"D" + tid + "\"}");
                return br.readLine().toUpperCase().contains("\"D" + tid + "\":\"1\"");
            } else {
                System.err.println("Socket Was Closed!");
            }
        } catch (Exception e) {
            System.err.println("查询失败...: " + e.getMessage());
        }
        return false;
    }

    /**
     * 进行LED灯的操作，开启或者关闭
     *
     * @param op 传入操作指令，如：'play','stop'
     * @return 返回执行结果
     */
    public static boolean ledOperation(String op) {
        try {
            if (!socket.isClosed()) {
                ps.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"" + op + "\"}");
                return br.readLine().toLowerCase().contains("led turn");
            } else {
                System.err.println("Socket Was Closed!");
            }
        } catch (Exception e) {
            System.err.println("LED 操作异常...: " + e.getMessage());
        }
        return false;
    }

    public static void ledOperation(final int frequency, final boolean flag) {
        try {
            if (!socket.isClosed()) {
                if (flag) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                while (true) {
                                    ps.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"play\"}");
                                    Thread.sleep(frequency);
                                    ps.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"stop\"}");
                                    Thread.sleep(frequency);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.setName("frequency");
                    thread.start();
                } else {
                    ledOperation("stop");
                    interruptThread(Thread.currentThread(), "frequency");
                }
            } else {
                System.err.println("Socket Was Closed!");
            }
        } catch (Exception e) {
            System.err.println("LED 操作异常...: " + e.getMessage());
        }
    }

    public static String getRealTimeT_H() {
        try {
            if (!socket.isClosed()) {
                ps.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"up\"}");
                line = br.readLine();
                if (line.toUpperCase().contains("T:") && line.toUpperCase().contains("H:")) {
                    return  line.substring(line.indexOf("T:"), line.indexOf("H:")).split(":")[1].split(";")[0]
                            +"--"+  line.substring(line.indexOf("H:"), line.indexOf("\",\"T\"")).split(":")[1];
                } else {
                    return "error";
                }
            } else {
                System.err.println("Socket Was Closed!");
            }
        } catch (Exception e) {
            System.err.println("温度查询操作异常...: " + e.getMessage());
        }
        return "error";
    }

    /**
     * 设备不能正常保持在线状态就会关闭socket连接
     * <p>
     * socket 与服务器通信的socket
     * ps     向服务器写数据
     *
     * @param alive 是否保持在线
     */
    public static boolean keepAlive(boolean alive) {
        try {
            if (!socket.isClosed()) {
                KeepAlive keepAlive = KeepAlive.GetInstance();
                if (alive) {
                    Thread t = new Thread(keepAlive);
                    t.setName("alive");
                    keepAlive.setAlive(true);
                    keepAlive.setPs(ps);
                    t.start();
                } else {
                    interruptThread(Thread.currentThread(), "alive");
                    return true;
                }
            } else {
                System.err.println("Socket Was Closed!");
            }
        } catch (Exception e) {
            System.err.println("Keep Alive Failed...: " + e.getMessage());
        }
        return false;
    }

    /**
     * 断开与服务器的连接
     *
     * @return 如果收到服务器确认下线数据以及成功停止线程返回true
     */
    public static boolean disConnect() {
        try {
            if (!socket.isClosed()) {
                ps.println("{\"M\":\"checkout\",\"ID\":\"" + id + "\",\"K\":\"" + key + "\"}");
                return br.readLine().toLowerCase().contains("checkout") && keepAlive(false);
            } else {
                System.out.println("Socket Was Closed!");
            }
        } catch (Exception e) {
            System.err.println("关闭连接出现异常...: " + e.getMessage());
        }
        return false;
    }

    public static void interruptThread(Thread t, String threadName) {
        //遍历线程以获得名为alive的线程，然后终止它
        ThreadGroup threadGroup = t.getThreadGroup();
        int threads = threadGroup.activeCount();
        Thread[] list = new Thread[threads];
        threadGroup.enumerate(list);
        for (int i = 0; i < list.length; i++) {
            if (list[i].getName().equals(threadName)) {
                list[i].interrupt();
            }
        }
    }

    public static boolean checkConnection(){
        return !socket.isConnected() || socket.isClosed();
    }

    public static int getId() {
        return id;
    }

    public static void setId(int id) {
        Utils.id = id;
    }

    public static void setKey(String key) {
        Utils.key = key;
    }

    public static void setTid(int tid) {
        Utils.tid = tid;
    }
}
