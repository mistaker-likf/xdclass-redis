package net.xdclass.xdclassredis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 小滴课堂,愿景：让技术不再难学
 *
 * @Description
 * @Author 二当家小D
 * @Remark 有问题直接联系我，源码-笔记-技术交流群
 * @Version 1.0
 **/

@Configuration
public class AppConfiguration {

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 使用Jackson2JsonRedisSerialize 替换默认序列化 序列化器
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 设置key和value的序列化规则
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);

        // 设置hashKey和hashValue的序列化规则
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 设置支持事物 需要使用redis事务时开启
        //redisTemplate.setEnableTransactionSupport(true);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    /**
     * 新的分⻚插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor =
                new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new
                PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /*
    spring cache 缓存时间及序列化配置
     */
    @Bean
    public RedisCacheManager cacheManager1Minute(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = instanceConfig(60L);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
    @Bean
    @Primary   //Primary表示不指定时的默认配置
    public RedisCacheManager cacheManager1Hour(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = instanceConfig(3600L);
        return RedisCacheManager.builder(connectionFactory)
                        .cacheDefaults(config)
                        .transactionAware()
                        .build();
    }
    @Bean
    public RedisCacheManager cacheManager1Day(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = instanceConfig(3600 * 24L);
        return RedisCacheManager.builder(connectionFactory)
                        .cacheDefaults(config)
                        .transactionAware()
                        .build();
    }
    private RedisCacheConfiguration instanceConfig(Long ttl) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new
                Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new
                JavaTimeModule());
        // 去掉各种@JsonSerialize注解的解析

        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, false);
        // 只针对⾮空的值进⾏序列化

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 将类型序列化到属性json字符串中

        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance ,

                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        return RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(ttl))
                        //.disableCachingNullValues() //禁止缓存null值
                        .serializeValuesWith(RedisSerializationContext.
                                SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
    }

    @Bean
    public KeyGenerator springCacheDefaultKeyGenerator(){
        return new KeyGenerator() {
            @Override
            public Object generate(Object o, Method method, Object... objects) {

                return o.getClass().getSimpleName() + "_" + method.getName() + "_"
                                + StringUtils.arrayToDelimitedString(objects, "_");
            }
        };
    }

}