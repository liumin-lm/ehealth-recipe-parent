package recipe.util;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationUtils;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;
import org.springframework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * created by shiyuping on 2018/9/3
 */
public class RedisClient {
    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    private RedisTemplate redisTemplate;

    /**
     * instance来源仍是spring
     */
    private static RedisClient instance;

    private RedisSerializer<String> keySerializer;

    private RedisSerializer valueSerializer;

    private RedisClient() {
        instance = this;
    }

    public static RedisClient instance() {
        if (instance == null) {
            synchronized (RedisClient.class) {
                if (null == instance) {
                    instance = new RedisClient();
                }
            }
        }

        return instance;
    }


    /**
     * 用于设置hash结构数据的ttl
     */
    private Set<String> keySet = new HashSet<>();

    /**
     * 配合hset使用，用于获取java对象或者String
     */
    @SuppressWarnings("unchecked")
    public <T> T hget(final String name, final String field) {
        return (T) redisTemplate.execute(new RedisCallback<T>() {
            public T doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                byte[] value_ = connection.hGet(name_, field_);
                return (T) valueSerializer.deserialize(value_);
            }
        });
    }

    /**
     * 配合hget使用，用于存放java 对象或者String
     */
    @SuppressWarnings("unchecked")
    public <T> boolean hset(final String name, final String field, final T val) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                byte[] value_ = valueSerializer.serialize(val);
                return connection.hSet(name_, field_, value_);
            }
        });
    }

    /**
     * 配合hset使用，用于分页获取hash结构
     *
     * @param name
     * @param count   每次扫描行数，并非分页数
     * @param pattern 用于模糊匹配field,类似通配符的使用:field*
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hScan(final String name, final int count, final String pattern) {
        return (Map<String, T>) redisTemplate.execute(new RedisCallback<Map<String, T>>() {
            public Map<String, T> doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                ScanOptions.ScanOptionsBuilder builder = ScanOptions.scanOptions().count(count);
                if (StringUtils.isNotBlank(pattern)) {
                    builder.match(pattern);
                }
                Cursor<Map.Entry<byte[], byte[]>> entryCursor = connection.hScan(name_, builder.build());
                Map<String, T> result = new HashMap<>();
                while (entryCursor.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = entryCursor.next();
                    String field_ = keySerializer.deserialize(entry.getKey());
                    T value_ = (T) valueSerializer.deserialize(entry.getValue());
                    result.put(field_, value_);
                }
                return result;
            }
        });
    }

    /**
     * 配合hget使用，慎用，数量太大会挂起其他操作线程，还可能引起OOM
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hGetAll(final String name) {
        return (Map<String, T>) redisTemplate.execute(new RedisCallback<Map<String, T>>() {
            public Map<String, T> doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                Map<byte[], byte[]> map = connection.hGetAll(name_);
                Map<String, T> result = new HashMap<>();
                for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
                    result.put(
                            keySerializer.deserialize(entry.getKey()),
                            (T) valueSerializer.deserialize(entry.getValue()));
                }
                return result;
            }
        });
    }

    /**
     * 配合hincrby方法,用于获取计数值
     */
    public Long hgetNum(final String name, final String field) throws UnsupportedEncodingException {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                byte[] value_ = connection.hGet(name_, field_);
                try {
                    return Long.parseLong(new String(value_, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                return null;
            }
        });
    }

    /**
     * 配合hgetNum使用，设置计数，支持加减
     */
    public Long hincrby(final String name, final String field, final long val) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                return connection.hIncrBy(name_, field_, val);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> boolean hsetEx(final String name, final String field, final T val, final Long ttl) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                byte[] value_ = valueSerializer.serialize(val);
                if (!keySet.contains(name)) {
                    boolean flag = connection.expire(name_, ttl == null ? 7 * 24 * 3600L : ttl);
                    if (flag) {
                        keySet.add(name);
                    }
                }
                return connection.hSet(name_, field_, value_);
            }
        });
    }

    /**
     * Created by shenhj on 2017/4/22.
     * return items(Long)
     */
    public <T> Long hdel(final String name, final String field) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                return connection.hDel(name_, field_);
            }
        });
    }

    public <T> Long hlength(final String name) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                return connection.hLen(name_);
            }
        });
    }

    public <T> boolean hexists(final String name, final String field) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] name_ = keySerializer.serialize(name);
                byte[] field_ = keySerializer.serialize(field);
                if (Boolean.TRUE.equals(connection.hExists(name_, field_))) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    /**
     * @param pattern,类似key:*
     * @return
     */
    public Set<String> scan(final String pattern) {
        return scan(pattern, 10000);
    }

    /**
     * @param pattern
     * @param count   每次扫描记录数
     * @return
     */
    @SuppressWarnings("unchecked")
    private Set<String> scan(final String pattern, final int count) {
        return (Set<String>) redisTemplate.execute(new RedisCallback<Set<String>>() {
            public Set<String> doInRedis(RedisConnection connection)
                    throws DataAccessException {
                ScanOptions.ScanOptionsBuilder builder = ScanOptions.scanOptions().count(count);
                if (StringUtils.isNotBlank(pattern)) {
                    builder.match(pattern);
                }
                Cursor<byte[]> cursor = connection.scan(builder.build());
                Set<String> result = Sets.newHashSetWithExpectedSize(1 << 10);
                while (cursor.hasNext()) {
                    byte[] bytes = cursor.next();
                    String field_ = keySerializer.deserialize(bytes);
                    result.add(field_);
                }
                return result;
            }
        });
    }


    @SuppressWarnings("unchecked")
    public <T> T get(final String key) {
        return (T) redisTemplate.execute(new RedisCallback<T>() {
            public T doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = connection.get(key_);
                return (T) valueSerializer.deserialize(value_);
            }
        });
    }

    /**
     * Created by shenhj on 2017/4/22.
     * EX   seconds  -- Set the specified expire time, in seconds.
     * ttl default one week
     */
    @SuppressWarnings("unchecked")
    public <T> void set(final String key, final T val) {
        redisTemplate.execute(new RedisCallback<Void>() {
            public Void doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = valueSerializer.serialize(val);
                connection.setEx(key_, 7 * 24 * 3600L, value_);
                return null;
            }
        });
    }

    public <T> void setForever(final String key, final T val) {
        redisTemplate.execute(new RedisCallback<Void>() {
            public Void doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = valueSerializer.serialize(val);
                connection.set(key_, value_);
                return null;
            }
        });
    }

    public boolean exists(final String key) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                if (Boolean.TRUE.equals(connection.exists(key_))) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public Long del(final String key) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                return connection.del(key_);
            }
        });
    }

    /**
     * Created by shenhj on 2017/4/22.
     * EX   seconds  -- Set the specified expire time, in seconds.
     * PX   milliseconds  -- Set the specified expire time, in milliseconds.
     * NX  -- Only set the key if it does not already exist
     * XX  -- Only set the key if it already exist.
     */
    @SuppressWarnings("unchecked")
    public <T> boolean setNX(final String key, final T val) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = valueSerializer.serialize(val);
                if (Boolean.TRUE.equals(connection.setNX(key_, value_))) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
    /**
     * key不存在才设值并设值失效时间-单位秒
     * @param key
     * @param value
     * @param expire 单位秒
     * @param <T>
     * @return
     */
    public <T>boolean setIfAbsentAndExpire(final String key, final T value, final long expire) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                // set:set命令，NX:set命令操作类型为setNX，EX:有效时间单位-秒
                Object obj = connection.execute("set", keySerializer.serialize(key), valueSerializer.serialize(value),
                        SafeEncoder.encode("NX"), SafeEncoder.encode("EX"), Protocol.toByteArray(expire));
                return obj != null;
            }
        });
    }

    /**
     * setNX、expire两个操作不能保证原子性
     * @param key
     * @param val
     * @param expire
     * @param <T>
     * @return
     */
    public <T>boolean setNxAndExpire(final String key, final T val, final long expire) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = valueSerializer.serialize(val);
                if (connection.setNX(key_, value_) && connection.expire(key_, expire)) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }



    /**
     * Created by shenhj on 2017/4/22.
     * Set the specified expire time, in seconds.
     * timeout  /s
     */
    @SuppressWarnings("unchecked")
    public <T> void setEX(final String key, final Long timeout, final T val) {
        redisTemplate.execute(new RedisCallback<Void>() {
            public Void doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = valueSerializer.serialize(val);
                connection.setEx(key_, timeout, value_);
                return null;
            }
        });
    }

    /**
     * 设置超时时间
     *
     * @param key
     * @param timeout 超时时间 单位秒
     * @return
     */
    public boolean setex(final String key, final long timeout) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                if (Boolean.TRUE.equals(connection.expire(key_, timeout))) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    /**
     * 设置次数
     *
     * @param key key
     * @param num 增加的次数
     * @return 加上之后的次数
     */
    public Long incr(final String key, final long num) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                byte[] key_ = keySerializer.serialize(key);
                return connection.incrBy(key_, num);
            }
        });
    }

    public Long incr(final String key) {
        return incr(key, 1L);
    }

    /**
     * 用于存放set结构
     */
    public <T> Long sAdd(final String key, final T... values) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection) {
                byte[] key_ = keySerializer.serialize(key);
                byte[][] rawValues = serializeValues(values);
                return connection.sAdd(key_, rawValues);
            }
        });
    }

    /**
     * set结构中是否存在成员
     */
    @SuppressWarnings("unchecked")
    public <T> boolean sIsMemeber(final String key, final T val) {
        return (boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection) {
                byte[] key_ = keySerializer.serialize(key);
                byte[] value_ = valueSerializer.serialize(val);
                if (Boolean.TRUE.equals(connection.sIsMember(key_, value_))) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    /**
     * 获取全部set成员
     */
    public Set sMembers(final String key) {
        return (Set) redisTemplate.execute(new RedisCallback<Set>() {
            public Set doInRedis(RedisConnection connection) {
                byte[] key_ = keySerializer.serialize(key);
                Set<byte[]> rawValues = connection.sMembers(key_);
                return (Set) deserializeRawValues(rawValues);
            }
        });
    }

    /**
     * set结构,删除成员
     */
    public <T> Long sRemove(final String key, final T... values) {
        return (Long) redisTemplate.execute(new RedisCallback<Long>() {
            public Long doInRedis(RedisConnection connection) {
                byte[] key_ = keySerializer.serialize(key);
                byte[][] rawValues = serializeValues(values);
                return connection.sRem(key_, rawValues);
            }
        });
    }

    /**
     * redis 存储list数据
     *
     * @param key     键
     * @param list    值
     * @param timeout 过期时间
     * @param <T>
     */
    public <T> void addList(String key, List<T> list, Long timeout) {
        if (CollectionUtils.isEmpty(list) || null == key) {
            return;
        }
        redisTemplate.opsForList().rightPushAll(key, list);
        if (null != timeout) {
            setex(key, timeout);
        }
    }

    /**
     * redis 获取list数据
     *
     * @param key 键
     * @param <T>
     * @return
     */
    public <T> List<T> getList(String key) {
        //取出如果结束位是-1， 则表示取所有的值
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    @SuppressWarnings("unchecked")
    private <T> Set<T> deserializeRawValues(Set<byte[]> rawValues) {
        return SerializationUtils.deserialize(rawValues, valueSerializer);
    }

    @SuppressWarnings("unchecked")
    private byte[][] serializeValues(Object... values) {
        byte[][] rawValues = new byte[values.length][];
        int i = 0;
        Object[] objects = values;
        int length = values.length;

        for (int j = 0; j < length; ++j) {
            Object value = objects[j];
            rawValues[i++] = valueSerializer.serialize(value);
        }
        return rawValues;
    }

    public RedisSerializer<String> getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(RedisSerializer<String> keySerializer) {
        this.keySerializer = keySerializer;
    }

    public RedisSerializer getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(RedisSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
