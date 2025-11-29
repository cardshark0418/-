package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.Null;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.query;
import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    //发送验证码
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合则返回错误信息
            return Result.fail("手机号错误！");
        }

        //符合则生成验证码
        String code = RandomUtil.randomNumbers(4);
        //保存验证码和手机号到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set("login:phone:"+phone,phone,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("验证码已发送：{}",code);

        return Result.ok();
    }

    //登录
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //检查手机号是否发生变动
        String phone = loginForm.getPhone();
        if(!(phone.equals(stringRedisTemplate.opsForValue().get("login:phone:"+phone)))){
            return Result.fail("手机号不一致！");
        }
        //检查验证码是否符合
        if(!(loginForm.getCode().equals(stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone)))){
            return Result.fail("验证码错误！");
        }
        //检查数据库中是否有该用户
        User user = query().eq("phone", phone).one();
        //若没有 则注册新用户 保存到数据库
        if(user == null){
             user = creatUserWithPhone(phone);
        }
        // 若有 则登录 并保存用户到Redis 并返回Token
        //随机生成TOKEN
        String token = UUID.randomUUID(false).toString();
        //将User转为hash
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        //todo!!ai!!!
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : userMap.entrySet()) {
            if (entry.getValue() != null) {
                stringMap.put(entry.getKey(), entry.getValue().toString());
            } else {
                stringMap.put(entry.getKey(), "");
            }
        }
        //todo!!ai!!!


        //存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,stringMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //返回TOKEN

        return Result.ok(token);
    }

    @Override
    public Result sign() {
        return null;
    }

    private User creatUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(8));
        save(user);
        return user;
    }
}
