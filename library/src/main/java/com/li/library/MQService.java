package com.li.library;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashSet;
import java.util.List;

/**
 * @author li
 * 版本：1.0
 * 创建日期：2020-09-04 10
 * 描述：
 */
public abstract class MQService extends Service {
    private final static String TAG = "MQService";
    private static MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mMqttConnectOptions;
    private static final Config mConfig = new Config();
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        String serverURI = mConfig.HOST; //服务器地址（协议+地址+端口号）
        mqttAndroidClient = new MqttAndroidClient(this, serverURI, mConfig.CLIENT_ID);
        mqttAndroidClient.setCallback(mqttCallback); //设置监听订阅消息的回调
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(mConfig.CONNECTION_TIMEOUT); //设置超时时间，单位：秒
        mMqttConnectOptions.setKeepAliveInterval(mConfig.KEEP_ALIVE_INTERVAL); //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions.setUserName(mConfig.USERNAME); //设置用户名
        mMqttConnectOptions.setPassword(mConfig.PASSWORD.toCharArray()); //设置密码

        // last will message
        boolean doConnect = true;

        String topic = mConfig.PUBLISH_WILL;
        int qos = 2;
        String message = mConfig.WILL_MESSAGE;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            try {
                mMqttConnectOptions.setWill(topic, message.getBytes(), qos, false);
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }
        if (doConnect) {
            doClientConnection();
        }
    }

    //订阅主题的回调
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            Log.i(TAG, "收到消息： " + new String(message.getPayload()));
               //收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等
             onMqttMessageListener.acceptMess(topic, message.toString());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i(TAG, "连接断开 ");
            doClientConnection();//连接断开，重连
        }
    };


    /**
     * 发布
     *
     * @param message 消息
     */
    public  void publish(String topic,String message) {
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }



    //MQTT是否连接成功的监听
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                for (String topic : mConfig.ALL_TOPIC) {
                    mqttAndroidClient.subscribe(topic, 2);//订阅主题，参数：主题、服务质量
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            Log.i(TAG, "连接失败 ");
            doClientConnection();//连接失败，重连（可关闭服务器进行模拟）
        }
    };

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "没有可用网络");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();
                }
            }, 3000);
            return false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return setOnBind();
    }

    protected abstract Binder setOnBind();

    @Override
    public void onDestroy() {
        try {
            mqttAndroidClient.disconnect(); //断开连接
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void setOnMqttMessageListener(OnMqttMessageListener onMqttMessageListener){
        this.onMqttMessageListener = onMqttMessageListener;
    }
    private OnMqttMessageListener onMqttMessageListener;

    public interface OnMqttMessageListener{
        void acceptMess(String topic,String message);
    }

    public static final class Config {
        public String HOST = "tcp://192.168.1.10:61613";//服务器地址（协议+地址+端口号）
        public String USERNAME = "admin";//用户名
        public String PASSWORD = "password";//密码
        public String PUBLISH_WILL = "tourist_will";//发布遗嘱
        public int CONNECTION_TIMEOUT = 10;//超时时间
        public int KEEP_ALIVE_INTERVAL = 20 ;//心跳包时间
        public String CLIENT_ID =  getUUID(mContext);

        public String WILL_MESSAGE =   "{\"terminal_uid\":\"" + CLIENT_ID + "\"}";;

        public HashSet<String> ALL_TOPIC = new HashSet<>();//全部主题



        public static Config getConfig() {
            return mConfig;
        }

        public Config setCLIENT_ID(String CLIENT_ID) {
            this.CLIENT_ID = CLIENT_ID;
            return this;
        }

        public Config setWILL_MESSAGE(String WILL_MESSAGE) {
            this.WILL_MESSAGE = WILL_MESSAGE;
            return this;
        }

        public Config setPublishWill(String publishWill) {
            this.PUBLISH_WILL = publishWill;
            return this;
        }

        public Config setHost(String host) {
            this.HOST = host;
            return this;
        }

        public Config setUsername(String username) {
            this.USERNAME = username;
            return this;
        }

        public Config setPassword(String password) {
            this.PASSWORD = password;
            return this;
        }

        public Config setTopic(String topic) {
            this.ALL_TOPIC.add(topic);
            return this;
        }

        public Config setTopics(HashSet<String> topics) {
            this.ALL_TOPIC.clear();
            this.ALL_TOPIC.addAll(topics);
            return this;
        }

        public Config removeTopic(String topic) {
            this.ALL_TOPIC.remove(topic);
            try {
                if (mqttAndroidClient!=null){
                    mqttAndroidClient.unsubscribe(topic);
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return this;
        }

        public Config removeAllTopic() {
            try {
                if (mqttAndroidClient!=null){
                    for (String topic : ALL_TOPIC) {
                        mqttAndroidClient.unsubscribe(topic);
                    }
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
            this.ALL_TOPIC.clear();
            return this;
        }

        private  String getUUID(Context context){
            SharedPreferences preference = context.getSharedPreferences("id",MODE_PRIVATE) ;
            String identity = preference.getString("identity", null);
            if (identity == null) {
                identity = java.util.UUID.randomUUID().toString();
                preference.edit().putString("identity", identity).commit();
            }
            return identity;
        }

    }
}
