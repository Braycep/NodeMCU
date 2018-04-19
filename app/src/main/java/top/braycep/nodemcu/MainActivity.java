package top.braycep.nodemcu;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import top.braycep.nodemcu.utils.Utils;

/**
 * 安卓主活动
 */
public class MainActivity extends AppCompatActivity {

    /**
     * 定义全局变量，便于在各个事件中访问
     */
    EditText et_id;
    EditText et_key;
    EditText et_tid;

    TextView tv_time;
    TextView tv_t;
    TextView tv_h;

    Button btn_login;
    Button btn_logout;
    Button btn_query;

    Switch ledSwitch;
    Switch thSwitch;

    MyCT myCT;
    String t = "";
    String h = "";
    String tag;
    int id = 0;

    Thread th = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化各个组件的变量
        et_id = findViewById(R.id.et_id);
        et_key = findViewById(R.id.et_key);
        et_tid = findViewById(R.id.et_tid);

        tv_time = findViewById(R.id.tv_time);
        tv_t = findViewById(R.id.tv_t);
        tv_h = findViewById(R.id.tv_h);

        btn_login = findViewById(R.id.btn_login);
        btn_logout = findViewById(R.id.btn_logout);
        btn_query = findViewById(R.id.btn_query);

        ledSwitch = findViewById(R.id.switch1);
        thSwitch = findViewById(R.id.switch2);

        //设置在未登录情况下不能经行其他操作
        btn_logout.setVisibility(View.INVISIBLE);
        et_tid.setVisibility(View.INVISIBLE);
        btn_query.setVisibility(View.INVISIBLE);
        ledSwitch.setVisibility(View.INVISIBLE);
        thSwitch.setVisibility(View.INVISIBLE);

