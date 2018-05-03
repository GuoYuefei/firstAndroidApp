package www.tingyumian.com.test;

import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * 难点全部攻克了，功能还没完善
 */



public class MainActivity extends AppCompatActivity {

    private String ip = "10.0.2.2";
    private int port = 8080;

    private Socket sk = null;            //这个适用于发送消息的socket，在不发送信息的时候关闭状态的。需要手动打开
    private Socket saveSk = null;       //这个是用于接收的信息的Socket
    private PrintWriter pw = null;      //这个使用于发送消息的socket使用的发送的liu对象
    private BufferedReader in = null;   //这个使用于接收的socket的流对象
    private String content = "";        //接收的信息
    private String key = "";
    private String value = "";
    private String flag = "";           //解析出这是第几个检测点




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new SaveMessageThread().start();

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }

    /**
     * 判定是哪个检测点的数据
     */
    private void where(TextView[] tvs){
        switch (key){
            case "illum":
                tvs[0].setText(value+"lx");break;
            case "temperature":
                tvs[1].setText(value+"℃");break;
            case "humidity":
                tvs[2].setText(value+"%RH");break;
            case "soild":
                tvs[3].setText(value+"%RH");break;
        }
    }


    /**
     * 非主线程不能操作ui，所以需要使用Handler进行通讯。让通讯线程告诉UI线程需要改变UI界面了
     */
    private final Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 0x123){
                Log.d("debug","test handler");

                TextView tvGuang1 = (TextView) findViewById(R.id.illum11);
                TextView tvWenDu1 = (TextView) findViewById(R.id.wen11);
                TextView tvShiDu1 = (TextView) findViewById(R.id.shi11);
                TextView tvTu1 = (TextView) findViewById(R.id.tu11);

                TextView tvGuang2 = (TextView) findViewById(R.id.illum21);
                TextView tvWenDu2 = (TextView) findViewById(R.id.wen21);
                TextView tvShiDu2 = (TextView) findViewById(R.id.shi21);
                TextView tvTu2 = (TextView) findViewById(R.id.tu21);

                TextView tvGuang3 = (TextView) findViewById(R.id.illum31);
                TextView tvWenDu3 = (TextView) findViewById(R.id.wen31);
                TextView tvShiDu3 = (TextView) findViewById(R.id.shi31);
                TextView tvTu3 = (TextView) findViewById(R.id.tu31);

                TextView tv = (TextView) findViewById(R.id.textView);

                TextView[][] tvs = {{tvGuang1,tvWenDu1,tvShiDu1,tvTu1},{tvGuang2,tvWenDu2,tvShiDu2,tvTu2},{tvGuang3,tvWenDu3,tvShiDu3,tvTu3}};
                tv.setText(content);
                content = null;                 //content清空
                switch (flag){
                    case "1":
                        where(tvs[0]);break;
                    case "2":
                        where(tvs[1]);break;
                    case "3":
                        where(tvs[2]);break;
                }
            }
        }
    };

    /**
     * 解析下下位机发送来的数据
     * 约定的数据格式是这样的：
     * illum:233LX
     * temperature:112C
     * humidity:112%
     */
    private void parseInfo(){
        int i = content.lastIndexOf(":");
        int j = content.indexOf("@");
        char[] temp0 = new char[5];
        char[] temp1 = new char[16];
        char[] temp2 = new char[4];

        content.getChars(0,j,temp0,0);
        flag = String.valueOf(temp0);
        flag = flag.trim();
        content.getChars(j+1,i,temp1,0);
        key = String.valueOf(temp1);
        key = key.trim();
        content.getChars(i+1,content.length(),temp2,0);
        value = String.valueOf(temp2);
        value = value.trim();
        Log.d(key,value);
    }

    /**
     * 线程类，这个是用于接收下位机发送来数据的线程
     * 需要使用Handle消息传递
     */
    private class SaveMessageThread extends Thread {
        @Override
        public void run(){
            try{
                saveSk = new Socket(ip,port);
                Log.d("d","succ");
                saveMessage();
            }catch (Exception e){
                Log.d("smt","error");
                e.printStackTrace();
            }
        }

        /**
         * 用于接收服务器发送过来的信息,这个需要发在一个线程类里面
         */
        public void saveMessage(){
            try {
                in = new BufferedReader(new InputStreamReader(saveSk.getInputStream(), "UTF-8"));
                while (true) {
                    Log.d("saveM","succ");
                    if (saveSk.isConnected()) {
                        if (!saveSk.isInputShutdown()) {
                            if ((content = in.readLine()) != null) {
//                                content += "\n";
                                parseInfo();                    //解析信息
                                myHandler.sendEmptyMessage(0x123);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.d("er","error");
                e.printStackTrace();
            }
        }

    }

    /**
     * 这个是向服务器发送信息的函数，在此之前需要连接服务器。而且，每次调用该函数都会断开连接
     * @param info 这个是你要发送的信息
     * @throws IOException
     */
    private void acceptServer(String info) throws IOException {
//        Toast.makeText(this,"before", LENGTH_SHORT).show();
        sk.setSoTimeout(10000);
        //获取输入流，像服务器端发送信息
        OutputStream os = sk.getOutputStream();     //字节输出流
        pw = new PrintWriter(os,true);       //将输出流包装为打印流
        pw.print(info);
        pw.flush();
        sk.shutdownOutput();                //关闭输出流
        sk.close();
    }


    /**
     *
     * @param str 发送的消息
     */
    private void sendMsg(final String str){
        new Thread(){
            @Override
            public void run(){
                try {
                    sk = new Socket(ip,port);
                    acceptServer(str);
                } catch (IOException e) {
                    Log.d("debug","error come");
                    e.printStackTrace();
                }
            }
        }.start();
        Toast.makeText(this,"connecting...", LENGTH_SHORT).show();
    }

    /**
     * 以下函数都是绑定了按钮事件
     * @param view
     */
    public void connect1(View view) {
        sendMsg("A");

    }

    public void connect2(View view) {
        sendMsg("B");
    }

    public void connect3(View view) {
        sendMsg("C");
    }
}
