package com.li.mqtt;

import android.app.Application;

/**
 * @author 版本：1.0
 * 创建日期：2020-09-04 13
 * 描述：
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MyService.Config.getConfig()
                .setHost(MyConfig.HOST)
                .setPassword(MyConfig.PASSWORD)
                .setTopics(MyConfig.ALL_TOPIC)
                .setUsername(MyConfig.USERNAME);
    }
}
