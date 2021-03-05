package recipe.drugsenterprise;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.department.service.IDepartmentService;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.HealthCardService;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepStyleBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.axis.Constants;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.DrugInventoryBean;
import recipe.drugsenterprise.bean.InventoryDrug;
import recipe.drugsenterprise.bean.YsqDrugResponse;
import recipe.drugsenterprise.bean.yd.httpclient.HttpsClientUtils;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeOrderService;
import recipe.service.common.RecipeCacheService;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.MapValueUtil;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

/**
 * 钥世圈对接服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/3/7.
 */
public class YsqRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YsqRemoteService.class);

    private static String NAME_SPACE = "http://tempuri.org/";

    private static String RESULT_SUCCESS = "00";

    private static final String ACCEPT_PRESCRIPTION = "AcceptPrescription";

    private static final String PRESCRIPTION_GYS_LISTS = "PrescriptionGYSLists";

    private static final String CHECK_PRESCRIPTION_FIALDETAIL = "CheckPrescriptionFialDetail";

    public static final String YSQ_SPLIT = "-";

    private static final String KEY_RCP_DRUG_INVENTORY_LOCK = "RCP_DRUG_INVENTORY_";

    private static final String imgHead = "data:image/jpeg;base64,";

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("YsqRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
        if(CollectionUtils.isNotEmpty(organDrugLists)){
            OrganDrugList organDrugList = organDrugLists.get(0);
            DrugsDataBean drugsDataBean = new DrugsDataBean();
            drugsDataBean.setOrganId(organId);
            List<RecipeDetailBean> recipeDetailBeans = new ArrayList<>();
            RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
            recipeDetailBean.setOrganDrugCode(organDrugList.getOrganDrugCode());
            recipeDetailBean.setDrugId(drugId);
            recipeDetailBean.setDrugName(organDrugList.getDrugName());
            recipeDetailBean.setUseTotalDose(5.0);
            recipeDetailBeans.add(recipeDetailBean);
            drugsDataBean.setRecipeDetailBeans(recipeDetailBeans);
            List<String> result = getDrugInventoryForApp(drugsDataBean, drugsEnterprise, 1);
            if (CollectionUtils.isNotEmpty(result)) {
                return "有库存";
            }
        }
        return "无库存";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(drugsDataBean.getOrganId());
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        //同时生成订单 0不生成 1生成
        sendInfo.put("EXEC_ORD", "0");
        List<Map<String, Object>> titlesInfoList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("DOCTOR", "黄晨屹");
        map.put("HOSNAME", organDTO.getName());
        map.put("AGE", 23);
        map.put("RECEIVENAME", "李笑飞");
        map.put("RANGE", 20000);
        Map<String, Object> position = new HashMap<>();
        position.put("LONGITUDE", "120.201685");
        position.put("LATITUDE", "30.255732");
        map.put("POSITION", position);
        List list = new ArrayList();
        List<String> resultDrugList = new ArrayList<>();
        Map<String, String> drugData = new HashMap<>();
        for (RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(drugsDataBean.getOrganId(), recipeDetailBean.getOrganDrugCode(), recipeDetailBean.getDrugId());
            Map drugMap = new HashMap();
            if (saleDrugList != null && organDrugList != null) {
                drugMap.put("BILLQTY", recipeDetailBean.getUseTotalDose());
                drugMap.put("DISEASE1", organDrugList.getUsePathways());
                drugMap.put("NAME", saleDrugList.getSaleName());
                drugMap.put("PRODUCER", organDrugList.getProducer());
                drugMap.put("GOODS", saleDrugList.getOrganDrugCode());
                drugMap.put("GNAME", saleDrugList.getDrugName());
                drugMap.put("DOSAGENAME", getFormatDouble(organDrugList.getUseDose()) + organDrugList.getUseDoseUnit());
                drugMap.put("SPEC", organDrugList.getDrugSpec());
                drugMap.put("PRC", saleDrugList.getPrice());
                drugMap.put("DISEASENAME", "每日一次");
                drugMap.put("DISEASENAME1", "口服");
                drugMap.put("MSUNITNO", organDrugList.getUnit());
                drugMap.put("DISEASE", "qd");
                list.add(drugMap);
                drugData.put(saleDrugList.getOrganDrugCode(), recipeDetailBean.getDrugName());
            }
        }
        map.put("REMARK", "无");
        map.put("YIBAOBILL", 1);
        map.put("RECEIVETEL", "13777407051");
        map.put("DEPT", "全科行政");
        map.put("PATIENTSENDADDR", "");
        map.put("TELPHONE", "13777407051");
        map.put("HOSCODE", "1223000042416122XC");
        map.put("ALLERGY", "");
        map.put("DOCTORCODE","15645");
        map.put("ACCAMOUNT", "0.10");
        map.put("SEX", "男");
        map.put("IDENTIFICATION", "");
        map.put("PRESCRIPTDATE", DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
        map.put("DIAGNOSIS", "测试1000");
        if (new Integer(1).equals(flag)) {
            map.put("METHOD", "0");
        } else {
            map.put("METHOD", "1");
        }
        map.put("PATNAME", "李笑飞");
        map.put("INBILLNO", "1005144-" + UUID.randomUUID().toString());
        map.put("VALIDDATE", DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));

        map.put("DETAILS", list);
        titlesInfoList.add(map);
        sendInfo.put("TITLES", titlesInfoList);
        String sendInfoStr = JSONUtils.toString(sendInfo);
        String methodName = "CheckPrescriptionFialDetail";
        LOGGER.info("发送[{}][{}]内容：{}", drugsEnterprise.getName(), methodName, sendInfoStr);
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        //发送药企信息
        sendAndDealResult(drugsEnterprise, methodName, sendInfoStr, result);
        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
            List resultList = (List)result.getObject();
            if (CollectionUtils.isNotEmpty(resultList)) {
                for (Object drugs : resultList) {
                    Map<String, Object> drugMap = (Map<String, Object>) drugs;
                    String drugCode = (String)drugMap.get("DRUGCODE");
                    Integer drugStatus = (Integer)drugMap.get("DRUGSTATUS");
                    String drugValue = drugData.get(drugCode);
                    if (new Integer(1).equals(drugStatus)) {
                        resultDrugList.add(drugValue);
                    }
                }
            }
        }
        return resultDrugList;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        String drugEpName = drugsEnterprise.getName();
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        //同时生成订单 0不生成 1生成
        sendInfo.put("EXEC_ORD", "0");
        Integer hosInteriorSupport = drugsEnterprise.getHosInteriorSupport();
        Boolean hosInteriorSupportFlag = true;
        if (hosInteriorSupport != null && hosInteriorSupport == 1) {
            //为补充库存
            hosInteriorSupportFlag = false;
        }
        List<Map<String, Object>> recipeInfoList = getYsqRecipeInfo(recipeIds, hosInteriorSupportFlag, drugsEnterprise);
        if (recipeInfoList.isEmpty()) {
            result.setMsg("钥世圈推送处方数量为0");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }
        sendInfo.put("TITLES", recipeInfoList);
        String sendInfoStr = JSONUtils.toString(sendInfo);
        String methodName = "AcceptPrescription";
        LOGGER.info("发送[{}][{}]内容：{}", drugEpName, methodName, sendInfoStr);
        updateEnterpriseInventory(recipeIds.get(0), drugsEnterprise);
        //发送药企信息
        sendAndDealResult(drugsEnterprise, methodName, sendInfoStr, result);

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
            recipeDAO.updatePushFlagByRecipeId(recipeIds);
            for (Integer recipeId : recipeIds) {
                orderService.updateOrderInfo(recipeOrderDAO.getOrderCodeByRecipeIdWithoutCheck(recipeId), ImmutableMap.of("pushFlag", 1, "depSn", result.getDepSn()), null);
                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.CHECK_PASS, "药企推送成功:" + drugsEnterprise.getName());
                //推送审核结果
                pushCheckResult(recipeIds.get(0), 1, drugsEnterprise);
                if (new Integer(3).equals(drugsEnterprise.getExpressFeePayWay())){
                    //推送处方运费待支付消息提醒
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_EXPRESSFEE_REMIND_NOPAY,recipeId);
                }
            }

        } else {
            for (Integer recipeId : recipeIds) {
                orderService.updateOrderInfo(recipeOrderDAO.getOrderCodeByRecipeIdWithoutCheck(recipeId), ImmutableMap.of("pushFlag", -1), null);
                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.CHECK_PASS, "推送处方失败,药企：" + drugsEnterprise.getName() + ",错误：" + result.getMsg());
            }
            //当前钥世圈没有在线支付的情况
            result.setMsg("推送处方失败，" + result.getMsg());
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    private void updateEnterpriseInventory(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);

        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        //岳阳钥匙圈的需要对库存进行操作
        if ("岳阳-钥世圈".equals(drugsEnterprise.getName())) {
            for (Recipedetail recipedetail : recipedetails) {
                Integer drugId = recipedetail.getDrugId();
                Double useTotalDose = recipedetail.getUseTotalDose();
                BigDecimal totalDose = new BigDecimal(useTotalDose);
                LOGGER.info("YsqRemoteService-updateEnterpriseInventory 更新库存成功,更新药品:{},更新数量:{},处方单号：{}.", drugId, totalDose, recipeId);
                saleDrugListDAO.updateInventoryByOrganIdAndDrugId(drugsEnterprise.getId(), drugId, totalDose);
            }
        }
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        List<Integer> recipeIds = Arrays.asList(recipeId);
        return findSupportDep(recipeIds, null, drugsEnterprise);
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (null == recipeId) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        Integer hosInteriorSupport = drugsEnterprise.getHosInteriorSupport();
        Boolean hosInteriorSupportFlag = true;
        if (hosInteriorSupport != null && hosInteriorSupport == 1) {
            //为补充库存
            hosInteriorSupportFlag = false;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        IOrganService iOrganService = ApplicationUtils.getBaseService(IOrganService.class);

        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        //处方的json数据
        Map<String, String> recipeMap = new HashMap<>(1);
        List<Map<String, String>> recipeInfoList = new ArrayList<>(1);
        recipeInfoList.add(recipeMap);
        sendInfo.put("TITLES", recipeInfoList);

        String drugEpName = drugsEnterprise.getName();
        Recipe recipe = recipeDAO.get(recipeId);
        if (null != recipe) {
            OrganBean organ;
            try {
                organ = iOrganService.get(recipe.getClinicOrgan());
            } catch (Exception e) {
                organ = null;
            }
            if (null == organ) {
                result.setMsg("机构不存在");
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
            if (!hosInteriorSupportFlag && drugsEnterprise.getHosInteriorSupport() == 1) {
                recipeMap.put("HOSCODE", organ.getOrganizeCode());
            } else {
                recipeMap.put("HOSCODE", organ.getOrganId().toString());
            }

            recipeMap.put("HOSNAME", organ.getName());
            //医院处方号  医院机构?处方编号
            recipeMap.put("INBILLNO", recipe.getClinicOrgan() + YSQ_SPLIT + recipe.getRecipeCode());
            //处方pdf文件Id   有药师签名则推送药师签名的pdf  无则推送医生签名的pdf
            recipeMap.put("PDFID", null != recipe.getChemistSignFile() ?
                    LocalStringUtil.toString(recipe.getChemistSignFile()) : LocalStringUtil.toString(recipe.getSignFile()));
            recipeMap.put("FLAG", (1 == checkFlag) ? "true" : "false");
        } else {
            result.setMsg("处方不存在");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        String sendInfoStr = JSONUtils.toString(sendInfo);
        String methodName = "PrescriptionReview";
        LOGGER.info("发送[{}][{}]内容：{}", drugEpName, methodName, sendInfoStr);

        //发送药企信息
        sendAndDealResult(drugsEnterprise, methodName, sendInfoStr, result);
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), result.getMsg());

        return result;
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext,DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID集合为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        String drugEpName = drugsEnterprise.getName();
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        //同时生成订单 0不生成 1生成
        sendInfo.put("EXEC_ORD", "0");
        List<Map<String, Object>> recipeInfoList = getYsqRecipeInfo(recipeIds, false, drugsEnterprise);
        if (recipeInfoList.isEmpty()) {
            result.setMsg("生成处方数量为0");
            result.setCode(DrugEnterpriseResult.FAIL);
            LOGGER.error("findSupportDep 生成处方数量为0. recipeIds={}, depId=[{}]", JSONUtils.toString(recipeIds), drugsEnterprise.getId());
            return result;
        }
        List<Map<String, Object>> titlesInfoList = new ArrayList<>();
        for (Map<String, Object> map : recipeInfoList) {
            if (ext != null) {
                map.put("RANGE", ext.get("range"));
                Map<String, Object> position = new HashMap<>();
                position.put("LONGITUDE", ext.get("longitude"));
                position.put("LATITUDE", ext.get("latitude"));
                map.put("POSITION", position);
            } else {
                map.put("RANGE", 20000);
                Map<String, Object> position = new HashMap<>();
                position.put("LONGITUDE", "120.201685");
                position.put("LATITUDE", "30.255732");
                map.put("POSITION", position);
            }
            titlesInfoList.add(map);
        }
        sendInfo.put("TITLES", titlesInfoList);
        String sendInfoStr = JSONUtils.toString(sendInfo);
        String methodName = "PrescriptionGYSLists";
        LOGGER.info("发送[{}][{}]内容：{}", drugEpName, methodName, sendInfoStr);

        //发送药企信息
        sendAndDealResult(drugsEnterprise, methodName, sendInfoStr, result);
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String url = recipeParameterDao.getByName("yy-url");
        //对岳阳-钥匙圈专门处理，更新药品库存
        if ("岳阳-钥世圈".equals(drugsEnterprise.getName())) {
            String appkey = drugsEnterprise.getUserId();
            String appsecret = drugsEnterprise.getPassword();
            //更新药品库存,实行全药品更新
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            List<Integer> saleDrugList = saleDrugListDAO.findSynchroDrug(drugsEnterprise.getId());
            if (CollectionUtils.isNotEmpty(saleDrugList)) {
                for (int i = 0; i < saleDrugList.size()/30 + 1; i++) {
                    List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganId(drugsEnterprise.getId(), i*30, 30);
                    //开始组装入参
                    DrugInventoryBean drugInventoryBean = new DrugInventoryBean();
                    List<InventoryDrug> inventoryDrugs = new ArrayList<>();
                    drugInventoryBean.setAppkey(appkey);
                    drugInventoryBean.setAppsecret(appsecret);
                    for (SaleDrugList saleDrug : saleDrugLists) {
                        InventoryDrug inventoryDrug = new InventoryDrug();
                        inventoryDrug.setDRUGCODE(saleDrug.getOrganDrugCode());
                        inventoryDrug.setDRUGGNAME(saleDrug.getSaleName());
                        inventoryDrug.setDRUGNAME(saleDrug.getDrugName());
                        inventoryDrugs.add(inventoryDrug);
                    }
                    drugInventoryBean.setDRUGS(inventoryDrugs);
                    String paramesRequest = JSONUtils.toString(drugInventoryBean);
                    LOGGER.info("YsqRemoteService-syncEnterpriseDrug paramesRequest:{}.", paramesRequest);
                    try{
                        //开始发送请求数据
                        String outputJson = HttpsClientUtils.doPost(url, paramesRequest);
                        LOGGER.info("YsqRemoteService-syncEnterpriseDrug outputJson:{}.", outputJson);
                        if(StringUtils.isNotEmpty(outputJson) && outputJson.contains("true") && outputJson.contains("[")){
                            outputJson = outputJson.substring(outputJson.lastIndexOf("["), outputJson.lastIndexOf("]")-1);
                            //获取成功,处理data数据,将其转成标准的JSON
                            List<YsqDrugResponse> ysqDrugResponses = stringToJson(outputJson);
                            if (ysqDrugResponses != null) {
                                for (YsqDrugResponse ysqDrugResponse : ysqDrugResponses) {
                                    String organDrugCode = ysqDrugResponse.getYygoods();
                                    String inventory = ysqDrugResponse.getInventorynum();
                                    double inventoryNum = Double.parseDouble(inventory);
                                    SaleDrugList saleDrug = saleDrugListDAO.getByOrganIdAndDrugCode(drugsEnterprise.getId(), organDrugCode);
                                    if (saleDrug != null) {
                                        if (saleDrug.getInventory() != null && saleDrug.getInventory().doubleValue() != inventoryNum) {
                                            saleDrug.setInventory(new BigDecimal(inventory));
                                            saleDrug.setLastModify(new Date());
                                            saleDrugListDAO.update(saleDrug);
                                        }
                                    }
                                }
                            }
                        }
                    }catch(Exception e){
                        LOGGER.error("YsqRemoteService-syncEnterpriseDrug error:{}.", e.getMessage(), e);
                    }
                }
            }

        }
        return DrugEnterpriseResult.getSuccess();
    }

    private List<YsqDrugResponse> stringToJson(String data) {
        if (StringUtils.isNotEmpty(data)) {
            data = data.replace("\\","");
            data = data.replace(";",",");
            data = data.replace("\"{","{");
            data = data.replace("}\"","}");
            data = data + "]";
            return JSONArray.parseArray(data, YsqDrugResponse.class);
        }
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_YSQ;
    }

    /**
     * 发送药企信息处理返回结果
     *
     * @param drugsEnterprise
     * @param method
     * @param sendInfoStr
     * @param result
     */
    private void sendAndDealResult(DrugsEnterprise drugsEnterprise, String method, String sendInfoStr, DrugEnterpriseResult result) {
        String drugEpName = drugsEnterprise.getName();
        String resultJson = null;
        try {
            Call call = getCall(drugsEnterprise, method);
            if (null != call) {
                call.addParameter(new QName(NAME_SPACE, "AppKey"), Constants.XSD_STRING, ParameterMode.IN);
                call.addParameter(new QName(NAME_SPACE, "AppSecret"), Constants.XSD_STRING, ParameterMode.IN);
                call.addParameter(new QName(NAME_SPACE, "PrescriptionInfo"), Constants.XSD_STRING, ParameterMode.IN);
                if ("PrescriptionGYSLists".equals(method)) {
                    call.addParameter(new QName(NAME_SPACE, "IsGYS"), Constants.XSD_STRING, ParameterMode.IN);
                }
                call.setReturnType(Constants.XSD_STRING);
                Object resultObj;
                if ("PrescriptionGYSLists".equals(method)) {
                    Object[] param = {drugsEnterprise.getUserId(), drugsEnterprise.getPassword(), sendInfoStr, "0"};
                    resultObj = call.invoke(param);
                } else {
                    Object[] param = {drugsEnterprise.getUserId(), drugsEnterprise.getPassword(), sendInfoStr};
                    resultObj = call.invoke(param);
                }


                if (null != resultObj && resultObj instanceof String) {
                    resultJson = resultObj.toString();
                    LOGGER.info("调用[{}][{}]结果返回={}", drugEpName, method, resultJson);
                } else {
                    LOGGER.error("调用[{}][{}]结果返回为空", drugEpName, method);
                    result.setMsg(drugEpName + "接口返回结果为空");
                    result.setCode(DrugEnterpriseResult.FAIL);
                }
            }
        } catch (Exception e) {
            resultJson = null;
            LOGGER.error(drugEpName + " invoke method[{}] error ", method, e);
            result.setMsg(drugEpName + "接口调用出错");
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        if (StringUtils.isNotEmpty(resultJson)) {
            Map resultMap = JSONUtils.parse(resultJson, Map.class);
            String resCode = MapValueUtil.getString(resultMap, "CODE");
            if (RESULT_SUCCESS.equals(resCode)) {
                //成功
                if (ACCEPT_PRESCRIPTION.equals(method)) {
                    result.setDepSn(MapValueUtil.getString(resultMap, "SN"));
                    result.setMsg("调用[" + drugEpName + "][" + method + "]成功.sn=" + result.getDepSn());
                } else if (PRESCRIPTION_GYS_LISTS.equals(method)) {
                    //供应商列表处理
                    List<Map<String, Object>> depList = MapValueUtil.getList(resultMap, "LIST");
                    if (CollectionUtils.isNotEmpty(depList)) {
                        List<DepDetailBean> detailList = new ArrayList<>();
                        DepDetailBean detailBean;
                        for (Map<String, Object> dep : depList) {
                            detailBean = new DepDetailBean();
                            detailBean.setDepName(MapValueUtil.getString(dep, "GYSNAME"));
                            detailBean.setRecipeFee(MapValueUtil.getBigDecimal(dep, "TOTALACCOUNT"));
                            detailBean.setExpressFee(MapValueUtil.getBigDecimal(dep, "PEISONGACCOUNT"));
                            detailBean.setGysCode(MapValueUtil.getString(dep, "GYSCODE"));
                            detailBean.setPharmacyCode(MapValueUtil.getString(dep, "GYSCODE"));
                            detailBean.setDistance(MapValueUtil.getDouble(dep, "GYSDISTANCE"));
                            String sendMethod = MapValueUtil.getString(dep, "SENDMETHOD");
                            detailBean.setAddress(MapValueUtil.getString(dep, "GYSADDRESS"));
                            Position position = new Position();
                            Map<String, Double> positionMap = (Map<String, Double>)MapValueUtil.getObject(dep, "GYSPOSITION");
                            position.setLatitude(positionMap.get("latitude"));
                            position.setLongitude(positionMap.get("longitude"));
                            detailBean.setPosition(position);
                            String giveModeText = "";
                            if (StringUtils.isNotEmpty(sendMethod)) {
                                if ("0".equals(sendMethod)) {
                                    detailBean.setPayMode(RecipeBussConstant.PAYMODE_COD);
                                    giveModeText = "配送到家";
                                } else if ("1".equals(sendMethod)) {
                                    detailBean.setPayMode(RecipeBussConstant.PAYMODE_TFDS);
                                }
                            }
                            detailBean.setGiveModeText(giveModeText);
                            detailBean.setSendMethod(sendMethod);
                            detailBean.setPayMethod(MapValueUtil.getString(dep, "PAYMETHOD"));
                            detailList.add(detailBean);
                        }
                        result.setObject(detailList);
                        result.setMsg("调用[" + drugEpName + "][" + method + "]成功，返回供应商数量:" + detailList.size());

                        //设置样式
                        DepStyleBean styleBean = new DepStyleBean();
                        styleBean.setPriceSize(MapValueUtil.getString(resultMap, "Price_Size"));
                        styleBean.setPriceColor(MapValueUtil.getString(resultMap, "Price_Color"));
                        styleBean.setPriceFont(MapValueUtil.getString(resultMap, "Price_Font"));

                        styleBean.setSupplierSize(MapValueUtil.getString(resultMap, "Supplier_Size"));
                        styleBean.setSupplierColor(MapValueUtil.getString(resultMap, "Supplier_Color"));
                        styleBean.setSupplierFont(MapValueUtil.getString(resultMap, "Supplier_Font"));
                        styleBean.setSupplierWeight(MapValueUtil.getString(resultMap, "Supplier_Weight"));

                        styleBean.setDdzfSize(MapValueUtil.getString(resultMap, "DDZF_Size"));
                        styleBean.setDdzfColor(MapValueUtil.getString(resultMap, "DDZF_Color"));
                        styleBean.setDdzfFont(MapValueUtil.getString(resultMap, "DDZF_Font"));

                        styleBean.setHdfkSize(MapValueUtil.getString(resultMap, "HDFK_Size"));
                        styleBean.setHdfkColor(MapValueUtil.getString(resultMap, "HDFK_Color"));
                        styleBean.setHdfkFont(MapValueUtil.getString(resultMap, "HDFK_Font"));

                        styleBean.setPsdjSize(MapValueUtil.getString(resultMap, "PSDJ_Size"));
                        styleBean.setPsdjColor(MapValueUtil.getString(resultMap, "PSDJ_Color"));
                        styleBean.setPsdjFont(MapValueUtil.getString(resultMap, "PSDJ_Font"));
                        result.setStyle(styleBean);
                    } else {
                        result.setMsg(drugEpName + "接口调用返回可用药店列表为空");
                        result.setCode(DrugEnterpriseResult.FAIL);
                    }
                } else if (CHECK_PRESCRIPTION_FIALDETAIL.equals(method)) {
                    List details = MapValueUtil.getList(resultMap, "DETAILS");
                    result.setObject(details);
                }else {
                    result.setMsg("调用[" + drugEpName + "][" + method + "]成功");
                }
            } else {
                result.setMsg("调用[" + drugEpName + "][" + method + "]失败.error:" + MapValueUtil.getString(resultMap, "MSG"));
                result.setCode(DrugEnterpriseResult.FAIL);
            }
        } else {
            result.setMsg(drugEpName + "接口调用返回为空");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
    }

    protected List<Map<String, Object>> getYsqRecipeInfo(List<Integer> recipeIds, boolean sendRecipe, DrugsEnterprise drugsEnterprise) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        IDepartmentService iDepartmentService = ApplicationUtils.getBaseService(IDepartmentService.class);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);
        IOrganService iOrganService = ApplicationUtils.getBaseService(IOrganService.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        Recipe recipe;
        RecipeOrder order = null;
        PatientBean patient;
        OrganBean organ;
        //药品信息MAP，减少DB查询
        Map<Integer, DrugList> drugListMap = new HashMap<>(10);
        List<Map<String, Object>> recipeInfoList = new ArrayList<>(recipeIds.size());
        //每个处方的json数据
        Map<String, Object> recipeMap;
        for (Integer recipeId : recipeIds) {
            recipe = recipeDAO.getByRecipeId(recipeId);
            getMedicalInfo(recipe);
            if (null == recipe) {
                LOGGER.error("getYsqRecipeInfo ID为" + recipeId + "的处方不存在");
                continue;
            }

            if (sendRecipe) {
                if (StringUtils.isEmpty(recipe.getOrderCode())) {
                    LOGGER.error("getYsqRecipeInfo recipeId={}, 不存在订单编号.", recipeId);
                    continue;
                }

                order = orderDAO.getByOrderCode(recipe.getOrderCode());
                if (null == order) {
                    LOGGER.error("getYsqRecipeInfo code为" + recipe.getOrderCode() + "的订单不存在");
                    continue;
                }
            }
            try {
                patient = iPatientService.get(recipe.getMpiid());
            } catch (Exception e) {
                LOGGER.error("getYsqRecipeInfo patient :" + e.getMessage(),e);
                patient = null;
            }
            if (null == patient) {
                LOGGER.error("getYsqRecipeInfo ID为" + recipe.getMpiid() + "的患者不存在");
                continue;
            }
            try {
                organ = iOrganService.get(recipe.getClinicOrgan());
            } catch (Exception e) {
                organ = null;
            }
            if (null == organ) {
                LOGGER.error("getYsqRecipeInfo ID为" + recipe.getClinicOrgan() + "的机构不存在");
                continue;
            }
            recipeMap = Maps.newHashMap();
            if (sendRecipe) {
                //取药方式
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                    //1：自提；0：送货上门
                    recipeMap.put("METHOD", "0");
                } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                    recipeMap.put("METHOD", "1");
                } else {
                    //支持所有方式
                    recipeMap.put("METHOD", "");
                }
            } else {
                recipeMap.put("METHOD", "");
            }
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                order = orderDAO.getByOrderCode(recipe.getOrderCode());
                if (order != null ) {
                    //配送到家的方式
                    recipeMap.put("METHOD", "0");
                    recipeMap.put("PATIENTSENDADDR", getCompleteAddress(order));
                    recipeMap.put("STORECODE", order.getDrugStoreCode());
                    recipeMap.put("SENDNAME", order.getReceiver());
                    recipeMap.put("RECEIVENAME", order.getReceiver());
                    recipeMap.put("RECEIVETEL", order.getRecMobile());
                    recipeMap.put("ACCAMOUNT", order.getRecipeFee().toString());
                    if (order.getPayFlag() != null && 1 == order.getPayFlag()) {
                        recipeMap.put("ISPAYMENT", "1");
                    } else if (order.getPayFlag() != null && 0 == order.getPayFlag()){
                        recipeMap.put("ISPAYMENT", "0");
                    }
                    //快递费用
                    if (new Integer(1).equals(order.getExpressFeePayWay())) {
                        //已经支付快递费
                        recipeMap.put("DELIVERYFLAG", 1);
                    } else {
                        recipeMap.put("DELIVERYFLAG", 0);
                    }
                    //添加省市区信息
                    String province = getAddressDic(order.getAddress1());
                    String city = getAddressDic(order.getAddress2());
                    String district = getAddressDic(order.getAddress3());
                    recipeMap.put("PROVINCE", province);
                    recipeMap.put("CITY", city);
                    recipeMap.put("DISTRICT", district);
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                    if (recipeExtend != null) {
                        //添加挂号序号
                        recipeMap.put("REGISTRATIONNUMBER", recipeExtend.getRegisterID());
                    }
                    IRecipeCheckService recipeCheckService=  RecipeAuditAPI.getService(IRecipeCheckService.class,"recipeCheckServiceImpl");
                    RecipeCheckBean recipeCheckBean = recipeCheckService.getByRecipeId(recipe.getRecipeId());
                    if (recipeCheckBean != null) {
                        recipeMap.put("REVIEWUSER", recipeCheckBean.getCheckerName());
                        recipeMap.put("REVIEWSTATE", "true");
                        recipeMap.put("REVIEWMSG", recipeCheckBean.getMemo());
                        recipeMap.put("REVIEWTIME", recipeCheckBean.getCheckDate());
                    }

                    recipeMap.put("DELIVERYCASH", order.getExpressFee());
                    //添加代煎相关
                    if (recipe.getRecipeType() == 3 && order.getDecoctionFee() != null && order.getDecoctionFee().compareTo(BigDecimal.ZERO) == 1 ) {
                        //代煎费不为空
                        recipeMap.put("REPLACEFLY", "1");  //需要代煎
                        recipeMap.put("REPLACEFLYQTY", recipe.getCopyNum());  //代煎数量
                        recipeMap.put("REPLACEFLYPRC", order.getDecoctionFee().divide(new BigDecimal(recipe.getCopyNum())));  //代煎单价
                        recipeMap.put("REPLACEFLYAMOUNT", order.getDecoctionFee());  //代煎金额
                    } else {
                        recipeMap.put("REPLACEFLY", "0");  //不需代煎
                    }
                    //医保处方 0：是；1：否
                    if (new Integer(1).equals(order.getOrderType())) {
                        recipeMap.put("YIBAOBILL", "1");
                    } else {
                        recipeMap.put("YIBAOBILL", "0");
                    }
                } else {
                    if ("psysq".equals(drugsEnterprise.getAccount())) {
                        recipeMap.put("METHOD", "0");
                        recipeMap.put("PATIENTSENDADDR", "");
                    } else {
                        recipeMap.put("METHOD", "1");
                        recipeMap.put("PATIENTSENDADDR", "");
                    }
                }
            } else {
                if ("psysq".equals(drugsEnterprise.getAccount())) {
                    recipeMap.put("METHOD", "0");
                    recipeMap.put("PATIENTSENDADDR", "");
                } else {
                    recipeMap.put("METHOD", "1");
                    recipeMap.put("PATIENTSENDADDR", "");
                }
            }
            //icd10
            recipeMap.put("ICD10", recipe.getOrganDiseaseId());
            //中药贴数
            if (recipe.getRecipeType() == 3 && recipe.getCopyNum() != null) {
                recipeMap.put("COUNTTIENUM", recipe.getCopyNum());
            }
            //处方类型
            if (recipe.getRecipeType() == 1 || recipe.getRecipeType() == 2) {
                recipeMap.put("PRESCRIPTIONTYPE", "1");
            } else {
                recipeMap.put("PRESCRIPTIONTYPE", "2");
            }
            //医嘱
            if (StringUtils.isNotEmpty(recipe.getRecipeMemo())) {
                recipeMap.put("DCTTIPS", recipe.getRecipeMemo());
            }
            if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                order = orderDAO.getByOrderCode(recipe.getOrderCode());
                if (order != null ) {
                    recipeMap.put("STORECODE", order.getDrugStoreCode());
                }
            }

            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) && RecipeBussConstant.PAYMODE_OFFLINE.equals(order.getPayMode())) {
                recipeMap.put("ISPAYMENT", "0");
            }

            recipeMap.put("HOSCODE", organ.getOrganizeCode());

            recipeMap.put("HOSNAME", organ.getName());
            recipeMap.put("PRESCRIPTDATE", DateConversion.getDateFormatter(recipe.getSignDate(), DateConversion.DEFAULT_DATE_TIME));
            //医院处方号  医院机构?处方编号
            if (StringUtils.isNotEmpty(recipe.getRecipeCode())) {
                recipeMap.put("INBILLNO", recipe.getClinicOrgan() + YSQ_SPLIT + recipe.getRecipeCode());
            } else {
                recipeMap.put("INBILLNO", recipe.getClinicOrgan() + YSQ_SPLIT + recipe.getRecipeId() + "ngari999");
            }
            recipeMap.put("PATNAME", patient.getPatientName());
            //性别处理
            String sex = patient.getPatientSex();
            if (StringUtils.isNotEmpty(sex)) {
                try {
                    recipeMap.put("SEX", DictionaryController.instance().get("eh.base.dictionary.Gender").getText(sex));
                } catch (ControllerException e) {
                    LOGGER.error("getYsqRecipeInfo 获取性别类型失败*****sex:" + sex,e);
                    recipeMap.put("SEX", "男");
                }
            } else {
                LOGGER.error("getYsqRecipeInfo sex为空");
                recipeMap.put("SEX", "男");
            }
            //获取患者就诊卡号
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if (recipeExtend != null) {
                String cardNo = recipeExtend.getCardNo();
                if (StringUtils.isNotEmpty(cardNo)) {
                    recipeMap.put("ONECARDSOLUTION", cardNo);
                } else {
                    HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
                    List<HealthCardDTO> healthCardDTOS = healthCardService.findByMpiId(recipe.getMpiid());
                    if (CollectionUtils.isNotEmpty(healthCardDTOS)) {
                        recipeMap.put("ONECARDSOLUTION", healthCardDTOS.get(0).getCardId());
                    }
                }
            }

            //添加患者医保类型
            IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
            if (recipe.getClinicId() != null) {
                RevisitExDTO consultExDTO = exService.getByConsultId(recipe.getClinicId());
                if (recipe.getClinicOrgan() == 1004539) {
                    //重庆大学城医院复诊为个性化暂没做字典对照
                    if ("1".equals(consultExDTO.getInsureTypeCode())) {
                        //表示自费
                        recipeMap.put("YIBAOBILL", "1");
                    } else {
                        //表示普通医保
                        recipeMap.put("YIBAOBILL", "0");
                        recipeMap.put("YBTYPE", "0");
                    }
                } else {
                    if (StringUtils.isNotEmpty(consultExDTO.getInsureTypeCode())) {
                        if ("0".equals(consultExDTO.getInsureTypeCode())) {
                            //表示自费
                            recipeMap.put("YIBAOBILL", "1");
                        } else if ("1".equals(consultExDTO.getInsureTypeCode())) {
                            //表示普通医保
                            recipeMap.put("YIBAOBILL", "0");
                            recipeMap.put("YBTYPE", "0");
                        } else if ("2".equals(consultExDTO.getInsureTypeCode())) {
                            //表示门特
                            recipeMap.put("YIBAOBILL", "0");
                            recipeMap.put("YBTYPE", "1");
                        }
                    }
                }
            }

            //周岁处理
            Date birthday = patient.getBirthday();
            if (null != birthday) {
                recipeMap.put("AGE", Integer.toString(DateConversion.getAge(birthday)));
            } else {
                //有些医院不提供身份证号,年龄提供默认值
                recipeMap.put("AGE", 25);
            }
            recipeMap.put("BINGLINUMBER", recipe.getPatientID());
            //身份信息使用原始身份证号，暂定空
            recipeMap.put("IDENTIFICATION", patient.getCertificate());
            //recipeMap.put("USERID", recipe.getPatientID());
            recipeMap.put("TELPHONE", patient.getMobile());
            if (null != order){
                recipeMap.put("RECEIVENAME", order.getReceiver());
                recipeMap.put("RECEIVETEL", order.getRecMobile());
                recipeMap.put("ACCAMOUNT", order.getRecipeFee().toString());
                //放置药店编码和名称
                if (StringUtils.isNotEmpty(order.getDrugStoreCode())) {
                    recipeMap.put("GYSCODE", order.getDrugStoreCode());
                    recipeMap.put("GYSNAME", order.getDrugStoreName());
                }
            }
            recipeMap.put("ALLERGY", "");
            recipeMap.put("REMARK", StringUtils.defaultString(recipe.getMemo(), ""));
            recipeMap.put("DEPT", iDepartmentService.getNameById(recipe.getDepart()));
            recipeMap.put("DOCTORCODE", recipe.getDoctor().toString());
            recipeMap.put("DOCTOR", iDoctorService.getNameById(recipe.getDoctor()));
            //处理过期时间
            String validateDays = "7";
            Date validate = DateConversion.getDateAftXDays(recipe.getSignDate(), Integer.parseInt(validateDays));
            recipeMap.put("VALIDDATE", DateConversion.getDateFormatter(validate, DateConversion.DEFAULT_DATE_TIME));
            recipeMap.put("DIAGNOSIS", recipe.getOrganDiseaseName());
            //医保处方 0：是；1：否
            //recipeMap.put("YIBAOBILL", "1");
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
                String url = recipeParameterDao.getByName("fileImgUrl");
                url += null != recipe.getChemistSignFile() ?
                        LocalStringUtil.toString(recipe.getChemistSignFile()) : LocalStringUtil.toString(recipe.getSignFile());
                recipeMap.put("PDF_ID", url);
            }
            List<Map<String, String>> recipeDetailList = new ArrayList<>();
            recipeMap.put("DETAILS", recipeDetailList);
            //处方详情数据
            List<Recipedetail> recipedetail = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            if (CollectionUtils.isNotEmpty(recipedetail)) {
                Map<String, String> detailMap;
                DrugList drug;
                for (Recipedetail detail : recipedetail) {
                    detailMap = Maps.newHashMap();
                    Integer drugId = detail.getDrugId();
                    drug = drugListMap.get(drugId);
                    if (null == drug) {
                        drug = drugListDAO.get(drugId);
                        drugListMap.put(drugId, drug);
                    }


                    SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
                    LOGGER.info("YsqRemoteService-saleDrugList:[{}] [{}].", drugId, drugsEnterprise.getId());
                    if (saleDrugList != null) {
                        detailMap.put("GOODS", saleDrugList.getOrganDrugCode());
                    }

                    detailMap.put("NAME", drug.getSaleName());
                    detailMap.put("GNAME", drug.getDrugName());
                    detailMap.put("SPEC", drug.getDrugSpec());
                    detailMap.put("PRODUCER", drug.getProducer());
                    detailMap.put("MSUNITNO", drug.getUnit());
                    if (detail.getUseTotalDose() != null) {
                        detailMap.put("BILLQTY", getFormatDouble(detail.getUseTotalDose()));
                    }
                    detailMap.put("PRC", detail.getSalePrice().toString());
                    if (detail.getUseDays() != null) {
                        //添加用药时长（天）
                        detailMap.put("YYSC", detail.getUseDays().toString());
                    } else {
                        detailMap.put("YYSC", "");
                    }
                    //药品使用
                    detailMap.put("DOSAGE", "");
                    detailMap.put("DOSAGENAME", getFormatDouble(detail.getUseDose()) + detail.getUseDoseUnit());
                    detailMap.put("BOILDRUGMETHOD", detail.getMemo());
                    String userRate = detail.getUsingRate();
                    detailMap.put("DISEASE", userRate);
                    if (StringUtils.isNotEmpty(userRate)) {
                        if (recipe.getRecipeType() != 3) {
                            try {
                                detailMap.put("DISEASENAME", detail.getUsingRateTextFromHis()!=null?detail.getUsingRateTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(userRate));
                            } catch (ControllerException e) {
                                LOGGER.error("getYsqRecipeInfo 获取用药频次类型失败*****usingRate:" + userRate,e);
                                detailMap.put("DISEASENAME", "每日三次");
                            }
                        } else {
                            detailMap.put("DISEASENAME", userRate);
                        }
                    } else {
                        LOGGER.error("getYsqRecipeInfo usingRate为null");
                        detailMap.put("DISEASENAME", "每日三次");
                    }
                    String usePathways = detail.getUsePathways();
                    detailMap.put("DISEASE1", usePathways);
                    if (StringUtils.isNotEmpty(usePathways)) {
                        if (recipe.getRecipeType() != 3) {
                            try {
                                detailMap.put("DISEASENAME1", detail.getUsePathwaysTextFromHis()!=null?detail.getUsePathwaysTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(usePathways));
                            } catch (ControllerException e) {
                                LOGGER.error("getYsqRecipeInfo 获取用药途径类型失败*****usePathways:" + usePathways,e);
                                detailMap.put("DISEASENAME1", "口服");
                            }
                        } else {
                            detailMap.put("DISEASENAME1", usePathways);
                        }

                    } else {
                        LOGGER.error("getYsqRecipeInfo usePathways为null");
                        detailMap.put("DISEASENAME1", "口服");
                    }
                    recipeDetailList.add(detailMap);
                }
            }
            recipeInfoList.add(recipeMap);
        }

        return recipeInfoList;
    }

    /**
     * 获取wsdl调用客户端
     *
     * @param drugsEnterprise
     * @param method
     * @return
     */
    protected Call getCall(DrugsEnterprise drugsEnterprise, String method) throws Exception {
        String wsdlUrl = drugsEnterprise.getBusinessUrl();
        String nameSpaceUri = NAME_SPACE + method;
        Call call = null;
        try {
            Service s = new Service();
            call = (Call) s.createCall();
            if (null != call) {
                //单位毫秒
                call.setTimeout(20000);
                call.setTargetEndpointAddress(new URL(wsdlUrl));
                call.setOperationName(new QName(NAME_SPACE, method));
                call.setSOAPActionURI(nameSpaceUri);
            }
        } catch (Exception e) {
            call = null;
            LOGGER.error("create call error. wsdlUrl={}, nameSpaceUri={}", wsdlUrl, nameSpaceUri, e);
        } finally {
            if(null == call){
                LOGGER.error("create call error finally. wsdlUrl={}, nameSpaceUri={}", wsdlUrl, nameSpaceUri);
            }
        }

        return call;
    }

    //发送药品审核信息
    public DrugEnterpriseResult sendAuditDrugList(DrugsEnterprise drugsEnterprise, String organizeCode, String organDrugCode, Integer status) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organ = organService.getOrganByOrganizeCode(organizeCode);
        AuditDrugListDAO auditDrugListDAO = DAOFactory.getDAO(AuditDrugListDAO.class);
        AuditDrugList auditDrugList = auditDrugListDAO.getByOrganizeCodeAndOrganDrugCode(organizeCode, organDrugCode);

        String drugEpName = drugsEnterprise.getName();
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("HOSCODE", auditDrugList.getOrganizeCode());
        auditInfo.put("HOSNAME", organ.getName());
        auditInfo.put("CODE", auditDrugList.getOrganDrugCode());
        auditInfo.put("NAME", auditDrugList.getDrugName());
        auditInfo.put("GNAME", auditDrugList.getSaleName());
        auditInfo.put("SPEC", auditDrugList.getDrugSpec());
        auditInfo.put("PRODUCER", auditDrugList.getProducer());
        auditInfo.put("FORMNAME", "");
        auditInfo.put("MSUNITNO", auditDrugList.getUnit());
        auditInfo.put("MINMSUNITNO", auditDrugList.getUnit());
        if (status == 1) {
            auditInfo.put("INVENTORYNUM", 9999);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(auditInfo);
        sendInfo.put("DRUGS", list);
        String sendInfoStr = JSONUtils.toString(sendInfo);
        String methodName = "DrugInvAmount";
        LOGGER.info("发送[{}][{}]内容：{}", drugEpName, methodName, sendInfoStr);

        //发送药企信息
        sendAndDealResult(drugsEnterprise, methodName, sendInfoStr, result);
        return result;
    }

    /**
     * 获取区域文本
     * @param area 区域
     * @return     区域文本
     */
    private String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area,e);
            }
        }
        return "";
    }
}
