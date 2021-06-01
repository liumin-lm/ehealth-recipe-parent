package recipe.dao;

import com.ngari.recipe.entity.CommonRecipeExt;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author jiangtingfeng
 * date:2017/5/22.
 */
@RpcSupportDAO
public abstract class CommonRecipeExtDAO extends HibernateSupportDelegateDAO<CommonRecipeExt> {

    public CommonRecipeExtDAO() {
        super();
        this.setEntityName(CommonRecipeExt.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据常用id查询扩展信息
     *
     * @param commonRecipeIds
     * @return
     */
    @DAOMethod(sql = "from CommonRecipeExt where  common_recipe_id in (:commonRecipeIds)", limit = 0)
    public abstract List<CommonRecipeExt> findByCommonRecipeIds(@DAOParam("commonRecipeIds") List<Integer> commonRecipeIds);

    /**
     * 根据常用id查询扩展信息
     *
     * @param commonRecipeId
     * @return
     */
    @DAOMethod(sql = "from CommonRecipeExt where common_recipe_id=:commonRecipeId")
    public abstract CommonRecipeExt getByCommonRecipeId(@DAOParam("commonRecipeId") Integer commonRecipeId);


    /**
     * 根据常用方id删除扩展信息
     *
     * @param commonRecipeId
     */
    @DAOMethod(sql = "delete from CommonRecipeExt where common_recipe_id=:commonRecipeId")
    public abstract void deleteByCommonRecipeId(@DAOParam("commonRecipeId") Integer commonRecipeId);


}