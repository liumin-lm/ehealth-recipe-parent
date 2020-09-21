package recipe.drugsenterprise.CommonExtendCompatible;

import com.google.common.collect.Lists;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import recipe.dao.RecipeDAO;

import java.util.List;

/**
* @Description:
 * 当前枚举是为了兼容默认实现的药企流程实现
* @Author: JRK
* @Date: 2020/9/21
*/
public enum CommonExtendRemoteTypeEnum {

    //默认实现
    COMMON_TYPE(new CommonSelfRemoteType(), 0, "默认药企"),

    HIS_ADMINISTRATION(new HisAdministrationRemoteType(), 1, "his管理的药企");

    private CommonExtendRemoteInterface remoteType;

    //药企对接模式
    private Integer remoteDockType;

    private String mean;

    CommonExtendRemoteTypeEnum(CommonExtendRemoteInterface remoteType, Integer remoteDockType, String mean) {
        this.remoteType = remoteType;
        this.remoteDockType = remoteDockType;
        this.mean = mean;
    }

    public CommonExtendRemoteInterface getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(CommonExtendRemoteInterface remoteType) {
        this.remoteType = remoteType;
    }

    public Integer getRemoteDockType() {
        return remoteDockType;
    }

    public void setRemoteDockType(Integer remoteDockType) {
        this.remoteDockType = remoteDockType;
    }

    public String getMean() {
        return mean;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }

    /**
     * @method  fromCode
     * @description 根据code获得枚举类
     * @date: 2019/7/24
     * @author: JRK
     * @return recipe.constant.CommonExtendRemoteTypeEnum
     */
    public static CommonExtendRemoteInterface getTypeFromOrganId(Integer organId) {
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);

        //获取机构配置的支持购药方式
        Object dockType = configService.getConfiguration(organId, "EnterprisesDockType");
        Integer dockMode = null != dockType ? Integer.parseInt(dockType.toString()) : new Integer(0);
        for(CommonExtendRemoteTypeEnum ep : CommonExtendRemoteTypeEnum.values()){
            if(dockMode.equals(ep.getRemoteDockType())){
                return ep.getRemoteType();
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }



    public static CommonExtendRemoteInterface getTypeFromRecipeIds(List<Integer> recipeIds) {
        if (null != recipeIds && CollectionUtils.isNotEmpty(recipeIds)) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
            if(null != recipe){
                return getTypeFromOrganId(recipe.getClinicId());
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendRemoteInterface getTypeFromRecipeOrder(RecipeOrder order) {
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        if (null != recipeIdList && CollectionUtils.isNotEmpty(recipeIdList)) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeIdList.get(0));
            if(null != recipe){
                return getTypeFromOrganId(recipe.getClinicId());
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendRemoteInterface getTypeFromRecipeBean(RecipeBean recipeBean) {
        if (null != recipeBean) {
            return getTypeFromOrganId(recipeBean.getClinicId());
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendRemoteInterface getTypeFromRecipe(Recipe recipe) {
        if (null != recipe) {
            return getTypeFromOrganId(recipe.getClinicId());
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }
}