package com.ngari.recipe;

import ctd.util.annotation.RpcService;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/8/1.
 * @param <T>
 */
public interface IBaseService<T> {

    /**
     * 获取对象
     * @param id ID
     * @return T
     */
    @RpcService
    T get(Object id);
}
