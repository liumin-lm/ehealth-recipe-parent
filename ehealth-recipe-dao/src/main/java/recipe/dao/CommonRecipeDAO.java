package recipe.dao;

import com.ngari.recipe.entity.CommonRecipe;
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
public abstract class CommonRecipeDAO extends HibernateSupportDelegateDAO<CommonRecipe> {

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
     * 通过医生id查询常用方
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


}