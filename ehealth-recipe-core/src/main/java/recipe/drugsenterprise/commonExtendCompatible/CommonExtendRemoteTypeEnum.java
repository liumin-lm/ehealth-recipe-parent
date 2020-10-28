package recipe.drugsenterprise.commonExtendCompatible;

import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import net.coobird.thumbnailator.util.exif.IfdStructure;
import org.apache.commons.collections.CollectionUtils;
import recipe.dao.RecipeDAO;

import java.util.List;

/**
* @Description:
 * 当前枚举是为了兼容默认实现的药企流程实现（这些受用于流程抽象在药企环节上的）
* @Author: JRK
* @Date: 2020/9/21
*/
public enum CommonExtendRemoteTypeEnum {

    //默认实现
    COMMON_TYPE(new CommonSelfEnterprisesType(), 0, "默认药企"),

    HIS_ADMINISTRATION(new HisAdministrationEnterprisesType(), 1, "his管理的药企");

    private CommonExtendEnterprisesInterface remoteType;

    //药企对接模式
    private Integer remoteDockType;

    private String mean;

    CommonExtendRemoteTypeEnum(CommonExtendEnterprisesInterface remoteType, Integer remoteDockType, String mean) {
        this.remoteType = remoteType;
        this.remoteDockType = remoteDockType;
        this.mean = mean;
    }

    public CommonExtendEnterprisesInterface getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(CommonExtendEnterprisesInterface remoteType) {
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
    public static CommonExtendEnterprisesInterface getTypeFromOrganId(Integer organId) {
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);

        //获取机构配置配置的药企管理方式：his（1）还是平台（0）
        Object dockType = configService.getConfiguration(organId, "EnterprisesDockType");
        Integer dockMode = null != dockType ? Integer.parseInt(dockType.toString()) : new Integer(0);
        for(CommonExtendRemoteTypeEnum ep : CommonExtendRemoteTypeEnum.values()){
            if(dockMode.equals(ep.getRemoteDockType())){
                return ep.getRemoteType();
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendEnterprisesInterface getTypeFromOrganIdAndEnterprises(Integer organId, DrugsEnterprise drugsEnterprise) {
        if(null != organId){
            return getTypeFromOrganId(organId);
        }else{
            //这里判断当是杭州市药企的时候，为了处理这种没有机构id，导致无法判断是那种药企特性流程
            if(null != drugsEnterprise && "hzInternet".equals(drugsEnterprise.getAccount())){
                return CommonExtendRemoteTypeEnum.HIS_ADMINISTRATION.getRemoteType();
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendEnterprisesInterface getTypeFromRecipeIds(List<Integer> recipeIds) {
        if (null != recipeIds && CollectionUtils.isNotEmpty(recipeIds)) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
            if(null != recipe){
                return getTypeFromOrganId(recipe.getClinicOrgan());
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendEnterprisesInterface getTypeFromRecipeOrder(RecipeOrder order) {
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        if (null != recipeIdList && CollectionUtils.isNotEmpty(recipeIdList)) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeIdList.get(0));
            if(null != recipe){
                return getTypeFromOrganId(recipe.getClinicOrgan());
            }
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendEnterprisesInterface getTypeFromRecipeBean(RecipeBean recipeBean) {
        if (null != recipeBean) {
            return getTypeFromOrganId(recipeBean.getClinicOrgan());
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }

    public static CommonExtendEnterprisesInterface getTypeFromRecipe(Recipe recipe) {
        if (null != recipe) {
            return getTypeFromOrganId(recipe.getClinicOrgan());
        }
        return CommonExtendRemoteTypeEnum.COMMON_TYPE.getRemoteType();
    }
}