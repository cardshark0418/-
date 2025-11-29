package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

@SpringBootTest
public class HmDianPingApplicationTests {
    @Resource
    private RedisIdWorker redisIdWorker ;
//    @Resource
//    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;
    private ExecutorService executorService = Executors.newFixedThreadPool(300);
//    @Test
//    public void testIdWorker() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(300);
//        Runnable task = () ->{
//            for (int i = 0; i < 100; i++) {
//                long id = redisIdWorker.nextId("order");
//                System.out.println("id="+id);
//            }
//            latch.countDown();
//        };
//        long begin = System.currentTimeMillis();
//        for (int i = 0; i < 300; i++) {
//            executorService.submit(task);
//        }
//        latch.await();
//        long end = System.currentTimeMillis();
//        System.out.println("time"+(end-begin));
//    }
//    @Test
//    public void loadShopData(){
//        List<Shop> list = shopService.list();
//        Map<Long,List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
//
//        map.forEach((typeId,shops) -> {
//            String key = "shop:geo:"+typeId;
//            shops.forEach(shop -> {
//                stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
//            });
//        });
//    }
    @Test
    public void maxSubArray() {
        int[] nums= new int[]{-2,1};
        int maxResult=nums[0];
        for(int i=0;i<nums.length;i++){
            if(nums[i]>=0){
                break;
            }
            if(i==nums.length-1){
                System.out.println(maxResult);
            }
            if(nums[i]<0){
                maxResult=Math.max(nums[i],maxResult);
                continue;
            }

        }
        int[] maxOfSum = nums;
        for(int i=nums.length-1;i>=0;i--){
            if(nums[i]<0){
                continue;
            }
            int max=0;
            int result1=nums[i];
            int result2=nums[i];
            for(int j=i+1;j<nums.length;j++){
                result2+=maxOfSum[j];
                if(maxOfSum[j]>=0){
                    break;
                }
            }
            max=Math.max(result1,result2);
            maxOfSum[i]=max;
            maxResult=Math.max(maxOfSum[i],maxResult);
        }
        System.out.println(maxResult);
    }

}
