package recipe.dao;

import com.ngari.recipe.entity.CompareDrug;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

/**
 * 药品对照
 * @author yinsheng
 * date:2019/3/10 20:08.
 */
@RpcSupportDAO
public abstract class CompareDrugDAO extends HibernateSupportDelegateDAO<CompareDrug> {

    public CompareDrugDAO() {
        super();
        this.setEntityName(CompareDrug.class.getName());
        this.setKeyField("originalDrugId");
    }

    /**
     * 通过原来的药品id查询对照药品id
     *
     * @param originalDrugId 原药品id
     * @return targetDrugId  对照药品id
     */
    public Integer findTargetDrugIdByOriginalDrugId(final Integer originalDrugId) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "select targetDrugId from CompareDrug where originalDrugId = :originalDrugId ";
                Query q = ss.createQuery(hql);
                q.setParameter("originalDrugId", originalDrugId);
                setResult((Integer) q.uniqueResult());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

}