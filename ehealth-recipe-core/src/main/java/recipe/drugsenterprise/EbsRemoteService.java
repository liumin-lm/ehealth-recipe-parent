package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
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
import recipe.service.RecipeLogService;
import recipe.util.AppSiganatureUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\4\15 0015 14:33
 */
@RpcBean("ebsRemoteService")
public class EbsRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbsRemoteService.class);

    @Resource
    private RecipeDAO recipeDAO;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Resource
    private RecipeOrderDAO recipeOrderDAO;

    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("EbsRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    @RpcService
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("EbsRemoteService.pushRecipeInfo recipeIds:{}", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        EbsBean ebsBean = new EbsBean();
        if (!CollectionUtils.isEmpty(recipeList)) {
            PatientService patientService = BasicAPI.getService(PatientService.class);
            OrganService organService = BasicAPI.getService(OrganService.class);
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);

            Recipe recipe = recipeList.get(0);
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());

            OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
            DepartmentDTO departmentDTO = departmentService.get(recipe.getDepart());
            PatientDTO patientDTO = patientService.getByMpiId(recipe.getMpiid());

            ebsBean.setPrescripNo(recipe.getRecipeCode());
            ebsBean.setPrescribeDate(recipe.getSignDate().getTime());
            ebsBean.setHospitalCode(organDTO.getOrganizeCode());
            ebsBean.setHospitalName(organDTO.getName());
            ebsBean.setDepartment(departmentDTO.getName());
            ebsBean.setDoctorName(recipe.getDoctorName());
            ebsBean.setName(recipe.getPatientName());
            if (patientDTO != null && new Integer(1).equals(patientDTO.getPatientSex())) {
                ebsBean.setSex(1);
            } else {
                ebsBean.setSex(0);
            }
            ebsBean.setAge(patientDTO.getAge());
            ebsBean.setMobile(patientDTO.getMobile());
            ebsBean.setIdCard(patientDTO.getCertificate());
            ebsBean.setSocialSecurityCard("");
            ebsBean.setAddress("");
            if (recipeOrder != null && new Integer(0).equals(recipeOrder.getOrderType())) {
                ebsBean.setFeeType(0);
            } else {
                ebsBean.setFeeType(1);
            }
            ebsBean.setDistrictName(recipe.getOrganDiseaseName());
            if (recipeOrder != null) {
                ebsBean.setTotalAmount(recipeOrder.getTotalFee());
                ebsBean.setReceiver(recipeOrder.getReceiver());
                ebsBean.setReceiverMobile(recipeOrder.getRecMobile());
                String province = getAddressDic(recipeOrder.getAddress1());
                String city = getAddressDic(recipeOrder.getAddress2());
                String district = getAddressDic(recipeOrder.getAddress3());
                ebsBean.setProvinceName(province);
                ebsBean.setCityName(city);
                ebsBean.setDistrictName(district);
            }
            ebsBean.setRemark(recipe.getMemo());
            List<EbsDetail> details = new ArrayList<>();
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            for (Recipedetail recipedetail : recipedetails) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
                if (saleDrugList != null) {
                    EbsDetail ebsDetail = new EbsDetail();
                    ebsDetail.setMedName(saleDrugList.getDrugName());
                    ebsDetail.setMedCode(saleDrugList.getOrganDrugCode());
                    ebsDetail.setSpec(saleDrugList.getDrugSpec());
                    ebsDetail.setDrugForm(recipedetail.getDrugForm());
                    try{
                        Dictionary usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
                        Dictionary usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
                        ebsDetail.setDirections(usingRateDic.getText(recipedetail.getUsingRate()) + usePathwaysDic.getText(recipedetail.getUsePathways()));
                    }catch(Exception e){
                        LOGGER.error("pushRecipeInfo 用法用量获取失败.");
                    }
                    ebsDetail.setUnitName(recipedetail.getDrugUnit());
                    ebsDetail.setAmount(recipedetail.getUseTotalDose());
                    ebsDetail.setUnitPrice(saleDrugList.getPrice().doubleValue());
                    details.add(ebsDetail);
                }
                ebsBean.setDetails(details);
            }
            //以下开始推送处方信息
            String sendRecipeUrl = "prescriptionService/addSingle";
            String url = enterprise.getBusinessUrl() + sendRecipeUrl;
            String json = JSONObject.toJSONString(ebsBean);

            JSONObject jsonObject = sendRequest(url, json);
            if (jsonObject != null) {
                Boolean success = jsonObject.getBoolean("success");
                String code = jsonObject.getString("code");
                if("0".equals(code) && success){
                    //表示推送成功
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "处方推送成功");
                } else {
                    //表示推送失败
                    getDrugEnterpriseResult(result, "处方推送失败");
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "处方推送失败");
                }
            } else {
                LOGGER.info("pushRecipeInfo recipeId:{}", recipeIds.get(0));
                getDrugEnterpriseResult(result, "获取上药返回数据为空");
                //表示推送失败
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "处方推送失败");
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "";
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String stockUrl = "partnerService/getMedicineStock";
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        for (Recipedetail recipedetail : recipedetails) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            String parames = "{\"sku\":\""+saleDrugList.getOrganDrugCode()+"\",\"pageNo\":1,\"pageSize\":\"100\"}";
            JSONObject jsonObject = sendRequest(drugsEnterprise.getBusinessUrl()+stockUrl, parames);
            if (jsonObject != null) {
                Boolean success = jsonObject.getBoolean("success");
                String code = jsonObject.getString("code");
                if ("0".equals(code) && success) {
                    List drugResult = jsonObject.getJSONArray("result");
                    LOGGER.info("scanStock drugResult:{}.", JSONUtils.toString(drugResult));

                } else {
                    getDrugEnterpriseResult(result, "药品库存不足");
                    return result;
                }
            } else {
                getDrugEnterpriseResult(result, "药品库存不足");
                return result;
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        //获取HIS处方状态更新平台处方信息
        return DrugEnterpriseResult.getSuccess();
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
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area);
            }
        }
        return "";
    }

    private JSONObject sendRequest(String url, String json) {
        try{
            LOGGER.info("sendRequest input:{},{}.", url, json);
            RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            String APP_ID = recipeParameterDao.getByName("logistics_shsy_app_id");
            String APP_SECRET = recipeParameterDao.getByName("logistics_shsy_app_secret");
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
}
