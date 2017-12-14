package recipe.service;

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
 * @date:2017/5/24.
 */
public class RecipeBaseService {

    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(RecipeBaseService.class);


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
     * 获取业务对象列表
     *
     * @param originList
     * @param clazz
     * @return
     */
    public List getList(List<? extends Object> originList, Class clazz) {
        List list = new ArrayList<>(originList.size());
        if (CollectionUtils.isNotEmpty(originList)) {
            for (Object obj : originList) {
                list.add(getBean(obj, clazz));
            }
        }

        return list;
    }

}
