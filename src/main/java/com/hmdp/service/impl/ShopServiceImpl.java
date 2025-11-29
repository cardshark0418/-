package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
//        //从Redis查询店铺数据
//        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //判断是否存在 存在则返回 不存在则查询数据库
//        if(!StrUtil.isBlank(s)){
//            //存在
//            Shop shop = JSONUtil.toBean(s,Shop.class);
//            log.debug(shop.toString());
//            return Result.ok(shop);
//        }
//        if(s!=null){
//            return Result.fail("店铺不存在");
//        }
//        //缓存重建
//        String lockKey = LOCK_SHOP_KEY+id;
//        Shop shop = null;
//        try {
//            boolean flag = tryLock(lockKey);
//            if(!flag){
//                Thread.sleep(50);
//                return queryById(id);
//            }
//            //获取锁后再次检查缓存是否存在 做DoubleCheck 若存在则释放锁
//            String s2 = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//            if(!StrUtil.isBlank(s2)){
//                //存在
//                unLock(lockKey);
//                return Result.ok(JSONUtil.toBean(s2,Shop.class));
//            }
//            if(s2!=null){
//                return Result.fail("店铺不存在");
//            }
//            //查询数据库
//            shop = getById(id);
//            Thread.sleep(200);
//            //存在则写入Redis 并返回 不存在则返回404
//            if (shop==null) {
//                //不存在
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return Result.fail("店铺不存在");
//            }
//            //存在 写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            unLock(lockKey);
//        }
//        return Result.ok(shop);
        Shop shop =cacheClient.queryWithPathThrough(CACHE_SHOP_KEY,id,Shop.class, this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if(shop==null){
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        //操作数据库
        Long id = shop.getId();
        if(id==null){
            return  Result.fail("id不能为空");
        }
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if(x==null || y==null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = (current) * SystemConstants.DEFAULT_PAGE_SIZE;
        String key = SHOP_GEO_KEY+typeId;
        //查询附近5000米内end条商家
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        if(results==null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> geoResultList = results.getContent();
        if(geoResultList.size()<=from){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(geoResultList.size());
        Map<Long,Double> distanceMap = new HashMap<>(geoResultList.size());
        //利用stream流截取从from到end的部分 再取出id和距离
        geoResultList.stream().skip(from).forEach(result -> {
            String shopIdStr = result.getContent().getName();
            double distance = result.getDistance().getValue();
            ids.add(Long.valueOf(shopIdStr));
            distanceMap.put(Long.valueOf(shopIdStr),distance);
        });
        String idStr = StrUtil.join(",", ids);
        List<Shop> list = query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")")
                .list();
        list.forEach(shop -> {
            shop.setDistance(distanceMap.get(shop.getId()));
        });
        return Result.ok(list);
    }

//    private boolean tryLock(String lockKey){
//        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(b);
//    }
//    private void unLock(String lockKey){
//        stringRedisTemplate.delete(lockKey);
//    }
}

