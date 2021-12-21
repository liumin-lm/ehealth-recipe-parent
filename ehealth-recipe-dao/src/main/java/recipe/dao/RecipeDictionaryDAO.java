package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.ngari.recipe.entity.RecipeDictionary;
import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.dao.comment.ExtendDao;

import java.util.List;

@RpcSupportDAO
public abstract class RecipeDictionaryDAO extends HibernateSupportDelegateDAO<RecipeDictionary> implements ExtendDao<RecipeDictionary> {
    public RecipeDictionaryDAO() {
        super();
        this.setEntityName(RecipeDictionary.class.getName());
        this.setKeyField(SQL_KEY_ID);
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(RecipeDictionary recipeDictionary) {
        return updateNonNullFieldByPrimaryKey(recipeDictionary, SQL_KEY_ID);
    }

    @DAOMethod(sql = "from RecipeDictionary where organId=:organId and isDelete=0 order by dictionarySort desc")
    public abstract List<RecipeDictionary> findAllRecipeDictionaryByOrganId(@DAOParam("organId") int organId);

    /**
     * 查询字典
     * @param organId
     * @param name
     * @return
     */
    public List<RecipeDictionary> findRecipeDictionaryByName(final Integer organId, final String name){
        HibernateStatelessResultAction<List<RecipeDictionary>> action =
                new AbstractHibernateStatelessResultAction<List<RecipeDictionary>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder(" from RecipeDictionary where organ_id =:organId and is_deleted = 0 ");
                        if (!StringUtils.isEmpty(name)) {
                            hql.append(" and (dictionary_name like :name or dictionary_pingying like :name) ");
                        }
                        hql.append(" order by dictionary_sort asc, gmt_create desc ");
                        Query query = ss.createQuery(hql.toString());
                        if (!StringUtils.isEmpty(name)) {
                            query.setParameter("name", "%"+name+"%");
                        }
                        if (null != organId) {
                            query.setParameter("organId", organId);
                        }
                        List<RecipeDictionary> list = query.list();
                        setResult(list);
                    }
                };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

}
