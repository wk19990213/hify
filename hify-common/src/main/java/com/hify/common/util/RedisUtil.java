package com.hify.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get failed, key={}", key, e);
            return null;
        }
    }

    /**
     * 设置值
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 设置值（带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间（秒）
     * @return 是否成功
     */
    public boolean set(String key, Object value, long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Redis set failed, key={}, timeout={}", key, timeout, e);
            return false;
        }
    }

    /**
     * 设置值（带过期时间和时间单位）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    public boolean set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("Redis set failed, key={}, timeout={}, unit={}", key, timeout, unit, e);
            return false;
        }
    }

    /**
     * 删除键
     *
     * @param key 键
     * @return 是否成功
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Redis delete failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 批量删除
     *
     * @param keys 键集合
     * @return 删除数量
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis delete failed, keys={}", keys, e);
            return 0;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间（秒）
     * @return 是否成功
     */
    public boolean expire(String key, long timeout) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("Redis expire failed, key={}, timeout={}", key, timeout, e);
            return false;
        }
    }

    /**
     * 设置过期时间（指定时间单位）
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            log.error("Redis expire failed, key={}, timeout={}, unit={}", key, timeout, unit, e);
            return false;
        }
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间（秒），-1 表示永不过期，-2 表示已过期或不存在
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -2;
        } catch (Exception e) {
            log.error("Redis getExpire failed, key={}", key, e);
            return -2;
        }
    }

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis hasKey failed, key={}", key, e);
            return false;
        }
    }
}
