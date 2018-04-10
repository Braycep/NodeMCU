package top.braycep.nodemcu;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
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

    TextView tv_query;

    Button btn_login;
    Button btn_logout;
    Button btn_query;

    Switch ledSwitch;

    MyCT myCT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化各个组件的变量
        et_id = findViewById(R.id.et_id);
        et_key = findViewById(R.id.et_key);
        et_tid = findViewById(R.id.et_tid);

        tv_query = findViewById(R.id.textView1);

        btn_login = findViewById(R.id.btn_login);
        btn_logout = findViewById(R.id.btn_logout);
        btn_query = findViewById(R.id.btn_query);

        ledSwitch = findViewById(R.id.switch1);

        //设置在未登录情况下不能经行其他操作
        btn_logout.setVisibility(View.INVISIBLE);
        et_tid.setVisibility(View.INVISIBLE);
        btn_query.setVisibility(View.INVISIBLE);
        ledSwitch.setVisibility(View.INVISIBLE);

        //登陆事件
        btn_login.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                int id = 0;
                try {
                    //尝试获取ID，出现异常即ID不正确
                    id = Integer.parseInt(et_id.getText().toString().trim());
                    String key = et_key.getText().toString().trim();
                    //判断id和key的基本正确性
                    if (id != 0 && key.length() < 9) {
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
                                    Utils.keepAlive();
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
                        if (myCT != null){
                            myCT.cancel();
                            myCT = null;
                        }
                        //向服务器发送离线数据包，并停止心跳包的发送
                        if (Utils.disConnect()) {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    tv_query.setText("目标ID");
                                    btn_login.setVisibility(View.VISIBLE);
                                    btn_logout.setVisibility(View.INVISIBLE);
                                    btn_query.setVisibility(View.INVISIBLE);
                                    et_tid.setVisibility(View.INVISIBLE);
                                    ledSwitch.setVisibility(View.INVISIBLE);
                                }
                            });
                            Looper.prepare();
                            showToast("注销成功");
                            Looper.loop();
                        } else {
                            Looper.prepare();
                            showToast("注销失败，请稍后再试");
                            Looper.loop();
                        }
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
                    int tid = 0;
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
                                            ledSwitch.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    Looper.prepare();
                                    showToast("目标设备在线，查询间隔15s");
                                    Looper.loop();
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ledSwitch.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                    Looper.prepare();
                                    showToast("目标设备不在线，查询间隔15s");
                                    Looper.loop();
                                }
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
                Thread operate = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        String operation = "stop";
                        if (open) {
                            operation = "play";
                        }else {
                            operation = "stop";
                        }
                        if (Utils.ledOperation(operation)) {
                            Looper.prepare();
                            showToast("操作完成");
                            Looper.loop();
                        }else {
                            Looper.prepare();
                            showToast("操作失败");
                            Looper.loop();
                        }
                    }
                });
                operate.start();
            }
        });
    }

    /**
     * 调用Toast.makeToast方法
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
            tv_query.setText("目标ID");
            btn_query.setVisibility(View.VISIBLE);
        }

        @Override
        public void onTick(long arg0) {
            tv_query.setText("目标ID --"+arg0/1000+"s");
        }

    }
}
