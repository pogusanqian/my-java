package com.example.secondkill.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 双重检查单例模式:
 *      第一次校验是为了防止创建对象之后, 后面的线程在进行创建
 *      第二次校验是防止当对象没有创建时, 多个线程线程都穿透了第一层校验(这些穿透的线程都会进行创建对象)
 * 单例模式也可以通过静态代码块来创建
 */
public class RedisPoolUtil {
    private static volatile JedisPool jedisPool = null;
    public static JedisPool getJedisPoolInstance() {
        if (null == jedisPool) {
            // synchronized(object): 不同对象代表不同锁, 可以在线程外部新建对象
            synchronized (RedisPoolUtil.class) {
                if (null == jedisPool) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    // 设置最大连接数, 默认18个
                    poolConfig.setMaxTotal(200);
                    // 最小空闲连接时间: 连接的最小空闲时间, 达到此值后空闲连接移除
                    // 设置最大空闲连接数(默认为8): 当空闲连接超过了最大连接数, 就直接将多出的空闲连接删除
                    poolConfig.setMaxIdle(32);
                    // 设置等待时间
                    poolConfig.setMaxWaitMillis(100 * 1000);
                    // 连接超时后, 是否继续等待
                    poolConfig.setBlockWhenExhausted(true);
                    // 连接成功后, 使用ping命令进行测试
                    poolConfig.setTestOnBorrow(true);
                    jedisPool = new JedisPool(poolConfig, "192.168.40.1", 6379, 60000, "123123");
                }
            }
        }
        return jedisPool;
    }
}
