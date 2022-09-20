local userid = KEYS[1];
local prodid = KEYS[2];
-- ..表示拼接字符串
local proOver = prodid .. ":over";
local buyProUsers = prodid .. ":user";
if (redis.call("get", proOver) == nil) then
    return "改商品还未开始秒杀"
end
if tonumber(redis.call("sismember", buyProUsers, userid)) == 1 then
    return "用户已经购买了改商品";
end
if tonumber(redis.call("get", proOver)) <= 0 then
    return "该商品已经售完"
end
redis.call("decr", proOver);
redis.call("sadd", buyProUsers, userid);
return "秒杀商品成功";
