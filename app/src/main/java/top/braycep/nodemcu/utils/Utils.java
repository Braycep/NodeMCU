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
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            String line;
            if ((line = br.readLine()) != null) {
                System.out.println("Receive from Server: " + line);
                if (line.contains("WELCOME TO BIGIOT")) {
                    System.out.println("连接成功...");
                }
            }

            //登陆设备
            if (br != null && socket != null) {
                ps = new PrintStream(socket.getOutputStream());
                ps.println("{\"M\":\"checkin\",\"ID\":\"" + id + "\",\"K\":\"" + key + "\"}");
                System.out.println("Send to Server: {\"M\":\"checkin\",\"ID\":\"" + id + "\",\"K\":\"" + key + "\"}");
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: " + line);
                    if (line.contains("checkinok\",\"ID\":\"D" + id + "\"")) {
                        System.out.println("登陆成功...");
                        return true;
                    }
                }
            } else {
                System.out.println("br or socket is null");
            }
        } catch (IOException e) {
            System.err.println("连接到服务器失败...: " + e.getMessage());
        }
        return false;
    }

    /**
     * 使用Runnable的实现类KeepAlive来发送心跳包
     *
     * @return 返回是否成功开启心跳线程
     */
    public static boolean keepAlive() {
        return keepAlive(socket, ps, true);
    }

    /**
     * 查询目标设备是否在线，使用tid
     *
     * @return 返回查询结果
     */
    public static boolean isOL() {
        try {
            if (ps != null && br != null) {
                ps.println("{\"M\":\"isOL\",\"ID\":\"D" + tid + "\"}");
                System.out.println("Send to Server: {\"M\":\"isOL\",\"ID\":\"D" + tid + "\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: " + line);
                    if (line.contains("\"D" + tid + "\":\"1\"")) {
                        return true;
                    }
                }
            } else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("查询失败...: " + e.getMessage());
        }
        return false;
    }

    public static boolean isOL(int tid) {
        try {
            if (ps != null && br != null) {
                ps.println("{\"M\":\"isOL\",\"ID\":\"D" + tid + "\"}");
                System.out.println("Send to Server: {\"M\":\"isOL\",\"ID\":\"D" + tid + "\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: " + line);
                    if (line.contains("\"D" + tid + "\":\"1\"")) {
                        return true;
                    }
                }
            } else {
                System.out.println("ps or br is null");
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
            if (ps != null && br != null) {
                ps.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"" + op + "\"}");
                System.out.println("Send to Server: {\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"" + op + "\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: " + line);
                    if (line.toLowerCase().contains("led turn")) {
                        return true;
                    }
                }
            } else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("LED 操作异常...: " + e.getMessage());
        }
        return false;
    }

    public static String getRealTimeT_H() {
        String t = "";
        String h = "";
        try {
            if (ps != null && br != null) {
                ps.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"up\"}");
                System.out.println("{\"M\":\"say\",\"ID\":\"D" + tid + "\",\"C\":\"up\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: " + line);
                    if (line.toUpperCase().contains("T:") && line.toUpperCase().contains("H:")) {
                        t = line.substring(line.indexOf("T:"), line.indexOf("H:")).split(":")[1];
                        h = line.substring(line.indexOf("H:"), line.indexOf("\",\"T\"")).split(":")[1];
                        return t + "--" + h;
                    } else {
                        return null;
                    }
                }
            } else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("温度查询操作异常...: " + e.getMessage());
        }
        return null;
    }

    /**
     * 设备不能正常保持在线状态就会关闭socket连接
     *
     * @param socket 与服务器通信的socket
     * @param ps     向服务器写数据
     * @param alive  是否保持在线
     */
    public static boolean keepAlive(Socket socket, PrintStream ps, boolean alive) {
        try {
            if (socket != null && ps != null) {
                KeepAlive keepAlive = KeepAlive.GetInstance();
                if (alive) {
                    Thread t = new Thread(keepAlive);
                    t.setName("alive");
                    keepAlive.setAlive(true);
                    keepAlive.setPs(ps);
                    t.start();
                } else {
                    //遍历线程以获得名为alive的线程，然后终止它
                    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                    int threads = threadGroup.activeCount();
                    Thread[] list = new Thread[threads];
                    threadGroup.enumerate(list);
                    for (int i = 0; i < list.length; i++) {
                        if (list[i].getName().equals("alive")) {
                            list[i].interrupt();
                        }
                    }
                    return true;
                }
            } else {
                System.out.println("ps or socket is null");
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
            if (ps != null && br != null) {
                ps.println("{\"M\":\"checkout\",\"ID\":\"" + id + "\",\"K\":\"" + key + "\"}");
                System.out.println("Send to Server: {\"M\":\"checkout\",\"ID\":\"" + id + "\",\"K\":\"" + key + "\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: " + line);
                    if (line.contains("checkout")) {
                        return keepAlive(socket, ps, false);
                    }
                }
            } else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("关闭连接出现异常...: " + e.getMessage());
        }
        return false;
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
