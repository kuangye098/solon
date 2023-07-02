package org.noear.solon.cloud.extend.mqtt.service;

import org.eclipse.paho.client.mqttv3.*;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.core.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear
 * @since 1.3
 */
class MqttCallbackImp implements MqttCallback {
    static Logger log = LoggerFactory.getLogger(MqttCallbackImp.class);

    final MqttClient client;
    final String eventChannelName;

    public MqttCallbackImp(MqttClient client , CloudProps cloudProps) {
        this.client = client;
        this.eventChannelName = cloudProps.getEventChannel();
    }

    CloudEventObserverManger observerManger;

    public void subscribe(CloudEventObserverManger observerManger) throws MqttException {
        this.observerManger = observerManger;

        String[] topicAry = observerManger.topicAll().toArray(new String[0]);
        int[] topicQos = new int[topicAry.length];
        for (int i = 0, len = topicQos.length; i < len; i++) {
            topicQos[i] = 1;
        }

        client.subscribe(topicAry, topicQos);
    }

    //在断开连接时调用
    @Override
    public void connectionLost(Throwable e) {
        EventBus.pushTry(e);
    }

    //已经预订的消息
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            Event event = new Event(topic, new String(message.getPayload()))
                    .qos(message.getQos())
                    .retained(message.isRetained())
                    .channel(eventChannelName);

            CloudEventHandler handler = observerManger.getByTopic(topic);

            if (handler != null) {
                handler.handle(event);
            } else {
                //只需要记录一下
                log.warn("There is no observer for this event topic[{}]", event.topic());
            }
        } catch (Throwable ex) {
            ex = Utils.throwableUnwrap(ex);

            EventBus.pushTry(ex);

            if (ex instanceof Exception) {
                throw (Exception) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    //发布的 QoS 1 或 QoS 2 消息的传递令牌时调用
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