        //登陆事件
        btn_login.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    //尝试获取ID，出现异常即ID不正确
                    id = Integer.parseInt(et_id.getText().toString().trim());
                    String key = et_key.getText().toString().trim();
                    //判断id和key的基本正确性
                    if (id <= 0 && key.length() < 6) {
                        showToast("请输入有效的API Key");
                    } else {
                        //将ID和Key设置到Utils中，以便Utils中的方法能够调用
                        Utils.setId(id);
                        Utils.setKey(key);
                        //安卓UI线程不允许用来进行网络访问，可能导致阻塞UI的响应，新建线程来开启socket连接
                        Thread startup = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                //Utils.init()返回登陆成功或者失败的结果
                                if (Utils.init()) {
                                    //调用Utils中的方法来发送心跳包，使当前App在线
                                    Utils.keepAlive(true);
                                    //UI 的操作只能在主线程中，所以使用runOnUiThread来控制UI更新
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            //以藏登陆按钮，并启用其它按钮
                                            btn_login.setVisibility(View.INVISIBLE);
                                            btn_logout.setVisibility(View.VISIBLE);
                                            et_tid.setVisibility(View.VISIBLE);
                                            btn_query.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    //Toast不能在其他线程中创建，因为showToast方法参数是MainActivity.this，但可以使用Looper来控制
                                    Looper.prepare();
                                    showToast("登陆成功");
                                    Looper.loop();
                                } else {
                                    Looper.prepare();
                                    showToast("登陆失败");
                                    Looper.loop();
                                }
                            }
                        });
                        //开启线程
                        startup.start();
                    }
                } catch (Exception e) {
                    showToast("请输入有效的ID");
                }
            }
        });

        //注销事件
        btn_logout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Thread close = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        //注销前判断查询的倒计时是否结束，否则结束倒计时
                        if (myCT != null) {
                            myCT.cancel();
                            myCT = null;
                        }

                        try {
                            if (th != null && !th.isInterrupted()) {
                                th.interrupt();
                                Thread.interrupted();
                                th = null;
                            }

                            //向服务器发送离线数据包，并停止心跳包的发送
                            Utils.disConnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                tv_time.setText(R.string.def_t_h);
                                btn_login.setVisibility(View.VISIBLE);
                                btn_logout.setVisibility(View.INVISIBLE);
                                btn_query.setVisibility(View.INVISIBLE);
                                et_tid.setVisibility(View.INVISIBLE);
                                ledSwitch.setVisibility(View.INVISIBLE);
                                thSwitch.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });
                close.start();
            }
        });

        //查询事件
        btn_query.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //尝试获取目标设备的ID
                try {
                    int tid;
                    tid = Integer.parseInt(et_tid.getText().toString().trim());
                    if (tid != 0) {
                        Utils.setTid(tid);
                        Thread query = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //判断是否在线
                                if (Utils.isOL()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            thSwitch.setVisibility(View.VISIBLE);
                                            ledSwitch.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    tag = "在线，查询间隔15s";
                                } else {
                                    tag = "不在线，查询间隔15s";
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            thSwitch.setVisibility(View.INVISIBLE);
                                            ledSwitch.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                }
                                Looper.prepare();
                                showToast(tag);
                                Looper.loop();
                            }
                        });
                        query.start();
                        //BigIOT给出的文档表示查询设备是否在线有时间间隔，最少10s，这里使用倒计时来控制查询频率
                        btn_query.setVisibility(View.INVISIBLE);
                        myCT = new MyCT(15000, 1000);
                        myCT.start();
                    }
                } catch (Exception e) {
                    showToast("目标ID无效");
                    System.out.println(e.getMessage());
                }
            }
        });

        //开关事件，当设备在线时才可以控制开关，并且，查询的ID会更新到Utils中，也就是查询其他设备，tid就变成其他设备的id
        ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean open) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        String operation;
                        if (open) {
                            operation = "play";
                        } else {
                            operation = "stop";
                        }
                        tag = Utils.ledOperation(operation) ? "操作完成" : "操作失败";
                        Looper.prepare();
                        showToast(tag);
                        Looper.loop();
                    }
                }).start();
                System.gc();
            }
        });

        thSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean open) {
                Thread operate = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        if (open) {
                            th = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    int temperature;
                                    boolean b = false;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ledSwitch.setEnabled(false);
                                        }
                                    });
                                    while (true) {
                                        String str = Utils.getRealTimeT_H();
                                        if (str.equals("error")) {
                                            /*Looper.prepare();
                                            showToast("未能获取到温度信息，请检测线路连接！");
                                            Looper.loop();*/
                                            break;
                                        } else {
                                            t = str.split("--")[0];
                                            h = str.split("--")[1];
                                            temperature = Integer.parseInt(t);
                                            if (temperature >= 30) {
                                                b = true;
                                                Utils.ledOperation(1000, true);
                                            } else if (b) {
                                                Utils.ledOperation(1000, false);
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    tv_t.setText(t);
                                                    tv_h.setText(h);
                                                }
                                            });
                                        }
                                        try {
                                            th.sleep(5000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    System.err.println("已经终止循环");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            thSwitch.setChecked(false);
                                            tv_t.setText(R.string.def_t_h);
                                            tv_h.setText(R.string.def_t_h);
                                            ledSwitch.setEnabled(true);
                                        }
                                    });
                                }
                            });
                            th.start();
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_t.setText(R.string.def_t_h);
                                    tv_h.setText(R.string.def_t_h);
                                    ledSwitch.setChecked(false);
                                    ledSwitch.setEnabled(true);
                                }
                            });
                            if (th != null && !th.isInterrupted()) {
                                th.interrupt();
                                Thread.interrupted();
                                th = null;
                            }
                            //关闭LED闪烁线程
                            Utils.ledOperation(1000, false);
                        }
                    }
                });
                operate.start();
            }
        });
    }

    /**
     * 调用Toast.makeToast方法
     *
     * @param text 传入需要显示的字符串
     */
    public void showToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 继承倒计时类，以控制查询频率
     */
    class MyCT extends CountDownTimer {

        public MyCT(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            tv_time.setText("00");
            btn_query.setVisibility(View.VISIBLE);
        }

        @Override
        public void onTick(long arg0) {
            tv_time.setText("" + arg0 / 1000);
        }

    }
}
