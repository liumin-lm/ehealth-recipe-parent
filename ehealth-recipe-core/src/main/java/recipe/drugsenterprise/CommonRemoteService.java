package recipe.drugsenterprise;

import com.google.common.collect.Maps;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.service.RecipeLogService;
import recipe.thread.CommonSyncDrugCallable;
import recipe.thread.PushRecipToEpCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.HttpHelper;
import recipe.util.MapValueUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用药企对接服务实现(国药协议)
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/3/7.
 */
@RpcBean(value = "commonRemoteService")
public class CommonRemoteService extends AccessDrugEnterpriseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonRemoteService.class);

    private static Integer RESULT_SUCCESS = 1;

    @Autowired
    private SaleDrugListDAO saleDrugListDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        Map<String, Object> map = Maps.newHashMap();
        map.put("userid", drugsEnterprise.getUserId());
        map.put("password", drugsEnterprise.getPassword());
        String depName = drugsEnterprise.getName();
        Integer depId = drugsEnterprise.getId();

        LOGGER.info("[{}][{}]token更新开始", depId, depName);
        try {
            if (-1 != drugsEnterprise.getAuthenUrl().indexOf("http:")) {
                String backMsg = HttpHelper.doPost(drugsEnterprise.getAuthenUrl(), JSONUtils.toString(map));
                LOGGER.info("[{}][{}]token更新返回：{}", depId, depName, backMsg);
                if (StringUtils.isNotEmpty(backMsg)) {
                    Map backMap = JSONUtils.parse(backMsg, Map.class);
                    // code 1成功
                    String code = "code";
                    if (Integer.valueOf(1).equals(MapValueUtil.getInteger(backMap, code))) {
                        //成功
                        String token = MapValueUtil.getString(backMap, "access_token");
                        if (StringUtils.isNotEmpty(token)) {
                            drugsEnterpriseDAO.updateTokenById(depId, token);
                        }
                    } else {
                        LOGGER.warn("[{}][{}]更新返回失败. msg={}", depId, depName, MapValueUtil.getString(backMap, "message"));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[{}][{}]更新异常。", depId, depName, e);
        }
    }

    @RpcService
    public void synchroDrug (Integer depId, Integer organId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

        List<Integer> drugIds = saleDrugListDAO.findSynchroDrug(depId);
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(depId,drugIds);

        if (!saleDrugLists.isEmpty()) {
            String method = "setGoods";
            Map<String, Object> sendMap = Maps.newHashMap();
            sendMap.put("access_token", drugsEnterprise.getToken());
            sendMap.put("action", method);

            List<Map<String, Object>> drugs = new ArrayList<>(0);
            sendMap.put("data", drugs);

            for (SaleDrugList saleDrugList : saleDrugLists) {
                Map<String, Object> map = new HashMap<>();
                OrganDrugList organDrugList = organDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), organId);
                if (organDrugList != null) {
                    map.put("goodsid", saleDrugList.getDrugId());
                    map.put("gname", saleDrugList.getDrugName());
                    map.put("spec", saleDrugList.getDrugSpec());
                    map.put("drugname", saleDrugList.getSaleName());
                    map.put("packnum", organDrugList.getPack());
                    map.put("msunitno", organDrugList.getUnit());
                    map.put("producer", organDrugList.getProducer());
                    drugs.add(map);
                }
            }
            String sendInfoStr = JSONUtils.toString(sendMap);
            LOGGER.info("发送[{}][{}]内容：{}", drugsEnterprise.getName(), method, sendInfoStr);
            String backMsg;
            try {
                backMsg = HttpHelper.doPost(drugsEnterprise.getBusinessUrl(), sendInfoStr);
                LOGGER.info("调用[{}][{}]结果返回={}", LOGGER.getName(), method, backMsg);
            } catch (IOException e) {
                LOGGER.error("调用[{}][{}] IOException: " + e.getMessage() + "，详细数据：" + sendInfoStr, drugsEnterprise.getName(), method,e);
            }
        }
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        IOrganService iOrganService = ApplicationUtils.getBaseService(IOrganService.class);
        IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

        //药企与处方数据关系
        Map<Integer, List<Map<String, Object>>> enterpriseRecipesMap = Maps.newHashMap();
        //方便更新处方推送状态标志位
        Map<Integer, Set<Integer>> enterpriseRecipeIdsMap = Maps.newHashMap();
        //药品相关数据,key为druglist表drugId,value为药品数据，如果推送处方的数据里存在药企没有的数据，则需要将该数据发给药企
        Map<Integer, Map<String, Object>> drugsMap = Maps.newHashMap();

        Recipe recipe;
        RecipeOrder order;
        PatientBean patient;
        OrganBean organ;
        Map<String, Object> recipeMap;
        List<Map<String, Object>> recipeDetailList;
        for (Integer recipeId : recipeIds) {
            recipe = recipeDAO.getByRecipeId(recipeId);
            if (null == recipe) {
                LOGGER.error("pushRecipInfo ID为" + recipeId + "的处方不存在");
                continue;
            }

            if (StringUtils.isEmpty(recipe.getOrderCode())) {
                LOGGER.error("pushRecipeInfo recipeId={}, 不存在订单编号.", recipeId);
                continue;
            }

            order = orderDAO.getByOrderCode(recipe.getOrderCode());
            if (null == order) {
                LOGGER.error("pushRecipeInfo code为" + recipe.getOrderCode() + "的订单不存在");
                continue;
            }

            Integer enterpriseId = order.getEnterpriseId();
            if (null == enterpriseId) {
                LOGGER.error("pushRecipeInfo 该订单推送药企ID为null，订单编号:" + order.getOrderCode());
                continue;
            }

            try {
                patient = iPatientService.get(recipe.getMpiid());
            } catch (Exception e) {
                patient = null;
            }
            if (null == patient) {
                LOGGER.error("pushRecipeInfo ID为" + recipe.getMpiid() + "的患者不存在");
                continue;
            }

            try {
                organ = iOrganService.get(recipe.getClinicOrgan());
            } catch (Exception e) {
                organ = null;
            }
            if (null == organ) {
                LOGGER.error("pushRecipeInfo ID为" + recipe.getClinicOrgan() + "的机构不存在");
                continue;
            }

            recipeMap = Maps.newHashMap();
            recipeDetailList = new ArrayList<>();

            if (null == enterpriseRecipesMap.get(enterpriseId)) {
                enterpriseRecipesMap.put(enterpriseId, new ArrayList<Map<String, Object>>(0));
                enterpriseRecipeIdsMap.put(enterpriseId, new HashSet<Integer>(0));
            }

            enterpriseRecipesMap.get(enterpriseId).add(recipeMap);
            enterpriseRecipeIdsMap.get(enterpriseId).add(recipeId);

            //组装recipe数据
            recipeMap.put("recipeid", recipe.getRecipeId());
            recipeMap.put("recipecode", recipe.getRecipeCode());
            recipeMap.put("recipetype", recipe.getRecipeType());
            //此处接口那边不是中药方需要置为1
            if (new Integer(3).equals(recipe.getRecipeType())) {
                recipeMap.put("copynum", recipe.getCopyNum());
            } else {
                recipeMap.put("copynum", 1);
            }
            recipeMap.put("createdate", recipe.getSignDate());
            recipeMap.put("requestdate", new Date());
            recipeMap.put("patientid", recipe.getMpiid());
            recipeMap.put("patientname", patient.getPatientName());
            recipeMap.put("patientsex", Integer.parseInt(patient.getPatientSex()));
            recipeMap.put("nric", patient.getCertificate());
            //医保卡号
            HealthCardBean healthCardBean = iPatientService.getHealthCard(recipe.getMpiid(), recipe.getClinicOrgan(), "2");
            if (null != healthCardBean) {
                recipeMap.put("medicarecard", healthCardBean.getCardId());
            } else {
                recipeMap.put("medicarecard", "");
            }

            //地址信息在订单表里
            recipeMap.put("receiver", order.getReceiver());
            recipeMap.put("tel", order.getRecMobile());
            recipeMap.put("zipcode", order.getZipCode());
            recipeMap.put("address", getCompleteAddress(order));
            recipeMap.put("transvalue", order.getExpressFee().doubleValue());

            recipeMap.put("paymode", order.getPayMode());
            //医院(药店)信息
            recipeMap.put("cstid", recipe.getClinicOrgan());
            recipeMap.put("cstname", organ.getName());
            recipeMap.put("csttype", "00");
            recipeMap.put("cstaddress", organ.getAddress());
            recipeMap.put("hospitalid", recipe.getClinicOrgan());
            recipeMap.put("doctorid", recipe.getDoctor());
            recipeMap.put("doctorname", iDoctorService.getNameById(recipe.getDoctor()));
            recipeMap.put("signfile", (null == recipe.getSignFile()) ? null : recipe.getSignFile());
            recipeMap.put("dtl", recipeDetailList);

            //处方详情数据
            List<Recipedetail> recipedetail = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            Map<String, Object> detailMap;
            Map<String, Object> drugMap;
            DrugList drug;
            for (Recipedetail detail : recipedetail) {
                detailMap = Maps.newHashMap();
                detailMap.put("dtlid", detail.getRecipeDetailId());
                detailMap.put("spec", detail.getDrugSpec());
                detailMap.put("prc", detail.getSalePrice());
                detailMap.put("usedose", detail.getUseDose());
                detailMap.put("usedoseunit", detail.getUseDoseUnit());
                String userRate = detail.getUsingRate();
                if (StringUtils.isNotEmpty(userRate)) {
                    try {
                        detailMap.put("usingratename", StringUtils.isNotEmpty(detail.getUsingRateTextFromHis())?detail.getUsingRateTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(userRate));
                    } catch (ControllerException e) {
                        LOGGER.error("pushRecipInfo 获取用药频次类型失败*****usingRate:" + userRate,e);
                        detailMap.put("usingratename", "每日三次");
                    }
                } else {
                    LOGGER.error("pushRecipInfo usingRate为null");
                    detailMap.put("usingratename", "每日三次");
                }
                String usePathways = detail.getUsePathways();
                if (StringUtils.isNotEmpty(usePathways)) {
                    try {
                        detailMap.put("usepathwaysname", StringUtils.isNotEmpty(detail.getUsePathwaysTextFromHis())?detail.getUsePathwaysTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(usePathways));
                    } catch (ControllerException e) {
                        LOGGER.error("pushRecipInfo 获取用药途径类型失败*****usePathways:" + usePathways,e);
                        detailMap.put("usepathwaysname", "口服");
                    }
                } else {
                    LOGGER.error("pushRecipInfo usePathways为null");
                    detailMap.put("usepathwaysname", "口服");
                }
                detailMap.put("usedays", detail.getUseDays());
                detailMap.put("msunitno", detail.getDrugUnit());
                detailMap.put("qty", detail.getUseTotalDose());
                detailMap.put("goodsid", detail.getDrugId());

                //处理药品数据
                if (!drugsMap.containsKey(detail.getDrugId())) {
                    drugMap = Maps.newHashMap();
                    drug = drugListDAO.getById(detail.getDrugId());
                    if (null != drug) {
                        drugMap.put("producer", drug.getProducer());
                        drugMap.put("gname", drug.getDrugName());
                        drugMap.put("goodsid", drug.getDrugId());
                        drugMap.put("msunitno", detail.getDrugUnit());
                        drugMap.put("spec", detail.getDrugSpec());
                        drugMap.put("drugname", drug.getSaleName());
                        drugsMap.put(drug.getDrugId(), drugMap);
                    }
                }
                recipeDetailList.add(detailMap);
            }
        }

        //推送给药企处方，按照药企ID来推送
        if (!enterpriseRecipesMap.isEmpty()) {
            List<PushRecipToEpCallable> callables = new ArrayList<>(0);
            for (Integer enterpriseId : enterpriseRecipesMap.keySet()) {
                callables.add(new PushRecipToEpCallable(enterpriseId, enterpriseRecipesMap.get(enterpriseId),
                        enterpriseRecipeIdsMap.get(enterpriseId), drugsMap));
            }

            if (CollectionUtils.isNotEmpty(callables)) {
                try {
                    RecipeBusiThreadPool.submitList(callables);
                } catch (InterruptedException e) {
                    LOGGER.error("pushRecipInfo 线程池异常",e);
                }
            }
        }

        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String number = recipeParameterDao.getByName("gy_drug_inventory");
        return getInvertoryResult(drugId, drugsEnterprise, number);
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        try {
            LOGGER.info("scanEnterpriseDrugStock recipeDetails:{}", JSONUtils.toString(recipeDetails));
            DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
            List<DrugInfoDTO> drugInfoList = new ArrayList<>();
            List<Integer> drugList = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
            List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIdsEffectivity(drugsEnterprise.getId(), drugList);
            Map<Integer, SaleDrugList> saleDrugListMap = saleDrugLists.stream().collect(Collectors.toMap(SaleDrugList::getDrugId,a->a,(k1,k2)->k1));
            recipeDetails.forEach(recipeDetail -> {
                DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
                BeanUtils.copyProperties(recipeDetail, drugInfoDTO);
                drugInfoDTO.setStock(false);
                SaleDrugList saleDrugList = saleDrugListMap.get(recipeDetail.getDrugId());
                if (null != saleDrugList) {
                    String result = getInvertoryResult(recipeDetail.getDrugId(), drugsEnterprise, recipeDetail.getUseTotalDose().toString());
                    drugInfoDTO.setStock("有库存".equals(result));
                    drugInfoDTO.setStockAmountChin(result);
                } else {
                    drugInfoDTO.setStockAmountChin("无库存");
                }
                drugInfoList.add(drugInfoDTO);
            });
            super.setDrugStockAmountDTO(drugStockAmountDTO, drugInfoList);
            LOGGER.info("scanEnterpriseDrugStock drugStockAmountDTO:{}", JSONUtils.toString(drugStockAmountDTO));
            return drugStockAmountDTO;
        } catch (Exception e) {
            LOGGER.error("scanEnterpriseDrugStock error", e);
        }
        return super.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
    }

    private String getInvertoryResult(Integer drugId, DrugsEnterprise drugsEnterprise, String number) {
        String method = "scanStock";
        Map<String, Object> recipeInfo = Maps.newHashMap();
        Map<String, Object> sendMap = Maps.newHashMap();
        Map<String, Object> detailInfo;
        Map<Integer, String> drugInfo = Maps.newHashMap();
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.getById(drugId);
        List<Map<String, Object>> detailInfoList = new ArrayList<>(10);
        recipeInfo.put("dtl", detailInfoList);
        drugInfo.put(drugList.getDrugId(), drugList.getDrugName());
        detailInfo = Maps.newHashMap();
        detailInfo.put("dtlid", 1);
        detailInfo.put("goodsid", drugList.getDrugId());
        detailInfo.put("qty", number);
        detailInfoList.add(detailInfo);
        sendMap.put("access_token", drugsEnterprise.getToken());
        sendMap.put("action", method);
        sendMap.put("data", recipeInfo);

        String sendInfoStr = JSONUtils.toString(sendMap);
        LOGGER.info("发送[{}][{}]内容：{}", drugsEnterprise.getName(), method, sendInfoStr);

        String backMsg = "";
        try {
            backMsg = HttpHelper.doPost(drugsEnterprise.getBusinessUrl(), sendInfoStr);
            if (StringUtils.isEmpty(backMsg)) {
                return "暂无库存";
            }
        } catch (Exception e) {
            LOGGER.error("暂无库存",e);
            return "暂无库存";
        }

        if (StringUtils.isNotEmpty(backMsg)) {
            Map backMap = null;
            try {
                backMap = JSONUtils.parse(backMsg, Map.class);
            } catch (Exception e) {
                LOGGER.error("暂无库存",e);
                return "暂无库存";
            }
            Integer code = MapValueUtil.getInteger(backMap, "code");
            // code 1成功
            if (RESULT_SUCCESS.equals(code)) {
                return "有库存";
            } else {
                return "暂无库存";
            }
        } else {
            return "暂无库存";
        }
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        List<String> result = new ArrayList<>();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        for (RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                String inventory = getInvertoryResult(saleDrugList.getDrugId(), drugsEnterprise, recipeDetailBean.getUseTotalDose().toString());
                if (StringUtils.isNotEmpty(inventory) && "有库存".equals(inventory)) {
                    result.add(recipeDetailBean.getDrugName());
                }
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        List<CommonSyncDrugCallable> callAbles = new ArrayList<>(10);
        int size = splitGroupSize(drugIdList.size());

        for (int i = 0; i < size; i++) {
            int start = i * ONCETIME_DEAL_NUM;
            int end = start + ONCETIME_DEAL_NUM;
            if (end > drugIdList.size()) {
                end = drugIdList.size();
            }

            callAbles.add(new CommonSyncDrugCallable(drugsEnterprise, drugIdList.subList(start, end)));
        }

        if (CollectionUtils.isNotEmpty(callAbles)) {
            try {
                RecipeBusiThreadPool.submitList(callAbles);
            } catch (InterruptedException e) {
                LOGGER.error("syncDrug 线程池异常",e);
            }
        }

        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        LOGGER.info("CommonRemoteService pushCheckResult not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        LOGGER.info("CommonRemoteService findSupportDep not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMMON;
    }
}
