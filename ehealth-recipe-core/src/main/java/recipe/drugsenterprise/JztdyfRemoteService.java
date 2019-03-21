package recipe.drugsenterprise;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
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
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.service.RecipeOrderService;
import recipe.service.common.RecipeCacheService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * 九州通药企
 * @author yinsheng
 * @date 2019\3\14 0014 11:15
 */
public class JztdyfRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JztdyfRemoteService.class);

    private String APP_ID;

    private String APP_KEY;

    private String APP_SECRET;

    public JztdyfRemoteService() {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        APP_ID = cacheService.getParam("jzt_appid");
        APP_KEY = cacheService.getParam("jzt_appkey");
        APP_SECRET = cacheService.getParam("jzt_appsecret");
    }

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        String depName = drugsEnterprise.getName();
        Integer depId = drugsEnterprise.getId();
        // 创建默认的httpClient实例.
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            if (drugsEnterprise.getAuthenUrl().contains("https:")) {
                HttpPost httpPost = new HttpPost(drugsEnterprise.getAuthenUrl());
                JztTokenRequest request = new JztTokenRequest();
                String timestamp = getTimestamp();
                String nonce = getNonce();
                //组装请求参数
                String signature = getSignature(APP_KEY, nonce, timestamp, APP_SECRET);

                request.setApp_id(APP_ID);
                request.setApp_key(APP_KEY);
                request.setNonce(nonce);
                request.setSignature(signature);
                request.setTimestamp(timestamp);

                StringEntity requestEntity = new StringEntity(JSONUtils.toString(request), ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);

                //获取响应消息
                CloseableHttpResponse response = httpclient.execute(httpPost);
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                LOGGER.info("[{}][{}]token更新返回:{}", depId, depName, responseStr);
                JztTokenResponse jztResponse = JSONUtils.parse(responseStr, JztTokenResponse.class);
                if (jztResponse.getCode() == 200 && jztResponse.isSuccess()) {
                    //成功
                    drugsEnterpriseDAO.updateTokenById(depId, jztResponse.getData().getAccess_token());
                } else {
                    //失败
                    LOGGER.info("[{}][{}]token更新失败:{}", depId, depName, jztResponse.getMsg());
                }
                //关闭 HttpEntity 输入流
                EntityUtils.consume(httpEntity);
                response.close();
            }
        } catch (Exception e) {
            LOGGER.warn("[{}][{}]更新异常。", depId, depName, e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                LOGGER.warn("资源关闭失败。", e);
            }
        }
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (StringUtils.isEmpty(enterprise.getBusinessUrl())) {
           return getDrugEnterpriseResult(result,"药企处理业务URL为空");
        }
        if (CollectionUtils.isEmpty(recipeIds)) {
            return getDrugEnterpriseResult(result,"处方ID参数为空");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //准备处方数据
        JztRecipeDTO jztRecipe = new JztRecipeDTO();
        Integer depId = enterprise.getId();
        String depName = enterprise.getName();
        String orderCode ;
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isNotEmpty(recipeList)) {
            Recipe dbRecipe = recipeList.get(0);
            //加入订单信息
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
            if (dbRecipe.getClinicOrgan() == null) {
                LOGGER.warn("机构编码不存在,处方ID:{}.", dbRecipe.getRecipeId());
                return getDrugEnterpriseResult(result, "机构编码不存在");
            }
            jztRecipe.setClinicOrgan(converToString(dbRecipe.getClinicOrgan()));
            setJztRecipeInfo(jztRecipe, dbRecipe);
            if (!setJztRecipePatientInfo(jztRecipe, dbRecipe.getMpiid())) return getDrugEnterpriseResult(result, "患者不存在");
            if (!setJztRecipeDoctorInfo(jztRecipe, dbRecipe)) return getDrugEnterpriseResult(result, "医生或主执业点不存在");
            if (!setJztRecipeOrderInfo(order, jztRecipe, dbRecipe)) return getDrugEnterpriseResult(result, "订单不存在");
            if (!setJztRecipeDetailInfo(jztRecipe, dbRecipe.getRecipeId(), depId)) return getDrugEnterpriseResult(result, "处方详情不存在");

            orderCode = order.getOrderCode();
            //推送给九州通
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try{
                HttpPost httpPost = new HttpPost(enterprise.getBusinessUrl());
                //组装请求参数
                httpPost.setHeader("app-key", APP_KEY);
                httpPost.setHeader("access-token", enterprise.getToken());
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
                    orderService.updateOrderInfo(orderCode, ImmutableMap.of("pushFlag", 1), null);
                    LOGGER.info("[{}][{}] pushRecipeInfo {} success.", depId, depName, JSONUtils.toString(recipeIds));
                } else {
                    //失败
                    result.setMsg(jztResponse.getMsg());
                    orderService.updateOrderInfo(orderCode, ImmutableMap.of("pushFlag", -1), null);
                    LOGGER.warn("[{}][{}] pushRecipeInfo {} fail. msg={}", depId, depName,
                            JSONUtils.toString(recipeIds), jztResponse.getMsg());
                }
                //关闭 HttpEntity 输入流
                EntityUtils.consume(httpEntity);
                response.close();
            }catch (Exception e) {
                result.setMsg("推送异常");
                orderService.updateOrderInfo(orderCode, ImmutableMap.of("pushFlag", -1), null);
                LOGGER.warn("[{}][{}] pushRecipeInfo 异常。", depId, depName, e);
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                   //e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
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
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, DrugsEnterprise enterprise) {
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
                jztRecipe.setDoctorName(doctor.getName());
                jztRecipe.setDoctorNumber(employment.getJobNumber());
                jztRecipe.setDepartId(converToString(employment.getDepartment()));
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
        jztRecipe.setCreateDate(dbRecipe.getSignDate().toString());
        jztRecipe.setOrganDiseaseName(dbRecipe.getOrganDiseaseName());
        jztRecipe.setOrganDiseaseId(dbRecipe.getOrganDiseaseId());
        jztRecipe.setStatus(converToString(dbRecipe.getStatus()));
        jztRecipe.setPayMode(converToString(dbRecipe.getPayMode()));
        jztRecipe.setPayFlag(converToString(dbRecipe.getPayFlag()));
        jztRecipe.setGiveMode(converToString(dbRecipe.getGiveMode()));
        jztRecipe.setGiveUser(dbRecipe.getGiveUser());
        jztRecipe.setMedicalPayFlag(converToString(dbRecipe.getMedicalPayFlag()));
        jztRecipe.setDistributionFlag(converToString(dbRecipe.getDistributionFlag()));
        jztRecipe.setRecipeMemo(dbRecipe.getRecipeMemo());
        jztRecipe.setTcmUsePathways(dbRecipe.getTcmUsePathways());
        jztRecipe.setTcmUsingRate(dbRecipe.getTcmUsingRate());
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
            OrganDrugListDAO organDrugDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

            List<OrganDrugList> organDrugList = organDrugDAO.findByOrganIdAndDrugIds(depId, drugIdList);

            List<DrugList> drugList = drugDAO.findByDrugIds(drugIdList);
            if (detailList.size() != organDrugList.size() || organDrugList.size() != drugList.size()) {
                LOGGER.warn("药品数据存在问题,recipeId:{}.", recipeId);
                return false;
            }

            Map<Integer, OrganDrugList> organDrugMap = Maps.uniqueIndex(organDrugList, new Function<OrganDrugList, Integer>() {
                @Override
                public Integer apply(OrganDrugList input) {
                    return input.getDrugId();
                }
            });
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
                jztDetail.setDrugCode(organDrugMap.get(drugId).getOrganDrugCode());
                jztDetail.setDrugName(detail.getDrugName());
                jztDetail.setSpecification(detail.getDrugSpec());
                jztDetail.setLicenseNumber(drugMap.get(drugId).getApprovalNumber());
                jztDetail.setProducer(drugMap.get(drugId).getProducer());
                jztDetail.setTotal(converToString(detail.getUseTotalDose()));
                jztDetail.setUseDose(converToString(detail.getUseDose()));
                jztDetail.setDrugFee(detail.getSalePrice().toPlainString());
                jztDetail.setDrugTotalFee(detail.getDrugCost().toPlainString());
                jztDetail.setUesDays(converToString(detail.getUseDays()));
                jztDetail.setUsingRate(detail.getUsingRate());
                jztDetail.setUsePathways(detail.getUsePathways());
                jztDetail.setMemo(detail.getMemo());
                jztDetail.setStandardCode(drugMap.get(drugId).getStandardCode());
                jztDetail.setDrugForm(drugMap.get(drugId).getDrugForm());
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
     * 设置九州通订单信息
     * @param order      订单信息
     * @param jztRecipe  九州通处方信息
     * @param dbRecipe   平台处方单
     * @return           是否设置成功
     */
    private boolean setJztRecipeOrderInfo(RecipeOrder order, JztRecipeDTO jztRecipe, Recipe dbRecipe) {
        if (ObjectUtils.isEmpty(order)) {
            LOGGER.warn("处方单不存在,recipeId:{}.", dbRecipe.getRecipeId());
            return false;
        } else {
            jztRecipe.setRecipeFee(order.getRecipeFee().toPlainString());
            jztRecipe.setActualFee(converToString(order.getActualPrice()));
            jztRecipe.setCouponFee(order.getCouponFee().toPlainString());
            jztRecipe.setDecoctionFee(order.getDecoctionFee().toPlainString());
            jztRecipe.setOrderTotalFee(order.getTotalFee().toPlainString());
            jztRecipe.setExpressFee(order.getExpressFee().toPlainString());
        }
        return true;
    }

    /**
     * toString
     * @param o 源
     * @return  目标
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
