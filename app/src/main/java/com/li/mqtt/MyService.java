package com.li.mqtt;

import android.os.Binder;
import android.util.Log;

import com.li.library.MQService;

/**
 * @author 版本：1.0
 * 创建日期：2020-09-04 13
 * 描述：
 */
public class MyService extends MQService {

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    protected Binder setOnBind() {
        return new LocalBind();
    }
    public class LocalBind extends Binder {
        public MyService getService() {

            return MyService.this;
        }
    }

}
