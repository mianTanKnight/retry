package com.lishicloud.lretry.manager;

/**
 * @author ztq
 */
public interface IManager {

    /**
     * 资源的初始化
     */
    void init() throws Exception;

    /**
     * 资源的销毁
     */
    void destroy();

}
