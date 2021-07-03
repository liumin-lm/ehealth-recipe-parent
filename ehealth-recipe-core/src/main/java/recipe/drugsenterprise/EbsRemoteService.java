package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.mvc.support.HttpClientUtils;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.EbsBean;
import recipe.drugsenterprise.bean.EbsDetail;
import recipe.drugsenterprise.bean.EsbWebService;
import recipe.service.RecipeLogService;
import recipe.util.AppSiganatureUtils;
import recipe.util.DateConversion;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author yinsheng
 * @date 2020\4\15 0015 14:33
 */
@RpcBean(value = "ebsRemoteService")
public class EbsRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbsRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("EbsRemoteService tokenUpdateImpl not implement.");
    }

    @RpcService
    public void test(Integer recipeId, Integer depId){
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        List<Integer> recipeIds = Arrays.asList(recipeId);
        pushRecipeInfo(recipeIds, drugsEnterprise);
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        LOGGER.info("EbsRemoteService.pushRecipeInfo recipeIds:{}", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (!CollectionUtils.isEmpty(recipeList)) {
            Recipe recipe = recipeList.get(0);
            //flag 1 正常  0 退货
            pushRecipeInfoForSy(enterprise, result, recipe, 1);
        }
        return result;
    }

    /**
     * 这里只有处方新增，处方取消放前置机处理了，如果这个接口修改了入参需要通知前置机同步修改取消处方接口
     *
     * @param enterprise
     * @param result
     * @param recipe
     * @param flag
     */
    private void pushRecipeInfoForSy(DrugsEnterprise enterprise, DrugEnterpriseResult result, Recipe recipe, Integer flag) {
        getMedicalInfo(recipe);
        LOGGER.info("pushRecipeInfoForSy recipeId:{}, flag:{}.", recipe.getRecipeId(), flag);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);

        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());

        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
        DepartmentDTO departmentDTO = departmentService.get(recipe.getDepart());
        PatientDTO patientDTO = patientService.getPatientByMpiId(recipe.getMpiid());
        EbsBean ebsBean = new EbsBean();
        ebsBean.setPrescripNo(recipe.getRecipeCode());
        ebsBean.setPrescribeDate(recipe.getSignDate().getTime());
        ebsBean.setHospitalCode(organDTO.getOrganizeCode());
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String organName = recipeParameterDao.getByName(recipe.getClinicOrgan()+"_shyy-organname");
        ebsBean.setHospitalName(organName);
        ebsBean.setDepartment(departmentDTO.getName());
        ebsBean.setDoctorName(recipe.getDoctorName());
        ebsBean.setName(recipe.getPatientName());
        ebsBean.setDiagnoseResult(recipe.getOrganDiseaseName());
        if (patientDTO != null) {
            if (StringUtils.isNotEmpty(patientDTO.getPatientSex())) {
                Integer sex = Integer.parseInt(patientDTO.getPatientSex());
                if (sex == 1) {
                    ebsBean.setSex(1);
                } else {
                    ebsBean.setSex(0);
                }
            }
            ebsBean.setAge(DateConversion.getAge(patientDTO.getBirthday()));
            ebsBean.setMobile(patientDTO.getMobile());
            ebsBean.setIdCard(patientDTO.getCertificate());
        }
        ebsBean.setSocialSecurityCard("");
        ebsBean.setAddress("");
        if (recipeOrder != null && new Integer(0).equals(recipeOrder.getOrderType())) {
            ebsBean.setFeeType(0);
        } else {
            ebsBean.setFeeType(1);
        }
        ebsBean.setDistrictName(convertParame(recipe.getOrganDiseaseName()));
        if (recipeOrder != null) {
            if (flag == 1) {
                ebsBean.setTotalAmount(recipeOrder.getTotalFee());
            } else {
                if (recipeOrder.getTotalFee() != null) {
                    ebsBean.setTotalAmount(recipeOrder.getTotalFee().multiply(new BigDecimal(-1)));
                }
            }

            ebsBean.setReceiver(recipeOrder.getReceiver());
            ebsBean.setReceiverMobile(recipeOrder.getRecMobile());
            String province = getAddressDic(recipeOrder.getAddress1());
            String city = getAddressDic(recipeOrder.getAddress2());
            String district = getAddressDic(recipeOrder.getAddress3());
            ebsBean.setProvinceName(province);
            ebsBean.setCityName(city);
            ebsBean.setDistrictName(district);
            ebsBean.setShippingAddress(recipeOrder.getAddress4());
        }
        ebsBean.setRemark(convertParame(recipe.getMemo()));
        List<EbsDetail> details = new ArrayList<>();
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        for (Recipedetail recipedetail : recipedetails) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
            if (saleDrugList != null) {
                EbsDetail ebsDetail = new EbsDetail();
                ebsDetail.setMedName(saleDrugList.getDrugName());
                ebsDetail.setMedCode(saleDrugList.getOrganDrugCode());
                ebsDetail.setSpec(saleDrugList.getDrugSpec());
                ebsDetail.setDrugForm(convertParame(recipedetail.getDrugForm()));
                try{
                    Dictionary usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
                    Dictionary usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
                    ebsDetail.setDirections(StringUtils.isNotEmpty(recipedetail.getUsingRateTextFromHis())?recipedetail.getUsingRateTextFromHis():usingRateDic.getText(recipedetail.getUsingRate()) + (StringUtils.isNotEmpty(recipedetail.getUsePathwaysTextFromHis())?recipedetail.getUsePathwaysTextFromHis():usePathwaysDic.getText(recipedetail.getUsePathways())));
                }catch(Exception e){
                    LOGGER.error("pushRecipeInfo 用法用量获取失败.",e);
                }
                ebsDetail.setUnitName(recipedetail.getDrugUnit());
                if (flag == 1) {
                    ebsDetail.setAmount(recipedetail.getUseTotalDose());
                } else {
                    ebsDetail.setAmount(recipedetail.getUseTotalDose()*-1);
                }

                ebsDetail.setUnitPrice(saleDrugList.getPrice().doubleValue());
                details.add(ebsDetail);
            }
        }
        ebsBean.setDetails(details);

        String recipeXml = recipeToXml(ebsBean);
        //以下开始推送处方信息
        String addSingleMethod = "addSingle";
        LOGGER.info("request:{}.", recipeXml);

        Map<String, String> param=new HashMap<String, String>();
        param.put("url", enterprise.getBusinessUrl());
        EsbWebService xkyyHelper = new EsbWebService();
        xkyyHelper.initConfig(param);
        try{
            String webServiceResult = xkyyHelper.HXCFZT(recipeXml, addSingleMethod);
            LOGGER.info("pushRecipeInfoForSy recipeId:{},webServiceResult:{},", recipe.getRecipeId(), webServiceResult);
            Map maps = (Map)JSON.parse(webServiceResult);
            if (maps == null) {
                getDrugEnterpriseResult(result, "推送失败");
            } else {
                Boolean success = (Boolean) maps.get("success");
                String code = (String) maps.get("code");
                String msg = (String) maps.get("msg");
                if (success && "0".equals(code)) {
                    if (flag == 1) {
                        //表示推送成功
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), enterprise.getName() + msg);
                    } else {
                        //表示推送失败
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), enterprise.getName() + "处方退货推送成功");
                    }
                } else {
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), enterprise.getName() +  msg);
                }
            }
        }catch (Exception ex){
            LOGGER.error("pushRecipeInfoForSy error recipeId:{}, {}", recipe.getRecipeId(), ex.getMessage(), ex);
            getDrugEnterpriseResult(result, enterprise.getName() + "推送失败");
        }
    }

    private String convertParame(Object o){
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

    private String recipeToXml(EbsBean ebsBean) {
        StringBuilder result = new StringBuilder("<root><body><params>");
        result.append("<prescripNo>").append(ebsBean.getPrescripNo()).append("</prescripNo>");
        result.append("<prescribeDate>").append(ebsBean.getPrescribeDate()).append("</prescribeDate>");
        result.append("<hospitalCode>").append(ebsBean.getHospitalCode()).append("</hospitalCode>");
        result.append("<hospitalName>").append(ebsBean.getHospitalName()).append("</hospitalName>");
        result.append("<department>").append(ebsBean.getDepartment()).append("</department>");
        result.append("<doctorName>").append(ebsBean.getDoctorName()).append("</doctorName>");
        result.append("<name>").append(ebsBean.getName()).append("</name>");
        result.append("<sex>").append(ebsBean.getSex()).append("</sex>");
        result.append("<age>").append(ebsBean.getAge()).append("</age>");
        result.append("<mobile>").append(ebsBean.getMobile()).append("</mobile>");
        result.append("<idCard>").append(ebsBean.getIdCard()).append("</idCard>");
        result.append("<socialSecurityCard>").append(ebsBean.getSocialSecurityCard()).append("</socialSecurityCard>");
        result.append("<address>").append(ebsBean.getAddress()).append("</address>");
        result.append("<feeType>").append(ebsBean.getFeeType()).append("</feeType>");
        result.append("<totalAmount>").append(ebsBean.getTotalAmount()).append("</totalAmount>");
        result.append("<diagnoseResult>").append(ebsBean.getDiagnoseResult()).append("</diagnoseResult>");
        result.append("<receiver>").append(ebsBean.getReceiver()).append("</receiver>");
        result.append("<receiverMobile>").append(ebsBean.getReceiverMobile()).append("</receiverMobile>");
        result.append("<provinceName>").append(ebsBean.getProvinceName()).append("</provinceName>");
        result.append("<cityName>").append(ebsBean.getCityName()).append("</cityName>");
        result.append("<districtName>").append(ebsBean.getDistrictName()).append("</districtName>");
        if (StringUtils.isNotEmpty(ebsBean.getShippingAddress())) {
            String address = ebsBean.getShippingAddress().replace("\n", "");
            result.append("<shippingAddress>").append(address).append("</shippingAddress>");
        }
        result.append("<remark>").append(ebsBean.getRemark()).append("</remark>");
        for (EbsDetail ebsDetail : ebsBean.getDetails()) {
            result.append("<medName>").append(ebsDetail.getMedName()).append("</medName>");
            result.append("<medCode>").append(ebsDetail.getMedCode()).append("</medCode>");
            result.append("<spec>").append(ebsDetail.getSpec()).append("</spec>");
            result.append("<drugForm>").append(ebsDetail.getDrugForm()).append("</drugForm>");
            result.append("<directions>").append(ebsDetail.getDirections()).append("</directions>");
            result.append("<amount>").append(ebsDetail.getAmount()).append("</amount>");
            result.append("<unitName>").append(ebsDetail.getUnitName()).append("</unitName>");
            result.append("<unitPrice>").append(ebsDetail.getUnitPrice()).append("</unitPrice>");
        }
        result.append("</params></body></root>");
        return result.toString();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        try{
            String stockMethod = "getMedicineStock";
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(drugId, drugsEnterprise.getId());
            if (saleDrugList != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("sku", saleDrugList.getOrganDrugCode());
                params.put("pageNo","1");
                params.put("pageSize","100");
                String parames = jsonToXml(params);
                LOGGER.info("scanStock parames:{}.", parames);
                EsbWebService xkyyHelper = new EsbWebService();
                Map<String, String> param=new HashMap<String, String>();
                param.put("url", drugsEnterprise.getBusinessUrl());
                xkyyHelper.initConfig(param);
                try{
                    String webServiceResult = xkyyHelper.HXCFZT(parames, stockMethod);
                    LOGGER.info("getDrugInventory webServiceResult:{}. ", webServiceResult);
                    Map maps = (Map)JSON.parse(webServiceResult);
                    Boolean success = (Boolean) maps.get("success");
                    String code = (String) maps.get("code");
                    if (success && "0".equals(code)) {
                        List<Map<String, Object>> list = (List)maps.get("result");
                        LOGGER.info("scanStock list:{}", JSONUtils.toString(list));
                        if (!CollectionUtils.isEmpty(list)) {
                            Map<String, Object> map = list.get(0);
                            String stockIsEnough = (String)map.get("stockIsEnough");
                            if ("1".equals(stockIsEnough)) {
                                return "有库存";
                            }
                        } else {
                            return "无库存";
                        }
                    }
                }catch (Exception ex){
                    LOGGER.error("scanStock error drugId:{}, {}", drugId, ex.getMessage(), ex);
                    return "无库存";
                }
            }
        }catch (Exception e){
            LOGGER.info("getDrugInventory error:{}.", e.getMessage(), e);
            return "无库存";
        }
        return "无库存";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        List<String> result = new ArrayList<>();
        if (new Integer(1).equals(flag)) {
            for (RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                String inventory = getDrugInventory(recipeDetailBean.getDrugId(), drugsEnterprise, drugsDataBean.getOrganId());
                if (StringUtils.isNotEmpty(inventory) && "有库存".equals(inventory)) {
                    result.add(recipeDetailBean.getDrugName());
                }
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        try{
            String stockMethod = "getMedicineStock";
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
            for (Recipedetail recipedetail : recipedetails) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
                if (saleDrugList != null) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("sku", saleDrugList.getOrganDrugCode());
                    params.put("pageNo","1");
                    params.put("pageSize","100");
                    String parames = jsonToXml(params);
                    LOGGER.info("scanStock parames:{}.", parames);
                    EsbWebService xkyyHelper = new EsbWebService();
                    Map<String, String> param=new HashMap<String, String>();
                    param.put("url", drugsEnterprise.getBusinessUrl());
                    xkyyHelper.initConfig(param);
                    try{
                        String webServiceResult = xkyyHelper.HXCFZT(parames, stockMethod);
                        System.out.println("result:"+webServiceResult);
                        Map maps = (Map)JSON.parse(webServiceResult);
                        Boolean success = (Boolean) maps.get("success");
                        String code = (String) maps.get("code");
                        if (success && "0".equals(code)) {
                            List<Map<String, Object>> list = (List)maps.get("result");
                            LOGGER.info("scanStock list:{}", JSONUtils.toString(list));
                            if (!CollectionUtils.isEmpty(list)) {
                                Map<String, Object> map = list.get(0);
                                String stockIsEnough = (String)map.get("stockIsEnough");
                                if ("0".equals(stockIsEnough)) {
                                    getDrugEnterpriseResult(result, "药品库存不足");
                                    return result;
                                }
                            } else {
                                getDrugEnterpriseResult(result, "药品库存不足");
                                return result;
                            }

                        }
                    }catch (Exception ex){
                        LOGGER.error("scanStock error recipeId:{}, {}", recipeId, ex.getMessage(), ex);
                        getDrugEnterpriseResult(result, "药品库存不足");
                        return result;
                    }

                } else {
                    getDrugEnterpriseResult(result, "药品不存在");
                    return result;
                }
            }
        }catch(Exception e){
            LOGGER.info("scanStock error:{}.", e.getMessage(), e);
            getDrugEnterpriseResult(result, "药品不存在");
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        /*DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //获取HIS处方状态更新平台处方信息
        //查询物流信息更新物流状态
        String getStatusUrl = "prsPrescriptionService/getPrescriptionStatus";
        List<Recipe> recipes = recipeDAO.findReadyToSendRecipeByDepId(drugsEnterprise.getId());
        for (Recipe recipe : recipes) {
            //获取处方状态
            Map<String, Object> params = new HashMap<>();
            params.put("prescripNo", recipe.getRecipeCode());
            String parames = JSONUtils.toString(params);
            JSONObject jsonObject = sendRequest(drugsEnterprise.getBusinessUrl()+getStatusUrl, parames, drugsEnterprise);
            if (jsonObject != null) {
                Boolean success = jsonObject.getBoolean("success");
                String code = jsonObject.getString("code");
                if ("0".equals(code) && success) {
                    Integer drugResult = jsonObject.getInteger("result");
                    LOGGER.info("syncEnterpriseDrug drugResult:{}.", drugResult);
                    if (drugResult != null) {
                        ThirdEnterpriseCallService thirdEnterpriseCallService = ApplicationUtils.getService(ThirdEnterpriseCallService.class, "takeDrugService");
                        if (drugResult == 43) {
                            //已经配送待签收,同步配送信息 准备配送
                            Map<String, Object> paramMap = new HashMap<>();
                            paramMap.put("recipeId", recipe.getRecipeId());
                            paramMap.put("sendDate", DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
                            paramMap.put("sender", "上海益友");
                            thirdEnterpriseCallService.readyToSend(paramMap);
                            //配送中
                            Map<String, Object> toSendParamMap = new HashMap<>();
                            toSendParamMap.put("recipeId", recipe.getRecipeId());
                            toSendParamMap.put("sendDate", DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
                            toSendParamMap.put("sender", "上海益友");
                            toSendParamMap.put("logisticsCompany", 16);
                            toSendParamMap.put("trackingNumber","L2003251318067955");
                            thirdEnterpriseCallService.toSend(toSendParamMap);
                        } else if (drugResult == 50) {
                            //配送完成
                            Map<String, Object> finishParamMap = new HashMap<>();
                            finishParamMap.put("recipeId", recipe.getRecipeId());
                            finishParamMap.put("sendDate", DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
                            finishParamMap.put("sender", "上海益友");
                            thirdEnterpriseCallService.finishRecipe(finishParamMap);
                        }
                    }
                }
            }
        }*/
        return DrugEnterpriseResult.getSuccess();
    }

    @RpcService
    public void cancelOrders(Integer recipeId, Integer depId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        pushRecipeInfoForSy(drugsEnterprise, result, recipe, 0);
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_EBS;
    }

    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        LOGGER.info(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
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

    private String jsonToXml(Map<String, Object> params){
        StringBuilder result = new StringBuilder("<root><body><params>");
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                result.append("<").append(entry.getKey()).append(">").append(entry.getValue()).append("</").append(entry.getKey()).append(">");
            }
        }
        result.append("</params></body></root>");
        return result.toString();
    }

    private JSONObject sendRequest(String url, String json, DrugsEnterprise drugsEnterprise) {
        try{
            LOGGER.info("sendRequest input:{},{}.", url, json);
            String APP_ID = drugsEnterprise.getUserId();
            String APP_SECRET = drugsEnterprise.getPassword();
            long timestamp = System.currentTimeMillis();
            HttpPost method = new HttpPost(url);
            method.addHeader("ACCESS_APPID", APP_ID);
            method.addHeader("ACCESS_TIMESTAMP", String.valueOf(timestamp));
            method.addHeader("ACCESS_SIGANATURE", AppSiganatureUtils.createSiganature(json, APP_ID, APP_SECRET,
                    timestamp));
            method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            HttpClient httpClient = HttpClientUtils.getHttpClient();
            HttpResponse httpResponse = httpClient.execute(method);
            HttpEntity entity = httpResponse.getEntity();
            String response = EntityUtils.toString(entity);
            JSONObject jsonObject = JSON.parseObject(response);
            LOGGER.info("sendRequest output:{}.", JSONUtils.toString(jsonObject));
            return jsonObject;
        }catch(Exception e){
            LOGGER.error("sendRequest error :{},", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 生成完整地址
     *
     * @param order 订单
     * @return
     */
    public String getCompleteAddressForSH(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            this.getAddressDic(address, order.getAddress1());
            this.getAddressDic(address, order.getAddress3());
            this.getAddressDic(address, order.getStreetAddress());
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }

}
