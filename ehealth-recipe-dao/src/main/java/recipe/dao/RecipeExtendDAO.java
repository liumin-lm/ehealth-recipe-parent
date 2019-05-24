package recipe.dao;

import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;

/**
 * 处方扩展表
 * Created by yuzq on 2019/3/1.
 */
@RpcSupportDAO
public abstract class RecipeExtendDAO extends HibernateSupportDelegateDAO<RecipeExtend> {

    private static final Log LOGGER = LogFactory.getLog(RecipeExtendDAO.class);

    public RecipeExtendDAO() {
        super();
        this.setEntityName(RecipeExtend.class.getName());
        this.setKeyField("recipeId");
    }

    /**
     * 根据id获取
     *
     * @param recipeId
     * @return
     */
    @DAOMethod
    public abstract RecipeExtend getByRecipeId(int recipeId);


    public void saveRecipeExtend(RecipeExtend recipeExtend) {
        LOGGER.info("处方扩展表保存：" + JSONUtils.toString(recipeExtend));
        super.save(recipeExtend);
    }

    public Boolean updateCardInfoById(final int recipeId, final String cardTypeName , final String cardNo) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update RecipeExtend set cardNo=:cardNo, cardTypeName=:cardTypeName where recipeId=:recipeId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("recipeId", recipeId);
                q.setParameter("cardTypeName", cardTypeName);
                q.setParameter("cardNo", cardNo);
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 保存OR更新
     * @param recipeExtend
     */
    public void saveOrUpdateRecipeExtend(RecipeExtend recipeExtend) {
        if(null == recipeExtend.getRecipeId()){
            return;
        }
        if (ObjectUtils.isEmpty(this.getByRecipeId(recipeExtend.getRecipeId()))) {
            this.save(recipeExtend);
        } else {
            this.update(recipeExtend);
        }
    }


}
