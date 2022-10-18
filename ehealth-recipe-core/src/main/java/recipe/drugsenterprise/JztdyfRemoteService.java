package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.JztDrugDTO;
import recipe.drugsenterprise.bean.JztRecipeDTO;
import recipe.drugsenterprise.bean.JztTokenRequest;
import recipe.drugsenterprise.bean.JztTokenResponse;
import recipe.service.common.RecipeCacheService;
import recipe.third.IFileDownloadService;
import recipe.util.Base64;
import recipe.util.DateConversion;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 九州通药企
 * @author yinsheng
 * @date 2019\3\14 0014 11:15
 */
@RpcBean(value = "jztdyfRemoteService", mvc_authentication = false)
public class JztdyfRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JztdyfRemoteService.class);

    private String APP_KEY;
    private String SERVER_CODE;
    private String SERVER_SECRET;

    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    public JztdyfRemoteService() {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        SERVER_CODE = recipeParameterDao.getByName("jzt_appid");
        APP_KEY = recipeParameterDao.getByName("jzt_appkey");
        SERVER_SECRET = recipeParameterDao.getByName("jzt_appsecret");
    }

    @RpcService
    @LogRecord
    public DrugsEnterprise test(Integer depId, List<Integer> recipeList){
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        pushRecipeInfo(recipeList, drugsEnterprise);
        return drugsEnterprise;
    }

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(drugsEnterprise.getAuthenUrl());
        String request = "{\"serverCode\":\""+SERVER_CODE+"\",\"serverSecret\":\""+SERVER_SECRET+"\"}";
        StringEntity requestEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);
        httpPost.setHeader("APP-KEY", APP_KEY);
        try {
            //获取响应消息
            CloseableHttpResponse response = httpclient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("[{}][{}]token更新返回:{}", drugsEnterprise.getId(), drugsEnterprise.getName(), responseStr);
            JSONObject jsonObject = JSON.parseObject(responseStr);
            Map<String, Object> drugMap = jsonObject;
            Integer code = (Integer) drugMap.get("code");
            if (200 == code) {
                Map<String, Object> data = (Map<String, Object>) drugMap.get("data");
                String token = (String) data.get("token");
                drugsEnterpriseDAO.updateTokenById(drugsEnterprise.getId(), token);
            }
        } catch (IOException e) {
            LOGGER.error("JztdyfRemoteService tokenUpdateImpl error", e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                LOGGER.warn("资源关闭失败。", e);
            }
        }
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (CollectionUtils.isEmpty(recipeIds)) {
            return getDrugEnterpriseResult(result,"没有获取到处方信息");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //准备处方数据
        JztRecipeDTO jztRecipe = new JztRecipeDTO();
        Integer depId = enterprise.getId();
        String depName = enterprise.getName();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        OrganService organService = BasicAPI.getService(OrganService.class);
        if (CollectionUtils.isNotEmpty(recipeList)) {
            Recipe dbRecipe = recipeList.get(0);
            getMedicalInfo(dbRecipe);
            if (dbRecipe.getClinicOrgan() == null) {
                LOGGER.warn("机构编码不存在,处方ID:{}.", dbRecipe.getRecipeId());
                return getDrugEnterpriseResult(result, "机构编码不存在");
            }
            String organCode = organService.getOrganizeCodeByOrganId(dbRecipe.getClinicOrgan());
            if (StringUtils.isNotEmpty(organCode)) {
                jztRecipe.setOrganId("12420106441364790P");
                jztRecipe.setOrganName("水果湖街社区卫生服务中心");
            } else {
                LOGGER.warn("机构不存在,处方ID:{}.", dbRecipe.getRecipeId());
                return getDrugEnterpriseResult(result, "机构不存在");
            }
            jztRecipe.setClinicOrgan("1001780");
            setJztRecipeInfo(jztRecipe, dbRecipe);
            if (!setJztRecipePatientInfo(jztRecipe, dbRecipe.getMpiid())) return getDrugEnterpriseResult(result, "患者不存在");
            if (!setJztRecipeDoctorInfo(jztRecipe, dbRecipe)) return getDrugEnterpriseResult(result, "医生或主执业点不存在");
            if (!setJztRecipeDetailInfo(jztRecipe, dbRecipe.getRecipeId(), depId)) return getDrugEnterpriseResult(result, "处方详情不存在");

            //推送给九州通
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try{
                HttpPost httpPost = new HttpPost(enterprise.getBusinessUrl());
                //组装请求参数
                httpPost.setHeader("APP-KEY", APP_KEY);
                httpPost.setHeader("TOKEN", enterprise.getToken());
                String requestStr = JSONUtils.toString(jztRecipe);
                LOGGER.info("[{}][{}] pushRecipeInfo send :{}", depId, depName, requestStr);
                StringEntity requestEntry = new StringEntity(requestStr, ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntry);

                //获取响应消息
                CloseableHttpResponse response = httpClient.execute(httpPost);
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                LOGGER.info("[{}][{}] pushRecipeInfo 返回:{}", depId, depName, responseStr);
                JztTokenResponse jztResponse = JSONUtils.parse(responseStr, JztTokenResponse.class);
                if (jztResponse.getCode() == 200 && jztResponse.isSuccess()) {
                    //成功
                    result.setCode(DrugEnterpriseResult.SUCCESS);
                    //说明成功,更新处方标志
                    recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), ImmutableMap.of("pushFlag", 1));
                    LOGGER.info("[{}][{}] pushRecipeInfo {} success.", depId, depName, JSONUtils.toString(recipeIds));
                } else {
                    //失败
                    result.setMsg(jztResponse.getMsg());
                    LOGGER.warn("[{}][{}] pushRecipeInfo {} fail. msg={}", depId, depName,
                            JSONUtils.toString(recipeIds), jztResponse.getMsg());
                }
                //关闭 HttpEntity 输入流
                EntityUtils.consume(httpEntity);
                response.close();
            }catch (Exception e) {
                result.setMsg("推送异常");
                LOGGER.warn("[{}][{}] pushRecipeInfo 异常。", depId, depName, e);
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                   LOGGER.error("pushRecipeInfo error ", e);
                }
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
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
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_JZTDYF;
    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        return result;
    }

    /**
     * 设置九州通处方中患者信息
     * @param jztRecipe   九州通处方详情
     * @param mpiId       患者mpiId
     * @return            是否设置成功
     */
    private boolean setJztRecipePatientInfo(JztRecipeDTO jztRecipe, String mpiId) {
        PatientService patientService = BasicAPI.getService(PatientService.class);
        //加入患者信息
        PatientDTO patient = patientService.get(mpiId);
        if (ObjectUtils.isEmpty(patient)) {
            LOGGER.warn("患者不存在,mpiId:{}.", mpiId);
            return false;
        } else {
            jztRecipe.setPatientName(patient.getPatientName());
            jztRecipe.setPatientTel(patient.getMobile());
            jztRecipe.setCertificateType(converToString(patient.getCertificateType()));
            jztRecipe.setCertificate(patient.getCertificate());
            jztRecipe.setPatientAddress(converToString(patient.getAddress()));
            jztRecipe.setPatientSex(patient.getPatientSex());
        }
        return true;
    }

    /**
     * 设置九州通医生信息
     * @param jztRecipe  九州通处方详情
     * @param dbRecipe   处方信息
     * @return           是否设置成功
     */
    private boolean setJztRecipeDoctorInfo(JztRecipeDTO jztRecipe, Recipe dbRecipe) {
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        //加入医生信息
        DoctorDTO doctor = doctorService.get(dbRecipe.getDoctor());
        if (!ObjectUtils.isEmpty(doctor)) {
            EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(dbRecipe.getDoctor());
            if (null != employment) {
                jztRecipe.setDoctorName("张玲");
                jztRecipe.setDoctorNumber("001003");
                jztRecipe.setDepartId(converToString(employment.getDepartment()));
                jztRecipe.setDepartName(converToString(employment.getDeptName()));
            } else {
                LOGGER.warn("医生执业点不存在,recipeId:{}.", dbRecipe.getRecipeId());
                return false;
            }
        } else {
                LOGGER.warn("医生不存在,recipeId:{}.", dbRecipe.getRecipeId());
                return false;
        }
        return true;
    }

    /**
     * 设置九州通处方信息
     * @param jztRecipe 九州通处方详情
     * @param dbRecipe  处方信息
     */
    private void setJztRecipeInfo(JztRecipeDTO jztRecipe, Recipe dbRecipe) {
        //加入处方信息
        jztRecipe.setRecipeCode(dbRecipe.getRecipeCode());
        jztRecipe.setRecipeType(converToString(dbRecipe.getRecipeType()));
        jztRecipe.setCreateDate(DateConversion.getDateFormatter(dbRecipe.getSignDate(),DateConversion.DEFAULT_DATETIME_WITHSECOND));
        jztRecipe.setOrganDiseaseName(converToString(dbRecipe.getOrganDiseaseName()));
        jztRecipe.setOrganDiseaseId(converToString(dbRecipe.getOrganDiseaseId()));
        jztRecipe.setRecipeFee(converToString(dbRecipe.getTotalMoney()));
        jztRecipe.setActualFee(converToString(dbRecipe.getActualPrice()));
        jztRecipe.setCouponFee(converToString(dbRecipe.getDiscountAmount()));
        jztRecipe.setDecoctionFee(""); //待煎费
        jztRecipe.setMedicalFee("");   //医保报销
        jztRecipe.setExpressFee("");   //配送费
        jztRecipe.setOrderTotalFee(converToString(dbRecipe.getOrderAmount()));
        jztRecipe.setStatus(converToString(dbRecipe.getStatus()));
        jztRecipe.setPayFlag(converToString(dbRecipe.getPayFlag()));
        jztRecipe.setGiveMode(converToString(dbRecipe.getGiveMode()));
        jztRecipe.setGiveUser(converToString(dbRecipe.getGiveUser()));
        jztRecipe.setMedicalPayFlag(converToString(dbRecipe.getMedicalPayFlag()));
        jztRecipe.setDistributionFlag(converToString(dbRecipe.getDistributionFlag()));
        jztRecipe.setRecipeMemo(converToString(dbRecipe.getRecipeMemo()));
        jztRecipe.setTcmUsePathways(converToString(dbRecipe.getTcmUsePathways()));
        jztRecipe.setTcmUsingRate(converToString(dbRecipe.getTcmUsingRate()));
        jztRecipe.setMemo(converToString(dbRecipe.getRecipeMemo()));
        jztRecipe.setTcmNum("");
        jztRecipe.setDistributorCode("");
        jztRecipe.setDistributorName("");
        jztRecipe.setPharmacyCode("");
        jztRecipe.setPharmacyName("");
        //设置base64位图片
        String ossId = dbRecipe.getSignImg();
        if (StringUtils.isNotEmpty(ossId)) {
            try {
                IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
                String imgStr = "data:image/jpeg;base64," + fileDownloadService.downloadImg(ossId);
                jztRecipe.setSignImg(imgStr);
            } catch (Exception e) {
                LOGGER.error("setJztRecipeInfo setSignImg error", e);
            }
        }
    }

    /**
     * 设置九州通药品详情信息
     * @param jztRecipe  九州通处方详情
     * @param recipeId   处方id
     * @param depId      depId
     * @return           是否设置成功
     */
    private boolean setJztRecipeDetailInfo(JztRecipeDTO jztRecipe, Integer recipeId, Integer depId){
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(detailList)) {
            List<JztDrugDTO> jztDetailList = new ArrayList<>(detailList.size());
            List<Integer> drugIdList = Lists.newArrayList(Collections2.transform(detailList, new Function<Recipedetail, Integer>() {
                @Nullable
                @Override
                public Integer apply(@Nullable Recipedetail input) {
                    return input.getDrugId();
                }
            }));

            DrugListDAO drugDAO = DAOFactory.getDAO(DrugListDAO.class);

            List<DrugList> drugList = drugDAO.findByDrugIds(drugIdList);

            Map<Integer, DrugList> drugMap = Maps.uniqueIndex(drugList, new Function<DrugList, Integer>() {
                @Override
                public Integer apply(DrugList input) {
                    return input.getDrugId();
                }
            });
            JztDrugDTO jztDetail;
            Integer drugId;
            for (Recipedetail detail : detailList) {
                jztDetail = new JztDrugDTO();
                drugId = detail.getDrugId();
                jztDetail.setDrugCode(converToString(detail.getOrganDrugCode()));
                jztDetail.setDrugName(converToString(detail.getDrugName()));
                jztDetail.setSpecification(converToString(detail.getDrugSpec()));
                jztDetail.setProducer(drugMap.get(drugId).getProducer());
                jztDetail.setTotal(converToString(detail.getUseTotalDose()));
                jztDetail.setUseDose(converToString(detail.getUseDose()));
                jztDetail.setUseDoseUnit(converToString(detail.getUseDoseUnit()));
                jztDetail.setDrugFee(detail.getSalePrice().toPlainString());
                jztDetail.setDrugTotalFee(detail.getDrugCost().toPlainString());
                jztDetail.setUesDays(null!=detail.getUseDays()?detail.getUseDays().intValue():1);
                jztDetail.setUsingRate(converToString(detail.getUsingRate()));
                jztDetail.setUsePathways(converToString(detail.getUsePathways()));
                jztDetail.setMemo(converToString(detail.getMemo()));
                jztDetail.setDrugForm(converToString(drugMap.get(drugId).getDrugForm()));
                jztDetailList.add(jztDetail);
            }
            jztRecipe.setDrugList(jztDetailList);
        } else {
            LOGGER.warn("处方详情不存在,recipeId:{}.", recipeId);
            return false;
        }
        return true;
    }

    /**
     * 字符串输出
     * @param o 源对象
     * @return  目标输出
     */
    private static String converToString(Object o) {
        if (o != null) {
            return o.toString();
        }
        return "";
    }

    /**
     * 九州通生成签名
     * @param appkey     应用Key
     * @param nonce      随机字符串
     * @param timestamp  时间戳
     * @param appsecret  secret
     * @return           秘钥
     */
    private static String getSignature(String appkey, String nonce, String timestamp,String appsecret) {
        String target = appkey + nonce + timestamp + appsecret;
        return DigestUtils.md5Hex(target.toLowerCase());
    }

    /**
     * 获取时间戳
     * @return 返回时间戳
     */
    private static String getTimestamp() {
        return System.currentTimeMillis() + "";
    }

    /**
     * 获取随机字符串
     * @return 随机字符串
     */
    private static String getNonce() {
        String uuid = UUID.randomUUID().toString();
        return StringUtils.remove(uuid, "-");
    }
}
