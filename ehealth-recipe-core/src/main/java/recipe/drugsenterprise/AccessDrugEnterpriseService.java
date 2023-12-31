package recipe.drugsenterprise;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseResponse;
import recipe.bean.RecipePayModeSupportBean;
import recipe.client.IConfigurationClient;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.enumerate.type.EnterpriseCreateTypeEnum;
import recipe.manager.EmrRecipeManager;
import recipe.manager.EnterpriseManager;
import recipe.service.RecipeOrderService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateDrugsEpCallable;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用药企对接服务(国药协议)
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/10/19.
 */
public abstract class AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessDrugEnterpriseService.class);

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private IConfigurationClient configurationClient = AppContextHolder.getBean("IConfigurationClient", IConfigurationClient.class);


    /**
     * 单个线程处理药企药品数量
     */
    protected static final int ONCETIME_DEAL_NUM = 100;

    public void updateAccessTokenById(Integer code, Integer depId) {
        Integer i = -2;
        if (i.equals(code)) {
            updateAccessToken(Arrays.asList(depId));
        }
    }

    /**
     * 获取药企AccessToken
     *
     * @param drugsEnterpriseIds
     * @return
     */
    public String updateAccessToken(List<Integer> drugsEnterpriseIds) {
        if (CollectionUtils.isNotEmpty(drugsEnterpriseIds)) {
            RecipeBusiThreadPool.execute(new UpdateDrugsEpCallable(drugsEnterpriseIds));
        }
        return null;
    }

    /**
     * 生成完整地址
     *
     * @param order 订单
     * @return
     */
    public String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order && StringUtils.isNotEmpty(order.getAddress1())) {
            this.getAddressDic(address, order.getAddress1());
            this.getAddressDic(address, order.getAddress2());
            this.getAddressDic(address, order.getAddress3());
            this.getAddressDic(address, order.getStreetAddress());
            if (StringUtils.isNotEmpty(order.getAddress5Text())) {
                address.append(order.getAddress5Text()).append(",");
            } else {
                address.append(",");
            }
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }

    /**
     * 作废（OrderManager getAddressDic）
     *
     * @param address
     * @param area
     */
    @Deprecated
    public void getAddressDic(StringBuilder address, String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                address.append(DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area)).append(",");
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
    }

    /**
     * 格式化Double
     *
     * @param d
     * @return
     */
    protected String getFormatDouble(Double d) {
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    /**
     * 某一列表分成多段
     *
     * @param osize
     * @return
     */
    protected int splitGroupSize(int osize) {
        return (int) Math.ceil(osize / Double.parseDouble(String.valueOf(ONCETIME_DEAL_NUM)));
    }

    /**
     * 更新token
     *
     * @param drugsEnterprise
     */
    public abstract void tokenUpdateImpl(DrugsEnterprise drugsEnterprise);

    /**
     * 获取互联网药企页面跳转地址
     *
     * @param drugsEnterprise
     */
    public void getJumpUrl(PurchaseResponse response, Recipe recipe, DrugsEnterprise drugsEnterprise) {

    }

    /**
     * 推送处方
     *
     * @param recipeIds  处方ID集合
     * @param enterprise
     * @return
     */
    public abstract DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise);

    /**
     * 通过前置机直接推送处方
     *
     * @param hospitalRecipeDTO 前置机传入的处方信息
     * @param enterprise        药企
     * @return 推送结果
     */
    public abstract DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise);


    /**
     * @param drugId          药品ID
     * @param drugsEnterprise 药企
     * @return 库存
     */
    public abstract String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId);

    public abstract List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag);

    /**
     * 查询药企库存
     *
     * @param recipe          处方
     * @param drugsEnterprise 药企
     * @param recipeDetails   处方详情
     * @return 库存信息
     */
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        LOGGER.info("scanEnterpriseDrugStock recipeDetails:{}", JSONUtils.toString(recipeDetails));
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        List<Integer> drugList = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(drugList)) {
            return new DrugStockAmountDTO();
        }
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugList);
        Map<Integer, Integer> saleMap = saleDrugLists.stream().collect(Collectors.toMap(SaleDrugList::getDrugId, SaleDrugList::getStatus));
        drugStockAmountDTO.setResult(true);
        List<DrugInfoDTO> drugInfoList = new ArrayList<>();
        recipeDetails.forEach(recipeDetail -> {
            DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
            BeanUtils.copyProperties(recipeDetail, drugInfoDTO);
            drugInfoDTO.setUseTotalDose(recipeDetail.getUseTotalDose());
            drugInfoDTO.setDrugName(recipeDetail.getDrugName());
            drugInfoDTO.setStock(false);
            drugInfoDTO.setStockAmountChin("无库存");
            if (new Integer(1).equals(saleMap.get(recipeDetail.getDrugId()))) {
                drugInfoDTO.setStock(true);
                drugInfoDTO.setStockAmountChin("有库存");
            }
            drugInfoList.add(drugInfoDTO);
        });
        this.setDrugStockAmountDTO(drugStockAmountDTO, drugInfoList);
        LOGGER.info("scanEnterpriseDrugStock drugStockAmountDTO:{}", JSONUtils.toString(drugStockAmountDTO));
        return drugStockAmountDTO;
    }

    /**
     * 库存检验
     *
     * @param drugsEnterprise 药企
     * @return
     */
    public DrugEnterpriseResult enterpriseStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        //todo 自建药企-查询药企库存默认有库存 如果要查询平台对接药企 需要实现其他实现类
        LOGGER.info("自建药企-查询药企库存默认有库存 {}", drugsEnterprise.getName());
        return DrugEnterpriseResult.getSuccess();
    }

    /**
     * 定时同步药企库存
     *
     * @param drugsEnterprise
     * @param drugIdList
     * @return
     */
    public abstract DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList);

    /**
     * 药师审核通过通知消息
     *
     * @param recipeId   处方ID
     * @param checkFlag  审核结果 1:审核通过 0:审核失败
     * @param enterprise
     * @return
     */
    public abstract DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise);

    /**
     * 查找供应商
     *
     * @param recipeIds
     * @param enterprise
     * @return
     */
    public abstract DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise);

    /**
     * @param rxId       处⽅Id
     * @param queryOrder 是否查询订单
     * @return 处方单
     */
    public DrugEnterpriseResult queryPrescription(String rxId, Boolean queryOrder) {
        return DrugEnterpriseResult.getSuccess();
    }


    /**
     * 推送药企处方状态，由于只是个别药企需要实现，故有默认实现
     *
     * @param rxId
     * @return
     */
    public DrugEnterpriseResult updatePrescriptionStatus(String rxId, int status) {
        return DrugEnterpriseResult.getSuccess();
    }

    /**
     * 获取药企实现简称字段
     *
     * @return
     */
    public abstract String getDrugEnterpriseCallSys();


    //当前处方为配送到家、到院取药的时候，当处方推送到药企后自建的药企需要推送短信消息给药企
    public static void pushMessageToEnterprise(List<Integer> recipeIds) {
        Integer recipeId;
        if (null != recipeIds && 0 < recipeIds.size()) {
            recipeId = recipeIds.get(0);
        } else {
            LOGGER.warn("当前推送的处方信息为空，无法推送消息！");
            return;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe nowRecipe = recipeDAO.get(recipeId);
        if (null != nowRecipe && null != nowRecipe.getEnterpriseId()) {

            //自建类型的药企需要给药企发送短信
            // 2021/11 新需求,非自建药企也要发送短信
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(nowRecipe.getEnterpriseId());
            if (drugsEnterprise != null && nowRecipe.getPayFlag() == 1) {
                LOGGER.info("pushMessageToEnterprise 当前处方[{}]需要推送订单消息给药企", recipeId);
                //给药企的电话号码推送短信
                EnterpriseManager enterpriseManager = AppContextHolder.getBean("enterpriseManager", EnterpriseManager.class);
                enterpriseManager.pushEnterpriseSendDrugPhone(nowRecipe, drugsEnterprise);
            }
        }
    }

    public String appEnterprise(RecipeOrder order) {
        String appEnterprise = null;
        if (null != order && order.getEnterpriseId() != null) {
            //设置配送方名称
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            appEnterprise = drugsEnterprise.getName();
        }
        LOGGER.info("appEnterprise 当前公用药企逻辑-返回的药企名为：{}", appEnterprise);
        return appEnterprise;
    }

    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo) {
        LOGGER.info("appEnterprise req order：{} extInfo:{}", JSONArray.toJSONString(order),JSONArray.toJSONString(extInfo));

        BigDecimal nowFee = recipeFee;
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        // 到院自取是否采用药企管理模式
        Boolean drugToHosByEnterprise = false;
        if (payModeSupport.isSupportToHos()) {
            drugToHosByEnterprise = configurationClient.getValueBooleanCatch(order.getOrganId(), "drugToHosByEnterprise", false);
        }
        if ((drugToHosByEnterprise || payModeSupport.isSupportCOD() || payModeSupport.isSupportTFDS() || payModeSupport.isSupportOnlinePay()) && null != order.getEnterpriseId()) {
            nowFee = orderService.reCalculateRecipeFee(order.getEnterpriseId(), recipeIds, null);
        } else {
            //不走药企管理模式
            updateSaleStrategy(recipeIds);
        }
        LOGGER.info("appEnterprise 当前公用药企逻辑-返回订单的处方费用为：{}", nowFee);
        return nowFee;
    }

    private void updateSaleStrategy(List<Integer> recipeIds){
        try {
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<Recipedetail> details = recipeDetailDAO.findByRecipeIds(recipeIds);
            details.forEach(recipeDetail -> {
                if (StringUtils.isNotEmpty(recipeDetail.getSaleUnit()) && null != recipeDetail.getSaleUseDose()) {
                    recipeDetail.setSaleUnit(null);
                    recipeDetail.setSaleUseDose(null);
                    recipeDetailDAO.update(recipeDetail);
                }
            });
        } catch (Exception e) {
            LOGGER.error("updateSaleStrategy error", e);
        }
    }


    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order) {
        //设置药企运费细则
        if (order.getEnterpriseId() != null) {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (drugsEnterprise != null) {
                order.setEnterpriseName(drugsEnterprise.getName());
                order.setTransFeeDetail(drugsEnterprise.getTransFeeDetail());
            }
        }
        LOGGER.info("setOrderEnterpriseMsg 当前公用药企逻辑-返回订单的药企信息：{}", JSONUtils.toString(order));
    }

    public void checkRecipeGiveDeliveryMsg(RecipeBean recipeBean, Map<String, Object> map) {
        LOGGER.info("checkRecipeGiveDeliveryMsg 当前公用药企逻辑-预校验，入参：recipeBean:{},map:{}", JSONUtils.toString(recipeBean), JSONUtils.toString(map));
    }

    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo) {
        order.setEnterpriseId(depId);
        LOGGER.info("当前公用药企逻辑-组装的订单：{}", JSONUtils.toString(order));
    }

    public Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe) {
        LOGGER.info("当前公用药企逻辑-判断个性化药企展示：drugsEnterprise：{}, dbRecipe:{}",
                JSONUtils.toString(drugsEnterprise), JSONUtils.toString(dbRecipe));
        return DrugEnterpriseConstant.COMPANY_HZ.equals(drugsEnterprise.getCallSys()) && dbRecipe.getRecipeCode().contains("ngari");
    }

    public DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult) {
        LOGGER.info("当前公用药企逻辑-确认订单前校验订单信息推送配送信息：dbRecipe：{}，extInfo:{},payResult:{}",
                recipeId, JSONUtils.toString(extInfo), JSONUtils.toString(payResult));
        return payResult;
    }

    public HisResponseTO doCancelRecipeForEnterprise(Recipe recipe){
        LOGGER.info("药企退费通知 recipe:{}", recipe.getRecipeId());
        return null;
    }

    /**
     * 药企公用获取新电子病历结构
     *
     * @param recipe
     * @param recipeExtend
     */
    protected void getMedicalInfo(Recipe recipe, RecipeExtend recipeExtend) {
        EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
    }

    /**
     * 药企公用获取新电子病历结构
     *
     * @param recipe
     */
    protected void getMedicalInfo(Recipe recipe) {
        if (null == recipe || null == recipe.getRecipeId()) {
            return;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
    }

    protected void setDrugStockAmountDTO(DrugStockAmountDTO drugStockAmountDTO, List<DrugInfoDTO> drugInfoList) {
        LOGGER.info("setDrugStockAmountDTO drugInfoList:{}.", JSONUtils.toString(drugInfoList));
        List<String> noDrugNames = Optional.ofNullable(drugInfoList).orElseGet(Collections::emptyList)
                .stream().filter(drugInfoDTO -> !drugInfoDTO.getStock()).map(DrugInfoDTO::getDrugName).collect(Collectors.toList());
        LOGGER.info("setDrugStockAmountDTO noDrugNames:{}", JSONUtils.toString(noDrugNames));
        if (CollectionUtils.isNotEmpty(noDrugNames)) {
            drugStockAmountDTO.setNotDrugNames(noDrugNames);
        }
        boolean stock = drugInfoList.stream().allMatch(DrugInfoDTO::getStock);
        drugStockAmountDTO.setResult(stock);
        drugStockAmountDTO.setDrugInfoList(drugInfoList);
        LOGGER.info("setDrugStockAmountDTO drugStockAmountDTO:{}", JSONUtils.toString(drugStockAmountDTO));
    }

    /**
     * 获取区域文本
     *
     * @param area 区域
     * @return 区域文本
     */
    protected String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
        return "";
    }

    protected void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
    }

}
