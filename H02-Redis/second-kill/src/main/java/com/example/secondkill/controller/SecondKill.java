package com.example.secondkill.controller;

import com.example.secondkill.util.RedisPoolUtil;
import com.example.secondkill.util.RedisPoolUtil2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 超卖现象: 当商品只剩下1件时, 此时有多个线程走到了秒杀过程, 可以使用乐观锁解决
 * 库存剩余现象: 当多个线程同时抢一个版本号时, 只有一个能成功, 剩下的全是失败
 */

@Controller
@ResponseBody
public class SecondKill {
    static String sha1;

    static {
        try {
            Jedis jedis = RedisPoolUtil2.getJedis();
            ClassPathResource classPathResource = new ClassPathResource("secondkill.lua");
            InputStream inputStream = classPathResource.getInputStream();
            sha1 = jedis.scriptLoad(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
            jedis.close();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/secondKill")
    public String secondKill(@RequestParam String userId, String productId) {
        return doSecKill3(userId, productId);
    }

    /**
     * 直接秒杀, 会出现超卖现象
     *
     * @param userId
     * @param productId
     * @return
     */
    public String doSecKill1(String userId, String productId) {
        JedisPool jedisPoolInstance = RedisPoolUtil.getJedisPoolInstance();
        Jedis jedis = jedisPoolInstance.getResource();
        String proOver = productId + ":over";
        String buyProUsers = productId + ":user";

        // 进行校验
        String kc = jedis.get(proOver);
        if (kc == null) {
            jedis.close();
            return String.format("商品%s秒杀还没有开始, 请等待!", productId);
        }
        if (jedis.sismember(buyProUsers, userId)) {
            jedis.close();
            return String.format("用户%s已经秒杀了%s商品, 不能重复秒杀", userId, productId);
        }
        if (Integer.parseInt(kc) <= 0) {
            jedis.close();
            return String.format("商品%s已经售完, 秒杀结束", productId);
        }

        // 进行秒杀
        jedis.decr(proOver);
        jedis.sadd(buyProUsers, userId);

        jedis.close();
        return String.format("用户%s秒杀商品%s成功", userId, productId);
    }

    /**
     * 使用乐观锁, 会出现库存剩余现象
     *
     * @param userId
     * @param productId
     * @return
     */
    public String doSecKill2(String userId, String productId) {
        // 获取连接
        Jedis jedis = RedisPoolUtil2.getJedis();
        // 拼接商品库存数量和购买商品的用户(用户是一个Set集合)
        String proOver = productId + ":over";
        String buyProUsers = productId + ":user";

        // 注意这里的监听范围
        jedis.watch(proOver);
        String kc = jedis.get(proOver);
        if (kc == null) {
            jedis.close();
            return String.format("商品%s秒杀还没有开始, 请等待!", productId);
        }
        if (jedis.sismember(buyProUsers, userId)) {
            jedis.close();
            return String.format("用户%s已经秒杀了%s商品, 不能重复秒杀", userId, productId);
        }
        if (Integer.parseInt(kc) <= 0) {
            jedis.close();
            // return String.format("商品%s已经售完, 秒杀结束", productId);
            return this.secondKill(userId, productId);
        }

        // 使用乐观锁进行秒杀
        Transaction multi = jedis.multi();
        multi.decr(proOver);
        multi.sadd(buyProUsers, userId);
        List<Object> results = multi.exec();
        // 直接返回"秒杀失败"会出现大批量少买现象
        // TODO 递归调用, 会严重影响效率, 而且并不能完全解决掉库存剩余现象, 只是尽量多买了一点
        if (results == null || results.size() == 0) {
            jedis.close();
            return "秒杀失败";
        }

        // 直接秒杀(会出现超卖现象)
        // jedis.decr(proOver);
        // jedis.sadd(buyProUsers, userId);

        jedis.close();
        return String.format("用户%s秒杀商品%s成功", userId, productId);
    }

    /**
     * 使用lua脚本串讲
     *
     * @param userId
     * @param productId
     * @return
     */
    public static String doSecKill3(String userId, String productId) {
        Jedis jedis = RedisPoolUtil2.getJedis();
        Object result = jedis.evalsha(sha1, 2, userId, productId);
        jedis.close();
        return String.valueOf(result);
    }
}
