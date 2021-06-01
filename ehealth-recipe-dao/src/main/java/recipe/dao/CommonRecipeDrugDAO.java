package recipe.dao;

import com.ngari.recipe.entity.CommonRecipeDrug;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.StatelessSession;

import java.util.List;

/**
 * @author jiangtingfeng
 * date:2017/5/23.
 */
@RpcSupportDAO
public abstract class CommonRecipeDrugDAO extends HibernateSupportDelegateDAO<CommonRecipeDrug> {

    public CommonRecipeDrugDAO() {
        super();
        this.setEntityName(CommonRecipeDrug.class.getName());
        this.setKeyField("Id");
    }

    /**
     * 通过常用方id查询药品
     * @param commonRecipeId
     * @return
     */
    @DAOMethod(sql = "from CommonRecipeDrug where commonRecipeId=:commonRecipeId")
    public abstract List<CommonRecipeDrug> findByCommonRecipeId(@DAOParam("commonRecipeId") Integer commonRecipeId);


    /**
     * 根据常用方id进行批量获取常用方药品信息
     *
     * @param commonRecipeIdList
     * @return
     */
    @DAOMethod(sql = "from CommonRecipeDrug where commonRecipeId in :commonRecipeIdList", limit = 0)
    public abstract List<CommonRecipeDrug> findByCommonRecipeIdList(@DAOParam("commonRecipeIdList") List<Integer> commonRecipeIdList);

    /**
     * 根据常用方id删除药品
     * @param commonRecipeId
     */
    @DAOMethod(sql = "delete from CommonRecipeDrug where commonRecipeId=:commonRecipeId")
    public abstract void deleteByCommonRecipeId(@DAOParam("commonRecipeId") Integer commonRecipeId);

    /**
     * 插入常用方药品列表，使用事务方式
     *
     * @param drugList
     * @param commonRecipeId
     * @param now
     * @throws DAOException
     */
    public void addCommonRecipeDrugList(final List<CommonRecipeDrug> drugList, final Integer commonRecipeId)
            throws DAOException {
        HibernateStatelessResultAction action = new AbstractHibernateStatelessResultAction() {
            @Override
            public void execute(StatelessSession ss) throws Exception {

                for (CommonRecipeDrug commonRecipeDrug : drugList) {
                    commonRecipeDrug.setCommonRecipeId(commonRecipeId);
                    ss.insert(commonRecipeDrug);
                }
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);

    }

}
