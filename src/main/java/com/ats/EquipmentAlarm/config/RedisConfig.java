package com.ats.EquipmentAlarm.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ats.EquipmentAlarm.Entity.ActiveequipmentalarmsviewEntity;
import com.ats.EquipmentAlarm.Entity.Resolvedequipmentalarms;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;



@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use JSON serializer instead of JDK serialization
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        template.setDefaultSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisTemplate<String, List<ActiveequipmentalarmsviewEntity>> activeAlarmRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, List<ActiveequipmentalarmsviewEntity>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, List<Resolvedequipmentalarms>> resolvedAlarmRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, List<Resolvedequipmentalarms>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}


