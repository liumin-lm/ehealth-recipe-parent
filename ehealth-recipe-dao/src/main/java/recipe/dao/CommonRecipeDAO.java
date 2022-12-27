package recipe.dao;

import com.ngari.recipe.entity.CommonRecipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * @author jiangtingfeng
 * date:2017/5/22.
 */
@RpcSupportDAO
public abstract class CommonRecipeDAO extends HibernateSupportDelegateDAO<CommonRecipe> implements ExtendDao<CommonRecipe> {

    @Override
    public boolean updateNonNullFieldByPrimaryKey(CommonRecipe commonRecipe) {
        return updateNonNullFieldByPrimaryKey(commonRecipe, "commonRecipeId");
    }

    public CommonRecipeDAO() {
        super();
        this.setEntityName(CommonRecipe.class.getName());
        this.setKeyField("commonRecipeId");
    }

    /**
     * 通过处方类型查询常用方列表
     *
     * @param recipeType
     * @param doctorId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from CommonRecipe where recipeType in (:recipeType) and doctorId=:doctorId order by lastModify desc")
    public abstract List<CommonRecipe> findByRecipeType(@DAOParam("recipeType") List<Integer> recipeType,
                                                        @DAOParam("doctorId") Integer doctorId,
                                                        @DAOParam(pageStart = true) int start,
                                                        @DAOParam(pageLimit = true) int limit);

    /**
     * 通过recipeType和organId进行查询常用方
     *
     * @param recipeType
     * @param doctorId
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from CommonRecipe where recipeType in (:recipeType) and doctorId=:doctorId and organId=:organId order by lastModify desc")
    public abstract List<CommonRecipe> findByRecipeTypeAndOrganId(@DAOParam("recipeType") List<Integer> recipeType,
                                                                  @DAOParam("doctorId") Integer doctorId,
                                                                  @DAOParam("organId") Integer organId,
                                                                  @DAOParam(pageStart = true) int start,
                                                                  @DAOParam(pageLimit = true) int limit);

    /**
     * 通过doctor和organId进行查找常用方
     *
     * @param doctorId
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from CommonRecipe where doctorId=:doctorId and organId=:organId order by lastModify desc")
    public abstract List<CommonRecipe> findByDoctorIdAndOrganId(@DAOParam("doctorId") Integer doctorId,
                                                                @DAOParam("organId") Integer organId,
                                                                @DAOParam(pageStart = true) int start,
                                                                @DAOParam(pageLimit = true) int limit);

    /**
     * 根据机构类型获取常用方
     *
     * @param organId          机构id
     * @param doctorId         医生id
     * @param commonRecipeType 类型
     * @return
     */
    @DAOMethod(limit = 0, sql = "from CommonRecipe where doctorId=:doctorId and organId=:organId and common_recipe_type=:commonRecipeType")
    public abstract List<CommonRecipe> findByOrganIdAndDoctorIdAndType(@DAOParam("organId") Integer organId,
                                                                       @DAOParam("doctorId") Integer doctorId,
                                                                       @DAOParam("commonRecipeType") Integer commonRecipeType);


    /**
     * 通过医生id查询常用方
     *
     * @param doctorId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from CommonRecipe where doctorId=:doctorId order by lastModify desc")
    public abstract List<CommonRecipe> findByDoctorId(@DAOParam("doctorId") Integer doctorId,
                                                      @DAOParam(pageStart = true) int start,
                                                      @DAOParam(pageLimit = true) int limit);


    /**
     * 判断是否存在相同常用方名称
     *
     * @param doctorId
     * @param commonRecipeName
     * @return
     */
    @DAOMethod(sql = "from CommonRecipe where doctorId=:doctorId and commonRecipeName=:commonRecipeName")
    public abstract List<CommonRecipe> findByName(@DAOParam("doctorId") Integer doctorId, @DAOParam("commonRecipeName") String commonRecipeName);


    @DAOMethod(sql = "from CommonRecipe where commonRecipeId=:commonRecipeId")
    public abstract CommonRecipe getByCommonRecipeId(@DAOParam("commonRecipeId") Integer commonRecipeId);

    /**
     * 根据机构跟医生id获取为同步过的常用方
     * @param organId
     * @param doctorId
     * @return
     */
    @DAOMethod(limit = 0, sql = "from CommonRecipe where doctorId=:doctorId and organId=:organId and validateStatus != 1")
    public abstract List<CommonRecipe> findCommonRecipeListByOrganIdAndDoctorId(@DAOParam("organId") Integer organId, @DAOParam("doctorId") Integer doctorId);

    /**
     * 根据常用方id变更同步状态
     * @param commonIds
     */
    @DAOMethod(sql = "update CommonRecipe set validateStatus = 1 where commonRecipeId in (:commonIds)", limit = 0)
    public abstract void updateCommonRecipeStatus(@DAOParam("commonIds")List<Integer> commonIds);
}