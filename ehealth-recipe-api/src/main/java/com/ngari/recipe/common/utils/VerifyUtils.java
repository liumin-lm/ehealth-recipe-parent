package com.ngari.recipe.common.utils;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.ngari.recipe.common.anno.Verify;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/29
 * @description： 对象数据校验工具
 * @version： 1.0
 */
public class VerifyUtils {

    public static Multimap<String, String> verify(Object target) throws Exception {
        Multimap<String, String> result = ArrayListMultimap.create();
        //默认日期转换格式
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Field[] fields = target.getClass().getDeclaredFields();
        Verify verify;
        String fieldName;
        Object value;
        for (Field field : fields) {
            field.setAccessible(true);
            //判断字段是否需要校验
            verify = field.getAnnotation(Verify.class);
            if (null != verify) {
//                fieldName = field.getName()+"["+verify.desc()+"]";
                fieldName = field.getName();
                //字段不能为空校验
                value = field.get(target);
                if (verify.isNotNull() && (null == value || "".equals(value.toString()))) {
                    result.put(fieldName, "不能为空");
                } else {
                    if (null != value) {
                        //长度校验
                        if (StringUtils.length(value.toString()) > verify.maxLength()) {
                            result.put(fieldName, "长度超过限制[" + verify.maxLength() + "]");
                        }

                        if (StringUtils.length(value.toString()) < verify.minLength()) {
                            result.put(fieldName, "长度低于限制[" + verify.minLength() + "]");
                        }

                        //日期字段校验
                        if (verify.isDate()) {
                            SimpleDateFormat _df = df;
                            try {
                                //是否存在自定义格式
                                if (StringUtils.isNotEmpty(verify.dateFormat())) {
                                    _df = new SimpleDateFormat(verify.dateFormat());
                                }
                                Date date = _df.parse(value.toString());
                                System.out.println(date.toString());
                            } catch (Exception e) {
                                result.put(fieldName, "日期格式错误[" + _df.toPattern() + "]");
                            }
                        }

                        //数字校验
                        if (verify.isInt()) {
                            try {
                                Integer.parseInt(value.toString());
                            } catch (NumberFormatException e) {
                                result.put(fieldName, "必须为数字");
                            }
                        }

                        //金额校验
                        if (verify.isMoney()) {
                            try {
                                new BigDecimal(value.toString());
                            } catch (Exception e) {
                                result.put(fieldName, "必须是金额");
                            }
                        }

                    }
                }
            }
        }

        return result;
    }


    public static void main(String[] args) {
        HospitalRecipeDTO hospitalRecipeDTO = HospitalRecipeDTO.getTestObject();
        try {
            Multimap<String, String> verifyMap = VerifyUtils.verify(hospitalRecipeDTO);
            System.out.println(verifyMap.keySet().size());
            System.out.println(verifyMap.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
