package com.hmdp.utils;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import static com.hmdp.utils.RedisConstants.*;


@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //set
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time, unit);
    }
    //get
    public <R,ID> R queryWithPathThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallBack,Long time,TimeUnit unit) {
        String key = keyPrefix+id;
        //从Redis查询数据
        String s = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在 存在则返回 不存在则查询数据库
        if(!StrUtil.isBlank(s)){
            //存在
            return JSONUtil.toBean(s,type);
        }
        if(s!=null){
            return null;
        }
        //缓存重建
        String lockKey = LOCK_SHOP_KEY+id;//怎么起名？todo
        R r = null;
        try {
            boolean flag = tryLock(lockKey);
            if(!flag){
                Thread.sleep(50);
                return queryWithPathThrough(keyPrefix, id, type, dbFallBack,time,unit);
            }
            //获取锁后再次检查缓存是否存在 做DoubleCheck 若存在则释放锁
            String s2 = stringRedisTemplate.opsForValue().get(key);
            if(!StrUtil.isBlank(s2)){
                //存在
                unLock(lockKey);
                return BeanUtil.toBean(s2,type);
            }
            if(s2!=null){
                return null;
            }
            //查询数据库
            r = dbFallBack.apply(id);
//            Thread.sleep(200);//模拟生产环境数据库查询时间
            //存在则写入Redis 并返回 不存在则缓存空值（防止缓存穿透）并返回空值
            if (r==null) {
                //不存在
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
                this.set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //存在 写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
            this.set(key,JSONUtil.toJsonStr(r),time,unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        return r;
    }
    private boolean tryLock(String lockKey){
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }
    private void unLock(String lockKey){
        stringRedisTemplate.delete(lockKey);
    }
}
