package com.hmdp.utils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1763673240L;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public long nextId(String keyPrefix){
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp= nowSecond-BEGIN_TIMESTAMP;
        //生成当前年月日
        String data = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+data);
        return timeStamp << 32 | count;
    }

//    public static void main(String[] args) {
//        LocalDateTime now = LocalDateTime.of(2025, 11, 20, 21, 14);
//        System.out.println(now.toEpochSecond(ZoneOffset.UTC));
//        return;
//    }
}
