package recipe.drugsenterprise;

import com.google.common.collect.Lists;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.PharmacyDAO;
import recipe.drugsenterprise.commonExtendCompatible.CommonExtendEnterprisesInterface;
import recipe.drugsenterprise.commonExtendCompatible.CommonExtendRemoteTypeEnum;
import recipe.drugsenterprise.commonExtendCompatible.CommonSelfEnterprisesType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公共自建药企
 * @author yinsheng
 * @date 2019\11\7 0007 14:20
 */
@RpcBean("commonSelfRemoteService")
public class CommonSelfRemoteService extends AccessDrugEnterpriseService{

    private static final String searchMapRANGE = "range";

    private static final String searchMapLatitude = "latitude";

    private static final String searchMapLongitude = "longitude";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSelfRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("CommonSelfRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return CommonExtendRemoteTypeEnum.getTypeFromRecipeIds(recipeIds).pushRecipeInfo(recipeIds, enterprise);
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    @RpcService
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return CommonExtendRemoteTypeEnum.getTypeFromRecipeIds(Lists.newArrayList(recipeId)).scanStock(recipeId, drugsEnterprise);
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return CommonExtendRemoteTypeEnum.getTypeFromRecipeIds(recipeIds).findSupportDep(recipeIds, ext, enterprise);
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_COMMON_SELF;
    }

    private List<Pharmacy> getPharmacies(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise, DrugEnterpriseResult result) {
        PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
        List<Pharmacy> pharmacyList = new ArrayList<Pharmacy>();
        if (ext != null && null != ext.get(searchMapRANGE) && null != ext.get(searchMapLongitude) && null != ext.get(searchMapLatitude)) {
            pharmacyList = pharmacyDAO.findByDrugsenterpriseIdAndStatusAndRangeAndLongitudeAndLatitude(enterprise.getId(), Double.parseDouble(ext.get(searchMapRANGE).toString()), Double.parseDouble(ext.get(searchMapLongitude).toString()), Double.parseDouble(ext.get(searchMapLatitude).toString()));
        }else{
            LOGGER.warn("CommonSelfRemoteService.findSupportDep:请求的搜索参数不健全" );
            getFailResult(result, "请求的搜索参数不健全");
        }
        if(CollectionUtils.isEmpty(recipeIds)){
            LOGGER.warn("CommonSelfRemoteService.findSupportDep:查询的处方单为空" );
            getFailResult(result, "查询的处方单为空");
        }
        return pharmacyList;
    }

    private void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        //date 20201023 当前接口有非机构下的查看库存，
        return CommonExtendRemoteTypeEnum.getTypeFromOrganIdAndEnterprises(organId, drugsEnterprise).getDrugInventory(drugId, drugsEnterprise, organId);
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails, List<SaleDrugList> saleDrugLists) {
        return CommonExtendRemoteTypeEnum.getTypeFromOrganId(recipe.getClinicOrgan()).scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails, saleDrugLists);
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return CommonExtendRemoteTypeEnum.getTypeFromOrganId(drugsDataBean.getOrganId()).getDrugInventoryForApp(drugsDataBean, drugsEnterprise, flag);
    }

    @Override
    //走默认实现
    public boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipe(dbRecipe);
        if(type instanceof CommonSelfEnterprisesType){
            return super.scanStock(dbRecipe, dep, drugIds);
        }else{
            return type.scanStock(dbRecipe, dep, drugIds);
        }
    }

    @Override
    public String appEnterprise(RecipeOrder order) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipeOrder(order);
        if(type instanceof CommonSelfEnterprisesType){
            return super.appEnterprise(order);
        }else{
            return type.appEnterprise(order);
        }
    }

    @Override
    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipeOrder(order);
        if(type instanceof CommonSelfEnterprisesType){
            return super.orderToRecipeFee(order, recipeIds, payModeSupport, recipeFee, extInfo);
        }else{
            return type.orderToRecipeFee(order, recipeIds, payModeSupport, recipeFee, extInfo);
        }
    }

    @Override
    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipeOrder(order);
        if(type instanceof CommonSelfEnterprisesType){
            super.setOrderEnterpriseMsg(extInfo, order);
        }else{
            type.setOrderEnterpriseMsg(extInfo, order);
        }
    }

    @Override
    public void checkRecipeGiveDeliveryMsg(RecipeBean recipeBean, Map<String, Object> map) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipeBean(recipeBean);
        if(type instanceof CommonSelfEnterprisesType){
            super.checkRecipeGiveDeliveryMsg(recipeBean, map);
        }else{
            type.checkRecipeGiveDeliveryMsg(recipeBean, map);
        }
    }

    @Override
    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipeOrder(order);
        if(type instanceof CommonSelfEnterprisesType){
            super.setEnterpriseMsgToOrder(order, depId, extInfo);
        }else{
            type.setEnterpriseMsgToOrder(order, depId, extInfo);
        }
    }

    @Override
    public Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipe(dbRecipe);
        if(type instanceof CommonSelfEnterprisesType){
            return super.specialMakeDepList(drugsEnterprise, dbRecipe);
        }else{
            return type.specialMakeDepList(drugsEnterprise, dbRecipe);
        }
    }

    @Override
    public DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult) {
        CommonExtendEnterprisesInterface type = CommonExtendRemoteTypeEnum.getTypeFromRecipeIds(Lists.newArrayList(recipeId));
        if(type instanceof CommonSelfEnterprisesType){
            return super.sendMsgResultMap(recipeId, extInfo, payResult);
        }else{
            return type.sendMsgResultMap(recipeId, extInfo, payResult);
        }
    }
}
