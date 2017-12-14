package recipe.serviceprovider;


import com.ngari.recipe.IBaseService;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;

import java.util.ArrayList;
import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/8/2.
 */
public class BaseService<T> implements IBaseService<T> {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(BaseService.class);

    @Override
    public T get(Object id) {
//        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
//        Class entityClass = (Class<T>) pt.getActualTypeArguments()[0];
//        System.out.println(entityClass.getSimpleName());

        return null;
    }

    /**
     * 复制对象
     *
     * @param dest
     * @param origin
     */
    public void copyProperties(Object dest, Object origin) {
        if (null == origin) {
            dest = null;
            return;
        }
        try {
            BeanUtils.copyProperties(origin, dest);
        } catch (BeansException e) {
            logger.error("BeansException copyProperties error. statck={}", JSONUtils.toString(e.getStackTrace()));
        }
    }

    /**
     * 获取业务对象bean
     *
     * @param origin
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getBean(Object origin, Class<T> clazz) {
        Object dest = null;
        if (null != origin) {
            try {
                dest = clazz.newInstance();
                copyProperties(dest, origin);
            } catch (InstantiationException e) {
                dest = null;
                logger.error("InstantiationException getBean error. statck={}", JSONUtils.toString(e.getStackTrace()));
            } catch (IllegalAccessException e) {
                dest = null;
                logger.error("IllegalAccessException getBean error. statck={}", JSONUtils.toString(e.getStackTrace()));
            }
        }

        return (null != dest) ? (T) dest : null;
    }

    /**
     * 获取业务对象列表
     *
     * @param originList
     * @param clazz
     * @return
     */
    public List<T> getList(List<?> originList, Class<T> clazz) {
        List<T> list = new ArrayList<>(originList.size());
        if (CollectionUtils.isNotEmpty(originList)) {
            for (Object obj : originList) {
                list.add(getBean(obj, clazz));
            }
        }

        return list;
    }
}
