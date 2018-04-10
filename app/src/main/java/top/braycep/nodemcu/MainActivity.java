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

public class MainActivity extends AppCompatActivity {

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

        et_id = (EditText) findViewById(R.id.et_id);
        et_key = (EditText) findViewById(R.id.et_key);
        et_tid = (EditText) findViewById(R.id.et_tid);

        tv_query = (TextView) findViewById(R.id.textView1);

        btn_login = (Button) findViewById(R.id.btn_login);
        btn_logout = (Button) findViewById(R.id.btn_logout);
        btn_query = (Button) findViewById(R.id.btn_query);

        ledSwitch = (Switch) findViewById(R.id.switch1);

        //changeViewStatus(true);
        //ledSwitch.setEnabled(false);
        btn_logout.setVisibility(View.INVISIBLE);
        et_tid.setVisibility(View.INVISIBLE);
        btn_query.setVisibility(View.INVISIBLE);
        ledSwitch.setVisibility(View.INVISIBLE);

        btn_login.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                int id = 0;
                try {
                    id = Integer.parseInt(et_id.getText().toString().trim());
                    String key = et_key.getText().toString().trim();
                    if (id != 0 && key.length() < 9) {
                        showToast("请输入有效的API Key");
                    } else {
                        Utils.setId(id);
                        Utils.setKey(key);
                        Thread startup = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                if (Utils.init()) {
                                    Utils.keepAlive();
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            btn_login.setVisibility(View.INVISIBLE);

                                            btn_logout.setVisibility(View.VISIBLE);
                                            et_tid.setVisibility(View.VISIBLE);
                                            btn_query.setVisibility(View.VISIBLE);
                                        }
                                    });
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
                        startup.start();
                    }
                } catch (Exception e) {
                    showToast("请输入有效的ID");
                }
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Thread close = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        if (myCT != null){
                            myCT.cancel();
                            myCT = null;
                        }
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

        btn_query.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    int tid = 0;
                    tid = Integer.parseInt(et_tid.getText().toString().trim());
                    if (tid != 0) {
                        Utils.setTid(tid);
                        Thread query = new Thread(new Runnable() {
                            @Override
                            public void run() {
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

    /*public void changeViewStatus(boolean flag) {
        btn_login.setEnabled(flag);
        et_id.setEnabled(flag);
        et_key.setEnabled(flag);

        btn_logout.setEnabled(!flag);
        et_tid.setEnabled(!flag);
        btn_query.setEnabled(!flag);
    }*/

    public void showToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

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
