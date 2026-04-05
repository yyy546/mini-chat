package com.minichat.common.listener;

import com.minichat.common.constants.MqConstants;
import com.minichat.common.util.CacheClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheSyncListener {

    private final CacheClient cacheClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    durable = "false",
                    exclusive = "true",
                    autoDelete = "true"
            ),
            exchange = @Exchange(
                    name = MqConstants.CACHE_SYNC_SIMPLE_EXCHANGE,
                    type = ExchangeTypes.FANOUT
            )
    ))
    public void handleCacheSyncSimple(String key){
        cacheClient.invalidateCaffeine(key);
        log.info("收到缓存同步消息,已清除本地缓存, key: {}", key);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    durable = "false",
                    exclusive = "true",
                    autoDelete = "true"
            ),
            exchange = @Exchange(
                    name = MqConstants.CACHE_SYNC_BATCH_EXCHANGE,
                    type = ExchangeTypes.FANOUT
            )
    ))
    public void handleCacheSyncBatch(Collection<String> keys){
        cacheClient.invalidateCaffeineBatch(keys);
        log.info("收到批量缓存同步消息,已清除本地缓存, keys: {}", keys);
    }

}
