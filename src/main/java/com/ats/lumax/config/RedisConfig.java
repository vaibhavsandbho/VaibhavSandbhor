package com.ats.lumax.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {


	    @Bean
	    public RedisTemplate<String, List<ActiveequipmentalarmsviewEntity>> activeAlarmsRedisTemplate(RedisConnectionFactory connectionFactory) {
	        RedisTemplate<String, List<ActiveequipmentalarmsviewEntity>> template = new RedisTemplate<>();
	        template.setConnectionFactory(connectionFactory);
	        template.setKeySerializer(new StringRedisSerializer());
	        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
	        return template;
	    }

	    @Bean
	    public RedisTemplate<String, List<Resolvedequipmentalarms>> resolvedAlarmsRedisTemplate(RedisConnectionFactory connectionFactory) {
	        RedisTemplate<String, List<Resolvedequipmentalarms>> template = new RedisTemplate<>();
	        template.setConnectionFactory(connectionFactory);
	        template.setKeySerializer(new StringRedisSerializer());
	        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
	        return template;
	    }
}


