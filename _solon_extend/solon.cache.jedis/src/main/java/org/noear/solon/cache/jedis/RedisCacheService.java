package org.noear.solon.cache.jedis;

import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.data.cache.Serializer;

import java.util.Properties;

/**
 * Redis 封装的缓存服务
 *
 * @author noear
 * @since 1.3
 */
public class RedisCacheService implements CacheService {
    protected String _cacheKeyHead;
    protected int _defaultSeconds;
    private Serializer<String> _serializer = null;

    protected final RedisClient client;

    public RedisCacheService serializer(Serializer<String> serializer) {
        if (serializer != null) {
            this._serializer = serializer;
        }

        return this;
    }

    public RedisCacheService(RedisClient client, String keyHeader, int defSeconds){
        this.client = client;

        _cacheKeyHead = keyHeader;
        _defaultSeconds = defSeconds;

        if (_defaultSeconds < 1) {
            _defaultSeconds = 30;
        }

        _serializer = JavabinSerializer.instance;
    }

    public RedisCacheService(Properties prop) {
        this(prop, prop.getProperty("keyHeader"), 0);
    }

    public RedisCacheService(Properties prop, String keyHeader, int defSeconds) {
        String defSeconds_str = prop.getProperty("defSeconds");
        String db_str = prop.getProperty("db");
        String maxTotal_str = prop.getProperty("maxTotal");

        if (defSeconds == 0) {
            if (Utils.isNotEmpty(defSeconds_str)) {
                defSeconds = Integer.parseInt(defSeconds_str);
            }
        }

        int db = 0;
        int maxTotal = 200;

        if (Utils.isNotEmpty(db_str)) {
            db = Integer.parseInt(db_str);
        }

        if (Utils.isNotEmpty(maxTotal_str)) {
            maxTotal = Integer.parseInt(maxTotal_str);
        }

        if (Utils.isEmpty(keyHeader)) {
            keyHeader = Solon.cfg().appName();
        }

        _cacheKeyHead = keyHeader;
        _defaultSeconds = defSeconds;

        if (_defaultSeconds < 1) {
            _defaultSeconds = 30;
        }

        if (Utils.isEmpty(_cacheKeyHead)) {
            _cacheKeyHead = Solon.cfg().appName();
        }

        _serializer = JavabinSerializer.instance;

        client = new RedisClient(prop, db, maxTotal);
    }

    /**
     * 获取 RedisClient
     */
    public RedisClient client() {
        return client;
    }

    @Override
    public void store(String key, Object obj, int seconds) {
        if (obj == null) {
            return;
        }

        String newKey = newKey(key);

        try {
            String val = _serializer.serialize(obj);

            if (seconds > 0) {
                client.open((ru) -> ru.key(newKey).expire(seconds).set(val));
            } else {
                client.open((ru) -> ru.key(newKey).expire(_defaultSeconds).set(val));
            }
        } catch (Exception e) {
            EventBus.pushTry(e);
        }
    }

    @Override
    public Object get(String key) {
        String newKey = newKey(key);
        String val = client.openAndGet((ru) -> ru.key(newKey).get());

        if (val == null) {
            return null;
        }

        try {
            return _serializer.deserialize(val);
        } catch (Exception e) {
            EventBus.pushTry(e);
            return null;
        }
    }

    @Override
    public void remove(String key) {
        String newKey = newKey(key);

        client.open((ru) -> {
            ru.key(newKey).delete();
        });
    }


    protected String newKey(String key) {
        return _cacheKeyHead + ":" + Utils.md5(key);
    }
}
