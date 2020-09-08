package recipe.dao;

import com.ngari.recipe.entity.HisRecipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:23
 */
@RpcSupportDAO
public abstract class HisRecipeDAO extends HibernateSupportDelegateDAO<HisRecipe> {

    public HisRecipeDAO() {
        super();
        this.setEntityName(HisRecipe.class.getName());
        this.setKeyField("hisRecipeID");
    }

    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and mpiId=:mpiId and status=:status order by createDate desc")
    public abstract List<HisRecipe> findHisRecipes(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("mpiId") String mpiId, @DAOParam("status") int status, @DAOParam(pageStart = true) int start,
                                                   @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = " update HisRecipe set status=:status where clinicOrgan=:clinicOrgan and recipeCode=:recipeCode ")
    public abstract void updateHisRecieStatus(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode, @DAOParam("status") int status);

    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and recipeCode=:recipeCode")
    public abstract HisRecipe getHisRecipeByRecipeCodeAndClinicOrgan(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode);

    /**
     * 根据 机构编号 和 处方单号 批量查询数据
     *
     * @param clinicOrgan    机构编号
     * @param recipeCodeList 处方单号
     * @return
     */
    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and recipeCode in (:recipeCodeList)")
    public abstract List<HisRecipe> findHisRecipeByRecipeCodeAndClinicOrgan(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCodeList") List<String> recipeCodeList);

    @DAOMethod(sql = " From HisRecipe where mpiId=:mpiId and clinicOrgan=:clinicOrgan and recipeCode=:recipeCode")
    public abstract HisRecipe getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(@DAOParam("mpiId") String mpiId, @DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode);

    @DAOMethod(sql = " From HisRecipe where mpiId=:mpiId and clinicOrgan=:clinicOrgan and recipeCode=:recipeCode")
    public abstract HisRecipe getHisRecipeRecipeCodeAndClinicOrgan( @DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode);

    /**
     * 根据处方id批量删除
     *
     * @param hisRecipeIds
     */
    @DAOMethod(sql = "delete from HisRecipe where hisRecipeId in (:hisRecipeIds)")
    public abstract void deleteByHisRecipeIds(@DAOParam("hisRecipeIds") List<Integer> hisRecipeIds);
}
