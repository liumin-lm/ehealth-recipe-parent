package com.ngari.recipe.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/17
 * @description： 校验字段数据
 * @version： 1.0
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Verify {

    boolean isNotNull() default true;

    String desc() default "";

    boolean isInt() default false;

    boolean isDate() default false;

    String dateFormat() default "yyyy-MM-dd HH:mm:ss";

    /**
     * 是否为金额数据，会转成BigDecimal进行校验
     * @return
     */
    boolean isMoney() default false;

    int maxLength() default 500;

    int minLength() default 0;




}
