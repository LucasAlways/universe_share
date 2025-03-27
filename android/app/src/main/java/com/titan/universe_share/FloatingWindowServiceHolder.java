package com.titan.universe_share;

/**
 * 持有FloatingWindowService实例的工具类，用于在不同组件间共享服务引用
 */
public class FloatingWindowServiceHolder {
    private static FloatingWindowService serviceInstance;

    /**
     * 设置服务实例
     */
    public static void setService(FloatingWindowService service) {
        serviceInstance = service;
    }

    /**
     * 获取服务实例
     */
    public static FloatingWindowService getService() {
        return serviceInstance;
    }
}