package recipe.drugsenterprise.compatible;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.hisservice.RecipeToHisService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @description 杭州互联网（金投）对接服务旧实现方式
 * 不再使用
 * @author JRK
 * @date 2020/3/16
 */
@Service("hzInternetRemoteOldType")
@Deprecated
public class HzInternetRemoteOldType implements HzInternetRemoteTypeInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(HzInternetRemoteOldType.class);
    @Override
    public Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe) {
        LOGGER.info("旧-杭州互联网虚拟药企展示配送列表是否需要个性化，入参：drugsEnterprise：{}，dbRecipe：{}",
                JSONUtils.toString(drugsEnterprise), JSONUtils.toString(dbRecipe));
        return false;
    }

    @Override
    public DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult) {
        LOGGER.info("旧-杭州互联网虚拟药企确认订单前检验订单信息同步配送信息，入参：dbRecipe:{},extInfo:{},payResult:{]",
                recipeId, JSONUtils.toString(extInfo), JSONUtils.toString(payResult));
        return payResult;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("旧-杭州互联网虚拟药企-更新取药信息至处方流转平台开始，处方ID：{}.", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        //1物流配送
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);


        UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
        updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
        //平台处方号
        updateTakeDrugWayReqTO.setNgarRecipeId(recipe.getRecipeId()+"");
        //医院处方号
        //流转到这里来的属于物流配送
        updateTakeDrugWayReqTO.setDeliveryType("1");
        updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
        updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));

        updateTakeDrugWayReqTO.setPayMode("1");
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if(recipeExtend != null && recipeExtend.getDeliveryCode() != null){
            updateTakeDrugWayReqTO.setDeliveryCode(recipeExtend.getDeliveryCode());
            updateTakeDrugWayReqTO.setDeliveryName(recipeExtend.getDeliveryName());
        } else {
            LOGGER.info("杭州互联网虚拟药企-未获取his返回的配送药-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
            result.setMsg("未获取his返回的配送药");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        if (StringUtils.isNotEmpty(recipe.getOrderCode())){
            RecipeOrderDAO dao = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = dao.getByOrderCode(recipe.getOrderCode());
            if (order!=null){
                //收货人
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                //联系电话
                updateTakeDrugWayReqTO.setContactTel(order.getRecTel());
                //详细收货地址
                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                updateTakeDrugWayReqTO.setAddress(commonRemoteService.getCompleteAddress(order));

                //收货地址代码
                updateTakeDrugWayReqTO.setReceiveAddrCode(order.getStreetAddress());
                String streetAddress = null;
                try {
                    streetAddress = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getStreetAddress());
                } catch (ControllerException e) {
                    LOGGER.warn("杭州互联网虚拟药企-未获取收货地址名称-add={}", JSONUtils.toString(order.getStreetAddress()));
                }
                //收货地址名称
                updateTakeDrugWayReqTO.setReceiveAddress(streetAddress);
                //期望配送日期
                updateTakeDrugWayReqTO.setConsignee(order.getExpectSendDate());
                //期望配送时间
                updateTakeDrugWayReqTO.setContactTel(order.getExpectSendTime());
            }else{
                LOGGER.info("杭州互联网虚拟药企-未获取有效订单-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
                result.setMsg("未获取有效订单");
                result.setCode(DrugEnterpriseResult.FAIL);
            }
        }

        HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
        if("200".equals(hisResult.getMsgCode())){
            LOGGER.info("杭州互联网虚拟药企-更新取药信息成功-his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));
            result.setCode(DrugEnterpriseResult.SUCCESS);
        }else{
            LOGGER.error("杭州互联网虚拟药企-更新取药信息失败-his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));

            result.setMsg(hisResult.getMsg());
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("旧-scanStock 虚拟药企库存入参为：{}，{}", recipeId, JSONUtils.toString(drugsEnterprise));
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        LOGGER.info("旧-findSupportDep 虚拟药企导出入参为：{}，{}，{}", JSONUtils.toString(recipeIds), JSONUtils.toString(ext), JSONUtils.toString(enterprise));
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        LOGGER.info("旧-scanStock recipeId:{},dep:{},drugIds:{}", dbRecipe.getRecipeId(), JSONUtils.toString(dep), JSONUtils.toString(drugIds));
        return true;
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        return new DrugStockAmountDTO();
    }

    @Override
    public String appEnterprise(RecipeOrder order) {
        LOGGER.info("旧-appEnterprise order:{}", JSONUtils.toString(order));
        AccessDrugEnterpriseService remoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
        return remoteService.appEnterprise(order);
    }

    @Override
    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo) {
        LOGGER.info("旧-orderToRecipeFee order:{}, recipeIds:{}, payModeSupport:{}, recipeFee:{}, extInfo:{}",
                JSONUtils.toString(order), JSONUtils.toString(recipeIds), JSONUtils.toString(payModeSupport), recipeFee, JSONUtils.toString(extInfo));
        AccessDrugEnterpriseService remoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
        return remoteService.orderToRecipeFee(order, recipeIds, payModeSupport, recipeFee, extInfo);
    }

    @Override
    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order) {

        LOGGER.info("旧-setOrderEnterpriseMsg extInfo：{}，order：{}", JSONUtils.toString(extInfo), JSONUtils.toString(order));
        AccessDrugEnterpriseService remoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
        remoteService.setOrderEnterpriseMsg(extInfo, order);
    }

    @Override
    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo) {
        LOGGER.info("旧-setEnterpriseMsgToOrder order:{}, depId:{}，extInfo:{} ", JSONUtils.toString(order), depId, JSONUtils.toString(extInfo));
        AccessDrugEnterpriseService remoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
        remoteService.setEnterpriseMsgToOrder(order, depId, extInfo);
    }
}
