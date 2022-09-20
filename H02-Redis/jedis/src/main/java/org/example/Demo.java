package org.example;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

/**
 * 但是Jedis实例是线程不安全的,多线程环境下需要基于连接池来使用
 */
public class Demo {
    @Test
    public void Test() {
        Jedis jedis = new Jedis("192.168.253.1", 6379);
        jedis.auth("123123");
        jedis.select(0);

        System.out.println(jedis.ping());
        jedis.set("age", "23");

        System.out.println(jedis.keys("*"));
        System.out.println(jedis.get("age"));

        // 关闭连接(注意在有连接池的情况下, 是将连接归还给了连接池)
        jedis.close();
    }
}
