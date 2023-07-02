package org.noear.solon.cloud.service;

import org.noear.solon.cloud.CloudJobHandler;

/**
 * 云端定时任务服务
 *
 * @author noear
 * @since 1.3
 */
public interface CloudJobService {
    /**
     * 注册任务
     *
     * @param name 任务名
     * @param cron7x 计划表达式
     * @param description 描述
     * @param handler 处理器
     */
    boolean register(String name, String cron7x, String description, CloudJobHandler handler);

    /**
     * 是否已注册
     *
     * @param name 任务名
     */
    boolean isRegistered(String name);
}
