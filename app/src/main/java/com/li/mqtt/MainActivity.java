package com.li.mqtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.li.library.MQService;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Intent serviceIntent;
    private boolean isBound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(this,MyService.class);
        startActivity(serviceIntent);

        isBound = bindService(serviceIntent,connection, Context.BIND_AUTO_CREATE);
        myService.publish("","");
        myService.setOnMqttMessageListener(new MQService.OnMqttMessageListener() {
            @Override
            public void acceptMess(String topic, String message) {

            }
        });
    }
    MyService myService;
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyService.LocalBind localBind = (MyService.LocalBind) iBinder;
            myService = localBind.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
