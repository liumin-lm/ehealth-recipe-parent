package com.ngari.recipe;

import ctd.util.AppContextHolder;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/4.
 */
public class RecipeAPI {

    public static <T> T getService(Class<T> clazz) {
        String className = clazz.getSimpleName();
        String serviceName = null;
        if("IHosPrescriptionService".equals(className)){
            serviceName = "hosPrescriptionService";
        }else {
            serviceName = "remote" + className.substring(1);
        }
        return AppContextHolder.getBean("eh." + serviceName, clazz);
    }
}
