package recipe.drugsenterprise;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.common.CommonConstant;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.util.DateConversion;
import recipe.util.DigestUtil;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 描述：药企华润
 * @author yinsheng
 * @date 2019\10\24 0024 16:50
 */
@RpcBean("hrRemoteService")
public class HrRemoteService extends AccessDrugEnterpriseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(HrRemoteService.class);

    @Resource
    private RecipeDAO recipeDAO;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Resource
    private RecipeOrderDAO recipeOrderDAO;


    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    //获取所有药店列表前缀
    private static final String ALL_STORE_LIST = "/api/platform/store/list";

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("HrRemoteService tokenUpdateImpl not implement.");
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
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
        }
        Integer depId = enterprise.getId();

        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        HrPushRecipeInfo hrPushRecipeInfo = new HrPushRecipeInfo();
        if (!ObjectUtils.isEmpty(recipeList)) {
            Recipe recipe = recipeList.get(0);
            getMedicalInfo(recipe);
            PatientService patientService = BasicAPI.getService(PatientService.class);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            OrganService organService = BasicAPI.getService(OrganService.class);
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);

            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());

            hrPushRecipeInfo.setOrderId(recipeOrder.getOrderCode());
            hrPushRecipeInfo.setStoreId(recipeOrder.getDrugStoreCode());
            hrPushRecipeInfo.setIsNeedInvoice(false);
            hrPushRecipeInfo.setPickMode(RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getGiveMode()) ? 0 : 1 );
            hrPushRecipeInfo.setSettleMode(RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getGiveMode()) ? 1 : 2);
            hrPushRecipeInfo.setPayFlag(RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getGiveMode()) ? 1 : 0);
            hrPushRecipeInfo.setAmount(recipeOrder.getRecipeFee().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            ReceiveAddress receiveAddress = new ReceiveAddress();
            //设置用户信息
            PatientDTO patientDTO = patientService.getPatientByMpiId(recipe.getMpiid());
            if (patientDTO == null) {
                return getDrugEnterpriseResult(result, "用户不存在");
            }
            hrPushRecipeInfo.setDescription(patientDTO.getPatientName() + "的订单");
            receiveAddress.setPhone(patientDTO.getMobile());
            receiveAddress.setReceiver(patientDTO.getPatientName());
            String province = getAddressDic(recipeOrder.getAddress1());
            String city = getAddressDic(recipeOrder.getAddress2());
            String district = getAddressDic(recipeOrder.getAddress3());
            String street = recipeOrder.getAddress4();
            //设置省
            receiveAddress.setProvince(province);
            //设置市
            receiveAddress.setCity(city);
            //设置区
            receiveAddress.setDistrict(district);
            //设置详细
            receiveAddress.setStreet(street);

            receiveAddress.setDetail(province + city + district + street);

            receiveAddress.setDescription("无");

            hrPushRecipeInfo.setReceiveAddress(receiveAddress);

            //设置患者

            List<HrPatient> Patients = new ArrayList<>();
            HrPatient hrPatient = new HrPatient();
            hrPatient.setPatientId(patientDTO.getMpiId());
            hrPatient.setName(patientDTO.getPatientName());
            hrPatient.setIdentityType(1);
            hrPatient.setIdentityNo(patientDTO.getIdcard());
            if ("1".equals(patientDTO.getPatientSex())) {
                hrPatient.setSex(0);
            } else {
                hrPatient.setSex(1);
            }
            hrPatient.setBirthday(patientDTO.getBirthday() != null ? DateConversion.formatDate(patientDTO.getBirthday()) : "");
            hrPatient.setMobile(patientDTO.getMobile());
            LinkAddress linkAddress = new LinkAddress();
            linkAddress.setProvince(province);
            linkAddress.setCity(city);
            linkAddress.setDistrict(district);
            linkAddress.setStreet(street);
            linkAddress.setDetail(province + city + district + street);
            linkAddress.setDescription("无");
            hrPatient.setLinkAddress(linkAddress);
            Patients.add(hrPatient);

            hrPushRecipeInfo.setPatients(Patients);

            List<HrPrescr> Prescrs = new ArrayList<>();
            HrPrescr hrPrescr = new HrPrescr();
            hrPrescr.setPatientId(patientDTO.getMpiId());
            hrPrescr.setPrescrDate(getTime(recipe.getSignDate()));

            hrPrescr.setPrescrNo(recipe.getRecipeId().toString());
            hrPrescr.setBuyerMobile(patientDTO.getMobile());
            hrPrescr.setBuyerId(patientDTO.getMpiId());
            hrPrescr.setDiagnosisResult(recipe.getOrganDiseaseName());
            hrPrescr.setMedicalOrder(recipe.getMemo());
            if (recipe.getChecker() != null) {
                DoctorDTO checker = doctorService.getByDoctorId(recipe.getChecker());
                hrPrescr.setReviewerName(checker.getName());
            } else {
                hrPrescr.setReviewerName("0000");
            }
            hrPrescr.setDescription("".equals(recipe.getMemo())?"无":recipe.getMemo());
            OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
            if (organDTO == null) {
                return getDrugEnterpriseResult(result, "机构不存在");
            }
            EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(recipe.getDoctor());
            DoctorDTO doctor = doctorService.getByDoctorId(recipe.getDoctor());
            if (doctor == null) {
                return getDrugEnterpriseResult(result, "医生不存在");
            }
            if (!ObjectUtils.isEmpty(employment)) {
                Integer departmentId = employment.getDepartment();
                DepartmentDTO departmentDTO = departmentService.get(departmentId);
                if (!ObjectUtils.isEmpty(departmentDTO)) {
                    hrPrescr.setDepartmentName(StringUtils.isEmpty(departmentDTO.getName())?"全科":departmentDTO.getName());
                } else {
                    return getDrugEnterpriseResult(result, "医生主执业点不存在");
                }

            } else {
                return getDrugEnterpriseResult(result, "医生主执业点不存在");
            }
            hrPrescr.setHospitalName(organDTO.getName());
            hrPrescr.setDoctorName(doctor.getName());
            hrPrescr.setHospitalId(organDTO.getOrganId().toString()); //医院编码
            hrPrescr.setDoctorId(recipe.getDoctor().toString());
            hrPrescr.setPrescrAmount(recipeOrder.getRecipeFee().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

            RecipeParameterDao parameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            String fileImgUrl = parameterDao.getByName("fileImgUrl");
            hrPrescr.setImageUri(fileImgUrl + recipe.getSignImg());
            List<HrDetail> Details = new ArrayList<>();
            List<HrDrugDetail> drugDetails = new ArrayList<>();
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            for (int i = 0; i < recipedetails.size(); i++) {
                HrDetail detail = new HrDetail();
                HrDrugDetail drugDetail = new HrDrugDetail();
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetails.get(i).getDrugId(), enterprise.getId());
                if (saleDrugList == null) {
                    return getDrugEnterpriseResult(result, "配送药品目录不存在" + recipedetails.get(i).getDrugId());
                }
                detail.setProductId(saleDrugList.getOrganDrugCode());
                drugDetail.setProductId(saleDrugList.getOrganDrugCode());
                detail.setPrice(Double.parseDouble(saleDrugList.getPrice().toString()));
                detail.setQuantity(recipedetails.get(i).getUseTotalDose().intValue());
                detail.setUnit(recipedetails.get(i).getDrugUnit());
                detail.setDescription("".equals(recipedetails.get(i).getMemo()) ? "无" : recipedetails.get(i).getMemo());
                detail.setDrugDoseQuantity(recipedetails.get(i).getUseDose());
                detail.setDrugDoseUnit(recipedetails.get(i).getUseDoseUnit());
                String usingRate ;
                String usePathways ;
                try {
                    usingRate = recipedetails.get(i).getUsingRateTextFromHis()!=null?recipedetails.get(i).getUsingRateTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(recipedetails.get(i).getUsingRate());
                    usePathways = recipedetails.get(i).getUsePathwaysTextFromHis()!=null?recipedetails.get(i).getUsePathwaysTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(recipedetails.get(i).getUsePathways());
                } catch (ControllerException e) {
                    return getDrugEnterpriseResult(result, "药物使用频率使用途径获取失败");
                }
                detail.setFrequency(recipedetails.get(i).getUsingRate());
                detail.setFrequencyDescription(usingRate);
                detail.setUsage(recipedetails.get(i).getUsePathways());
                detail.setUsageDescription(usePathways);
                detail.setPerDosageQuantity(recipedetails.get(i).getUseDose());
                detail.setPerDosageUnit(recipedetails.get(i).getUseDoseUnit());
                detail.setDays(recipedetails.get(i).getUseDays());
                //date 20200526
                //修改推给华润药企的天数big->int

                detail.setCommonName(saleDrugList.getDrugName());
                detail.setSpecs(saleDrugList.getDrugSpec());
                detail.setMedicineName(saleDrugList.getSaleName());
                drugDetail.setMedicineName(saleDrugList.getSaleName());
                drugDetail.setCommonName(saleDrugList.getDrugName());
                drugDetail.setSpecs(saleDrugList.getDrugSpec());
                List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipedetails.get(i).getDrugId(), recipe.getClinicOrgan());
                if (organDrugLists == null) {
                    return getDrugEnterpriseResult(result, "机构药品目录不存在" + recipedetails.get(i).getDrugId());
                }
                drugDetail.setProducer(organDrugLists.get(0).getProducer());
                drugDetail.setQuantity(recipedetails.get(i).getUseTotalDose().intValue());
                drugDetail.setPrice(Double.parseDouble(saleDrugList.getPrice().toString()));
                drugDetail.setUnit(recipedetails.get(i).getDrugUnit());

                BigDecimal totalAmount = saleDrugList.getPrice().multiply(new BigDecimal(recipedetails.get(i).getUseTotalDose().intValue()));
                drugDetail.setAmount(totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                drugDetail.setDescription("无");
                Details.add(detail);
                drugDetails.add(drugDetail);
            }
            hrPrescr.setDetails(Details);
            Prescrs.add(hrPrescr);
            hrPushRecipeInfo.setPrescrs(Prescrs);
            hrPushRecipeInfo.setDetails(drugDetails);
        }
        //转成JSON
        String hrPushRecipeInfoStr = JSONUtils.toString(hrPushRecipeInfo);
        //发送到华润
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try{
            String path = "/api/platform/PrescrOrder";
            HttpPost httpPost = new HttpPost(enterprise.getBusinessUrl() + path);
            //组装请求参数
            httpPost.setHeader("Content-Type", "application/json;charset=utf8");
            httpPost.setHeader("Sign", sign("", path, hrPushRecipeInfoStr, enterprise));
            LOGGER.info("[{}][{}] pushRecipeInfo send :{}", depId, enterprise.getName(), hrPushRecipeInfoStr);
            StringEntity requestEntry = new StringEntity(hrPushRecipeInfoStr, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntry);

            //获取响应消息
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("[{}][{}] pushRecipeInfo 返回:{}", depId, enterprise.getName(), responseStr);

            if (StringUtils.isNotEmpty(responseStr) && responseStr.contains("OrderId")) {
                Recipe dbRecipe = recipeList.get(0);
                //成功
                result.setCode(DrugEnterpriseResult.SUCCESS);
                //说明成功,更新处方标志
                recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), ImmutableMap.of("pushFlag", 1));
                LOGGER.info("[{}][{}] pushRecipeInfo {} success.", depId, enterprise.getName(), JSONUtils.toString(recipeIds));
            } else {
                //失败
                LOGGER.warn("[{}][{}] pushRecipeInfo {} fail. msg={}", depId, enterprise.getName(),
                        JSONUtils.toString(recipeIds), responseStr);
            }
            //关闭 HttpEntity 输入流
            EntityUtils.consume(httpEntity);
            response.close();
        }catch (Exception e) {
            result.setMsg("推送异常");
            LOGGER.warn("[{}][{}] pushRecipeInfo 异常。", depId, enterprise.getName(), e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOGGER.warn("资源关闭失败.", e);
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @RpcService
    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        List<HrStoreBean> hrStoreBeans = findHaveStockStores(Arrays.asList(recipeId), null, drugsEnterprise);
        if (hrStoreBeans != null && hrStoreBeans.size() == 0) {
            result.setMsg("药店无库存");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        return result;
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
        List<HrStoreBean> hrStoreBeans = findHaveStockStores(recipeIds, ext, enterprise);
        List<DepDetailBean> detailList = new ArrayList<>();
        DepDetailBean detailBean;
        for (HrStoreBean hrStoreBean : hrStoreBeans) {
            detailBean = new DepDetailBean();
            detailBean.setDepName(hrStoreBean.getStoreName());
            detailBean.setRecipeFee(BigDecimal.ZERO);
            detailBean.setExpressFee(BigDecimal.ZERO);
            detailBean.setPharmacyCode(hrStoreBean.getStoreId());
            detailBean.setDistance(hrStoreBean.getDistance());
            detailBean.setAddress(hrStoreBean.getStoreAddress().getAddress());
            Position position = new Position();
            position.setLatitude(Double.parseDouble(hrStoreBean.getStorePosition().getLatitude()));
            position.setLongitude(Double.parseDouble(hrStoreBean.getStorePosition().getLongitude()));
            detailBean.setPosition(position);
            detailList.add(detailBean);
        }
        result.setObject(detailList);
        return result;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_HR;
    }

    private List<HrStoreResponse> findScanStockStores(String queryString, DrugsEnterprise drugsEnterprise){
        LOGGER.info("HrRemoteService.findScanStockStores.queryString: {}.", queryString);
        String path = "/api/platform/stock";
        // 创建默认的httpClient实例.
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            queryString = getUtf8Str(queryString);
            CloseableHttpResponse response = sendStockHttpRequest(drugsEnterprise, path, queryString, httpClient);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("HrRemoteService.findScanStockStores.responseStr: {}.", responseStr);
            if (StringUtils.isNotEmpty(responseStr) && responseStr.contains("!DOCTYPE")) {
                //说明华润的服务器出现问题
                return new ArrayList<>();
            } else {
                if(CommonConstant.requestSuccessCode == response.getStatusLine().getStatusCode()){
                    List<HrStoreResponse> hrStoreResponses = JSONObject.parseArray(responseStr, HrStoreResponse.class);
                    LOGGER.info("HrRemoteService.findAllStores.HrStoreResponse: {}.", JSONUtils.toString(hrStoreResponses));
                    return hrStoreResponses;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("findScanStockStores error.", e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOGGER.warn("资源关闭失败.", e);
            }
        }
        return new ArrayList<>();
    }

    private List<HrStoreBean> findStoreByPosintion(Map ext, DrugsEnterprise drugsEnterprise){
        String range = MapValueUtil.getString(ext, "range");
        String longitude = MapValueUtil.getString(ext, "longitude");
        String latitude = MapValueUtil.getString(ext, "latitude");
        if (longitude == null || latitude == null) {
            return new ArrayList<>();
        }
        String path = "/api/platform/store/list/distance";
        String queryString = "Longitude=" + Double.parseDouble(longitude) + "&Latitude=" + Double.parseDouble(latitude) + "&Distance=" + Double.parseDouble(range);
        // 创建默认的httpClient实例.
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try{
            CloseableHttpResponse response = sendStockHttpRequest(drugsEnterprise, path, queryString, httpClient);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("HrRemoteService.findStoreByPosintion.responseStr: {}.", responseStr);
            if (StringUtils.isNotEmpty(responseStr) && responseStr.contains("!DOCTYPE")) {
                //说明华润的服务器出现问题
                return new ArrayList<>();
            } else {
                if(CommonConstant.requestSuccessCode == response.getStatusLine().getStatusCode() && responseStr.contains("StoreName")){
                    List<HrStoreBean> hrStoreBeans = JSONObject.parseArray(responseStr, HrStoreBean.class);
                    LOGGER.info("HrRemoteService.findStoreByPosintion.hrStoreBean: {}.", JSONUtils.toString(hrStoreBeans));
                    return hrStoreBeans;
                }
            }
        }catch (Exception e){
            LOGGER.warn("findStoreByPosintion error.", e);
        }finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOGGER.warn("资源关闭失败.", e);
            }
        }
        return new ArrayList<>();
    }

    private List<HrStoreBean> findAllStores(DrugsEnterprise drugsEnterprise){
        String queryString = "";
        // 创建默认的httpClient实例.
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            CloseableHttpResponse response = sendStockHttpRequest(drugsEnterprise, ALL_STORE_LIST, queryString, httpClient);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("HrRemoteService.findAllStores.responseStr: {}.", responseStr);
            if (StringUtils.isNotEmpty(responseStr) && responseStr.contains("!DOCTYPE")) {
                //说明华润的服务器出现问题
                return new ArrayList<>();
            } else {
                if(CommonConstant.requestSuccessCode == response.getStatusLine().getStatusCode()){
                    List<HrStoreBean> hrStoreBeans = JSONObject.parseArray(responseStr, HrStoreBean.class);
                    LOGGER.info("HrRemoteService.findAllStores.hrStoreBean: {}.", JSONUtils.toString(hrStoreBeans));
                    return hrStoreBeans;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("findAllStores error.", e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOGGER.warn("资源关闭失败.", e);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 签名
     * @return 签名结果
     */
    private String sign(String queryString, String path, String body, DrugsEnterprise enterprise){
        LOGGER.info("sign:{},{},{}", queryString, path, body);
        queryString = getUtf8Str(queryString);
        String userName = enterprise.getUserId();
        String password = enterprise.getPassword();
        String time = getTime(new Date());

        String signStr = userName + DigestUtil.md5For32(password) + path + queryString + body + time;
        return userName + "/" + DigestUtil.md5For32(signStr) + "/" + time;
    }

    /**
     * 获取ISO8601 标准格式时间
     * @return 返回时间
     */
    private String getTime(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        return sdf.format(new Date());
    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        LOGGER.info(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        return result;
    }

    private CloseableHttpResponse sendStockHttpRequest(DrugsEnterprise drugsEnterprise, String path, String queryString, CloseableHttpClient httpClient) throws IOException {
        HttpGet httpGet = new HttpGet(drugsEnterprise.getBusinessUrl() +
                path + "?" + queryString);
        httpGet.setHeader("Content-Type", "text;charset=utf-8");
        httpGet.setHeader("Sign", sign(queryString, path, "", drugsEnterprise));
        LOGGER.info("HrRemoteService.sendStockHttpRequest:[{}],[{}],[{}]", path, queryString);
        //获取响应消息
        return httpClient.execute(httpGet);
    }

    private List<HrStoreBean> findHaveStockStores(List<Integer> recipeIds, Map ext, DrugsEnterprise drugsEnterprise){
        Integer recipeId = recipeIds.get(0);
        List<HrStoreBean> totalHaveStockStores = new ArrayList<>();
        List<HrStoreBean> hrStoreBeans = new ArrayList<>();
        if (ext != null && ext.size() > 0) {
            //通过坐标获取一定范围内的药店
            hrStoreBeans = findStoreByPosintion(ext, drugsEnterprise);
        } else {
            //获取所有药店
            hrStoreBeans = findAllStores(drugsEnterprise);
        }
        //库存校验采用：医生开方时获取所有的药店,根据药店+药品去校验库存.
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            return new ArrayList<>();
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        List<Recipedetail> detailList = recipeDetailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        Map<String, String> map = new HashMap<>();
        for (Recipedetail recipedetail : recipeDetails) {
            drugIds.add(recipedetail.getDrugId());
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if(saleDrugList != null && StringUtils.isNotEmpty(saleDrugList.getOrganDrugCode())) {
                map.put(saleDrugList.getOrganDrugCode(), recipedetail.getUseTotalDose().toString());
            } else {
                LOGGER.info("HrRemoteService.findHaveStockStores 配送药品目录OrganDrugCode为空，drugId:{}.", recipedetail.getDrugId());
                return new ArrayList<>();
            }
        }
        StringBuilder parames = new StringBuilder();
        for (int i = 0; i < hrStoreBeans.size(); i++) {
            String storeId = hrStoreBeans.get(i).getStoreId();
            parames.append("StoreId[").append(i).append("]").append("=").append(storeId).append("&");
            for (int j = 0; j < drugIds.size(); j++) {
                Integer drugId = drugIds.get(j);
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
                parames.append("Products[").append(j).append("].").append("ProductId").append("=").append(saleDrugList.getOrganDrugCode()).append("&")
                        .append("Products[").append(j).append("].").append("MedicineName").append("=").append(saleDrugList.getDrugName()).append("&")
                        .append("Products[").append(j).append("].").append("CommonName").append("=").append(saleDrugList.getSaleName()).append("&")
                        .append("Products[").append(j).append("].").append("Specs").append("=").append(saleDrugList.getDrugSpec()).append("&");
            }
            parames.deleteCharAt(parames.lastIndexOf("&"));
            List<HrStoreResponse> hrStoreResponses = findScanStockStores(parames.toString(), drugsEnterprise);
            int whileCount = 0;
            int haveStockNum = 0;
            if (!CollectionUtils.isEmpty(hrStoreResponses)) {
                for (HrStoreResponse hrStoreResponse : hrStoreResponses) {
                    whileCount++;
                    if (StringUtils.isNotEmpty(hrStoreResponse.getQuantity()) && Integer.parseInt(hrStoreResponse.getQuantity()) >= 0) {
                        if (map.containsKey(hrStoreResponse.getProductId())){
                            String useTotalDose = map.get(hrStoreResponse.getProductId());
                            if (Double.parseDouble(useTotalDose) < Double.parseDouble(hrStoreResponse.getQuantity())) {
                                haveStockNum++;
                            }
                        }
                    }
                }
            }
            if (whileCount == haveStockNum && whileCount != 0 && haveStockNum == drugIds.size()) {
                totalHaveStockStores.add(hrStoreBeans.get(i));
            }
        }
        LOGGER.info("HrRemoteService.findHaveStockStores totalHaveStockStores:{}.", JSONUtils.toString(totalHaveStockStores));
        return totalHaveStockStores;
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

    private static String getUtf8Str(String queryString){
        String query = "";
        try {
            query = new String(queryString.getBytes("ISO-8859-1"), "utf-8");
        } catch (Exception e) {
            LOGGER.info("getUtf8Str error.",e);
        }
        return query;
    }
}
