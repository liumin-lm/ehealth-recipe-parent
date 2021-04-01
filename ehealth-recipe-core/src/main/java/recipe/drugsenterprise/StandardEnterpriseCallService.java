package recipe.drugsenterprise;

import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.his.recipe.mode.RecipeStatusUpdateReqTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.common.utils.VerifyUtils;
import com.ngari.recipe.drugsenterprise.model.ReadjustDrugDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.logistics.model.RecipeLogisticsBean;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.hisservice.RecipeToHisService;
import recipe.purchase.CommonOrder;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 药企标准服务
 * @version： 1.0
 */
@RpcBean("distributionService")
public class StandardEnterpriseCallService {

    @Autowired
    private TaobaoConf taobaoConf;

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardEnterpriseCallService.class);

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeOrderDAO orderDAO;

    @RpcService
    public StandardResultDTO send(List<Map<String, Object>> list) {
        //TODO
        LOGGER.info("send param : " + JSONUtils.toString(list));
        StandardResultDTO result = new StandardResultDTO();
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }


        return null;
    }

    /**
     * 处方状态变更
     *
     * @param list
     * @return
     */
    @RpcService
    public StandardResultDTO changeState(List<StandardStateDTO> list) {
        String listStr = JSONUtils.toString(list);
        LOGGER.info("changeState param = {}", listStr);
        StandardResultDTO result = new StandardResultDTO();
        //默认为失败
        result.setCode(StandardResultDTO.FAIL);
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }
        DrugsEnterpriseDAO depDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

        Integer recipeId = null;
        String recipeCode = null;
        String orderCode = null;
        Integer status = null;
        Integer clinicOrgan = null;
        Recipe dbRecipe = null;
        DrugsEnterprise dep = null;
        for (StandardStateDTO stateDTO : list) {
            try {
                Multimap<String, String> verifyMap = VerifyUtils.verify(stateDTO);
                if (!verifyMap.keySet().isEmpty()) {
                    result.setMsg(verifyMap.toString());
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("changeState 参数对象异常数据，StandardStateDTO={}", JSONUtils.toString(stateDTO), e);
                result.setMsg("参数对象异常数据");
                return result;
            }

            //转换组织结构编码
            try {
                clinicOrgan = getClinicOrganByOrganId(stateDTO.getOrganId(), stateDTO.getClinicOrgan());
            } catch (Exception e) {
                LOGGER.warn("changeState 查询机构异常，organId={}", stateDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOGGER.warn("changeState 平台未匹配到该组织机构编码，organId={}", stateDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }
            recipeCode = stateDTO.getRecipeCode();
            dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, clinicOrgan);
            if (null == dbRecipe) {
                result.setMsg("[" + recipeCode + "]处方单不存在");
                return result;
            }

            //重复处理
            status = Integer.valueOf(stateDTO.getStatus());
            if (status.equals(dbRecipe.getStatus())) {
//                result.setCode(StandardResultDTO.REPEAT);
//                result.setMsg("处方单状态重复修改");
//                return result;
                //暂时通过校验
                continue;
            }

            //获取药企ID
            dep = depDAO.getByAccount(stateDTO.getAccount());
            if (null == dep) {
                result.setMsg("未被授权调用药企");
                return result;
            }

            recipeId = dbRecipe.getRecipeId();
            orderCode = dbRecipe.getOrderCode();
            Map<String, Object> recipeAttrMap = Maps.newHashMap();
            Map<String, Object> orderAttrMap = Maps.newHashMap();
            //自由模式修改调用药企ID，知道是哪家药企接单
            if (RecipeBussConstant.GIVEMODE_FREEDOM.equals(dbRecipe.getGiveMode()) && null == dbRecipe.getEnterpriseId()) {
                recipeAttrMap.put("enterpriseId", dep.getId());
                orderAttrMap.put("enterpriseId", dep.getId());
            }
            switch (status) {
                /**
                 * 药企端用户已支付会推送该状态给平台
                 */
                case RecipeStatusConstant.WAIT_SEND:
                    //以免进行处方失效前提醒
                    recipeAttrMap.put("remindFlag", 1);
                    recipeAttrMap.put("payFlag", PayConstant.PAY_FLAG_PAY_SUCCESS);
                    //更新处方信息
                    Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId,
                            RecipeStatusConstant.WAIT_SEND, recipeAttrMap);
                    if (rs) {
                        //记录日志
                        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS_YS,
                                RecipeStatusConstant.WAIT_SEND, "HOS处方状态变更");

                        orderAttrMap.put("payFlag", PayConstant.PAY_FLAG_PAY_SUCCESS);
                        orderAttrMap.put("payTime", DateConversion.parseDate(stateDTO.getDate(),
                                DateConversion.DEFAULT_DATE_TIME));
                        orderAttrMap.put("status", OrderStatusConstant.READY_SEND);
                        boolean flag = orderDAO.updateByOrdeCode(orderCode, orderAttrMap);
                        if (flag) {
                            LOGGER.info("changeState HOS订单状态变更成功，recipeCode={}, status={}", recipeCode,
                                    OrderStatusConstant.READY_SEND);
                        } else {
                            result.setMsg("[" + recipeCode + "]订单更新失败");
                            LOGGER.warn("changeState HOS订单状态变更失败，recipeCode={}, status={}", recipeCode,
                                    OrderStatusConstant.READY_SEND);
                            return result;
                        }
                    } else {
                        result.setMsg("[" + recipeCode + "]处方单更新失败");
                        LOGGER.warn("changeState HOS处方单状态变更失败，recipeCode={}, status={}", recipeCode, status);
                        return result;
                    }
                    break;

                /**
                 * 药企端取消处方单会推送该状态给平台
                 */
                case RecipeStatusConstant.NO_DRUG:
                    //患者未取药
                    Boolean recipeRs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_DRUG,
                            recipeAttrMap);
                    if (recipeRs) {
                        if (StringUtils.isEmpty(stateDTO.getReason())){
                            stateDTO.setReason("药企端未设置取消原因");
                        }
                        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS_YS,
                                RecipeStatusConstant.NO_DRUG, "取药失败，原因:" + stateDTO.getReason());

                        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                        RecipeResultBean orderRs = orderService.cancelOrderByCode(orderCode, OrderStatusConstant.CANCEL_AUTO);
                        if (RecipeResultBean.SUCCESS.equals(orderRs.getCode())) {
                            orderAttrMap.put("cancelReason", stateDTO.getReason());
                            orderDAO.updateByOrdeCode(orderCode, orderAttrMap);
                            LOGGER.info("changeState HOS订单状态变更成功，recipeCode={}, status={}", recipeCode,
                                    OrderStatusConstant.CANCEL_AUTO);

                            //发送消息
                            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, dbRecipe);
                        } else {
                            result.setMsg("[" + stateDTO.getRecipeCode() + "]订单更新失败");
                            LOGGER.warn("changeState HOS订单状态变更失败，recipeCode={}, status={}", recipeCode,
                                    OrderStatusConstant.CANCEL_AUTO);
                            return result;
                        }

                    } else {
                        result.setMsg("[" + stateDTO.getRecipeCode() + "]处方单更新失败");
                        LOGGER.warn("changeState HOS处方单状态变更失败，recipeCode={}, status={}", recipeCode, status);
                        return result;
                    }
                    break;

                /**
                 * 药企端处方状态变更（更新处方表recipe而不更订单表）（O2O专用）
                 */
                case RecipeStatusConstant.EFFECTIVE:
                case RecipeStatusConstant.EXPIRED:
                case RecipeStatusConstant.RETURNED:
                    recipeAttrMap.put("chooseFlag", 0);
                    Boolean rsTao1 = recipeDAO.updateRecipeInfoByRecipeId(recipeId,
                        status, recipeAttrMap);
                    if (!rsTao1){
                        result.setMsg("[" + recipeCode + "]处方单更新失败");
                        LOGGER.warn("changeState HOS处方单状态变更失败，recipeCode={}, status={}", recipeCode, status);
                        return result;
                    }
                    break;
                case RecipeStatusConstant.USING:
                case RecipeStatusConstant.FINISH:
                    recipeAttrMap.put("chooseFlag", 1);
                    recipeAttrMap.put("payFlag", PayConstant.PAY_FLAG_PAY_SUCCESS);
                    Boolean rsTao2 = recipeDAO.updateRecipeInfoByRecipeId(recipeId,
                        status, recipeAttrMap);
                    if (!rsTao2){
                        result.setMsg("[" + recipeCode + "]处方单更新失败");
                        LOGGER.warn("changeState HOS处方单状态变更失败，recipeCode={}, status={}", recipeCode, status);
                        return result;
                    }
                    break;
                default:
                    result.setMsg("[" + stateDTO.getRecipeCode() + "]不支持变更的状态");
                    return result;
            }

        }

        result.setCode(StandardResultDTO.SUCCESS);
        LOGGER.info("changeState 处理完成. param = {}", listStr);
        return result;
    }

    @RpcService
    public StandardResultDTO finish(List<StandardFinishDTO> list) {
        String listStr = JSONUtils.toString(list);
        LOGGER.info("finish param = {}", listStr);
        StandardResultDTO result = new StandardResultDTO();
        //默认为失败
        result.setCode(StandardResultDTO.FAIL);
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }

        for (StandardFinishDTO finishDTO : list) {
            try {
                Multimap<String, String> verifyMap = VerifyUtils.verify(finishDTO);
                if (!verifyMap.keySet().isEmpty()) {
                    result.setMsg(verifyMap.toString());
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("finish 参数对象异常数据，StandardFinishDTO={}", JSONUtils.toString(finishDTO), e);
                result.setMsg("参数对象异常数据");
                return result;
            }

            boolean isSuccess = finishDTO.getCode().equals(StandardFinishDTO.SUCCESS) ? true : false;
            //转换组织结构编码
            Integer clinicOrgan = null;
            try {
                clinicOrgan = getClinicOrganByOrganId(finishDTO.getOrganId(), finishDTO.getClinicOrgan());
            } catch (Exception e) {
                LOGGER.warn("finish 查询机构异常，organId={}", finishDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOGGER.warn("finish 平台未匹配到该组织机构编码，organId={}", finishDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(finishDTO.getRecipeCode(), clinicOrgan);
            if (null == dbRecipe) {
                result.setMsg("[" + finishDTO.getRecipeCode() + "]处方单不存在");
                return result;
            }

            //重复处理
            if (RecipeStatusConstant.FINISH == dbRecipe.getStatus()) {
                continue;
            }

            Integer recipeId = dbRecipe.getRecipeId();
            if (isSuccess) {
                Map<String, Object> attrMap = Maps.newHashMap();
                attrMap.put("giveDate", StringUtils.isEmpty(finishDTO.getDate()) ? DateTime.now().toDate() :
                        DateConversion.parseDate(finishDTO.getDate(), DateConversion.DEFAULT_DATE_TIME));
                attrMap.put("giveFlag", 1);
                attrMap.put("payFlag", 1);
                attrMap.put("payDate", DateTime.now().toDate());
                //更新处方信息
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
                if (rs) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

                    //完成订单，不需要检查订单有效性，就算失效的订单也直接变成已完成
                    orderService.finishOrder(dbRecipe.getOrderCode(),  null);
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(),
                            RecipeStatusConstant.FINISH, "处方单配送完成,配送人：" + finishDTO.getSender());
                    //HIS消息发送
                    hisService.recipeFinish(recipeId);

                    //date:20190919
                    //添加用户消息
                    Integer msgStatus = null;
                    if(RecipeBussConstant.GIVEMODE_TO_HOS.equals(dbRecipe.getGiveMode())){
                        msgStatus = RecipeStatusConstant.PATIENT_GETGRUG_FINISH;
                    }else if(RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(dbRecipe.getGiveMode())){
                        msgStatus = RecipeStatusConstant.PATIENT_REACHPAY_FINISH;
                    }else if(RecipeBussConstant.GIVEMODE_TFDS.equals(dbRecipe.getGiveMode())){
                        msgStatus = RecipeStatusConstant.RECIPE_TAKE_MEDICINE_FINISH;
                    }
                    if(null != msgStatus){
                        RecipeMsgService.batchSendMsg(dbRecipe, msgStatus);
                    }
                    CommonOrder.finishGetDrugUpdatePdf(recipeId);
                }

                //HOS处方发送完成短信
                if (RecipeBussConstant.FROMFLAG_HIS_USE == dbRecipe.getFromflag()) {
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_FINISH_4HIS, dbRecipe);
                }

            } else {
                //患者未取药
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_DRUG, null);
                if (rs) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    orderService.cancelOrderByCode(dbRecipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                }

                //记录日志
                RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), RecipeStatusConstant.NO_DRUG,
                        "处方单配送失败:" + finishDTO.getMsg());

                //发送消息(根据不同的取药方式发送不同的消息)
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, dbRecipe);
            }

        }

        result.setCode(StandardResultDTO.SUCCESS);
        LOGGER.info("finish 处理完成. param = {}", listStr);
        return result;
    }

    @RpcService
    public StandardResultDTO updatePrescription(List<UpdatePrescriptionDTO> list) {
        String listStr = JSONUtils.toString(list);
        LOGGER.info("updatePrescription param = {}", listStr);
        StandardResultDTO result = new StandardResultDTO();
        //默认为失败
        result.setCode(StandardResultDTO.FAIL);
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }

        Integer clinicOrgan = null;
        String recipeCode = null;
        for (UpdatePrescriptionDTO updatePrescriptionDTO : list) {
            try {
                Multimap<String, String> verifyMap = VerifyUtils.verify(updatePrescriptionDTO);
                if (!verifyMap.keySet().isEmpty()) {
                    result.setMsg(verifyMap.toString());
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("updatePrescription 参数对象异常数据，updatePrescriptionDTO={}", JSONUtils.toString(updatePrescriptionDTO), e);
                result.setMsg("参数对象异常数据");
                return result;
            }

            //转换组织结构编码
            try {
                clinicOrgan = getClinicOrganByOrganId(updatePrescriptionDTO.getOrganId(), updatePrescriptionDTO.getClinicOrgan());
            } catch (Exception e) {
                LOGGER.warn("updatePrescription 查询机构异常，organId={}", updatePrescriptionDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOGGER.warn("updatePrescription 平台未匹配到该组织机构编码，organId={}", updatePrescriptionDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            recipeCode = updatePrescriptionDTO.getRecipeCode();
            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, clinicOrgan);
            if (null == dbRecipe) {
                result.setMsg("[" + recipeCode + "]处方单不存在");
                return result;
            }

            Integer recipeId = dbRecipe.getRecipeId();
            //更新处方信息
            String recipeFeeStr = updatePrescriptionDTO.getRecipeFee();
            Map<String, Object> attrMap = Maps.newHashMap();
            Map<String, Object> orderAttrMap = Maps.newHashMap();
            if (StringUtils.isNotEmpty(recipeFeeStr)) {
                BigDecimal recipeFee = new BigDecimal(recipeFeeStr);
                attrMap.put("totalMoney", recipeFee);
                orderAttrMap.put("recipeFee", recipeFee);
                orderAttrMap.put("actualPrice", recipeFee.doubleValue());
            }

            boolean success = false;
            try {
                if (!attrMap.isEmpty()) {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, dbRecipe.getStatus(), attrMap);
                }
                //修改处方单详情
                updateRecipeDetainInfo(dbRecipe, updatePrescriptionDTO);
                if (!orderAttrMap.isEmpty()) {
                    //修改订单详情
                    orderDAO.updateByOrdeCode(dbRecipe.getOrderCode(), orderAttrMap);
                }
                success = true;
            } catch (Exception e) {
                LOGGER.warn("updatePrescription 处方更新异常, recipeCode={}", recipeCode, e);
            } finally {
                if (!success) {
                    result.setMsg("[" + recipeCode + "]药品信息更新异常");
                    return result;
                }
            }
        }

        result.setCode(StandardResultDTO.SUCCESS);
        LOGGER.info("updatePrescription 处理完成. param = {}", listStr);
        return result;
    }

    private Integer getClinicOrganByOrganId(String organId, String clinicOrgan) throws Exception {
        Integer co = null;
        if (StringUtils.isEmpty(clinicOrgan)) {
            IOrganService organService = BaseAPI.getService(IOrganService.class);

            List<OrganBean> organList = organService.findByOrganizeCode(organId);
            if (CollectionUtils.isNotEmpty(organList)) {
                co = organList.get(0).getOrganId();
            }
        } else {
            co = Integer.parseInt(clinicOrgan);
        }
        return co;
    }

    /**
     * 更新处方详细信息
     *
     * @param recipe
     * @param updatePrescriptionDTO
     */
    private void updateRecipeDetainInfo(Recipe recipe, UpdatePrescriptionDTO updatePrescriptionDTO) throws Exception {
        List<StandardRecipeDetailDTO> list = updatePrescriptionDTO.getDetails();
        if (CollectionUtils.isNotEmpty(list)) {
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            SaleDrugListDAO saleDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugsEnterpriseDAO depDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

            //获取药企ID
            DrugsEnterprise dep = depDAO.getByAccount(updatePrescriptionDTO.getAccount());
            if (null == dep) {
                throw new Exception("药企不存在");
            }

            List<String> drugCodeList = Lists.newArrayList(Collections2.transform(list, new Function<StandardRecipeDetailDTO, String>() {
                @Nullable
                @Override
                public String apply(@Nullable StandardRecipeDetailDTO input) {
                    return input.getDrugCode();
                }
            }));

            List<SaleDrugList> saleList = saleDAO.findByOrganIdAndDrugCodes(dep.getId(), drugCodeList);
            Map<Integer, StandardRecipeDetailDTO> mapInfo = Maps.newHashMap();
            for (SaleDrugList sale : saleList) {
                for (StandardRecipeDetailDTO dto : list) {
                    if (sale.getOrganDrugCode().equals(dto.getDrugCode())) {
                        mapInfo.put(sale.getDrugId(), dto);
                        break;
                    }
                }
            }

            if (mapInfo.isEmpty()) {
                LOGGER.warn("updateRecipeDetainInfo mapInfo is empty. depId={}, drugCodeList={}",
                        dep.getId(), JSONUtils.toString(drugCodeList));
                throw new Exception("药企维护数据异常");
            }

            List<Recipedetail> detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            StandardRecipeDetailDTO detailDTO;
            Integer drugId;
            String salePrice;
            String drugCost;
            String drugBatch;
            String validDate;
            Map<String, Object> changeAttr = Maps.newHashMap();
            for (Recipedetail detailInfo : detailList) {
                changeAttr.clear();
                drugId = detailInfo.getDrugId();
                detailDTO = mapInfo.get(drugId);
                if (null == detailDTO) {
                    continue;
                }
                //更新信息
                salePrice = detailDTO.getSalePrice();
                if (StringUtils.isNotEmpty(salePrice)) {
                    changeAttr.put("salePrice", new BigDecimal(salePrice));
                }
                drugCost = detailDTO.getDrugCost();
                if (StringUtils.isNotEmpty(drugCost)) {
                    changeAttr.put("drugCost", new BigDecimal(drugCost));
                }
                drugBatch = detailDTO.getDrugBatch();
                if (StringUtils.isNotEmpty(drugBatch)) {
                    changeAttr.put("drugBatch", drugBatch);
                }
                validDate = detailDTO.getValidDate();
                if (StringUtils.isNotEmpty(validDate)) {
                    changeAttr.put("validDate", DateConversion.parseDate(validDate, DateConversion.DEFAULT_DATE_TIME));
                }

                if (!changeAttr.isEmpty()) {
                    detailDAO.updateRecipeDetailByRecipeDetailId(detailInfo.getRecipeDetailId(), changeAttr);
                }
            }
        }
    }

    /**
     * 接收库存状态的通知
     * @param recipeCode     处方单编号
     * @param clinicOrgan    机构编码
     * @param status         该处方药品库存状态 0 无 1 有
     * @return               接收信息
     */
    @RpcService
    public StandardResultDTO receiveDrugStockStatus (String recipeCode, String clinicOrgan, Integer status) {
        StandardResultDTO result = new StandardResultDTO();
        if (StringUtils.isEmpty(recipeCode) || StringUtils.isEmpty(clinicOrgan) || status == null) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("入参不能为空");
            return result;
        }
        Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, Integer.parseInt(clinicOrgan));
        if (null == dbRecipe) {
            result.setMsg("[" + recipeCode + "]处方单不存在");
            return result;
        }
        LOGGER.info("接收到武昌推送药品库存状态,recipeCode:{},clinicOrgan:{},status:{}.", recipeCode, clinicOrgan, status);
        if (status == 0) {
            //发送没有库存消息
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_NOINVENTORY, dbRecipe);
            result.setCode(StandardResultDTO.SUCCESS);
        }
        if (status == 1) {
            //发送有库存消息
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_INVENTORY, dbRecipe);
            result.setCode(StandardResultDTO.SUCCESS);
        }
        return result;
    }

    /**
     * 3.1.平台处方物流信息更新接口
     * @param recipeLogisticsBean  物流相关信息
     * @return 更新结果
     */
    @RpcService
    public StandardResultDTO updateLogisticsInfo(RecipeLogisticsBean recipeLogisticsBean) {
        LOGGER.info("distributionService-updateLogisticsInfo, recipeLogisticsBean:{}.", JSONUtils.toString(recipeLogisticsBean));
        StandardResultDTO result = new StandardResultDTO();
        result.setCode(StandardResultDTO.SUCCESS);
        if (recipeLogisticsBean == null) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("入参不能为空");
        }
        if (StringUtils.isEmpty(recipeLogisticsBean.getRecipeId())) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("处方单ID不能为空");
        }

        if (recipeLogisticsBean.getLogistics() == null) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("物流信息不能为空");
        }
        return result;
    }

    @RpcService
    public StandardResultDTO updateStatusByPatientGetDrug(List<ChangeStatusByGetDrugDTO> list) {
        LOGGER.info("updateStatusByPatientGetDrug 药企调用接口更新状态. param = {}", list);
        StandardResultDTO result = new StandardResultDTO();
        result.setCode(StandardResultDTO.SUCCESS);
        if (CollectionUtils.isEmpty(list)) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("入参信息不能为空");
            return result;
        }
        //校验提交修改状态的处方信息是否和
        for (ChangeStatusByGetDrugDTO changeStatusByGetDrugDTO : list) {
            //校验药企传输数据安全性
            changeStatusByGetDrugDTO.checkRation(result);
            if (StandardResultDTO.FAIL == result.getCode()) {
                return result;
            }
        }
        //接口目的是为了设置患者确认购药方式后获取药品过程中药企设置处方/处方订单状态
        //现在有两种情况：
        for (ChangeStatusByGetDrugDTO changeStatus : list) {
            Recipe nowRecipe = recipeDAO.get(changeStatus.getRecipeId());
            if(null == nowRecipe){
                result.setMsg("[" + changeStatus.getRecipeId() + "]当前处方信息不存在！");
                result.setCode(StandardResultDTO.FAIL);
                return result;
            }
            switch (changeStatus.getInstallScene().intValue()){
                //第一种：取药失败时候，处方的状态设置为失败，同时设置recipeLog中设置失败的原因
                case GetDrugChangeStatusSceneConstant.Fail_Change_Recipe:
                    failChangeRecipe(changeStatus, result, nowRecipe);
                    break;
                //第二种：药店取药购药方式下，患者支付成功后，药店确认库存，库存足够为待取药（有库存）/库存不足为待取药（无库存）的订单状态
                case GetDrugChangeStatusSceneConstant.Only_Change_RecipeOrderStatus:
                    onlyChangeRecipeOrderStatus(changeStatus, nowRecipe);
                    break;
                default:
                    result.setMsg("[" + changeStatus.getInstallScene() + "]不支持该变更设置场景");
                    result.setCode(StandardResultDTO.FAIL);
                    break;
            }
        }

        LOGGER.info("updateStatusByPatientGetDrug 接口调用结束");
        return result;
    }
    /**
     * @method  onlyChangeRecipeOrderStatus
     * @description 只需要修改订单状态
     * @date: 2019/8/23
     * @author: JRK
     * @param changeStatus 药企方修改处方状态信息
     * @param result 请求结果
     * @param nowRecipe 当前的处方
     * @return void
     */
    private void onlyChangeRecipeOrderStatus(ChangeStatusByGetDrugDTO changeStatus, Recipe nowRecipe) {

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("status", changeStatus.getChangeStatus());
        //返回一个RecipeResultBean
        orderService.updateOrderInfo(nowRecipe.getOrderCode(), updateMap, null);

        //发送取药提示信息给用户
        //设置发送消息的内容
        ThirdChangeStatusMsgEnum msgEnum = ThirdChangeStatusMsgEnum.fromStatusAndChangeStatus(1, changeStatus.getChangeStatus());
        if(null != msgEnum){
            RecipeMsgService.batchSendMsg(nowRecipe, msgEnum.getMsgStatus());
        }
    }

    /**
     * @method  failChangeRecipe
     * @description 修改处方失败状态
     * @date: 2019/8/23
     * @author: JRK
     * @param changeStatus 药企方修改处方状态信息
     * @param result 请求结果
     * @param nowRecipe 当前的处方
     * @return void
     */
    private void failChangeRecipe(ChangeStatusByGetDrugDTO changeStatus, StandardResultDTO result, Recipe nowRecipe) {
        //修改处方的状态，为失败（失败有多种失败的情况状态）
        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(changeStatus.getRecipeId(), changeStatus.getChangeStatus(), null);
        if (rs) {
            //更新处方状态后，结束当前订单的状态
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            orderService.cancelOrderByCode(nowRecipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
        }

        //记录日志,处方的状态变更为失败的状态，记录失败的原因
        RecipeLogService.saveRecipeLog(changeStatus.getRecipeId(), nowRecipe.getStatus(), changeStatus.getChangeStatus(),
                 changeStatus.getFailureReason());

    }

    /**
     * 同步药企药品价格
     * @param readjustDrugDTOS      药企标识
     * @return             000 成功
     */
    @RpcService
    public List<StandardResultDTO> readjustDrugPrice(List<ReadjustDrugDTO> readjustDrugDTOS){
        LOGGER.info("StandardEnterpriseCallService-readjustDrugPrice readjustDrugDTOS:{}.", JSONUtils.toString(readjustDrugDTOS));
        List<StandardResultDTO> standardResultDTOS = new ArrayList<>();
        for (ReadjustDrugDTO readjustDrugDTO : readjustDrugDTOS) {
            StandardResultDTO result = new StandardResultDTO();
            result.setCode(StandardResultDTO.SUCCESS);
            String account = readjustDrugDTO.getAccount();
            Integer hospitalId = readjustDrugDTO.getHospitalId();
            String drugCode = readjustDrugDTO.getDrugCode();
            Double price = readjustDrugDTO.getPrice();
            if (StringUtils.isEmpty(account) && StringUtils.isNotEmpty(readjustDrugDTO.getAppKey())) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getByAppKey(readjustDrugDTO.getAppKey());
                if (enterprise == null) {
                    result.setCode(StandardResultDTO.FAIL);
                    result.setMsg("未查到对应的药企");
                    result.setData(readjustDrugDTO);
                    standardResultDTOS.add(result);
                    return standardResultDTOS;
                }
                SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                SaleDrugList saleDrugList = saleDrugListDAO.getByOrganIdAndDrugCode(enterprise.getId(), drugCode);
                if (saleDrugList == null) {
                    result.setCode(StandardResultDTO.FAIL);
                    result.setMsg("未查到待调价的药品");
                    result.setData(readjustDrugDTO);
                    standardResultDTOS.add(result);
                } else {
                    saleDrugList.setPrice(new BigDecimal(price));
                    saleDrugListDAO.update(saleDrugList);
                    result.setData(readjustDrugDTO);
                    standardResultDTOS.add(result);
                }
            } else {
                OrganAndDrugsepRelationDAO drugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                List<DrugsEnterprise> drugsEnterprises = drugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(hospitalId, 1);
                for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                    if (account.equals(drugsEnterprise.getAccount())) {
                        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                        SaleDrugList saleDrugList = saleDrugListDAO.getByOrganIdAndDrugCode(drugsEnterprise.getId(), drugCode);
                        if (saleDrugList == null) {
                            result.setCode(StandardResultDTO.FAIL);
                            result.setMsg("未查到待调价的药品");
                            result.setData(readjustDrugDTO);
                            standardResultDTOS.add(result);
                        } else {
                            saleDrugList.setPrice(new BigDecimal(price));
                            saleDrugListDAO.update(saleDrugList);
                            result.setData(readjustDrugDTO);
                            standardResultDTOS.add(result);
                        }
                    }
                }
            }

        }
        LOGGER.info("StandardEnterpriseCallService-readjustDrugPrice standardResultDTOS:{}.", JSONUtils.toString(standardResultDTOS));
        return standardResultDTOS;
    }

    @RpcService
    public StandardResultDTO recipeStatusupdate(RecipeStatusUpdateReqTO request) {
        LOGGER.info("distributionService-recipeStatusupdate request:{}.", JSONUtils.toString(request));
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        StandardResultDTO result = new StandardResultDTO();
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        OrganDTO organDTO = organService.getOrganByOrganizeCode(request.getOrganizeCode());
        if (organDTO == null) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("机构不存在");
            return result;
        }
        request.setOrganID(organDTO.getOrganId().toString());  //固定写死
        request.setOrganizeCode(organDTO.getOrganizeCode());
        request.setOuthospno("");
        request.setExplain("无");
        request.setRecipeRecordStatus(2);
        request.setRecipeStatus("1");
        Boolean flag = service.recipeUpdate(request);
        if (flag) {
            result.setCode(StandardResultDTO.SUCCESS);
        } else {
            result.setCode(StandardResultDTO.FAIL);
        }
        return result;
    }

}
