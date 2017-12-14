package com.ngari.recipe;

import ctd.util.AppContextHolder;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/4.
 */
public class RecipeAPI {

    public static <T> T getService(Class<T> clazz) {
        String serviceName = "remote" + clazz.getSimpleName().substring(1);
        return AppContextHolder.getBean("eh." + serviceName, clazz);
    }
}
