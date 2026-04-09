package AiBot.example.AiBot.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {

	/**
	 * Configures Spring Cache to use Redis with:
	 * - a default TTL of 10 minutes
	 * - string keys (so the cached entries are easy to inspect)
	 */
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.disableCachingNullValues()
				.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				.transactionAware()
				.build();
	}
}

