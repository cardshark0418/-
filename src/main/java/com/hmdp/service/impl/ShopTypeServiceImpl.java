package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        //从Redis中查询店铺类型
        String listStr = stringRedisTemplate.opsForValue().get("shop:list");
        //若有 则返回
        if(!StrUtil.isBlank(listStr)){
            List<ShopType> list = JSONUtil.toList(listStr,ShopType.class);
            return Result.ok(list);
        }
        //没有 则查询数据库
        List<ShopType> list =query().orderByAsc("sort").list();
        //若数据库没有 则返回错误信息
        if(list.isEmpty()){
            return Result.fail("未找到店铺类型信息");
        }
        //若有 则保存到Redis 并返回
        stringRedisTemplate.opsForValue().set("shop:list",JSONUtil.toJsonStr(list));
        return Result.ok(list);
    }
}
