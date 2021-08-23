package recipe.util;

import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.util.*;


/**
 * Created by yejunjie on 2018/2/26 0026.
 */
public class ObjectCopyUtils {

    private static final Logger logger = LoggerFactory.getLogger(ObjectCopyUtils.class);

    /**
     * obj转obj
     *
     * @param origin
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T convert(Object origin, Class<T> clazz) {
        Object dest = null;
        if (null != origin) {
            try {
                dest = clazz.newInstance();
                copyProperties(dest, origin);
            } catch (InstantiationException e) {
                dest = null;
                logger.error("InstantiationException getBean error. ", e);
            } catch (IllegalAccessException e) {
                dest = null;
                logger.error("IllegalAccessException getBean error. ", e);
            }
        }

        return (null != dest) ? (T) dest : null;
    }

    /**
     * list转list
     *
     * @param srcList
     * @param targetClazz
     * @param <T>
     * @param <S>
     * @return
     */
    public static <T, S> List<T> convert(Collection<S> srcList, Class<T> targetClazz) {
        if (CollectionUtils.isEmpty(srcList)) {
            return Collections.emptyList();
        }
        T target = null;
        try {
            List<T> dist = new ArrayList<T>();
            for (S src : srcList) {
                //目标类注意必须实现空构造函数
                target = targetClazz.newInstance();
                copyProperties(target, src);
                dist.add(target);
            }
            return dist;
        } catch (Exception e) {
            logger.error("对象{}复制属性出错:{}", targetClazz.getSimpleName(), JSONUtils.toString(srcList));
            throw new IllegalArgumentException("对象" + targetClazz.getSimpleName() + "复制属性出错", e);
        }
    }

    public static void copyProperties(Object dest, Object origin) {
        if (null == origin) {
            dest = null;
            return;
        }
        try {
            BeanUtils.copyProperties(origin, dest);
        } catch (BeansException e) {
            logger.error("BeansException copyProperties error.", e);
        }
    }


    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static void copyPropertiesIgnoreNull(Object src, Object target) {
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }


}
