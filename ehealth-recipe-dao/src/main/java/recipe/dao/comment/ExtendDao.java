package recipe.dao.comment;

import com.google.common.collect.Maps;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.JSONUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import recipe.constant.ErrorCode;

import javax.persistence.Entity;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author binglang
 * @since 2020/7/22
 */
public interface ExtendDao<T> {
    Logger logger = LoggerFactory.getLogger(ExtendDao.class);

    String SQL_KEY_ID = "id";

    /**
     * 非null对象修改方法 ---null字段不做更新
     *
     * @param obj 修改对象
     * @return
     */
    boolean updateNonNullFieldByPrimaryKey(T obj);

    /**
     * updateNonNullField
     * 单表非null 字段更新
     *
     * @param entity   entity 需要更新的对象
     * @param keyField keyField 对象对应的主键
     * @return success
     */
    default boolean updateNonNullFieldByPrimaryKey(Object entity, String keyField) {
        try {
            logger.info("updateNonNullFieldByPrimaryKey entity = {} ,keyField = {}", JSONUtils.toString(entity), keyField);
            Assert.notNull(entity);
            Assert.notNull(keyField);
            HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
                @Override
                public void execute(StatelessSession statelessSession) throws Exception {
                    Map<String, Object> param = Maps.newHashMap();
                    String hql = assembleHqlForUpdateNonNullField(entity, param, keyField);
                    Query query = statelessSession.createQuery(hql);
                    param.remove(keyField);
                    param.forEach(query::setParameter);
                    setResult(query.executeUpdate() > 0);
                }
            };
            HibernateSessionTemplate.instance().execute(action);
            return action.getResult();
        } catch (Exception e) {
            logger.error("updateNonNullFieldByPrimaryKey error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * assembleHqlForUpdateNonNullField
     *
     * @param entity   entity
     * @param param    param
     * @param keyField keyField
     * @return hql
     * @throws Exception DAOException
     */
    default String assembleHqlForUpdateNonNullField(final Object entity,
                                                    final Map<String, Object> param, String keyField) throws Exception {
        Assert.notNull(entity);
        Assert.notNull(param);
        Assert.notNull(keyField);

        Class<?> clazz = entity.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();
        Entity entityAnnotation = entity.getClass().getAnnotation(Entity.class);
        if (entityAnnotation == null) {
            throw new DAOException("not entity");
        }
        String name = entity.getClass().getName();
        name = name.substring(name.lastIndexOf(".") + 1);
        StringBuilder hql = new StringBuilder("update ").append(name).append(" set ");
        for (Field field : declaredFields) {
            PropertyDescriptor pd;
            try {
                pd = new PropertyDescriptor(field.getName(), clazz);
            } catch (IntrospectionException e) {
                continue;
            }
            Method readMethod = pd.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            Object value = readMethod.invoke(entity);
            if (value == null) {
                continue;
            }
            if (!keyField.equals(field.getName())) {
                hql.append(field.getName()).append(" = :").append(field.getName()).append(", ");
            }
            param.put(field.getName(), value);
        }
        if (param.get(keyField) == null) {
            throw new DAOException("keyField cannot be null");
        }
        // 必须包含主键及一个以上的待更新字段
        if (param.size() < 2) {
            throw new DAOException("no NonNull field");
        }
        String result = hql.substring(0, hql.length() - 2) + " where " + keyField + " = " + param
                .get(keyField);

        logger.info("assembleHqlForUpdateNonNullField result = {}", result);
        return result;
    }
}
