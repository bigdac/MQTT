package com.li.mqtt;


import java.util.Arrays;
import java.util.HashSet;

/**
 * @author 版本：1.0
 * 创建日期：2020-09-04 13
 * 描述：
 */
public class MyConfig {
    public static String HOST = "tcp://192.168.1.10:61613";//服务器地址（协议+地址+端口号）
    public static String USERNAME = "admin";//用户名
    public static String PASSWORD = "password";//密码
    public static String PUBLISH_WILL = "tourist_will";//发布遗嘱
    public static int CONNECTION_TIMEOUT = 10;//超时时间
    public static int KEEP_ALIVE_INTERVAL = 20 ;//心跳包时间
    private static String [] topics = new String[]{"A","B"};
    public static HashSet<String> ALL_TOPIC = new HashSet<>(Arrays.asList(topics));
}
