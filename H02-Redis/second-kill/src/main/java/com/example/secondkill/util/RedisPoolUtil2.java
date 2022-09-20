package com.example.secondkill.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

// 使用静态代码块创建连接池
public class RedisPoolUtil2 {
    private static final JedisPool jedisPool;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(200);
        poolConfig.setMaxIdle(32);
        poolConfig.setMaxWaitMillis(100 * 1000);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(poolConfig, "192.168.40.1", 6379, 60000, "123123");
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
}
