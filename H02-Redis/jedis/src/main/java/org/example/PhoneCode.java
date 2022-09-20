package org.example;

import redis.clients.jedis.Jedis;

import java.util.Date;

/**
 * 1、输入手机号,点击发送后随机生成6位数字码,2分钟有效
 * 2、输入验证码,点击验证,返回成功或失败
 * 3、每个手机号每天只能输入3次
 */
public class PhoneCode {
    // 正式情况下一般连接全局变量, 而是使用的连接池
    static Jedis jedis = new Jedis("192.168.253.1", 6379);
    static {
        jedis.auth("123123");
    }

    public static void main(String[] args) {
        System.out.println(sendCode("183393"));
        System.out.println(checkCode("183393", "123456"));
    }

    public static Boolean checkCode(String phone, String code) {
        return jedis.get(phone.concat(":code")) == code;
    }

    public static String sendCode(String phone) {
        // 创建两个key, 一个key用来存储验证码, 一个key用来存储次数
        String phoneCount = phone + ":conunt";
        String phoneCode = phone + ":code";

        String count = jedis.get(phoneCount);
        if (count == null) {
            jedis.setex(phoneCount, 24 * 60 * 60, "1");
        } else if (Integer.parseInt(count) < 3) {
            jedis.incr(phoneCount);
        } else {
            return "验证码超频";
        }
        jedis.setex(phoneCode, 120, String.valueOf(System.currentTimeMillis()));
        String code = jedis.get(phoneCode);
        return code;
    }
}
