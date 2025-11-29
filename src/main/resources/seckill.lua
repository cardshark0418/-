local voucherId = ARGV[1]
local userId = ARGV[2]
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId
if(tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end

-- 2. 判断用户是否重复下单
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 存在，说明是重复下单，返回2
    return 2
end

-- 3. 扣减库存
redis.call('incrby', stockKey, -1)

-- 4. 记录用户订单
redis.call('sadd', orderKey, userId)

-- 成功，返回0
return 0