# 技术栈

- Spring Boot
- MySQL
- MyBatis Plus
- MyBatisX
- Redis
- Redisson
- Spring Scheduler
- Swagger
- Knife4j
- Gson

# 技术难点

## Knife4j 接口文档

Knife4j 主打一个简单实用，借助 swagger2 的`/v2/api-docs`获取项目的所有接口信息，再使用 vue + ant desgin vue 展示接口。

使用直接找官网文档，访问地址`http://ip:port/doc.html`**（一定不要忘记项目的全局前缀路径）**

https://doc.xiaominfo.com/

## 对象拷贝

如果业务涉及到 DTO ，很常见的问题就是对象的拷贝，可以用 spring 提供的 `BeanUtils.copyProperties(Object source, Object target)` 进行拷贝，只要属性匹配就能复制上。

## 分页

尤其是数据量大了后，**一定要分页！！！**

记得改数据库类型

```java
@Configuration
@MapperScan("scan.your.mapper.package")
public class MybatisPlusConfig {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor = false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

## 缓存

当数据量达到百万之后，即使是分页查询也会有两三秒的延迟，那如何将延迟降低到一秒内呢？

就是用缓存，将经查被查的数据存入内存级数据库 Redis（一个 key - value 存储系统）

**缓存类别：**

- Redis（分布式缓存）
- memcached（分布式）
- Etcd（云原生架构的一个分布式存储，**存储配置**，扩容能力）

------

- ehcache（单机）
- 本地缓存（Java 内存 Map）
- Caffeine（Java 内存缓存，高性能）
- Google Guava

**Redis 主要五大数据结构：**

- String 字符串类型： name: "yupi"
- List 列表：names: ["yupi", "dogyupi", "yupi"]
- Set 集合：names: ["yupi", "dogyupi"]（值不能重复）
- Hash 哈希：nameAge: { "yupi": 1, "dogyupi": 2 }
- Zset 集合：names: { yupi - 9, dogyupi - 12 }（适合做排行榜）

**Redis 客户端：**

- Spring Data
  通用的数据访问框架，定义了一组 **增删改查** 的接口
- Jedis
  独立于 Spring 操作 Redis 的 Java 客户端
  要配合 Jedis Pool 使用
- Lettuce
  **高阶** 的操作 Redis 的 Java 客户端
  异步、连接池
- Redisson
  分布式操作 Redis 的 Java 客户端，让你像在使用本地的集合一样操作 Redis（分布式 Redis 数据网格）

对比：

1. 如果你用的是 Spring，并且没有过多的定制化要求，可以用 Spring Data Redis，最方便
2. 如果你用的不是 SPring，并且追求简单，并且没有过高的性能要求，可以用 Jedis + Jedis Pool
3. 如果你的项目不是 Spring，并且追求高性能、高定制化，可以用 Lettuce，支持异步、连接池
4. 如果你的项目是分布式的，需要用到一些分布式的特性（比如分布式锁、分布式集合），推荐用 redisson

### Redis 序列化

redis 如果直接使用会有存进 redis 的 key 和 value 乱码的问题，需要配置

```java
package com.weeds.findsoul.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        // key序列化
        RedisSerializer<?> stringSerializer = new StringRedisSerializer();
        // value序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // 配置redisTemplate
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setKeySerializer(stringSerializer);// key序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);// value序列化
        redisTemplate.setHashKeySerializer(stringSerializer);// Hash key序列化
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);// Hash value序列化
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        // 配置序列化
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        RedisCacheConfiguration redisCacheConfiguration = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }
}
```

- **要设计缓存的 key ，不要与别人的冲突，一般就是全类名 + 方法名 + 参数**
- **redis 内存不能无限增加，一定要设置过期时间！！！**

## 缓存预热

上面应用于数据库查询的缓存配置在合适的位置，已经可以大大提高用户的访问速度、减轻数据库的压力，但是还有一点瑕疵：就是如果有个首当其冲的倒霉蛋充当第一个访问还没有缓存的用户，那么这个倒霉蛋的体验会非常差，如何拯救这个倒霉蛋呢？就是缓存预热！

缓存预热设置一个定时任务，定时预先将数据写入缓存，让这个定时任务充当倒霉蛋。

```java
@Component
@Slf4j
public class PreCacheTask {
    private final List<Long> vipUserIdList = Arrays.asList(1663454129109958657L, 1659908167863115777L);
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每天零点预热 RecommendUsers 的缓存
     * 预热不同用户的 RecommendUsers 缓存
     */
    @Scheduled(cron = "0 0 0 * * *") // 秒 分 时 日 月 年
    public void doCacheRecommendUsers() {
        ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
        for (Long userId : vipUserIdList) {
            String redisKey = String.format("com.weeds.findsoul.service.UserService.getRecommendUsers.%s", userId);
            Page<User> page = userService.page(new Page<>(1, 20));
            List<User> userList = page.getRecords();
            List<User> list = userList.stream()
            .map(user -> userService.getSafeUser(user))
            .collect(Collectors.toList());
            ArrayList<UserDto> userDtoList = userService.getUserDtoList(list);
            // 写缓存
            try {
                opsForValue.set(redisKey, userDtoList, 1000 * 60 * 10, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.info("redis set key error", e);
            }
        }
    }
}
```

## 分布式锁

上面是单机环境下的定时任务，如果是多机环境呢，**时间一到，百家争鸣？**

肯定是不行的，那怎么办？

1. **分离定时任务程序和主程序**，只在 1 个服务器运行定时任务。但是成本太大
2. 写死配置，每个服务器都执行定时任务，但是在执行任务前**判断只有 ip 符合配置的服务器才真实执行业务逻辑**，其他的直接返回。成本最低，但是我们的 IP 可能是不固定的，把 IP 写的太死了
3. 动态配置，配置是可以轻松的、很方便地更新的（**代码无需重启**），但是只有 ip 符合配置的服务器才真实执行业务逻辑。

- - 数据库
  - Redis
  - 配置中心（Nacos、Apollo、Spring Cloud Config）

问题：服务器多了、IP 不可控还是很麻烦，还是要人工修改

1. **分布式锁**，只有抢到锁的服务器才能执行业务逻辑。坏处：增加成本；好处：不用手动配置，多少个服务器都一样。

这么一来分布式锁的概念就出来了：**多台机器，同一时间保证只有一台机器（或某些线程）能执行**

那这个“锁”用什么实现呢？很容易想到的就是数据库，**将自己的标识存入数据库**，后来的人发现标识已存在，就抢锁失败，继续等待。等先来的人执行方法结束，把标识清空，其他的人继续抢锁。

而且数据库符合这两个特性：1、可以多台机器访问；2、数据库本身就有事务属性。

这里就**使用 Redis 实现**：内存数据库，**读写速度快**。支持 **setnx** 、lua 脚本，比较方便我们实现分布式锁。

setnx：set if not exists 如果不存在，则设置；只有设置成功才会返回 true，否则返回 false

### 流程细节

1. 用完锁要释放
2. **锁一定要加过期时间**
3. 如果锁的过期时间小于“加锁”期间方法的执行时间怎么办？会出现怎样的问题？

1. 1. 我的锁释放，别人拿到锁，我释放掉别人的锁
   2. 造成连锁效应
   3. **解决方案：续期**，在我获取锁期间，如果我还没有执行完，锁过期了，那锁就自动再把时间给续上，直到我执行完自己释放。其实完全可以写个 AOP ：

```java
boolean end = false;
new Thread(() -> {
    if (!end)
    续期
})
end = true;
```

1. 在我释放锁的时候，**一定要判断一下这个是不是我的锁**
2. 有一种情况就是在 A 判断的时候，刚判断完锁就过期了，然后第二个人见缝插针，导致我释放了第二个人的锁，还没完，我释放了第二个人的锁，还导致第三个人直接进来了！
   其实也就是说**我判断 + 释放锁必须是一个原子操作，**这就必须依赖 **Redis 的 Lua 脚本**

```java
// 原子操作
if(get lock == A) {
    // set lock B
    del lock
}
```

## Redisson

Redisson 是一个 java 操作 Redis 的客户端，**提供了大量的分布式数据集来简化对 Redis 的操作和使用，可以让开发者像使用本地集合一样使用 Redis，完全感知不到 Redis 的存在。**

在引入时最好单独引入，不要使用 springboot-start 引入，

配置类：

```java
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        // 1. Create config object
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(1);
        // 创建实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
```

实现代码：

```java
@Resource
private RedissonClient redissonClient;

// ...

RLock lock = redissonClient.getLock("lockKey");
try {
    while(true) {
        // 多机环境下只有一个机器的定时任务线程能执行
        if (lock.tryLock(0, TimeUnit.MILLISECONDS)) {
            // 需要上锁的操作
            // ...
        }
    }
} catch (InterruptedException e) {
    log.error("distributed lock error: ", e);
} finally {
    // 只能释放自己的锁
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
//1. 普通的可重入锁
RLock lock = redissonClient.getLock("generalLock");

// 拿锁失败时会不停的重试
// 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30s
lock.lock();

// 尝试拿锁10s后停止重试,返回false
// 具有Watch Dog 自动延期机制 默认续30s
boolean res1 = lock.tryLock(10, TimeUnit.SECONDS);

// 拿锁失败时会不停的重试
// 没有Watch Dog ，10s后自动释放
lock.lock(10, TimeUnit.SECONDS);

// 尝试拿锁100s后停止重试,返回false
// 没有Watch Dog ，10s后自动释放
boolean res2 = lock.tryLock(100, 10, TimeUnit.SECONDS);

//2. 公平锁 保证 Redisson 客户端线程将以其请求的顺序获得锁
RLock fairLock = redissonClient.getFairLock("fairLock");

//3. 读写锁 没错与JDK中ReentrantLock的读写锁效果一样
RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
readWriteLock.readLock().lock();
readWriteLock.writeLock().lock();
```

https://zhuanlan.zhihu.com/p/135864820

## 看门狗机制

Redisson 具体落实的“续期”方案就是：看门狗机制

https://blog.csdn.net/qq_26222859/article/details/79645203

Redisson提供了一个监控锁的看门狗，它的作用是在Redisson实例被关闭前，不断的延长锁的有效期，也就是说，如果一个拿到锁的线程一直没有完成逻辑，那么看门狗会帮助线程不断的延长锁超时时间，锁不会因为超时而被释放。（其实就是 leaseTime 是 -1）

默认情况下，看门狗的续期时间是30s，也可以通过修改Config.lockWatchdogTimeout来另行指定。

另外Redisson 还提供了可以指定leaseTime参数的加锁方法来指定加锁的时间。超过这个时间后锁便自动解开了，**不会延长锁的有效期**。

结论就是：

1. watch dog 在当前节点存活时每 10s 给分布式锁的 key 续期 30s；
2. watch dog 机制启动，且代码中没有释放锁操作时，watch dog 会不断的给锁续期；
3. 如果程序释放锁操作时因为异常没有被执行，那么锁无法被释放，所以释放锁操作一定要放到 finally 中；