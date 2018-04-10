package top.braycep.nodemcu.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Created by Braycep on 2018/4/10.
 */

public class Utils {
    private static Socket socket = null;
    private static BufferedReader br = null;
    private static PrintStream ps = null;

    private static int id;
    private static String key;
    private static int tid;

    public static boolean init() {
        String ip = "121.42.180.30";
        int port = 8282;

		/*//当前设备
		int id = 5238;
		String key = "94ea5b1c7";
		//目标设备
		int tid = 5141;*/

        try {
            socket = new Socket(ip, port);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            String line;
            if ((line = br.readLine()) != null) {
                System.out.println("Receive from Server: "+line);
                if (line.contains("WELCOME TO BIGIOT")) {
                    System.out.println("连接成功...");
                }
            }

            if (br != null && socket != null) {
                ps = new PrintStream(socket.getOutputStream());
                ps.println("{\"M\":\"checkin\",\"ID\":\""+id+"\",\"K\":\""+key+"\"}");
                System.out.println("Send to Server: {\"M\":\"checkin\",\"ID\":\""+id+"\",\"K\":\""+key+"\"}");
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: "+line);
                    if (line.contains("checkinok\",\"ID\":\"D"+id+"\"")) {
                        System.out.println("登陆成功...");
                        return true;
                    }
                }
            }else {
                System.out.println("br or socket is null");
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("连接到服务器失败...: "+e.getMessage());
        }
        return false;
    }

    public static boolean keepAlive(){
        return keepAlive(socket, ps, true);
    }

    public static boolean isOL() {
        try {
            if (ps != null && br != null) {
                ps.println("{\"M\":\"isOL\",\"ID\":\"D"+tid+"\"}");
                System.out.println("Send to Server: {\"M\":\"isOL\",\"ID\":\"D"+tid+"\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: "+line);
                    if (line.contains("\"D"+tid+"\":\"1\"")) {
                        return true;
                    }
                }
            }else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("查询失败...: "+e.getMessage());
        }
        return false;
    }

    public static boolean ledOperation(String op) {
        try {
            if (ps != null && br != null) {
                ps.println("{\"M\":\"say\",\"ID\":\"D"+tid+"\",\"C\":\""+op+"\"}");
                System.out.println("Send to Server: {\"M\":\"say\",\"ID\":\"D"+tid+"\",\"C\":\""+op+"\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: "+line);
                    if (line.toLowerCase().contains("led turn")) {
                        return true;
                    }
                }
            }else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("LED 操作异常...: "+e.getMessage());
        }
        return false;
    }

    /**
     * 设备不能正常保持在线状态就会关闭socket连接
     * @param socket
     * @param ps
     * @param alive
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
                }else {
                    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                    int threads = threadGroup.activeCount();
                    Thread[] list = new Thread[threads];
                    threadGroup.enumerate(list);
                    for(int i = 0;i < list.length;i++) {
                        if (list[i].getName().equals("alive")) {
                            list[i].interrupt();
                        }
                    }
                    return true;
                }
            }else {
                System.out.println("ps or socket is null");
            }
        } catch (Exception e) {
            System.err.println("Keep Alive Failed...: "+e.getMessage());
        }
        return false;
    }

    public static boolean disConnect() {
        try {
            if (ps != null && br != null) {
                ps.println("{\"M\":\"checkout\",\"ID\":\""+id+"\",\"K\":\""+key+"\"}");
                System.out.println("Send to Server: {\"M\":\"checkout\",\"ID\":\""+id+"\",\"K\":\""+key+"\"}");
                String line;
                if ((line = br.readLine()) != null) {
                    System.out.println("Receive from Server: "+line);
                    if (line.contains("checkout")) {
                        return keepAlive(socket, ps, false);
                    }
                }
            }else {
                System.out.println("ps or br is null");
            }
        } catch (Exception e) {
            System.err.println("关闭连接出现异常...: "+e.getMessage());
        }
        return false;
    }

    public static int getId() {
        return id;
    }

    public static void setId(int id) {
        Utils.id = id;
    }

    public static String getKey() {
        return key;
    }

    public static void setKey(String key) {
        Utils.key = key;
    }

    public static int getTid() {
        return tid;
    }

    public static void setTid(int tid) {
        Utils.tid = tid;
    }
}
