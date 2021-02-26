package recipe.drugsenterprise;

import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.beanutils.BeanUtils;
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
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseResponse;
import recipe.constant.CacheConstant;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.ZfbDrugDTO;
import recipe.drugsenterprise.bean.ZfbRecipeDTO;
import recipe.drugsenterprise.bean.ZfbTokenRequest;
import recipe.drugsenterprise.bean.ZfbTokenResponse;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;
import recipe.util.RSAUtil;
import recipe.util.RedisClient;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 支付宝配送商实现
 * @version： 1.0
 */
public class ZfbRemoteService extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ZfbRemoteService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private TaobaoConf taobaoConf;

    @Autowired
    TmdyfRemoteService tmdyfRemoteService;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        String depName = drugsEnterprise.getName();
        Integer depId = drugsEnterprise.getId();
        // 创建默认的httpClient实例.
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            if (-1 != drugsEnterprise.getAuthenUrl().indexOf("http:")) {
                HttpPost httpPost = new HttpPost(drugsEnterprise.getAuthenUrl());
                //组装请求参数
                String sign = RSAUtil.privateEncrypt(RSAUtil.getAppid() + Calendar.getInstance().getTimeInMillis(),
                        RSAUtil.getPrivateKey());
                ZfbTokenRequest request = new ZfbTokenRequest();
                request.setSign(sign);
                request.setAppid(RSAUtil.getAppid());
                StringEntity requestEntity = new StringEntity(JSONUtils.toString(request), ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);

                //获取响应消息
                CloseableHttpResponse response = httpclient.execute(httpPost);
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                LOGGER.info("[{}][{}]token更新返回:{}", depId, depName, responseStr);
                ZfbTokenResponse zfbResponse = JSONUtils.parse(responseStr, ZfbTokenResponse.class);
                if ("0".equals(zfbResponse.getCode())) {
                    //成功
                    drugsEnterpriseDAO.updateTokenById(depId, zfbResponse.getToken());
                } else {
                    //失败
                    LOGGER.warn("[{}][{}]token更新失败:{}", depId, depName, zfbResponse.getMsg());
                }
                //关闭 HttpEntity 输入流
                EntityUtils.consume(httpEntity);
                response.close();
                httpclient.close();
            }
        } catch (Exception e) {
            LOGGER.warn("[{}][{}]更新异常。", depId, depName, e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
//                e.printStackTrace();
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
    public void getJumpUrl(PurchaseResponse response, Recipe recipe, DrugsEnterprise drugsEnterprise) {
        tmdyfRemoteService.getJumpUrl(response, recipe, drugsEnterprise);
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (StringUtils.isEmpty(enterprise.getBusinessUrl())) {
            result.setMsg("药企处理业务URL为空");
            return result;
        }

        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID参数为空");
            return result;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

        ZfbRecipeDTO zfbRecipe = new ZfbRecipeDTO();
        String depName = enterprise.getName();
        Integer depId = enterprise.getId();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        String orderCode = "";
        if (CollectionUtils.isNotEmpty(recipeList)) {
            PatientService patientService = BasicAPI.getService(PatientService.class);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            OrganService organService = BasicAPI.getService(OrganService.class);
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);

            Recipe dbRecipe = recipeList.get(0);
            getMedicalInfo(dbRecipe);
            String organCode = organService.getOrganizeCodeByOrganId(dbRecipe.getClinicOrgan());
            if (StringUtils.isNotEmpty(organCode)) {
                zfbRecipe.setOrganId(organCode);
                zfbRecipe.setOrganName(dbRecipe.getOrganName());
            } else {
                result.setMsg("机构不存在");
                return result;
            }

            PatientDTO patient = patientService.get(dbRecipe.getMpiid());
            if (null != patient) {
                zfbRecipe.setCertificateType(patient.getCertificateType().toString());
                zfbRecipe.setCertificate(patient.getCertificate());
                zfbRecipe.setPatientName(patient.getPatientName());
                zfbRecipe.setPatientSex(patient.getPatientSex());
                zfbRecipe.setPatientTel(patient.getMobile());
                zfbRecipe.setPatientAddress(patient.getAddress());
            } else {
                result.setMsg("患者不存在");
                return result;
            }

            DoctorDTO doctor = doctorService.get(dbRecipe.getDoctor());
            if (null != doctor) {
                EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(dbRecipe.getDoctor());
                if (null != employment) {
                    zfbRecipe.setDoctorName(doctor.getName());
                    zfbRecipe.setDoctorNumber(employment.getJobNumber());
                    zfbRecipe.setDepartId(employment.getDepartment().toString());
                    DepartmentDTO departmentDTO = departmentService.getById(employment.getDepartment());
                    if (null != departmentDTO) {
                        zfbRecipe.setDepartName(departmentDTO.getName());
                    }
                } else {
                    result.setMsg("医生主执业点不存在");
                    return result;
                }
            } else {
                result.setMsg("医生不存在");
                return result;
            }

            zfbRecipe.setRecipeCode(dbRecipe.getRecipeCode());
            zfbRecipe.setClinicOrgan(dbRecipe.getClinicOrgan().toString());
            zfbRecipe.setDepartId(dbRecipe.getDepart().toString());
            zfbRecipe.setRecipeType(dbRecipe.getRecipeType().toString());
            zfbRecipe.setCreateDate(DateConversion.formatDateTimeWithSec(dbRecipe.getCreateDate()));
            zfbRecipe.setOrganDiseaseName(dbRecipe.getOrganDiseaseName());
            zfbRecipe.setOrganDiseaseId(dbRecipe.getOrganDiseaseId());
            zfbRecipe.setMemo(dbRecipe.getMemo());
//            zfbRecipe.setPayMode(null != dbRecipe.getPayMode() ? dbRecipe.getPayMode().toString() : "0");
            zfbRecipe.setGiveMode(null != dbRecipe.getGiveMode() ? dbRecipe.getGiveMode().toString() : "0");
            zfbRecipe.setPayFlag(dbRecipe.getPayFlag().toString());
            zfbRecipe.setGiveUser(dbRecipe.getGiveUser());
            zfbRecipe.setStatus(dbRecipe.getStatus().toString());
            zfbRecipe.setMedicalPayFlag(dbRecipe.getMedicalPayFlag().toString());
            zfbRecipe.setDistributionFlag(dbRecipe.getDistributionFlag().toString());
            zfbRecipe.setRecipeMemo(dbRecipe.getRecipeMemo());

            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
            if (null != order) {
                orderCode = order.getOrderCode();
                zfbRecipe.setPatientTel(StringUtils.isEmpty(order.getRecMobile()) ?
                        zfbRecipe.getPatientTel() : order.getRecMobile());
                zfbRecipe.setPatientAddress(StringUtils.isEmpty(order.getAddress4()) ?
                        zfbRecipe.getPatientAddress() : order.getAddress4());

                zfbRecipe.setRecipeFee(order.getRecipeFee().toPlainString());
                zfbRecipe.setActualFee(order.getActualPrice().toString());
                zfbRecipe.setCouponFee(order.getCouponFee().toPlainString());
                zfbRecipe.setDecoctionFee(order.getDecoctionFee().toPlainString());
                zfbRecipe.setExpressFee(order.getExpressFee().toPlainString());
                zfbRecipe.setOrderTotalFee(order.getTotalFee().toPlainString());
                //TODO 处理药店信息
                zfbRecipe.setDistributorCode(depId.toString());
                zfbRecipe.setDistributorName(depName);
                zfbRecipe.setPharmacyCode(null != order.getEnterpriseId() ? order.getEnterpriseId().toString() : "");
                zfbRecipe.setPharmacyName(order.getDrugStoreName());
            } else {
                result.setMsg("订单不存在");
                return result;
            }

            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
            if (CollectionUtils.isNotEmpty(detailList)) {
                List<ZfbDrugDTO> zfbDetailList = new ArrayList<>(detailList.size());
                List<Integer> drugIdList = Lists.newArrayList(Collections2.transform(detailList, new Function<Recipedetail, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable Recipedetail input) {
                        return input.getDrugId();
                    }
                }));

                SaleDrugListDAO saleDrugDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                DrugListDAO drugDAO = DAOFactory.getDAO(DrugListDAO.class);

                List<SaleDrugList> saleDrugList = saleDrugDAO.findByOrganIdAndDrugIds(depId, drugIdList);
                List<DrugList> drugList = drugDAO.findByDrugIds(drugIdList);
                if (detailList.size() != saleDrugList.size() || saleDrugList.size() != drugList.size()) {
                    result.setMsg("药品数据存在问题");
                    return result;
                }
                Map<Integer, SaleDrugList> saleDrugMap = Maps.uniqueIndex(saleDrugList, new Function<SaleDrugList, Integer>() {
                    @Override
                    public Integer apply(SaleDrugList input) {
                        return input.getDrugId();
                    }
                });

                Map<Integer, DrugList> drugMap = Maps.uniqueIndex(drugList, new Function<DrugList, Integer>() {
                    @Override
                    public Integer apply(DrugList input) {
                        return input.getDrugId();
                    }
                });

                ZfbDrugDTO zfbDetail;
                Integer drugId;
                for (Recipedetail detail : detailList) {
                    zfbDetail = new ZfbDrugDTO();
                    drugId = detail.getDrugId();
                    zfbDetail.setDrugCode(saleDrugMap.get(drugId).getOrganDrugCode());
                    zfbDetail.setDrugName(detail.getDrugName());
                    zfbDetail.setSpecification(detail.getDrugSpec());
                    zfbDetail.setProducer(drugMap.get(drugId).getProducer());
                    zfbDetail.setTotal(detail.getUseTotalDose().toString());
                    zfbDetail.setUseDose(detail.getUseDose().toString());
                    zfbDetail.setDrugFee(detail.getSalePrice().toPlainString());
                    zfbDetail.setDrugTotalFee(detail.getDrugCost().toPlainString());
                    zfbDetail.setUesDays(detail.getUseDays().toString());
                    zfbDetail.setUsingRate(detail.getUsingRate());
                    zfbDetail.setUsePathways(detail.getUsePathways());
                    zfbDetail.setMemo(detail.getMemo());
                    zfbDetailList.add(zfbDetail);
                }

                zfbRecipe.setDrugList(zfbDetailList);
            } else {
                result.setMsg("处方详情不存在");
                return result;
            }

        }

        //发送支付宝
        // 创建默认的httpClient实例.
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(enterprise.getBusinessUrl());
            //组装请求参数
            httpPost.setHeader("token", enterprise.getToken());
            Map<String, ZfbRecipeDTO> request = ImmutableMap.of("recipeDetail", zfbRecipe);
            String reqeustStr = JSONUtils.toString(request);
            LOGGER.info("[{}][{}] pushRecipeInfo send :{}", depId, depName, JSONUtils.toString(request));
            StringEntity requestEntity = new StringEntity(reqeustStr, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);

            //获取响应消息
            CloseableHttpResponse response = httpclient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("[{}][{}] pushRecipeInfo 返回:{}", depId, depName, responseStr);
            ZfbTokenResponse zfbResponse = JSONUtils.parse(responseStr, ZfbTokenResponse.class);
            if ("0".equals(zfbResponse.getCode())) {
                //成功
                result.setCode(DrugEnterpriseResult.SUCCESS);
                orderService.updateOrderInfo(orderCode, ImmutableMap.of("pushFlag", 1), null);
                LOGGER.info("[{}][{}] pushRecipeInfo {} success.", depId, depName, JSONUtils.toString(recipeIds));
            } else {
                //失败
                result.setMsg(zfbResponse.getMsg());
                orderService.updateOrderInfo(orderCode, ImmutableMap.of("pushFlag", -1), null);
                LOGGER.warn("[{}][{}] pushRecipeInfo {} fail. msg={}", depId, depName,
                        JSONUtils.toString(recipeIds), zfbResponse.getMsg());
            }
            //关闭 HttpEntity 输入流
            EntityUtils.consume(httpEntity);
            response.close();
            httpclient.close();
        } catch (Exception e) {
            result.setMsg("推送异常");
            orderService.updateOrderInfo(orderCode, ImmutableMap.of("pushFlag", -1), null);
            LOGGER.warn("[{}][{}] pushRecipeInfo 异常。", depId, depName, e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
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
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String testData = redisClient.get(CacheConstant.KEY_PHARYACY_TEST_DATA);
        if (StringUtils.isNotEmpty(testData)) {
            List<DepDetailBean> backList = Lists.newArrayList();
            List<Map> list = JSONUtils.parse(testData, List.class);
            DepDetailBean bean;
            for (Map map : list) {
                bean = new DepDetailBean();
                try {
                    BeanUtils.populate(bean, map);
                } catch (Exception e) {

                }
                backList.add(bean);
            }
            result.setObject(backList);
        }
        return result;
    }



    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_ZFB;
    }
}
