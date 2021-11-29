package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogInfo;
import recipe.bean.DrugEnterpriseResult;
import recipe.client.DrugStockClient;
import recipe.client.RevisitClient;
import recipe.constant.ErrorCode;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.dao.*;
import recipe.enumerate.type.EnterpriseCreateTypeEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.manager.ButtonManager;
import recipe.manager.EnterpriseManager;
import recipe.service.DrugListExtService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeOrderService;
import recipe.service.common.RecipeCacheService;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.util.AppContextHolder.getBean;

/**
 * 业务使用药企对接类，具体实现在CommonRemoteService
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/3/7.
 */
@RpcBean(value = "remoteDrugEnterpriseService")
public class RemoteDrugEnterpriseService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDrugEnterpriseService.class);

    private static final String COMMON_SERVICE = "commonRemoteService";

    @Autowired
    DrugListDAO drugListDAO;
    DrugListExtService drugListExtService = ApplicationUtils.getRecipeService(DrugListExtService.class, "drugList");

    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private EnterpriseManager enterpriseManager;

    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Resource
    private IDrugEnterpriseBusinessService drugEnterpriseBusinessService;
    @Resource
    private DrugStockClient drugStockClient;

    //手动推送给第三方
    @RpcService
    public void pushRecipeInfoForThirdSd(Integer recipeId, Integer depId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        enterpriseManager.pushRecipeInfoForThird(recipe, drugsEnterprise, 0);

    }

    /**
     * 推送处方
     *
     * @param recipeId 处方ID集合
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipeInfo(Integer recipeId) {
        DrugEnterpriseResult result = getServiceByRecipeId(recipeId);
        DrugsEnterprise enterprise = result.getDrugsEnterprise();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (enterprise != null && new Integer(1).equals(enterprise.getOperationType())) {
            //药企对应的service为空，则通过前置机进行推送
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            PushRecipeAndOrder pushRecipeAndOrder = enterpriseManager.getPushRecipeAndOrder(recipe, enterprise);
            LOGGER.info("pushSingleRecipeInfo pushRecipeAndOrder:{}.", JSONUtils.toString(pushRecipeAndOrder));
            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                if (new Integer(1).equals(recipeOrder.getPushFlag())) {
                    //表示已经推送
                    return result;
                }
            }
            HisResponseTO responseTO = recipeEnterpriseService.pushSingleRecipeInfo(pushRecipeAndOrder);
            LOGGER.info("pushSingleRecipeInfo responseTO:{}.", JSONUtils.toString(responseTO));
            if (responseTO != null && responseTO.isSuccess()) {
                //推送药企处方成功
                orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("pushFlag", 1), null);
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "纳里给" + enterprise.getName() + "推送处方成功");
                result.setCode(1);
                String prescId = (String) responseTO.getExtend().get("prescId");
                RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                if (StringUtils.isNotEmpty(prescId)) {
                    recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("rxid", prescId));
                }
            } else {
                orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("pushFlag", -1), null);
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "纳里给" + enterprise.getName() + "推送处方失败");
                result.setCode(0);
            }
        } else {
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
                if (recipe != null && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                    RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                    List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
                    result = result.getAccessDrugEnterpriseService().pushRecipeInfo(recipeIdList, enterprise);
                    if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
                        result.setDrugsEnterprise(enterprise);
                    }
                }
            }
        }

        // 非自建药企 推送短信 支付成功且非自建
        if (null != recipe && enterprise != null && PayFlagEnum.PAYED.getType().equals(recipe.getPayFlag()) && EnterpriseCreateTypeEnum.OTHER_SELF.getType().equals(enterprise.getCreateType())) {
            // 2021/11 新需求,非自建药企也要发送短信
                LOGGER.info("pushMessageToEnterprise 当前处方[{}]需要推送订单消息给药企", recipeId);
                //设置药企的电话号码
                String mobile = enterprise.getEnterprisePhone();

                if(StringUtils.isNotEmpty(mobile)) {
                    SmsInfoBean smsInfo = new SmsInfoBean();
                    smsInfo.setBusType("RecipeOrderCreate");
                    smsInfo.setSmsType("RecipeOrderCreate");
                    smsInfo.setBusId(recipeId);
                    smsInfo.setOrganId(0);

                    Map<String, Object> smsMap = Maps.newHashMap();

                    smsMap.put("mobile", mobile);

                    smsInfo.setExtendValue(JSONUtils.toString(smsMap));
                    ISmsPushService smsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);
                    smsPushService.pushMsgData2OnsExtendValue(smsInfo);
                    LOGGER.info("pushMessageToEnterprise 当前处方[{}]已推送药企[{}],订单消息", recipeId, recipe.getEnterpriseId());
                }
        }
        LOGGER.info("pushSingleRecipeInfo recipeId:{}, result:{}", recipeId, JSONObject.toJSONString(result));
        return result;
    }


    @RpcService
    @LogInfo
    public void uploadRecipePdfToHis(Integer recipeId) {
        enterpriseManager.uploadRecipePdfToHis(recipeId);
    }

    /**
     * 根据药企推送处方
     *
     * @param drugsEnterprise 药企
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        result.setAccessDrugEnterpriseService(getServiceByDep(drugsEnterprise));
        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
            result = result.getAccessDrugEnterpriseService().pushRecipe(hospitalRecipeDTO, drugsEnterprise);
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
                result.setDrugsEnterprise(drugsEnterprise);
            }
        }
        LOGGER.info("pushSingleRecipeInfo drugsEnterpriseName:{}, result:{}", drugsEnterprise.getName(), JSONUtils.toString(result));
        return result;
    }

    /**
     * 带药企ID进行推送
     *
     * @param recipeId
     * @param depId
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipeInfoWithDepId(Integer recipeId, Integer depId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise dep = null;
        if (null != depId) {
            dep = drugsEnterpriseDAO.get(depId);
            if (null != dep) {
                result.setAccessDrugEnterpriseService(getServiceByDep(dep));
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg("药企" + depId + "未找到");
            }
        } else {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setMsg("处方单" + recipeId + "未分配药企");
        }

        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
            result = result.getAccessDrugEnterpriseService().pushRecipeInfo(Collections.singletonList(recipeId), dep);
        }
        LOGGER.info("pushSingleRecipeInfoWithDepId recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    /**
     * 库存检验
     *
     * @param recipeId        处方ID
     * @param drugsEnterprise 药企
     * @return
     */
    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("scanStock recipeId:{}, drugsEnterprise:{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 0) {
            // 没有药品的药企还是不展示
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<Integer> drugIds = recipeDetailDAO.findDrugIdByRecipeId(recipeId);
            List<SaleDrugList> list = saleDrugListDAO.getByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
            if (Objects.isNull(list)) {
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
            Map<Integer, List<SaleDrugList>> collect = list.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
            Integer code = DrugEnterpriseResult.SUCCESS;
            for (Integer drugId : drugIds) {
                if (Objects.isNull(collect.get(drugId))) {
                    code = DrugEnterpriseResult.FAIL;
                }
            }
            //不需要校验库存
            result.setCode(code);
            return result;
        }

        //查询医院库存  药企配置：校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存
        //根据处方查询医院库存
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 3) {
            return HosscanStock(recipeId, drugsEnterprise);
        }

        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            ScanRequestBean scanRequestBean = getScanRequestBean(recipe, drugsEnterprise);
            LOGGER.info("scanStock scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
            HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
            LOGGER.info("scanStock responseTO:{}.", JSONUtils.toString(responseTO));
            if (responseTO != null && responseTO.isSuccess()) {
                result.setCode(DrugEnterpriseResult.SUCCESS);
                return result;
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
        }
        AccessDrugEnterpriseService drugEnterpriseService = null;
        if (null == drugsEnterprise) {
            //药企对象为空，则通过处方id获取相应药企实现
            DrugEnterpriseResult result1 = getServiceByRecipeId(recipeId);
            if (DrugEnterpriseResult.SUCCESS.equals(result1.getCode())) {
                drugEnterpriseService = result1.getAccessDrugEnterpriseService();
                drugsEnterprise = result1.getDrugsEnterprise();
            }
        } else {
            drugEnterpriseService = getServiceByDep(drugsEnterprise);
        }

        if (null != drugEnterpriseService) {
            result = drugEnterpriseService.scanStock(recipeId, drugsEnterprise);
        }
        LOGGER.info("scanStock recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return null;
    }

    private ScanRequestBean getScanRequestBean(Recipe recipe, DrugsEnterprise drugsEnterprise) {
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        List<ScanDrugListBean> scanDrugListBeans = new ArrayList<>();
        for (Recipedetail recipedetail : recipedetails) {
            ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                scanDrugListBean.setDrugCode(saleDrugList.getOrganDrugCode());
                scanDrugListBean.setTotal(recipedetail.getUseTotalDose().toString());
                scanDrugListBean.setUnit(recipedetail.getDrugUnit());
                scanDrugListBeans.add(scanDrugListBean);
            }
            scanDrugListBean.setDrugSpec(recipedetail.getDrugSpec());
            scanDrugListBean.setProducerCode(recipedetail.getProducerCode());
            scanDrugListBean.setProducer(recipedetail.getProducer());
            scanDrugListBean.setPharmacy(recipedetail.getPharmacyName());
            scanDrugListBean.setName(recipedetail.getSaleName());
            scanDrugListBean.setGname(recipedetail.getDrugName());
            try {
                RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
                if (revisitExDTO != null) {
                    scanDrugListBean.setChannelCode(revisitExDTO.getProjectChannel());
                }
            } catch (Exception e) {
                LOGGER.error("queryPatientChannelId error:", e);
            }
        }
        scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
        scanRequestBean.setOrganId(recipe.getClinicOrgan());

        LOGGER.info("getScanRequestBean scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
        return scanRequestBean;
    }

    @RpcService
    public String getDrugInventory(Integer depId, Integer drugId, Integer organId) {
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
        List<Recipedetail> recipeDetails = new LinkedList<>();
        organDrugLists.forEach(a -> {
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setOrganDrugCode(a.getOrganDrugCode());
            recipedetail.setDrugId(a.getDrugId());
            recipedetail.setUseTotalDose(1D);
            recipeDetails.add(recipedetail);
        });
        List<EnterpriseStock> list = drugEnterpriseBusinessService.enterpriseStockCheck(organId, recipeDetails, depId);
        if (CollectionUtils.isEmpty(list)) {
            return "无库存";
        }
        EnterpriseStock enterpriseStock = list.get(0);
        if (!enterpriseStock.getStock()) {
            return "无库存";
        }
        List<DrugInfoDTO> drugInfoList = enterpriseStock.getDrugInfoList();
        if (CollectionUtils.isEmpty(drugInfoList)) {
            return "无库存";
        }
        return drugInfoList.get(0).getStockAmountChin();
    }

    @Deprecated
    public String getDrugInventoryOld(Integer depId, Integer drugId, Integer organId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        LOGGER.info("getDrugInventory depId:{}, drugId:{}", depId, drugId);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        result.setAccessDrugEnterpriseService(getServiceByDep(drugsEnterprise));

        //查询医院库存  药企配置：校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存
        //根据药品id查询
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 3) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            //通过机构Id查找对应药品库存列表
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
            DrugInfoResponseTO response = drugListExtService.getHisDrugStock(organId, organDrugLists, null);
            if (null == response) {
                return "无库存";
            } else {
                //前置机返回0或者200
                if (Integer.valueOf(0).equals(response.getMsgCode()) || Integer.valueOf(200).equals(response.getMsgCode())) {
                    if (CollectionUtils.isEmpty(response.getData())) {
                        return "有库存";
                    } else {
                        List<DrugInfoTO> data = response.getData();
                        Double stockAmount = data.get(0).getStockAmount();
                        if (stockAmount != null) {
                            return BigDecimal.valueOf(stockAmount).toPlainString();
                        } else {
                            return "有库存";
                        }
                    }
                } else {
                    if (StringUtils.isNotEmpty(response.getNoInventoryTip())) {
                        return response.getNoInventoryTip();
                    }
                    return "无库存";
                }
            }
        }

        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            List<com.ngari.recipe.recipe.model.RecipeDetailBean> recipeDetailBeans = new ArrayList<>();
            com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean = new com.ngari.recipe.recipe.model.RecipeDetailBean();
            recipeDetailBean.setDrugId(drugId);
            recipeDetailBeans.add(recipeDetailBean);
            ScanRequestBean scanRequestBean = getDrugInventoryRequestBean(drugsEnterprise.getOrganId(), drugsEnterprise, recipeDetailBeans);
            LOGGER.info("getDrugInventory requestBean:{}.", JSONUtils.toString(scanRequestBean));
            HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
            LOGGER.info("getDrugInventory responseTO:{}.", JSONUtils.toString(responseTO));
            if (null != responseTO && responseTO.isSuccess()) {
                String inventor = (String) responseTO.getExtend().get("inventor");
                if (StringUtils.isEmpty(inventor)) {
                    return "有库存";
                }
                return inventor;
            } else {
                return "无库存";
            }
        } else {
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
                return result.getAccessDrugEnterpriseService().getDrugInventory(drugId, drugsEnterprise, organId);
            } else {
                return "无库存";
            }
        }

    }


    /**
     * ;
     * 医生端展示药品库存情况
     *
     * @param drugsDataBean 药品数据
     * @return 药品数据
     */
    @RpcService
    public List getDrugsEnterpriseInventory(DrugsDataBean drugsDataBean) {
        LOGGER.info("getDrugsEnterpriseInventory drugsDataBean:{}.", JSONUtils.toString(drugsDataBean));
        List result = new ArrayList();
        Map payOnlineType = new HashMap();
        Map payShowSendToHosType = new HashMap();
        Map toStoreType = new HashMap();
        Map toHosType = new HashMap();
        Map downLoadType = new HashMap();
        DrugEnterpriseResult drugEnterpriseResult = DrugEnterpriseResult.getSuccess();
        //通过前置机调用
        IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(drugsDataBean.getOrganId());
        if (organDTO == null) {
            throw new DAOException("没有查询到机构信息");
        }
        //根据机构获取该机构配置的药企,需要查出药企支持的类型
        OrganAndDrugsepRelationDAO drugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = drugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(drugsDataBean.getOrganId(), 1);

        GiveModeShowButtonDTO giveModeShowButtonVO = buttonManager.getGiveModeSettingFromYypt(drugsDataBean.getOrganId());
        Map configurations = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));

        Map<String, List> supportOnlineMap;
        List supportOnlineList = new ArrayList();
        Map<String, List> supportSendToHosMap;
        List supportSendToHosList = new ArrayList();
        Map<String, List> toStoreMap;
        List toStoreList = new ArrayList();
        List<String> haveInventoryForOnlineList;
        List<String> haveInventoryForStoreList;
        //查找非自建药企配送主体为药企的药企
        if (configurations.containsKey("showSendToEnterprises") || configurations.containsKey("supportTFDS") || configurations.containsKey("showSendToHos")) {
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                //药企配送
                if (new Integer(2).equals(drugsEnterprise.getSendType())) {
                    supportOnlineMap = new LinkedHashMap<>();
                    drugEnterpriseResult.setAccessDrugEnterpriseService(this.getServiceByDep(drugsEnterprise));
                    if (payModeSupport(drugsEnterprise, 1) && configurations.containsKey("showSendToEnterprises")) {
                        //获取医院或者药企库存（看配置）
                        haveInventoryForOnlineList = compareGetHaveDrugInventoryForApp(drugsEnterprise, result, drugEnterpriseResult, drugsDataBean, recipeEnterpriseService, 1);
                        LOGGER.info("getDrugsEnterpriseInventory haveInventoryForOnlineList:{}.", JSONUtils.toString(haveInventoryForOnlineList));
                        if (CollectionUtils.isNotEmpty(haveInventoryForOnlineList)) {
                            supportOnlineMap.put(drugsEnterprise.getName(), haveInventoryForOnlineList);
                            supportOnlineList.add(supportOnlineMap);
                        }
                    }
                } else {
                    //医院配送
                    supportSendToHosMap = new LinkedHashMap<>();
                    drugEnterpriseResult.setAccessDrugEnterpriseService(this.getServiceByDep(drugsEnterprise));
                    if (payModeSupport(drugsEnterprise, 1) && configurations.containsKey("showSendToHos")) {
                        //获取医院或者药企库存（看配置）
                        haveInventoryForOnlineList = compareGetHaveDrugInventoryForApp(drugsEnterprise, result, drugEnterpriseResult, drugsDataBean, recipeEnterpriseService, 1);
                        if (CollectionUtils.isNotEmpty(haveInventoryForOnlineList)) {
                            supportSendToHosMap.put(drugsEnterprise.getName(), haveInventoryForOnlineList);
                            supportSendToHosList.add(supportSendToHosMap);
                        }
                    }
                }

                toStoreMap = new LinkedHashMap<>();
                if (payModeSupport(drugsEnterprise, 3) && configurations.containsKey("supportTFDS")) {
                    //该药企配置了这个药品,可以查询该药品在药企是否有库存了
                    //获取医院或者药企库存（看配置）
                    List<Integer> drugList = drugsDataBean.getRecipeDetailBeans().stream().map(RecipeDetailBean::getDrugId).collect(Collectors.toList());
                    if (null != drugsEnterprise) {
                        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIdsEffectivity(drugsEnterprise.getId(), drugList);
                        List<Integer> drugLists = saleDrugLists.stream().map(SaleDrugList::getDrugId).collect(Collectors.toList());
                        List<RecipeDetailBean> recipeDetails = drugsDataBean.getRecipeDetailBeans().stream().filter(recipeDetail -> drugLists.contains(recipeDetail.getDrugId())).collect(Collectors.toList());
                        drugsDataBean.setRecipeDetailBeans(recipeDetails);
                    }
                    haveInventoryForStoreList = compareGetHaveDrugInventoryForApp(drugsEnterprise, result, drugEnterpriseResult, drugsDataBean, recipeEnterpriseService, 2);
                    if (CollectionUtils.isNotEmpty(haveInventoryForStoreList)) {
                        toStoreMap.put(drugsEnterprise.getName(), haveInventoryForStoreList);
                        toStoreList.add(toStoreMap);
                    }
                }
            }

            if (StringUtils.isNotEmpty(drugsDataBean.getNewVersionFlag())) {
                //表示为新版的请求
                if (CollectionUtils.isNotEmpty(supportOnlineList)) {
                    payOnlineType.put("supportKey", "showSendToEnterprises");
                    payOnlineType.put(configurations.get("showSendToEnterprises"), supportOnlineList);
                    result.add(payOnlineType);
                }
                if (CollectionUtils.isNotEmpty(supportSendToHosList)) {
                    payShowSendToHosType.put("supportKey", "showSendToHos");
                    payShowSendToHosType.put(configurations.get("showSendToHos"), supportSendToHosList);
                    result.add(payShowSendToHosType);
                }
                if (CollectionUtils.isNotEmpty(toStoreList)) {
                    toStoreType.put("supportKey", "supportTFDS");
                    toStoreType.put(configurations.get("supportTFDS"), toStoreList);
                    result.add(toStoreType);
                }
            } else {
                supportOnlineList.addAll(supportSendToHosList);
                if (CollectionUtils.isNotEmpty(supportOnlineList)) {
                    payOnlineType.put("配送到家", supportOnlineList);
                    result.add(payOnlineType);
                }
                if (CollectionUtils.isNotEmpty(toStoreList)) {
                    toStoreType.put("药店取药", toStoreList);
                    result.add(toStoreType);
                }
            }
        }
        if (configurations.containsKey("supportToHos")) {
            IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
            if (iHisConfigService.isHisEnable(drugsDataBean.getOrganId())) {
                //到院取药,需要验证HIS
                List<String> list = new ArrayList<>();
                //获取医院库存，把有库存的药放到list中
                getHosDrugInventory(drugsDataBean, list);
                Map<String, List> map = new HashMap<>();
                map.put("", list);
                List toHosList = new ArrayList();
                toHosList.add(map);
                if (StringUtils.isNotEmpty(drugsDataBean.getNewVersionFlag())) {
                    toHosType.put("supportKey", "supportToHos");
                    toHosType.put(configurations.get("supportToHos"), toHosList);
                } else {
                    toHosType.put("到院取药", toHosList);
                }
                if (CollectionUtils.isNotEmpty(list)) {
                    result.add(toHosType);
                }
            }
        }
        if (configurations.containsKey("supportDownload")) {
            //下载处方,只要配置这个开关,默认都支持
            List<String> list = new ArrayList<>();
            for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                list.add(recipeDetailBean.getDrugName());
            }
            Map<String, List> map = new HashMap<>();
            map.put("", list);
            List downList = new ArrayList();
            downList.add(map);
            if (StringUtils.isNotEmpty(drugsDataBean.getNewVersionFlag())) {
                downLoadType.put("supportKey", "supportDownload");
                downLoadType.put(configurations.get("supportDownload"), downList);
            } else {
                downLoadType.put("下载处方", downList);
            }

            // 查询药品是否不支持下载处方
            Set<Integer> drugIds = drugsDataBean.getRecipeDetailBeans().stream().collect(Collectors.groupingBy(com.ngari.recipe.recipe.model.RecipeDetailBean::getDrugId)).keySet();
            Integer integer = organDrugListDAO.countIsSupperDownloadRecipeByDrugIds(drugsDataBean.getOrganId(), drugIds);

            if (CollectionUtils.isNotEmpty(list) || integer == 0) {
                result.add(downLoadType);
            }
        }
        return result;
    }

    private static boolean isBelongHos(OrganDrugList organDrugList) {
        if (organDrugList != null && StringUtils.isNotEmpty(organDrugList.getPharmacy())) {
            PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
            if (StringUtils.isNotEmpty(organDrugList.getPharmacy())) {
                String[] pharmacys = organDrugList.getPharmacy().split(",");
                for (String pharmacy : pharmacys) {
                    PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(Integer.parseInt(pharmacy));
                    if (pharmacyTcm != null && "院外药房".equals(pharmacyTcm.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取医院库存，把有库存的药放到list中
     *
     * @param drugsDataBean
     * @param list
     */
    private void getHosDrugInventory(DrugsDataBean drugsDataBean, List<String> list) {
        try{
            List<Recipedetail> detailList = new LinkedList<>();
            List<OrganDrugList> organDrugLists = new LinkedList<>();
            drugsDataBean.getRecipeDetailBeans().forEach(recipeDetailBean -> {
                Recipedetail recipeDetail = ObjectCopyUtils.convert(recipeDetailBean, Recipedetail.class);
                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(drugsDataBean.getOrganId(), recipeDetailBean.getOrganDrugCode(), recipeDetailBean.getDrugId());
                if (organDrugList != null && !isBelongHos(organDrugList)) {
                    recipeDetail.setPack(organDrugList.getPack());
                    recipeDetail.setDrugUnit(organDrugList.getUnit());
                    recipeDetail.setProducerCode(organDrugList.getProducerCode());
                    detailList.add(recipeDetail);
                    organDrugLists.add(organDrugList);
                }
            });
            DrugStockAmountDTO drugStockAmountDTO = drugStockClient.scanDrugStock(detailList, drugsDataBean.getOrganId(), organDrugLists, new LinkedList<>());
            LOGGER.info("getHosDrugInventory drugStockAmountDTO:{}.", JSONUtils.toString(drugStockAmountDTO));
            List<String> haveInventoryNames = drugStockAmountDTO.getDrugInfoList().stream().filter(DrugInfoDTO::getStock).filter(a->StringUtils.isNotEmpty(a.getDrugName())).map(DrugInfoDTO::getDrugName).collect(Collectors.toList());
            LOGGER.info("getHosDrugInventory haveInventoryNames:{}.", JSONUtils.toString(haveInventoryNames));
            if (CollectionUtils.isEmpty(haveInventoryNames)) {
                List<String> haveInventoryCodes = drugStockAmountDTO.getDrugInfoList().stream().filter(DrugInfoDTO::getStock).map(DrugInfoDTO::getOrganDrugCode).distinct().collect(Collectors.toList());
                LOGGER.info("getHosDrugInventory haveInventoryCodes:{}.", JSONUtils.toString(haveInventoryCodes));
                haveInventoryNames = organDrugLists.stream().filter(organDrugList -> haveInventoryCodes.contains(organDrugList.getOrganDrugCode())).map(OrganDrugList::getDrugName).collect(Collectors.toList());
            }
            list.addAll(haveInventoryNames);
        }catch (Exception e){
            LOGGER.error("getHosDrugInventory error", e);
        }
    }

    /**
     * 判断药企的校验库存方式获取库存
     *
     * @param drugsEnterprise
     * @param drugEnterpriseResult
     * @param drugsDataBean
     * @param recipeEnterpriseService
     * @param flag                    是否有药店  1否 2是
     */
    private List compareGetHaveDrugInventoryForApp(DrugsEnterprise drugsEnterprise, List result, DrugEnterpriseResult drugEnterpriseResult, DrugsDataBean drugsDataBean, IRecipeEnterpriseService recipeEnterpriseService, Integer flag) {
        List haveInventoryList = Lists.newArrayList();
        //校验药品库存标志 0 不需要校验  1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存
        //校验医院库存
        if (new Integer(3).equals(drugsEnterprise.getCheckInventoryFlag())) {
            getHosDrugInventory(drugsDataBean, haveInventoryList);
        }
        //不需要校验库存
        else if (new Integer(0).equals(drugsEnterprise.getCheckInventoryFlag())) {
            List<com.ngari.recipe.recipe.model.RecipeDetailBean> recipeDetailBeans = drugsDataBean.getRecipeDetailBeans();
            // 药企如果没有其中一个药品,就不展示
            List<Integer> drugIds = recipeDetailBeans.stream().map(com.ngari.recipe.recipe.model.RecipeDetailBean::getDrugId).collect(Collectors.toList());
            List<SaleDrugList> list = saleDrugListDAO.getByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
            if (Objects.isNull(list)) {
                return haveInventoryList;
            }
            Map<Integer, List<SaleDrugList>> collect = list.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
            for (Integer drugId : drugIds) {
                if (Objects.isNull(collect.get(drugId))) {
                    return haveInventoryList;
                }
            }
            for (com.ngari.recipe.recipe.model.RecipeDetailBean drugresult : recipeDetailBeans) {
                haveInventoryList.add(drugresult.getDrugName());
            }
            return haveInventoryList;
        } else {
            //校验药企库存
            //该机构配制配送并且药企支持配送或者药店取药,校验该药企是否支持药品
            //该药企配置了这个药品,可以查询该药品在药企是否有库存了
            if (new Integer(1).equals(drugsEnterprise.getOperationType())) {
                for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                    ScanRequestBean scanRequestBean = getDrugInventoryRequestBean(drugsDataBean.getOrganId(), drugsEnterprise, Arrays.asList(recipeDetailBean));
                    if (CollectionUtils.isNotEmpty(scanRequestBean.getScanDrugListBeans())) {
                        LOGGER.info("getDrugsEnterpriseInventory requestBean:{}.", JSONUtils.toString(scanRequestBean));
                        HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
                        LOGGER.info("getDrugsEnterpriseInventory responseTO:{}.", JSONUtils.toString(responseTO));
                        if (responseTO != null && responseTO.isSuccess()) {
                            haveInventoryList.add(recipeDetailBean.getDrugName());
                        }
                    }
                };
            } else {//通过平台调用
                if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode()) && null != drugEnterpriseResult.getAccessDrugEnterpriseService()) {
                    haveInventoryList = drugEnterpriseResult.getAccessDrugEnterpriseService().getDrugInventoryForApp(drugsDataBean, drugsEnterprise, flag);
                    LOGGER.info("compareGetHaveDrugInventoryForApp haveInventoryList:{}.", JSONUtils.toString(haveInventoryList));
                }
            }
        }
        return haveInventoryList;
    }

    /**
     * 查询药企是否支持指定的购药方式
     *
     * @param drugsEnterprise 药企
     * @param type            支持类型
     * @return 是否支持
     */
    public static boolean payModeSupport(DrugsEnterprise drugsEnterprise, Integer type) {
        Integer[] online_pay = {RecipeBussConstant.DEP_SUPPORT_ONLINE, RecipeBussConstant.DEP_SUPPORT_COD, RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS,
                RecipeBussConstant.DEP_SUPPORT_COD_TFDS, RecipeBussConstant.DEP_SUPPORT_COD, RecipeBussConstant.DEP_SUPPORT_ALL};
        List<Integer> online_pay_list = Arrays.asList(online_pay);
        Integer[] to_tfds = {RecipeBussConstant.DEP_SUPPORT_TFDS, RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS, RecipeBussConstant.DEP_SUPPORT_COD_TFDS,
                RecipeBussConstant.DEP_SUPPORT_ALL};
        List<Integer> to_tfds_list = Arrays.asList(to_tfds);
        if (new Integer(1).equals(type)) {
            //支持配送
            return online_pay_list.contains(drugsEnterprise.getPayModeSupport());
        } else if (new Integer(2).equals(type)) {
            //支持到院取药
            if ("commonSelf".equals(drugsEnterprise.getCallSys())) {
                return true;
            } else {
                return false;
            }
        } else if (new Integer(3).equals(type)) {
            //支持药店取药
            return to_tfds_list.contains(drugsEnterprise.getPayModeSupport());
        } else {
            return true;
        }
    }

    @RpcService
    public Boolean isShowSendTypeButton(Integer organId) {
        Boolean flag = false;
        GiveModeShowButtonDTO giveModeShowButtonVO = buttonManager.getGiveModeSettingFromYypt(organId);
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonBean giveModeButtonBean = (GiveModeButtonBean) iterator.next();
            if ("supportMedicalPayment".equals(giveModeButtonBean.getShowButtonKey())) {
                iterator.remove();
            }
        }
        if (CollectionUtils.isNotEmpty(giveModeButtonBeans)) {
            flag = true;
        }
        return flag;
    }

    /**
     * 封装前置机所需参数
     *
     * @param organId
     * @param drugsEnterprise
     * @return
     */
    private ScanRequestBean getDrugInventoryRequestBean(Integer organId, DrugsEnterprise drugsEnterprise, List<com.ngari.recipe.recipe.model.RecipeDetailBean> recipeDetailBeans) {
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        try {
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            String total = recipeParameterDao.getByName(organId + "_drugsEnterprise_num");
            List<ScanDrugListBean> scanDrugListBeans = new ArrayList<>();
            for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
                if (saleDrugList != null) {
                    ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
                    scanDrugListBean.setDrugCode(saleDrugList.getOrganDrugCode());
                    if (StringUtils.isNotEmpty(total)) {
                        scanDrugListBean.setTotal(total);
                    } else {
                        scanDrugListBean.setTotal("5");
                    }
                    if (organId != null && organId < 0) {
                        DrugList drugList = drugListDAO.getById(recipeDetailBean.getDrugId());
                        scanDrugListBean.setUnit(drugList.getUnit());
                    } else {
                        LOGGER.info("getDrugInventoryRequestBean recipeDetailBean{},organId= {}", JSON.toJSONString(recipeDetailBean), organId);
                        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipeDetailBean.getDrugId(), organId);
                        if (CollectionUtils.isNotEmpty(organDrugLists)) {
                            scanDrugListBean.setUnit(organDrugLists.get(0).getUnit());
                            scanDrugListBean.setDrugSpec(organDrugLists.get(0).getDrugSpec());
                            scanDrugListBean.setProducerCode(organDrugLists.get(0).getProducerCode());
                            scanDrugListBean.setPharmacy(organDrugLists.get(0).getPharmacy());
                        }
                    }
                    scanDrugListBeans.add(scanDrugListBean);
                }
            }
            scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
            scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
            LOGGER.info("getDrugInventoryRequestBean :{}.", JSONUtils.toString(scanRequestBean));
        } catch (Exception e) {
            LOGGER.info("getDrugInventoryRequestBean error: {}", e.getMessage(), e);
        }
        scanRequestBean.setOrganId(organId);
        return scanRequestBean;
    }

    /**
     * 药师审核通过通知消息
     *
     * @param recipeId  处方ID
     * @param checkFlag 审核结果
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag) {
        DrugEnterpriseResult result = getServiceByRecipeId(recipeId);
        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
            result = result.getAccessDrugEnterpriseService().pushCheckResult(recipeId, checkFlag, result.getDrugsEnterprise());
        }
        LOGGER.info("pushCheckResult recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    /**
     * 查找供应商
     *
     * @param recipeIds 处方列表
     * @param ext       额外信息
     * @return 供应商信息
     */
    @Override
    @RpcService
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            ScanRequestBean scanRequestBean = getScanRequestBean(recipe, drugsEnterprise);
            scanRequestBean.setExt(ext);
            if (recipeExtend != null && StringUtils.isNotEmpty(recipeExtend.getRxid())) {
                scanRequestBean.setRxid(recipeExtend.getRxid());
            }
            LOGGER.info("findSupportDep 发给前置机入参:{}.", JSONUtils.toString(scanRequestBean));
            List<DepDetailBean> depDetailBeans = recipeEnterpriseService.findSupportDep(scanRequestBean);
            LOGGER.info("findSupportDep 前置机出参:{}.", JSONUtils.toString(depDetailBeans));
            result.setObject(ObjectCopyUtils.convert(depDetailBeans, com.ngari.recipe.drugsenterprise.model.DepDetailBean.class));
            return result;
        }
        if (CollectionUtils.isNotEmpty(recipeIds) && null != drugsEnterprise) {
            AccessDrugEnterpriseService drugEnterpriseService = this.getServiceByDep(drugsEnterprise);
            result = drugEnterpriseService.findSupportDep(recipeIds, ext, drugsEnterprise);
            LOGGER.info("findSupportDep recipeIds={}, DrugEnterpriseResult={}", JSONUtils.toString(recipeIds), JSONUtils.toString(result));
        } else {
            LOGGER.warn("findSupportDep param error. recipeIds={}, drugsEnterprise={}", JSONUtils.toString(recipeIds), JSONUtils.toString(drugsEnterprise));
        }

        return result;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return null;
    }

    /**
     * 药品库存同步
     *
     * @return
     */
    @RpcService
    public DrugEnterpriseResult syncEnterpriseDrug() {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);

        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(1);
        if (CollectionUtils.isNotEmpty(drugsEnterpriseList)) {
            AccessDrugEnterpriseService drugEnterpriseService;
            for (DrugsEnterprise drugsEnterprise : drugsEnterpriseList) {
                if (null != drugsEnterprise) {
                    List<Integer> drugIdList = saleDrugListDAO.findSynchroDrug(drugsEnterprise.getId());
                    if (CollectionUtils.isNotEmpty(drugIdList)) {
                        drugEnterpriseService = this.getServiceByDep(drugsEnterprise);
                        if (null != drugEnterpriseService) {
                            LOGGER.info("syncDrugTask 开始同步药企[{}]药品，药品数量[{}]", drugsEnterprise.getName(), drugIdList.size());
                            drugEnterpriseService.syncEnterpriseDrug(drugsEnterprise, drugIdList);
                        }
                    } else {
                        LOGGER.warn("syncDrugTask 药企[{}]无可同步药品.", drugsEnterprise.getName());
                    }
                }
            }
        }

        return result;
    }


    @Override
    @RpcService
    public void updateAccessTokenById(Integer code, Integer depId) {
        AccessDrugEnterpriseService drugEnterpriseService = getBean(COMMON_SERVICE, AccessDrugEnterpriseService.class);
        drugEnterpriseService.updateAccessTokenById(code, depId);
    }

    @Override
    public String updateAccessToken(List<Integer> drugsEnterpriseIds) {
        AccessDrugEnterpriseService drugEnterpriseService = getBean(COMMON_SERVICE, AccessDrugEnterpriseService.class);
        return drugEnterpriseService.updateAccessToken(drugsEnterpriseIds);
    }

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return null;
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @RpcService
    @LogInfo
    public void updateAccessTokenByDep(DrugsEnterprise drugsEnterprise) {
        try {
            AccessDrugEnterpriseService service = getServiceByDep(drugsEnterprise);
            service.tokenUpdateImpl(drugsEnterprise);
        } catch (Exception e) {
            LOGGER.error("updateAccessTokenByDep drugsEnterprise:{}, error ", drugsEnterprise.getId(), e);
        }
    }

    /**
     * 根据单个处方ID获取具体药企实现
     *
     * @param recipeId
     * @return
     */
    public static DrugEnterpriseResult getServiceByRecipeId(Integer recipeId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (null == recipeId) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setMsg("处方ID为空");
        }

        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            //PS:药企ID取的是订单表的药企ID
            Integer depId = recipeOrderDAO.getEnterpriseIdByRecipeId(recipeId);
            if (depId == null) {
                depId = recipeDAO.getByRecipeId(recipeId).getEnterpriseId();
            }
            if (null != depId) {
                DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
                if (null != dep) {
                    result.setAccessDrugEnterpriseService(getServiceByDep(dep));
                    result.setDrugsEnterprise(dep);
                } else {
                    result.setCode(DrugEnterpriseResult.FAIL);
                    result.setMsg("药企" + depId + "未找到");
                }
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg("处方单" + recipeId + "未分配药企");
            }
        }

        LOGGER.info("getServiceByRecipeId recipeId:{}, result:{}", recipeId, result.toString());
        return result;
    }

    /**
     * 通过药企实例获取具体实现
     *
     * @param drugsEnterprise
     * @return
     */
    public static AccessDrugEnterpriseService getServiceByDep(DrugsEnterprise drugsEnterprise) {
        AccessDrugEnterpriseService drugEnterpriseService = null;
        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            return ApplicationUtils.getService(RemoteDrugEnterpriseService.class, "remoteDrugEnterpriseService");
        }
        if (null != drugsEnterprise) {
            //先获取指定实现标识，没有指定则根据帐号名称来获取
            String callSys = StringUtils.isEmpty(drugsEnterprise.getCallSys()) ? drugsEnterprise.getAccount() : drugsEnterprise.getCallSys();
            String beanName = COMMON_SERVICE;
            if (StringUtils.isNotEmpty(callSys)) {
                beanName = callSys + "RemoteService";
            }
            try {
                LOGGER.info("getServiceByDep 获取[{}]协议实现.service=[{}]", drugsEnterprise.getName(), beanName);
                drugEnterpriseService = getBean(beanName, AccessDrugEnterpriseService.class);
            } catch (Exception e) {
                LOGGER.warn("getServiceByDep 未找到[{}]药企实现，使用通用协议处理. beanName={}", drugsEnterprise.getName(), beanName, e);
                drugEnterpriseService = getBean(COMMON_SERVICE, AccessDrugEnterpriseService.class);
            }
        }

        return drugEnterpriseService;
    }


    /**
     * 获取药企帐号
     *
     * @param depId
     * @return
     */
    public String getDepAccount(Integer depId) {
        if (null == depId) {
            return null;
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return drugsEnterpriseDAO.getAccountById(depId);
    }

    /**
     * 获取钥世圈订单详情URL
     *
     * @param recipe
     * @return
     */
    public String getYsqOrderInfoUrl(Recipe recipe) {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        String backUrl = "";
        String ysqUrl = cacheService.getParam(ParameterConstant.KEY_YSQ_SKIP_URL);
        if (RecipeStatusConstant.FINISH != recipe.getStatus()) {
            backUrl = ysqUrl + "Order/Index?id=0&inbillno=" + recipe.getClinicOrgan() + YsqRemoteService.YSQ_SPLIT + recipe.getRecipeCode();
        }
        return backUrl;
    }

    /**
     * 获取运费
     *
     * @return
     */
    @RpcService
    public Map<String, Object> getExpressFee(Map<String, Object> parames) {
        LOGGER.info("getExpressFee parames:{}.", JSONUtils.toString(parames));
        Map<String, Object> result = new HashMap<>();
        if (parames == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "获取运费参数不能为空");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        Integer depId = (Integer) parames.get("depId"); //获取药企ID
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (drugsEnterprise == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "获取药企失败");
        }
        if (new Integer(1).equals(drugsEnterprise.getExpressFeeType())) {
            //此时运费为从第三方获取
            Integer recipeId = (Integer) parames.get("recipeId"); //获取处方ID
            String provinceCode = (String) parames.get("provinceCode"); //获取省份编码
            String province = (String) parames.get("province"); //获取省份
            String city = (String) parames.get("city"); //获取市
            String district = (String) parames.get("district"); //获取区县
            String address = (String) parames.get("address");
            String recvMobilePhone = (String) parames.get("phone");
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            String rxid = recipeExtend.getRxid();
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            EnterpriseResTo enterpriseResTo = new EnterpriseResTo();
            enterpriseResTo.setOrganId(recipe.getClinicOrgan());
            enterpriseResTo.setDepId(depId.toString());
            enterpriseResTo.setRid(rxid);
            enterpriseResTo.setProvince(province);
            enterpriseResTo.setCity(city);
            enterpriseResTo.setDistrict(district);
            enterpriseResTo.setProvinceCode(provinceCode + "0000");
            enterpriseResTo.setAddress(address);
            enterpriseResTo.setRecvMobilePhone(recvMobilePhone);
            LOGGER.info("getExpressFee enterpriseResTo:{}.", JSONUtils.toString(enterpriseResTo));
            HisResponseTO hisResponseTO = recipeEnterpriseService.getEnterpriseExpress(enterpriseResTo);
            LOGGER.info("getExpressFee hisResponseTO:{}.", JSONUtils.toString(hisResponseTO));
            if (hisResponseTO != null && hisResponseTO.isSuccess()) {
                //表示获取第三方运费成功
                Map<String, Object> extend = hisResponseTO.getExtend();
                Boolean expressFeeFlag = (Boolean) extend.get("result");
                Object expressFee = extend.get("postagePrice");
                if (expressFeeFlag) {
                    result.put("expressFee", expressFee);
                } else {
                    result.put("expressFee", 0);
                }
                result.put("expressFeeType", 1);
            } else {
                //获取第三方失败 默认从平台获取
                LOGGER.info("getExpressFee 获取第三方运费失败,默认从平台获取");
                result.put("expressFeeType", 0);
            }

        } else {
            //此时运费从平台获取
            result.put("expressFeeType", 0);
        }
        return result;
    }

    private String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
        return "";
    }

    /**
     * 通过机构分页查找药品库存
     *
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findEnterpriseStockByPage(String organId, Integer start, Integer limit) {
        LOGGER.info("syncEnterpriseStockByOrganIdForHis organId:{}, start:{}, limit:{}", organId, start, limit);
        IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
        FindEnterpriseStockByPageTo findEnterpriseStockByPageTo = new FindEnterpriseStockByPageTo();
        findEnterpriseStockByPageTo.setOrgan(organId);
        findEnterpriseStockByPageTo.setStart(start);
        findEnterpriseStockByPageTo.setLimit(limit);
        LOGGER.info("findEnterpriseStockByPage requestBean:{}.", JSONUtils.toString(findEnterpriseStockByPageTo));
        HisResponseTO<List<Map<String, Object>>> responseTO = recipeEnterpriseService.findEnterpriseStockByPage(findEnterpriseStockByPageTo);
        LOGGER.info("String responseTO:{}.", JSONUtils.toString(responseTO));
        if (responseTO.isSuccess()) {
            return responseTO.getData();
        }
        return new ArrayList<>();
    }

    public DrugEnterpriseResult HosscanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("scanStock recipeId:{}, drugsEnterprise:{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();//result=0检验失败
        //1.医院处方服务
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        //2.根据处方Id查询医院的库存
        RecipeResultBean recipeResultBean = hisService.scanDrugStockByRecipeId(recipeId);
        //返回code=1,代表有库存，否则没有库存
        if (recipeResultBean.getCode() == RecipeResultBean.SUCCESS) {
            result.setCode(DrugEnterpriseResult.SUCCESS);
            return result;
        }
        return result;
    }

}
