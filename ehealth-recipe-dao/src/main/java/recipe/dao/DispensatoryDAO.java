package recipe.dao;

import com.ngari.recipe.entity.Dispensatory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Chuwei
 */

@RpcSupportDAO
public abstract class DispensatoryDAO extends HibernateSupportDelegateDAO<Dispensatory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DispensatoryDAO.class);

    public DispensatoryDAO() {
        super();
        this.setEntityName(Dispensatory.class.getName());
        this.setKeyField("dispensatoryId");
    }


    /**
     * 根据页面Id和信息来源获取
     * @param pageId
     * @param source
     * @return
     */
    @DAOMethod
    public abstract Dispensatory getByPageIdAndSource(String pageId, int source);

    /**
     * 根据页面Id和信息来源更新
     * @param pageId
     * @param name
     * @param source
     */
    @DAOMethod(sql = "update Dispensatory set name=:name where pageId =:pageId and source = :source")
    public abstract void updateByPageId(@DAOParam("pageId") String pageId,
                                        @DAOParam("name") String name,
                                        @DAOParam("source") int source);

    /**
     * 保存
     * @param list
     */
    public void saveDispensatory(List<Dispensatory> list) {
        for (Dispensatory dispensatory : list) {
            save(dispensatory);
        }
    }

    /**
     * 根据名字获取
     * @param name
     * @return
     */
    public Dispensatory getByNameLike(final String name) {
        HibernateStatelessResultAction<Dispensatory> action = new AbstractHibernateStatelessResultAction<Dispensatory>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Dispensatory where name like :name");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("name", name);
                List<Dispensatory> list = q.list();
                if (list.size() > 0) {
                    setResult(list.get(0));
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据药品id获取
     * @param drugId
     * @return
     */
    @DAOMethod
    public abstract Dispensatory getByDrugId(Integer drugId);
}
