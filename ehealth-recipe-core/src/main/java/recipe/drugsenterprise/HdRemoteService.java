package recipe.drugsenterprise;

import com.alibaba.fastjson.JSONObject;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
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
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.HdFindSupportDepStatusEnum;
import recipe.constant.HdHttpUrlEnum;
import recipe.constant.HdPushRecipeStatusEnum;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.service.RecipeLogService;
import recipe.third.IFileDownloadService;
import recipe.util.DateConversion;
import recipe.util.DistanceUtil;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
* @Description: HdRemoteService 类（或接口）是 对接华东药企服务接口
* @Author: JRK
* @Date: 2019/7/8
*/
@RpcBean("hddyfRemoteService")
public class HdRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdRemoteService.class);

    private static final String GRANT_TYPE = "client_credentials";

    private static final String searchMapRANGE = "range";

    private static final String searchMapLatitude = "latitude";

    private static final String searchMapLongitude = "longitude";

    private static final String requestHeadAuthorizationKey = "Authorization";

    private static final String requestAuthorizationValueHead = "Bearer ";

    private static final Integer requestSuccessCode = 200;

    private static final String requestHeadJsonKey = "Content-Type";

    private static final String requestHeadJsonValue = "application/json";

    private static final String distributionFlagDefault = "10";

    private static final String payModeDefault = "2";

    private static final String recipeTypeDefault = "1";

    private static final String recipeIdDefault = "0";

    private static final String useTotalDoseDefault = "0.0";

    private static final String useMsgDefault = "0";

    private static final Double doubleParameterDefault = 0d;

    private static final String feeDefault = "0";

    private static final String certificateTypeDefault = "1";

    private static final String getAllPharmacyRange = "0";

    private static final String drugRecipeTotal = "0";

    private static final String hdPayMethod = "0";

    private static final String hdSendMethod = "1";

    private static final String imgHead = "data:image/jpeg;base64,";

    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;
    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Resource
    private RecipeOrderDAO recipeOrderDAO;
    @Resource
    private RecipeParameterDao recipeParameterDAO;
    @Resource
    private RecipeDetailDAO detailDAO;
    @Resource
    private SaleDrugListDAO saleDrugListDAO;


    public HdRemoteService() {
    }

    @Override
    @RpcService
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("HdRemoteService.tokenUpdateImpl:[{}][{}]token更新", drugsEnterprise.getHosInteriorSupport(), drugsEnterprise.getName());
        //获取药企对应的用户名，密码和机构
        HdTokenRequest request = new HdTokenRequest();
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();

        //组装请求，判断是否组装成功,发送http请求获取,获取新的token
        getNewToken(drugsEnterprise, request, result);
    }

    /**
     * @method  getNewToken
     * @description 获取新的验证token
     * @date: 2019/7/26
     * @author: JRK
     * @param drugsEnterprise 药企
     * @param request 新的token请求对象
     * @param result 总的请求结果
     * @return void
     */
    private void getNewToken(DrugsEnterprise drugsEnterprise, HdTokenRequest request, DrugEnterpriseResult result) {
        //组装请求对象
        if (!assembleRequest(drugsEnterprise, request, result)) return;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            if (drugsEnterprise.getAuthenUrl().contains("http:")) {
                sendTokenAndUpdateHttpRequest(drugsEnterprise, request, httpclient, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("HdRemoteService.tokenUpdateImpl:[{}][{}]更新token异常：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), e.getMessage(),e);
            getFailResult(result, "更新token异常");
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("HdRemoteService.tokenUpdateImpl:http请求资源关闭异常: {}", e.getMessage(),e);
                getFailResult(result, "http请求资源关闭异常");
            }
        }
    }

    /**
     * @method  sendTokenAndUpdateHttpRequest
     * @description 发送http请求获得新的token信息,并更新
     * @date: 2019/7/8
     * @author: JRK
     * @param drugsEnterprise
     * @param request 英特请求对象
     * @param httpclient http请求服务
     * @return void
     */
    private void sendTokenAndUpdateHttpRequest(DrugsEnterprise drugsEnterprise, HdTokenRequest request, CloseableHttpClient httpclient, DrugEnterpriseResult result) throws IOException {
        //生成post请求
        Map<String, String> requestMap = JSONUtils.parse(JSONUtils.toString(request), HashMap.class);
        String requestUrl = getRequestUrl(requestMap, drugsEnterprise.getAuthenUrl());
        HttpPost httpPost = new HttpPost(requestUrl);
        httpPost.setHeader(requestHeadJsonKey, requestHeadJsonValue);

        //获取响应消息
        LOGGER.info("HdRemoteService.tokenUpdateImpl:发送[{}][{}]token更新请求", drugsEnterprise.getId(), drugsEnterprise.getName());
        CloseableHttpResponse response = httpclient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        String responseStr =  EntityUtils.toString(responseEntity);
        if(requestSuccessCode == response.getStatusLine().getStatusCode()){
            LOGGER.info("HdRemoteService.tokenUpdateImpl:[{}][{}]token更新请求返回:{}", drugsEnterprise.getId(), drugsEnterprise.getName(), responseStr);
            HdTokenResponse tokenResponse = JSONUtils.parse(responseStr, HdTokenResponse.class);
            String newToken = tokenResponse.getAccessToken();
            //当更新请求返回的新token不为空的时候进行更新token的操作
            if(null != tokenResponse && null != newToken){
                drugsEnterpriseDAO.updateTokenById(drugsEnterprise.getId(), newToken);
                LOGGER.info("HdRemoteService.tokenUpdateImpl:[{}][{}]token更新成功:{}", drugsEnterprise.getId(), drugsEnterprise.getName(), newToken);
            }else{
                LOGGER.warn("HdRemoteService.tokenUpdateImpl:[{}][{}]token更新token请求失败", drugsEnterprise.getId(), drugsEnterprise.getName());
                getFailResult(result, "token更新token请求失败");
            }
        }else{
            LOGGER.warn("HdRemoteService.tokenUpdateImpl:[{}][{}]token更新请求，请求未成功,原因：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), responseStr);
            getFailResult(result, "token更新请求，请求未成功");
        }
        //关闭 HttpEntity 输入流
        EntityUtils.consume(responseEntity);
        response.close();
    }

    /**
     * @method  getRequestUrl
     * @description 拼接请求的参数
     * @date: 2019/7/30
     * @author: JRK
     * @param requestMap 请求参数
     * @param authorizeUrl 请求url
     * @return 拼接好的请求url
     */
    private String getRequestUrl(Map<String, String> requestMap, String authorizeUrl) {
        StringBuffer requestUrl = new StringBuffer(authorizeUrl + "?");
        for (Map.Entry<String, String> entry : requestMap.entrySet()) {
            requestUrl.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return requestUrl.toString();
    }

    /**
     * @method  assembleRequest
     * @description 组装请求对象
     * @date: 2019/7/8
     * @author: JRK
     * @param drugsEnterprise
     * @param depId 药企id
     * @param depName 药企名称
     * @param request 请求对象
     * @return java.lang.Boolean 是否组装成功
     */
    private Boolean assembleRequest(DrugsEnterprise drugsEnterprise, HdTokenRequest request, DrugEnterpriseResult result) {
        String userId = drugsEnterprise.getUserId();
        String password = drugsEnterprise.getPassword();
        if (null != userId){
            request.setClientId(userId);
        }else{
            LOGGER.warn("HdRemoteService.tokenUpdateImpl:[{}][{}]的userId为空。", drugsEnterprise.getId(), drugsEnterprise.getName());
            getFailResult(result, "userId为空");
            return false;
        }
        request.setClientSecret(password);
        request.setGrantType(GRANT_TYPE);
        return true;
    }
    @RpcService
    public void tests(Integer recipeId){
        List<Integer> recipeIds = Arrays.asList(recipeId);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(224);
        pushRecipeInfo(recipeIds, drugsEnterprise);
    }

    @Override
    @RpcService
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();

        //组装处方和处方下药品信息，发送推送请求
        pushRecipe(recipeIds, enterprise, result);
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    /**
     * @method  pushRecipe
     * @description 推送处方信息
     * @date: 2019/7/26
     * @author: JRK
     * @param recipeIds 处方单id集合
     * @param enterprise 药企
     * @param result 总的结果结果
     * @return void
     */
    private void pushRecipe(List<Integer> recipeIds, DrugsEnterprise enterprise, DrugEnterpriseResult result) {
        //首先校验请求的数据是否合规
        if (StringUtils.isEmpty(enterprise.getBusinessUrl())) {
            LOGGER.warn("HdRemoteService.pushRecipeInfo:[{}][{}]药企的访问url为空", enterprise.getId(), enterprise.getName());
            getFailResult(result, "药企的访问url为空");
            return;
        }
        if (CollectionUtils.isEmpty(recipeIds)) {
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方列表为空");
            getFailResult(result, "处方列表为空");
            return;
        }
        //组装hdRecipeDTO和处方明细下的hdDrugDTO

        HdRecipeDTO sendHdRecipe = new HdRecipeDTO();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isNotEmpty(recipeList)) {
            try {
                Recipe nowRecipe = recipeList.get(0);
                getMedicalInfo(nowRecipe);
                assemblePushRecipeMessage(result, sendHdRecipe, nowRecipe, enterprise);
                if (DrugEnterpriseResult.FAIL == result.getCode())
                    return;
                //发送请求，获得推送的结果
                HdPushRecipeResponse responseResult = pushRecipeHttpRequest(result, enterprise, sendHdRecipe);

                HdTokenRequest request;
                if(null != responseResult && HdPushRecipeStatusEnum.
                        TOKEN_EXPIRE.equals(HdPushRecipeStatusEnum.fromCode(responseResult.getCode()))){
                    //校验token时效并更新
                    request = new HdTokenRequest();
                    this.getNewToken(enterprise, request, result);
                    if(DrugEnterpriseResult.FAIL == result.getCode())
                        return;
                    DrugsEnterprise newTokenEnterprise = drugsEnterpriseDAO.get(enterprise.getId());
                    if(null == newTokenEnterprise){
                        LOGGER.warn("HdRemoteService.pushRecipeInfo:[{}][{}]当前药企信息不存在", enterprise.getId(), enterprise.getName());
                        getFailResult(result, "当前药企信息不存在");
                    }
                    this.pushRecipeHttpRequest(result, newTokenEnterprise, sendHdRecipe);
                    if(DrugEnterpriseResult.FAIL == result.getCode())
                        return;
                }
                if(DrugEnterpriseResult.FAIL == result.getCode())
                    return;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("HdRemoteService.pushRecipeInfo:推送异常:{}", e.getMessage(),e);
                getFailResult(result, "组装数据异常");
            }
        }else{
            LOGGER.warn("HdRemoteService.pushRecipeInfo:未查询到匹配的处方列表");
            getFailResult(result, "未查询到匹配的处方列表");
            return;
        }
        return;
    }

    /**
     * @method  pushRecipeHttpRequest
     * @description 推送处方http请求
     * @date: 2019/7/10
     * @author: JRK
     * @param enterprise 药企
     * @param sendHdRecipe 推送的处方信息
     * @param httpClient 请求服务
     * @return void
     */
    private HdPushRecipeResponse pushRecipeHttpRequest(DrugEnterpriseResult result, DrugsEnterprise enterprise, HdRecipeDTO sendHdRecipe){
        HdPushRecipeResponse pushRecipeResponse = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HdHttpUrlEnum httpUrl;
        Recipe recipe = null;
        if (sendHdRecipe != null && sendHdRecipe.getRecipeId() != null) {
            recipe = recipeDAO.getByRecipeId(Integer.parseInt(sendHdRecipe.getRecipeId()));
        }
        try {
            if (enterprise.getBusinessUrl().contains("http:")) {

                //获取响应消息
                httpUrl = HdHttpUrlEnum.fromMethodName("pushRecipeInfo");
                if(null == httpUrl){
                    getFailResult(result, "没有请求对应的url");
                    return pushRecipeResponse;
                }
                String requestStr = JSONUtils.toString(sendHdRecipe);
                CloseableHttpResponse response = sendHttpRequest(enterprise, httpClient, requestStr, httpUrl);

                HttpEntity httpEntity = response.getEntity();
                String responseStr =  EntityUtils.toString(httpEntity);
                pushRecipeResponse =
                        JSONUtils.parse(responseStr, HdPushRecipeResponse.class);
                if(null == pushRecipeResponse || null == pushRecipeResponse.getSuccess()){
                    LOGGER.warn("HdRemoteService.pushRecipeInfo:[{}][{}]处方推送没有响应", enterprise.getId(), enterprise.getName());
                    getFailResult(result, "处方推送没有响应");
                }
                if(pushRecipeResponse.getSuccess()) {
                    if (recipe != null) {
                        LOGGER.info("HdRemoteService.pushRecipeInfo:[{}][{}][{}]处方推送成功，请求返回:{}", sendHdRecipe.getRecipeId(),enterprise.getId(), enterprise.getName());
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "纳里给"+enterprise.getName()+"推送处方成功");
                    } else {
                        LOGGER.info("YtRemoteService.pushRecipeInfo:[{}][{}]处方推送成功.",enterprise.getId(), enterprise.getName());
                    }
                }else if(HdPushRecipeStatusEnum.TOKEN_EXPIRE.equals(HdPushRecipeStatusEnum.fromCode(pushRecipeResponse.getCode()))){
                    LOGGER.info("HdRemoteService.pushRecipeInfo:处方推送请求鉴权失败需要重新获取token");
                }else{
                    if (recipe != null) {
                        LOGGER.warn("HdRemoteService.pushRecipeInfo:[{}][{}][{}]处方推送失败, 失败原因: {}", sendHdRecipe.getRecipeId(),enterprise.getId(), enterprise.getName(), pushRecipeResponse.getMessage());
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "纳里给"+enterprise.getName()+"推送处方失败,失败信息："+pushRecipeResponse.getMessage());
                    } else {
                        LOGGER.info("YtRemoteService.pushRecipeInfo:[{}][{}]处方推送失败.",enterprise.getId(), enterprise.getName());
                    }
                    getFailResult(result, HdFindSupportDepStatusEnum.fromCode(pushRecipeResponse.getCode()).getMean());
                }
                //关闭 HttpEntity 输入流
                EntityUtils.consume(httpEntity);
                response.close();
                return pushRecipeResponse;
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("HdRemoteService.pushRecipeInfo:[{}][{}]更新token异常：{}", enterprise.getId(), enterprise.getName(), e.getMessage(),e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("HdRemoteService.pushRecipeInfo:http请求资源关闭异常: {}！", e.getMessage(),e);
            }
            return pushRecipeResponse;
        }

    }

    /**
     * @method  assemblePushRecipeMessage
     * @description 组装推送的处方数据
     * @date: 2019/7/10
     * @author: JRK
     * @param result 返回的操作结果
     * @param organService 机构service
     * @param sendHdRecipe 组装的订单信息
     * @param nowRecipe 当前的订单
     * @return recipe.bean.DrugEnterpriseResult 操作的结果集
     */
    private DrugEnterpriseResult assemblePushRecipeMessage(DrugEnterpriseResult result, HdRecipeDTO sendHdRecipe, Recipe nowRecipe, DrugsEnterprise enterprise) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        //检验并组装处方对应的机构信息
        if(null == nowRecipe){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},处方不存在,", nowRecipe.getRecipeId());
            getFailResult(result, "处方单不存在");
            return result;
        }
        if (null == nowRecipe.getClinicOrgan()) {
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定的机构id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "开处方机构编码不存在");
            return result;
        }
        OrganDTO organ = organService.getByOrganId(nowRecipe.getClinicOrgan());
        if(null != organ && null != organ.getOrganId()){
            //这里保存的医院的社保编码
            sendHdRecipe.setOrganCode(organ.getOrganId().toString());
            sendHdRecipe.setOrganName(organ.getName());
        }else{
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},对应的开处方机构不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "开处方机构不存在");
            return result;
        }

        //标识推送的处方是纳里的
        sendHdRecipe.setSourceId("10");

        //检验并组装处方信息（上面已经校验过了）
        assembleRecipeMsg(sendHdRecipe, nowRecipe);
        if(result.FAIL.equals(result.getCode())){
            return result;
        }
        //检验并组装处方对应的医生信息
        assembleDoctorMsg(result, sendHdRecipe, nowRecipe);
        if(result.FAIL.equals(result.getCode())){
            return result;
        }
        //检验并组装处方对应的患者信息
        assemblePatientMsg(result, sendHdRecipe, nowRecipe);
        if(result.FAIL.equals(result.getCode())){
            return result;
        }
        //检验并组装处方对应的科室信息
        assembleDepartMsg(result, sendHdRecipe, nowRecipe);
        if(result.FAIL.equals(result.getCode())){
            return result;
        }
        //检验并组装处方对应的订单以及门店信息
        assembleStoreAndOrderMsg(result, sendHdRecipe, nowRecipe);
        if(result.FAIL.equals(result.getCode())){
            return result;
        }
        //设置处方笺base
        String ossId = nowRecipe.getSignImg();

        if(null != ossId){
            try {
                IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
                String imgStr = imgHead + fileDownloadService.downloadImg(ossId);
                if(ObjectUtils.isEmpty(imgStr)){
                    LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{}的ossid为{}处方笺不存在", nowRecipe.getRecipeId(), ossId);
                    getFailResult(result, "处方笺不存在");
                    return result;
                }
                LOGGER.warn("HdRemoteService.pushRecipeInfo:{}处方，下载处方笺服务成功", nowRecipe.getRecipeId());
                sendHdRecipe.setImage(imgStr);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warn("HdRemoteService.pushRecipeInfo:{}处方，下载处方笺服务异常：{}.", nowRecipe.getRecipeId(), e.getMessage(),e );
                getFailResult(result, "下载处方笺服务异常");
                return result;
            }

        }
        //检验并组装处方对应的详情信息
        assembleDrugListMsg(result, sendHdRecipe, nowRecipe, organ, enterprise);
        if(result.FAIL.equals(result.getCode())){
            return result;
        }
        return result;
    }
    /**
     * @method  assembleRecipeMsg
     * @description 组装推送的处方单中的处方信息
     * @date: 2019/7/10
     * @author: JRK
     * @param sendHdRecipe 推送的处方单信息
     * @param nowRecipe 当前的处方单
     * @return void
     */
    private void assembleRecipeMsg(HdRecipeDTO sendHdRecipe, Recipe nowRecipe) {
        sendHdRecipe.setRecipeCode(nowRecipe.getRecipeCode());
        sendHdRecipe.setRecipeMemo(nowRecipe.getRecipeMemo());
        sendHdRecipe.setMemo(nowRecipe.getMemo());
        sendHdRecipe.setOrganDiseaseName(nowRecipe.getOrganDiseaseName());
        sendHdRecipe.setOrganDiseaseId(nowRecipe.getOrganDiseaseId());
        if (nowRecipe.getCheckDateYs() != null) {
            sendHdRecipe.setAudiDate(DateConversion.getDateFormatter(nowRecipe.getCheckDateYs(), DateConversion.DEFAULT_DATE_TIME));

        } else {
            sendHdRecipe.setAudiDate(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
        }

        sendHdRecipe.setCreateDate(DateConversion.getDateFormatter(nowRecipe.getCreateDate(), DateConversion.DEFAULT_DATE_TIME));
        sendHdRecipe.setTcmUsePathways(nowRecipe.getTcmUsePathways());
        sendHdRecipe.setTcmUsingRate(nowRecipe.getTcmUsingRate());
        sendHdRecipe.setGiveUser(nowRecipe.getGiveUser());
        RecipeOrder order = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());
        sendHdRecipe.setPayMode(null == order.getPayMode() ? payModeDefault : order.getPayMode().toString());
        sendHdRecipe.setRecipeType(null == nowRecipe.getRecipeType() ? recipeTypeDefault : nowRecipe.getRecipeType().toString());
        sendHdRecipe.setRecipeId(null == nowRecipe.getRecipeId() ? recipeIdDefault : nowRecipe.getRecipeId().toString());
        sendHdRecipe.setPatientNumber(nowRecipe.getPatientID());

        //添加医保金额
        if (order != null && order.getOrderType() == 1) {
            if (order.getFundAmount() != null) {
                sendHdRecipe.setMedicalFee(String.valueOf(order.getFundAmount()));
                sendHdRecipe.setMedicalPay("1");
            } else {
                sendHdRecipe.setMedicalFee("0");
                sendHdRecipe.setMedicalPay("1");
            }
        } else {
            sendHdRecipe.setMedicalFee("0");
            sendHdRecipe.setMedicalPay("0");
        }

        if (nowRecipe.getGiveMode() == 1) {
            sendHdRecipe.setPatientAddress(getCompleteAddress(order));
            sendHdRecipe.setDistributionFlag("1");
            sendHdRecipe.setGiveMode("1");
        } else {
            sendHdRecipe.setDistributionFlag(distributionFlagDefault);
        }
        if (order != null && nowRecipe.getGiveMode() == 3 && StringUtils.isNotEmpty(order.getDrugStoreCode())) {
            sendHdRecipe.setGiveMode("3");
            sendHdRecipe.setPharmacyCode(order.getDrugStoreCode());
        }
        if (order.getPayMode() == 1 && order != null) {
            sendHdRecipe.setPayFlag(order.getPayFlag().toString());
        }
        if (order.getPayMode() == 2 ) {
            sendHdRecipe.setPayFlag("0");
        }
        //对浙四进行个性化处理,推送到指定药店配送
        String hdStores = recipeParameterDAO.getByName("hd_store_payonline");
        String storeOrganName = nowRecipe.getClinicOrgan() + "_" + "hd_organ_store";
        String organStore = recipeParameterDAO.getByName(storeOrganName);

        if (StringUtils.isNotEmpty(hdStores) && hasOrgan(nowRecipe.getClinicOrgan().toString(),hdStores) && nowRecipe.getGiveMode() != 3) {
            LOGGER.info("HdRemoteService.pushRecipeInfo organStore:{}.", organStore);
            sendHdRecipe.setGiveMode("4");
            sendHdRecipe.setPharmacyCode(organStore);
        }
    }

    /**
     * @method  assembleDrugListMsg
     * @description 组装推送的处方单中药品详情的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendHdRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @param organ 所属机构
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleDrugListMsg(DrugEnterpriseResult result, HdRecipeDTO sendHdRecipe, Recipe nowRecipe, OrganDTO organ , DrugsEnterprise enterprise) {
        List<Recipedetail> detailList = detailDAO.findByRecipeId(nowRecipe.getRecipeId());
        if(CollectionUtils.isEmpty(detailList)){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定订单不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定订单不存在");
            return result;
        }
        List<HdDrugDTO> drugList = new ArrayList<>();
        sendHdRecipe.setDrugList(drugList);
        Double price;
        Double quantity;
        HdDrugDTO nowHdDrugDTO;
        SaleDrugList saleDrug;
        for (Recipedetail nowDetail : detailList) {
            nowHdDrugDTO = new HdDrugDTO();
            drugList.add(nowHdDrugDTO);

            nowHdDrugDTO.setDrugName(nowDetail.getDrugName());
            nowHdDrugDTO.setSpecification(nowDetail.getDrugSpec());
            nowHdDrugDTO.setTotal(null == nowDetail.getUseTotalDose() ? useTotalDoseDefault : nowDetail.getUseTotalDose().toString());
            nowHdDrugDTO.setUsingDays(null == nowDetail.getUseDays() ? useMsgDefault : nowDetail.getUseDays().toString());
            nowHdDrugDTO.setUsePathways(null == nowDetail.getUsePathways() ? useMsgDefault : nowDetail.getUsePathways());
            nowHdDrugDTO.setMemo(nowDetail.getMemo());
            nowHdDrugDTO.setUseDose(nowDetail.getUseDose() + nowDetail.getUseDoseUnit());

            try {
                String usingRate = StringUtils.isNotEmpty(nowDetail.getUsingRateTextFromHis())?nowDetail.getUsingRateTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(nowDetail.getUsingRate());
                nowHdDrugDTO.setUsingRate(usingRate);
            } catch (ControllerException e) {
                LOGGER.warn("HdRemoteService.pushRecipeInfo:处方细节ID为{}.", nowDetail.getRecipeDetailId(),e);
                getFailResult(result, "药品频率出错");
                return result;
            }

            if(null == nowDetail.getRecipeDetailId()){
                LOGGER.warn("HdRemoteService.pushRecipeInfo:当前处方细节id不存在");
                getFailResult(result, "当前处方细节id不存在");
                return result;
            }
            //根据药品id和所属的药企在salrDrug下获取药品的编码
            saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(nowDetail.getDrugId(), enterprise.getId());
            if(null == saleDrug){
                LOGGER.warn("HdRemoteService.pushRecipeInfo:处方细节ID为{},对应销售药品的信息不存在", nowDetail.getRecipeDetailId());
                getFailResult(result, "销售药品的信息不存在");
                return result;
            }
            nowHdDrugDTO.setDrugCode(saleDrug.getOrganDrugCode());
            if(null == nowDetail.getSalePrice()){
                LOGGER.warn("HdRemoteService.pushRecipeInfo:处方细节ID为{},药品的单价为空", nowDetail.getRecipeDetailId());
                getFailResult(result, "药品的单价为空");
                return result;
            }
            price = nowDetail.getActualSalePrice() == null ? nowDetail.getSalePrice().doubleValue() : nowDetail.getActualSalePrice().doubleValue();
            quantity = null == nowDetail.getUseTotalDose() ? doubleParameterDefault : nowDetail.getUseTotalDose();

            nowHdDrugDTO.setDrugFee(price.toString());
            nowHdDrugDTO.setDrugTotalFee(String.valueOf(price * quantity));
        }
        return null;
    }

    /**
     * @method  assembleDoctorMsg
     * @description 组装推送的处方单中医生的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendHdRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleDoctorMsg(DrugEnterpriseResult result, HdRecipeDTO sendHdRecipe, Recipe nowRecipe) {
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        if(null == nowRecipe.getDoctor()){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定医生id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定医生id为空");
            return result;
        }
        if(null == nowRecipe.getChecker()){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},没有匹配审方医生", nowRecipe.getRecipeId());
            getFailResult(result, "没有匹配审方医生");
            return result;
        }
        if(null == nowRecipe.getDoctor()){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},没有匹配开方医生", nowRecipe.getRecipeId());
            getFailResult(result, "没有匹配开方医生");
            return result;
        }
        DoctorDTO checker = doctorService.get(nowRecipe.getChecker());
        DoctorDTO doctor = doctorService.get(nowRecipe.getDoctor());
        if(null == doctor){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定医生不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定医生不存在");
            return result;
        }
        if(null == checker){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},审核医生不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "审核医生不存在");
            return result;
        }
        sendHdRecipe.setDoctorName(doctor.getName());
        sendHdRecipe.setAuditor(checker.getName());
        sendHdRecipe.setDoctorNumber(doctor.getDoctorId().toString());
        return null;
    }

    /**
     * @method  assembleStoreMsg
     * @description 组装推送的处方单中药店和订单的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendHdRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleStoreAndOrderMsg(DrugEnterpriseResult result, HdRecipeDTO sendHdRecipe, Recipe nowRecipe) {
        if(null == nowRecipe.getOrderCode()){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定订单code为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定订单code为空");
            return result;
        }
        RecipeOrder order = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());

        if(null == order){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定订单不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定订单不存在");
            return result;
        }
        sendHdRecipe.setRecipeFee(null == order.getRecipeFee() ?  feeDefault : order.getRecipeFee().toString());
        sendHdRecipe.setActualFee(null == order.getActualPrice() ?  feeDefault : order.getActualPrice().toString());
        sendHdRecipe.setCouponFee(null == order.getCouponFee() ?  feeDefault : order.getCouponFee().toString());
        sendHdRecipe.setOrderTotalFee(null == order.getTotalFee() ?  feeDefault : order.getTotalFee().toString());
        sendHdRecipe.setExpressFee(null == order.getExpressFee() ?  feeDefault : order.getExpressFee().toString());
        sendHdRecipe.setDecoctionFee(null == order.getDecoctionFee() ?  feeDefault : order.getDecoctionFee().toString());
        sendHdRecipe.setRecipientName(order.getReceiver());
        sendHdRecipe.setRecipientTel(order.getRecMobile());
        return null;
    }

    /**
     * @method  assembleDepartMsg
     * @description 组装推送的处方单中科室的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendHdRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleDepartMsg(DrugEnterpriseResult result, HdRecipeDTO sendHdRecipe, Recipe nowRecipe) {
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        if(null == nowRecipe.getDepart()){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定科室id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定科室id为空");
            return result;
        }
        DepartmentDTO department = departmentService.get(nowRecipe.getDepart());
        if(null == department){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定科室不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定科室不存在");
            return result;
        }
        sendHdRecipe.setDepartName(department.getName());
        sendHdRecipe.setDepartId(department.getDeptId().toString());
        return null;
    }

    /**
     * @method  assemblePatientMsg
     * @description 组装推送的处方单中患者的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendHdRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assemblePatientMsg(DrugEnterpriseResult result, HdRecipeDTO sendHdRecipe, Recipe nowRecipe) {
        PatientService patientService = BasicAPI.getService(PatientService.class);
        if(null == nowRecipe.getMpiid()){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定患者id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定患者id为空");
            return result;
        }
        PatientDTO patient = patientService.get(nowRecipe.getMpiid());
        if(null == patient){
            LOGGER.warn("HdRemoteService.pushRecipeInfo:处方ID为{},绑定患者不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定患者不存在");
            return result;
        }
        sendHdRecipe.setPatientName(patient.getPatientName());
        sendHdRecipe.setPatientTelpatient(patient.getMobile());
        sendHdRecipe.setCertificate(patient.getCertificate());
        sendHdRecipe.setCertificateType(null == patient.getCertificateType() ? certificateTypeDefault : patient.getCertificateType().toString());
        try{
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());
            if (recipeOrder != null) {
                String province = getAddressDic(recipeOrder.getAddress1());
                String city = getAddressDic(recipeOrder.getAddress2());
                String district = getAddressDic(recipeOrder.getAddress3());
                String street = recipeOrder.getAddress4();
                sendHdRecipe.setProvince(province);
                sendHdRecipe.setCity(city);
                sendHdRecipe.setDistrict(district);
                sendHdRecipe.setAddress(street);
            }
        }catch(Exception e){
            LOGGER.info("HdRemoteService.assemblePatientMsg error:{}.", e.getMessage(),e);
        }

        return result;
    }

    public boolean sendScanStock(Integer recipeId, DrugsEnterprise drugsEnterprise, DrugEnterpriseResult result) {
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Map<String, Object> map = new HashMap<>();
        String methodName = "sendScanStock";
        List<Map<String, String>> hdDrugCodes = new ArrayList<>();
        Map<String, BigDecimal> drugCodes = new HashMap<>();
        drugsEnterprise = drugsEnterpriseDAO.getById(drugsEnterprise.getId());
        StringBuilder msg = new StringBuilder("药企名称:" + drugsEnterprise.getName() + ",");
        try{
            for (Recipedetail recipedetail : detailList) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
                if (saleDrugList == null) {
                    LOGGER.error("HdRemoteService.sendScanStock {}药品不在华东配送药品目录，recipeId:{}.", recipedetail.getDrugId(), recipe.getRecipeId());
                    return false;
                }
                LOGGER.info("HdRemoteService.sendScanStock.saleDrugList:{}.", JSONUtils.toString(saleDrugList));
                Map<String, String> drug = new HashMap<>();
                drug.put("drugCode", saleDrugList.getOrganDrugCode());
                LOGGER.info("HdRemoteService.sendScanStock.drug:{}.", JSONUtils.toString(drug));
                hdDrugCodes.add(drug);
                drugCodes.put(saleDrugList.getOrganDrugCode(), BigDecimal.valueOf(recipedetail.getUseTotalDose()));
                LOGGER.info("HdRemoteService.sendScanStock.drugCodes:{}.", JSONUtils.toString(drugCodes));
                if (recipe != null && recipe.getStatus() == 0) {
                    msg.append(" 药企药品编码:" + saleDrugList.getOrganDrugCode());
                    msg.append(",药品名称:" + recipedetail.getDrugName() + ",药品编码:"+ recipedetail.getDrugId());
                }
            }
            msg.append(",处方单号:" + recipeId);
            LOGGER.info("HdRemoteService.scanStock:{}", msg.toString());
            if (recipe != null && recipe.getStatus() == 0) {
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), msg.toString());
            }
        }catch(Exception e){
            LOGGER.error("HdRemoteService.checkDrugListByDeil error:{},{}.", recipeId, e.getMessage(),e);
            return false;
        }

        map.put("drugList", hdDrugCodes);
        //对浙四进行个性化处理,推送到指定药店配送
        String hdStores = recipeParameterDAO.getByName("hd_store_payonline");
        String storeOrganName = recipe.getClinicOrgan() + "_" + "hd_organ_store";
        String organStore = recipeParameterDAO.getByName(storeOrganName);

        if (StringUtils.isNotEmpty(hdStores) && hasOrgan(recipe.getClinicOrgan().toString(),hdStores)) {
            LOGGER.info("HdRemoteService.sendScanStock organStore:{}.", organStore);
            map.put("pharmacyCode", organStore);
        }
        String requestStr = JSONUtils.toString(map);
        //访问库存足够的药店列表以及药店下的药品的信息
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HdHttpUrlEnum httpUrl;
        try {
            if (drugsEnterprise.getBusinessUrl().contains("http:")) {

                httpUrl = HdHttpUrlEnum.fromMethodName(methodName);

                CloseableHttpResponse response = sendHttpRequest(drugsEnterprise, httpClient, requestStr, httpUrl);

                //当相应状态为200时返回json
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                if (StringUtils.isNotEmpty(requestStr) && responseStr.contains("401") && responseStr.contains("false")) {
                    LOGGER.info("HdRemoteService-scanStock 重新获取token .");
                    tokenUpdateImpl(drugsEnterprise);
                    sendScanStock(recipeId,drugsEnterprise,result);
                }
                LOGGER.info("HdRemoteService-scanStock sendScanStock responseStr:{}.", responseStr);
                JSONObject jsonObject = JSONObject.parseObject(responseStr);
                List drugList = (List)jsonObject.get("drugList");
                if (drugList != null && drugList.size() > 0) {
                    boolean scanStock = true;
                    if (drugList.size() != hdDrugCodes.size()) {
                        return false;
                    }
                    for (Object drug : drugList) {
                        Map<String, Object> drugMap = (Map<String, Object>) drug;
                        String drugCode = (String)drugMap.get("drugCode");
                        try{
                            BigDecimal availableSumQty = (BigDecimal)drugMap.get("availableSumQty");
                            LOGGER.info(drugCode + ":" + availableSumQty);
                            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "药企药品编码:"+drugCode + ",库存:" + availableSumQty);
                            BigDecimal num = drugCodes.get(drugCode);
                            if (num.compareTo(availableSumQty) == 1) {
                                LOGGER.info("HdRemoteService-scanStock sendScanStock drugCode:{} 库存不足.", drugCode);
                                //库存不足
                                return false;
                            }
                        }catch (Exception e){
                            Integer availableSumQty = (Integer)drugMap.get("availableSumQty");
                            LOGGER.info(drugCode + ":" + availableSumQty);
                            LOGGER.error("HdRemoteService-scanStock sendScanStock drugCode:{} 库存为0.", drugCode,e);
                            return false;
                        }
                    }
                    return scanStock;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("HdRemoteService." + methodName + ":查询可用的药店及其药品信息请求异常：{}", e.getMessage(),e);
            getFailResult(result, "请求异常");
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("HdRemoteService." + methodName + ":http请求资源关闭异常: {}", e.getMessage(),e);
                getFailResult(result, "http请求资源关闭异常");
            }
        }
        return false;
    }

    /**
     * @method  getHdStockResponse
     * @description 获取库存够得药店响应结果
     * @date: 2019/7/26
     * @author: JRK
     * @param recipeId 处方单id
     * @param drugsEnterprise 药企
     * @param result 总的请求结果集
     * @return recipe.drugsenterprise.bean.HdPharmacyAndStockResponse
     */
    private HdPharmacyAndStockResponse getHdStockResponse(Integer recipeId, DrugsEnterprise drugsEnterprise, DrugEnterpriseResult result) {
        //查询当前处方信息
        Recipe nowRecipe = recipeDAO.get(recipeId);
        //查询当前处方下详情信息
        List<Recipedetail> detailList = detailDAO.findByRecipeId(nowRecipe.getRecipeId());

        //根据药品请求华东旗下的所有可用药店，当有一个可用说明库存是足够的
        Map<String, HdDrugRequestData> resultMap = new HashMap<>();
        List<HdDrugRequestData> drugRequestDataList = getDrugRequestList(resultMap, detailList, drugsEnterprise, result);
        if(DrugEnterpriseResult.FAIL == result.getCode()) return null;
        HdPharmacyAndStockRequest hdPharmacyAndStockRequest = new HdPharmacyAndStockRequest();
        hdPharmacyAndStockRequest.setDrugList(drugRequestDataList);
        hdPharmacyAndStockRequest.setRange(getAllPharmacyRange);


        HdPharmacyAndStockResponse responseResult = checkPharmacyAndDrugMsgRequest(drugsEnterprise, hdPharmacyAndStockRequest, result, "scanStock");
        if(DrugEnterpriseResult.FAIL == result.getCode())
            return null;

        //如果返回token超时就刷新token重新进行请求(鉴权失败，重新请求token，更新后重新进行访问当前接口)
        HdTokenRequest request;
        if(HdFindSupportDepStatusEnum.TOKEN_EXPIRE.equals(HdFindSupportDepStatusEnum.fromCode(responseResult.getCode()))){
            request = new HdTokenRequest();
            this.getNewToken(drugsEnterprise, request, result);
            if(DrugEnterpriseResult.FAIL == result.getCode())
                return null;

            //重新获取token信息
            DrugsEnterprise newTokenEnterprise = drugsEnterpriseDAO.get(drugsEnterprise.getId());
            if(null == newTokenEnterprise){
                LOGGER.warn("HdRemoteService.scanStock:[{}][{}]当前药企信息不存在", drugsEnterprise.getId(), drugsEnterprise.getName());
                getFailResult(result, "当前药企信息不存在");
                return null;
            }
            //根据重新获取的token信息重新进行请求
            responseResult = this.checkPharmacyAndDrugMsgRequest(newTokenEnterprise, hdPharmacyAndStockRequest, result, "scanStock");
            if(DrugEnterpriseResult.FAIL.equals(result.getCode())){
                return null;
            }
        }
        if(DrugEnterpriseResult.FAIL.equals(result.getCode())){
            return null;
        }
        return responseResult;
    }

    /**
     * @method  checkPharmacyAndDrugMsgRequest
     * @description 检查库存获取药店的http请求
     * @date: 2019/7/25
     * @author: JRK
     * @param drugsEnterprise 药企信息
     * @param hdPharmacyAndStockRequest 检查库存获取药店请求
     * @param result 请求总的结果集
     * @param methodName 方法名
     * @return recipe.drugsenterprise.bean.HdPharmacyAndStockResponse 检查库存获取药店的响应结果
     */
    private HdPharmacyAndStockResponse checkPharmacyAndDrugMsgRequest(DrugsEnterprise drugsEnterprise, HdPharmacyAndStockRequest hdPharmacyAndStockRequest, DrugEnterpriseResult result, String methodName) {
        HdPharmacyAndStockResponse responseResult = null;

        //访问库存足够的药店列表以及药店下的药品的信息
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HdHttpUrlEnum httpUrl;
        try {
            if (drugsEnterprise.getBusinessUrl().contains("http:")) {

                httpUrl = HdHttpUrlEnum.fromMethodName(methodName);
                if(null == httpUrl){
                    getFailResult(result, "没有请求对应的url");
                    return responseResult;
                }
                String requestStr = JSONUtils.toString(hdPharmacyAndStockRequest);
                CloseableHttpResponse response = sendHttpRequest(drugsEnterprise, httpClient, requestStr, httpUrl);

                //当相应状态为200时返回json
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                responseResult = JSONUtils.parse(responseStr, HdPharmacyAndStockResponse.class);
                LOGGER.info("HdRemoteService." + methodName + "请求返回:{}", responseStr);
                //date 20191121
                //添加判断请求是否正常（是否返回成功失败标志位success）
                if(null == responseResult || null == responseResult.getSuccess()){
                    LOGGER.warn("HdRemoteService.findSupportDep:查询可用的药店及其药品信息,请求没有响应结果");
                    getFailResult(result, "请求没有响应结果");
                    return responseResult;
                }
                if(responseResult.getSuccess()){
                    LOGGER.info("HdRemoteService." + methodName + ":查询可用的药店及其药品信息请求成功，请求返回:{}", responseStr);
                    return responseResult;
                }else if(HdFindSupportDepStatusEnum.TOKEN_EXPIRE.equals(HdFindSupportDepStatusEnum.fromCode(responseResult.getCode()))){
                    LOGGER.info("HdRemoteService." + methodName + ":查询可用的药店及其药品信息请求鉴权失败需要重新获取token");
                    return responseResult;
                }else{
                    LOGGER.warn("HdRemoteService." + methodName + ":查询可用的药店及其药品信息请求失败,失败原因:{}", responseResult.getMessage());
                    getFailResult(result, HdFindSupportDepStatusEnum.fromCode(responseResult.getCode()).getMean());
                    return responseResult;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("HdRemoteService." + methodName + ":查询可用的药店及其药品信息请求异常：{}", e.getMessage(),e);
            getFailResult(result, "请求异常");
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("HdRemoteService." + methodName + ":http请求资源关闭异常: {}", e.getMessage(),e);
                getFailResult(result, "http请求资源关闭异常");
            }
        }
        return responseResult;
    }

    /**
     * @method  checkDrugStockMsgRequest
     * @description 检查药品库存的http请求
     * @date: 2019/7/25
     * @author: JRK
     * @param drugsEnterprise 药企信息
     * @param hdPharmacyAndStockRequest 检查库存获取药店请求
     * @param result 请求总的结果集
     * @return recipe.drugsenterprise.bean.HdPharmacyAndStockResponse 检查库存获取药店的响应结果
     */
    private HdPharmacyAndStockResponse checkDrugStockMsgRequest(DrugsEnterprise drugsEnterprise, HdPharmacyAndStockRequest hdPharmacyAndStockRequest, DrugEnterpriseResult result) {
        HdPharmacyAndStockResponse responseResult = null;

        //访问库存足够的药店列表以及药店下的药品的信息
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HdHttpUrlEnum httpUrl;
        try {
            if (drugsEnterprise.getBusinessUrl().contains("http:")) {

                httpUrl = HdHttpUrlEnum.fromMethodName("scanStock");
                if(null == httpUrl){
                    getFailResult(result, "没有请求对应的url");
                    return responseResult;
                }
                String requestStr = JSONUtils.toString(hdPharmacyAndStockRequest);
                CloseableHttpResponse response = sendHttpRequest(drugsEnterprise, httpClient, requestStr, httpUrl);

                //当相应状态为200时返回json
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                responseResult = JSONUtils.parse(responseStr, HdPharmacyAndStockResponse.class);
                if(null == responseResult || null == responseResult.getSuccess()){
                    LOGGER.warn("HdRemoteService.scanStock:查询可用的药店及其药品信息,请求没有响应结果");
                    getFailResult(result, "请求没有响应结果");
                    return responseResult;
                }
                if(responseResult.getSuccess()){
                    LOGGER.info("HdRemoteService.scanStock:查询可用的药店及其药品信息请求成功，请求返回:{}", responseStr);
                    return responseResult;
                }else if(HdFindSupportDepStatusEnum.TOKEN_EXPIRE.equals(HdFindSupportDepStatusEnum.fromCode(responseResult.getCode()))){
                    LOGGER.info("HdRemoteService.scanStock:查询可用的药店及其药品信息请求鉴权失败需要重新获取token");
                    return responseResult;
                }else{
                    LOGGER.warn("HdRemoteService.scanStock:查询可用的药店及其药品信息请求失败,失败原因:{}", responseResult.getMessage());
                    getFailResult(result, HdFindSupportDepStatusEnum.fromCode(responseResult.getCode()).getMean());
                    return responseResult;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("HdRemoteService.scanStock:查询可用的药店及其药品信息请求异常：{}", e.getMessage(),e);
            getFailResult(result, "请求异常");
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("HdRemoteService.scanStock:http请求资源关闭异常: {}", e.getMessage(),e);
                getFailResult(result, "http请求资源关闭异常");
            }
        }
        return responseResult;
    }

    /**
     * @method  getDrugRequestList
     * @description 获取请求药店下药品信息接口下的药品总量（根据药品的code分组）的list
     * @date: 2019/7/25
     * @author: JRK
     * @param detailList 处方下的详情列表
     * @param  finalResult 请求的最终结果
     * @return java.util.List<recipe.drugsenterprise.bean.HdDrugRequestData>请求药店下药品信息列表
     */
    private List<HdDrugRequestData> getDrugRequestList(Map<String, HdDrugRequestData> result, List<Recipedetail> detailList, DrugsEnterprise drugsEnterprise, DrugEnterpriseResult finalResult) {

        HdDrugRequestData hdDrugRequestData;
        Double sum;
        SaleDrugList saleDrug;
        //遍历处方详情，通过drugId判断，相同药品下的需求量叠加
        for (Recipedetail recipedetail : detailList) {
            //这里的药品是对接到药企下的所以取的是saleDrugList的药品标识
            saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if(null == saleDrug){
                LOGGER.warn("HdRemoteService.pushRecipeInfo:药品id:{},药企id:{}的药企药品信息不存在",
                        recipedetail.getDrugId(), drugsEnterprise.getId());
                getFailResult(finalResult, "对接的药品信息为空");
                return null;
            }
            hdDrugRequestData = result.get(saleDrug.getOrganDrugCode());

            if(null == hdDrugRequestData){
                HdDrugRequestData newHdDrugRequestData1 = new HdDrugRequestData();
                result.put(saleDrug.getOrganDrugCode(), newHdDrugRequestData1);
                newHdDrugRequestData1.setDrugCode(saleDrug.getOrganDrugCode());
                newHdDrugRequestData1.setTotal(null == recipedetail.getUseTotalDose() ? drugRecipeTotal : String.valueOf(new Double(recipedetail.getUseTotalDose()).intValue()));
            }else{
                //叠加需求量
                sum = Double.parseDouble(hdDrugRequestData.getTotal()) + recipedetail.getUseTotalDose();
                hdDrugRequestData.setTotal(String.valueOf(sum.intValue()));
                result.put(saleDrug.getOrganDrugCode(), hdDrugRequestData);
            }
        }
        //将叠加好总量的药品分组转成list
        List<HdDrugRequestData> hdDrugRequestDataList = new ArrayList<HdDrugRequestData>(result.values());
        return hdDrugRequestDataList;
    }

    /**
     * @method  sendHttpRequest
     * @description 查询药店或者药品信息的请求
     * @date: 2019/7/11
     * @author: JRK
     * @param drugsEnterprise
     * @param httpClient 请求服务
     * @param  requestStr 请求的json
     * @param  httpUrl 请求的url枚举
     * @return org.apache.http.client.methods.CloseableHttpResponse
     * 返回药店下药品信息的响应
     */
    private CloseableHttpResponse sendHttpRequest(DrugsEnterprise drugsEnterprise, CloseableHttpClient httpClient, String requestStr, HdHttpUrlEnum httpUrl) throws IOException {
        HttpPost httpPost = new HttpPost(drugsEnterprise.getBusinessUrl() + httpUrl.getUrl());
        //组装请求参数(组装权限验证部分)
        httpPost.setHeader(requestHeadJsonKey, requestHeadJsonValue);
        httpPost.setHeader(requestHeadAuthorizationKey, requestAuthorizationValueHead + drugsEnterprise.getToken());

        LOGGER.info("HdRemoteService.pushRecipeInfo:[{}][{}]药企正在发送{}，请求内容：{}",
                drugsEnterprise.getId(), drugsEnterprise.getName(), httpUrl.getMsg(), requestStr);
        StringEntity requestEntry = new StringEntity(requestStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntry);
        //获取响应消息
        return httpClient.execute(httpPost);
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    @RpcService
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();

        //组装查询接口，根据当前请求，判断对应药店下的处方的药品是否有库存

        //根据请求返回的药店列表，获取对应药店下药品需求的总价格
        Map<String, HdDrugRequestData> resultMap = new HashMap<>();
        HdPharmacyAndStockResponse responsResult = getFindPharamcyList(result, recipeIds, ext, enterprise, resultMap);
        if(DrugEnterpriseResult.FAIL == result.getCode()) {
            return result;
        }

        //数据封装成页面展示数据
        List<DepDetailBean> pharmacyDetailPage = assemblePharmacyPageMsg(ext, enterprise, responsResult, resultMap);
        result.setObject(pharmacyDetailPage);
        return result;
    }

    /**
     * @method  getFindPharamcyList
     * @description 根据请求获取药店列表
     * @date: 2019/7/26
     * @author: JRK
     * @param result 总的请求结果集
     * @param recipeIds 处方单集合
     * @param ext 查询map
     * @param enterprise 药企
     * @param  resultMap 处方下根据药品获取总需求量
     * @return recipe.drugsenterprise.bean.HdPharmacyAndStockResponse
     */
    private HdPharmacyAndStockResponse getFindPharamcyList(DrugEnterpriseResult result, List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise, Map<String, HdDrugRequestData> resultMap){
        HdPharmacyAndStockResponse resultResponse = null ;
        //现在默认只有一个处方单
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeIds.get(0));
        if(CollectionUtils.isEmpty(detailList)){
            LOGGER.warn("HdRemoteService.findSupportDep:处方单{}的细节信息为空", recipeIds.get(0));
            getFailResult(result, "处方单细节信息为空");
            return resultResponse;
        }

        Recipe nowRecipe = recipeDAO.get(recipeIds.get(0));
        if (null == nowRecipe) {
            LOGGER.warn("HdRemoteService.findSupportDep:处方单{}不存在", recipeIds.get(0));
            getFailResult(result, "处方单不存在");
            return resultResponse;
        }

        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organ = organService.getByOrganId(nowRecipe.getClinicOrgan());
        if(null == organ){
            LOGGER.warn("HdRemoteService.findSupportDep:处方ID为{},对应的开处方机构不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "开处方机构不存在");
            return resultResponse;
        }
        //首先组装请求对象
        HdPharmacyAndStockRequest hdPharmacyAndStockRequest = new HdPharmacyAndStockRequest();
        if (ext != null && null != ext.get(searchMapRANGE) && null != ext.get(searchMapLongitude) && null != ext.get(searchMapLatitude)) {
            List<HdDrugRequestData> drugRequestList = getDrugRequestList(resultMap, detailList, enterprise, result);
            if(DrugEnterpriseResult.FAIL == result.getCode()) return null;
            hdPharmacyAndStockRequest.setDrugList(drugRequestList);
            hdPharmacyAndStockRequest.setRange(ext.get(searchMapRANGE).toString());
            hdPharmacyAndStockRequest.setPosition(new HdPosition(ext.get(searchMapLongitude).toString(), ext.get(searchMapLatitude).toString()));
            hdPharmacyAndStockRequest.setRecipeCode(nowRecipe.getRecipeCode());
            hdPharmacyAndStockRequest.setRecipeId(recipeIds.get(0).toString());
            hdPharmacyAndStockRequest.setOrganId(organ.getOrganizeCode());
        }else{
            LOGGER.warn("HdRemoteService.findSupportDep:请求的搜索参数不健全" );
            getFailResult(result, "请求的搜索参数不健全");
            return resultResponse;
        }

        //根据请求返回的药店列表，获取对应药店下药品需求的总价格
        HdPharmacyAndStockResponse responseResult = checkPharmacyAndDrugMsgRequest(enterprise, hdPharmacyAndStockRequest, result, "findSupportDep");
        if(DrugEnterpriseResult.FAIL == result.getCode())
            return null;

        //如果返回token超时就刷新token重新进行请求(鉴权失败，重新请求token，更新后重新进行访问当前接口)
        HdTokenRequest request;
        if(HdFindSupportDepStatusEnum.TOKEN_EXPIRE.equals(HdFindSupportDepStatusEnum.fromCode(responseResult.getCode()))){
            //校验token时效并更新
            request = new HdTokenRequest();
            this.getNewToken(enterprise, request, result);
            if(DrugEnterpriseResult.FAIL == result.getCode())
                return null;

            //重新获取token信息
            DrugsEnterprise newTokenEnterprise = drugsEnterpriseDAO.get(enterprise.getId());
            if(null == newTokenEnterprise){
                LOGGER.warn("HdRemoteService.findSupportDep:[{}][{}]当前药企信息不存在", enterprise.getId(), enterprise.getName());
                getFailResult(result, "当前药企信息不存在");
                return null;
            }
            //根据重新获取的token信息重新进行请求
            responseResult = this.checkPharmacyAndDrugMsgRequest(newTokenEnterprise, hdPharmacyAndStockRequest, result, "findSupportDep");
            if(DrugEnterpriseResult.FAIL.equals(result.getCode())){
                return null;
            }
        }
        if(DrugEnterpriseResult.FAIL.equals(result.getCode())){
            return null;
        }
        return responseResult;
    }

    /**
     * @method  assemblePharmacyPageMsg
     * @description 组装页面展示的药店信息
     * @date: 2019/7/12
     * @author: JRK
     * @param ext 搜索map
     * @param enterprise 药企
     * @return java.util.List<com.ngari.recipe.drugsenterprise.model.DepDetailBean> 页面药店信息展示
     */
    private List<DepDetailBean> assemblePharmacyPageMsg(Map ext, DrugsEnterprise enterprise, HdPharmacyAndStockResponse responsResult, Map<String, HdDrugRequestData> drugResult) {
        List<DepDetailBean> pharmacyDetailPage = new ArrayList<>();
        Position position;
        DepDetailBean newDepDetailBean;

        for (HdPharmacyAndStockResponseData pharmacyMsg : responsResult.getData()) {
            newDepDetailBean = new DepDetailBean();
            //这里只储存药店处方药品信总价初始化成功的药店
            if(!pharmacyMsg.init(drugResult)){
                continue;
            }
            newDepDetailBean.setRecipeFee(pharmacyMsg.getTotalFee());
            newDepDetailBean.setDepId(enterprise.getId());
            newDepDetailBean.setDepName(pharmacyMsg.getPharmacyName());
            //根据药店信息获取上面跌加出的总价格

            newDepDetailBean.setSendMethod(hdSendMethod);
            newDepDetailBean.setPayMethod(hdPayMethod);
            newDepDetailBean.setPharmacyCode(pharmacyMsg.getPharmacyCode());
            newDepDetailBean.setAddress(pharmacyMsg.getAddress());
            LOGGER.info("HdRemoteService.findSupportDep pharmacyMsg:{}.", JSONUtils.toString(pharmacyMsg));
            //设置药店的坐标
            position = new Position();
            if(!HdPosition.checkParameter(pharmacyMsg.getPosition())){
                LOGGER.warn("HdRemoteService.findSupportDep:当前药店[{}][{}]坐标信息不健全",
                        pharmacyMsg.getPharmacyCode(), pharmacyMsg.getPharmacyName());
                continue;
            }
            position.setLatitude(Double.parseDouble(pharmacyMsg.getPosition().getLatitude()));
            position.setLongitude(Double.parseDouble(pharmacyMsg.getPosition().getLongitude()));
            position.setRange(Integer.parseInt(ext.get(searchMapRANGE).toString()));
            newDepDetailBean.setPosition(position);
            newDepDetailBean.setBelongDepName(enterprise.getName());
            //记录药店和用户两个经纬度的距离
            newDepDetailBean.setDistance(DistanceUtil.getDistance(Double.parseDouble(ext.get(searchMapLatitude).toString()),
                    Double.parseDouble(ext.get(searchMapLongitude).toString()), Double.parseDouble(pharmacyMsg.getPosition().getLatitude()), Double.parseDouble(pharmacyMsg.getPosition().getLongitude())));

            pharmacyDetailPage.add(newDepDetailBean);
        }
        return pharmacyDetailPage;
    }

    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_HDDYF;
    }

    private List getInventoryResult(Map<String, Object> map, Integer organId, DrugsEnterprise drugsEnterprise){
        tokenUpdateImpl(drugsEnterprise);
        String methodName = "sendScanStock";
        //对浙四进行个性化处理,推送到指定药店配送
        String hdStores = recipeParameterDAO.getByName("hd_store_payonline");
        String storeOrganName = organId + "_" + "hd_organ_store";
        String organStore = recipeParameterDAO.getByName(storeOrganName);

        if (StringUtils.isNotEmpty(hdStores) && organId != null && hasOrgan(organId.toString(),hdStores)) {
            LOGGER.info("HdRemoteService.sendScanStock organStore:{}.", organStore);
            map.put("pharmacyCode", organStore);
        }

        String requestParames = JSONUtils.toString(map);
        //访问库存足够的药店列表以及药店下的药品的信息
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HdHttpUrlEnum httpUrl;
        try {
            httpUrl = HdHttpUrlEnum.fromMethodName(methodName);
            CloseableHttpResponse response = sendHttpRequest(httpClient, requestParames, httpUrl, drugsEnterprise);
            //当相应状态为200时返回json
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("getInventoryResult responseStr:{}", responseStr);
            JSONObject jsonObject = JSONObject.parseObject(responseStr);
            List drugList = (List)jsonObject.get("drugList");
            LOGGER.info("getInventoryResult drugList:{}.", JSONUtils.toString(drugList));
            return drugList;
        } catch (Exception e) {
            LOGGER.error("getInventoryResult error", e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ArrayList();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        tokenUpdateImpl(drugsEnterprise);
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
        Map<String, Object> map = new HashMap<>();
        List<Map<String, String>> hdDrugCodes = new ArrayList<>();
        Map<String, String> drug = new HashMap<>();
        drug.put("drugCode", saleDrugList.getOrganDrugCode());
        hdDrugCodes.add(drug);
        map.put("drugList", hdDrugCodes);
        List drugList = getInventoryResult(map, organId, drugsEnterprise);
        LOGGER.info("getDrugInventory drugList:{}",JSONUtils.toString(drugList));
        try {
            if (CollectionUtils.isNotEmpty(drugList)) {
                for (Object drugs : drugList) {
                    Map<String, Object> drugMap = (Map<String, Object>) drugs;
                    LOGGER.info("getDrugInventory drugMap:{}",JSONUtils.toString(drugMap));
                    try{
                        BigDecimal availableSumQty = (BigDecimal)drugMap.get("availableSumQty");
                        return availableSumQty.toString();
                    }catch(Exception e){
                        LOGGER.error("getDrugInventory error ",e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("getDrugInventory error ", e);
        }
        return "暂不支持库存查询";
    }

    @RpcService
    public void test(Integer recipeId){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(3032);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetailList);
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        try {
            LOGGER.info("scanEnterpriseDrugStock recipeDetails:{}", JSONUtils.toString(recipeDetails));
            List<Integer> drugList = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
            List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIdsEffectivity(drugsEnterprise.getId(), drugList);
            LOGGER.info("scanEnterpriseDrugStock saleDrugLists:{}.", JSONUtils.toString(saleDrugLists));
            Map<Integer, String> saleDrugListMap = saleDrugLists.stream().collect(Collectors.toMap(SaleDrugList::getDrugId, SaleDrugList::getOrganDrugCode));
            Map<String, Object> map = new HashMap<>();
            List<Map<String, String>> hdDrugCodes = new ArrayList<>();
            recipeDetails.forEach(recipeDetail -> {
                Map<String, String> drug = new HashMap<>();
                drug.put("drugCode", saleDrugListMap.get(recipeDetail.getDrugId()));
                hdDrugCodes.add(drug);
            });
            map.put("drugList", hdDrugCodes);
            List result = new ArrayList();
            if (drugsEnterprise.getPayModeSupport() == 3) {
                result = checkStoreInventory(recipe, recipeDetails, saleDrugListMap, drugsEnterprise);
            } else {
                result = getInventoryResult(map, recipe.getClinicOrgan(), drugsEnterprise);
            }
            Map<String, Integer> inventory = new HashMap<>();
            if (CollectionUtils.isNotEmpty(result)) {
                for (Object drugs : result) {
                    Map<String, Object> drugMap = (Map<String, Object>) drugs;
                    try{
                        BigDecimal availableSumQty = (BigDecimal)drugMap.get("availableSumQty");
                        String drugCode = (String)drugMap.get("drugCode");
                        inventory.put(drugCode, availableSumQty.intValue());
                    }catch(Exception e){
                        Integer inventoryNum = getInventoryNum(drugMap.get("availableSumQty"));
                        String drugCode = (String)drugMap.get("drugCode");
                        inventory.put(drugCode, inventoryNum);
                    }
                }
            }
            DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
            List<DrugInfoDTO> drugInfoList = new ArrayList<>();
            recipeDetails.forEach(recipeDetail -> {
                DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
                BeanUtils.copyProperties(recipeDetail, drugInfoDTO);
                String saleDrugCode = saleDrugListMap.get(recipeDetail.getDrugId());
                drugInfoDTO.setStock(false);
                drugInfoDTO.setStockAmount(0);
                if (StringUtils.isNotEmpty(saleDrugCode) && inventory.size() > 0) {
                    Integer num = inventory.get(saleDrugCode);
                    if (null != num) {
                        drugInfoDTO.setStock(num > 0);
                        drugInfoDTO.setStockAmountChin(num + "");
                    }
                }
                drugInfoList.add(drugInfoDTO);
            });
            super.setDrugStockAmountDTO(drugStockAmountDTO, drugInfoList);
            LOGGER.info("scanEnterpriseDrugStock drugStockAmountDTO:{}", JSONUtils.toString(drugStockAmountDTO));
            return drugStockAmountDTO;
        } catch (Exception e) {
            LOGGER.error("scanEnterpriseDrugStock error", e);
        }
        return super.scanEnterpriseDrugStock(recipe,drugsEnterprise,recipeDetails);
    }

    /**
     * 查询药店
     * @param recipe
     * @param recipeDetailList
     * @param saleDrugListMap
     * @param drugsEnterprise
     * @return
     */
    private List checkStoreInventory(Recipe recipe, List<Recipedetail> recipeDetailList, Map<Integer, String> saleDrugListMap, DrugsEnterprise drugsEnterprise){
        LOGGER.info("checkStoreInventory recipe:{}", JSONUtils.toString(recipe));
        List result = new ArrayList();
        String methodName = "scanStock";
        try {
        //组装查询药店的入参
        HdPharmacyAndStockRequest hdPharmacyAndStockRequest = new HdPharmacyAndStockRequest();
        List<HdDrugRequestData> drugRequestDataList = new ArrayList<>();
        recipeDetailList.forEach(recipeDetail -> {
            HdDrugRequestData hdDrugRequestData = new HdDrugRequestData();
            hdDrugRequestData.setDrugCode(saleDrugListMap.get(recipeDetail.getDrugId()));
            hdDrugRequestData.setTotal(recipeDetail.getUseTotalDose().toString());
            hdDrugRequestData.setUnit(recipeDetail.getDrugUnit());
            drugRequestDataList.add(hdDrugRequestData);
        });
        hdPharmacyAndStockRequest.setDrugList(drugRequestDataList);
        //医生端不会传患者的坐标，默认0表示查询所有药店
        hdPharmacyAndStockRequest.setRange("0");
        if (null != recipe) {
            hdPharmacyAndStockRequest.setOrganId(recipe.getClinicOrgan().toString());
            hdPharmacyAndStockRequest.setRecipeCode(recipe.getRecipeCode());
            hdPharmacyAndStockRequest.setRecipeId(null!=recipe.getRecipeId()?recipe.getRecipeId().toString():"");
        }
            //访问库存足够的药店列表以及药店下的药品的信息
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HdHttpUrlEnum httpUrl = HdHttpUrlEnum.fromMethodName(methodName);
            String requestStr = JSONUtils.toString(hdPharmacyAndStockRequest);
            CloseableHttpResponse response = sendHttpRequest(drugsEnterprise, httpClient, requestStr, httpUrl);

            //当相应状态为200时返回json
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("checkStoreInventory responseStr:{}", responseStr);
            HdPharmacyAndStockResponse responseResult = JSONUtils.parse(responseStr, HdPharmacyAndStockResponse.class);
            LOGGER.info("checkStoreInventory responseResult:{}", JSONUtils.toString(responseResult));
            if (null != responseResult && responseResult.getSuccess() && CollectionUtils.isNotEmpty(responseResult.getData())) {
                //说明存在有库存的药店
                List<HdPharmacyAndStockResponseData> stockResponseList = responseResult.getData();
                //默认取一个药店
                HdPharmacyAndStockResponseData stockResponseData = stockResponseList.get(0);
                if (null != stockResponseData && CollectionUtils.isNotEmpty(stockResponseData.getDrugInvs())) {
                    List<HdDrugResponseData> hdDrugResponseDataList = stockResponseData.getDrugInvs();
                    hdDrugResponseDataList.forEach(drugRequestData -> {
                        Map<String, Object> drugMap = new HashMap<>();
                        drugMap.put("drugCode", drugRequestData.getDrugCode());
                        drugMap.put("availableSumQty", drugRequestData.getInvQty());
                        result.add(drugMap);
                    });
                }
            }
        } catch (IOException e) {
            LOGGER.error("checkStoreInventory error", e);
        }
        return result;
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        tokenUpdateImpl(drugsEnterprise);
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> hdDrugCodes = new ArrayList<>();
        Map<String, Object> drug = new HashMap<>();
        Map<String, String> drugData = new HashMap<>();
        for (RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                drug.put("drugCode", saleDrugList.getOrganDrugCode());
                drug.put("total", recipeDetailBean.getUseTotalDose().intValue()+"");
                hdDrugCodes.add(drug);
                drugData.put(saleDrugList.getOrganDrugCode(), recipeDetailBean.getUseTotalDose() + "&&" + recipeDetailBean.getDrugName());
            }
        }
        map.put("drugList", hdDrugCodes);
        List<String> result = new ArrayList<>();
        if (new Integer(1).equals(flag)) {
            //配送到家
            String methodName = "sendScanStock";

            //对浙四进行个性化处理,推送到指定药店配送
            String hdStores = recipeParameterDAO.getByName("hd_store_payonline");
            String storeOrganName = drugsDataBean.getOrganId() + "_" + "hd_organ_store";
            String organStore = recipeParameterDAO.getByName(storeOrganName);

            if (StringUtils.isNotEmpty(hdStores) && drugsDataBean.getOrganId() != null && hasOrgan(drugsDataBean.getOrganId().toString(),hdStores)) {
                LOGGER.info("HdRemoteService.sendScanStock organStore:{}.", organStore);
                map.put("pharmacyCode", organStore);
            }

            String requestParames = JSONUtils.toString(map);
            //访问库存足够的药店列表以及药店下的药品的信息
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HdHttpUrlEnum httpUrl;
            try {
                httpUrl = HdHttpUrlEnum.fromMethodName(methodName);
                CloseableHttpResponse response = sendHttpRequest(httpClient, requestParames, httpUrl, drugsEnterprise);
                //当相应状态为200时返回json
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                JSONObject jsonObject = JSONObject.parseObject(responseStr);
                List drugList = (List)jsonObject.get("drugList");

                if (drugList != null && drugList.size() > 0) {
                    for (Object drugs : drugList) {
                        Map<String, Object> drugMap = (Map<String, Object>) drugs;
                        try{
                            BigDecimal availableSumQty = (BigDecimal)drugMap.get("availableSumQty");
                            String drugCode = (String)drugMap.get("drugCode");
                            String drugValue = drugData.get(drugCode);
                            if (StringUtils.isNotEmpty(drugValue) && drugValue.contains("&&")) {
                                String[] values = drugValue.split("&&");
                                double useTotalDose = Double.parseDouble(values[0]);
                                if (availableSumQty.doubleValue() > useTotalDose) {
                                    result.add(values[1]);
                                }
                            }
                        }catch(Exception e){
                            LOGGER.info("getDrugInventoryForApp 配送处理数据 error msg:{}.", e.getMessage(), e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("getDrugInventoryForApp 配送解析数据 error msg:{}.", e.getMessage(), e);
            } finally {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        } else {
            //到店取药
            String methodName = "findSupportDep";
            map.put("range", "0");
            String requestParames = JSONUtils.toString(map);
            LOGGER.info("requestParames :{}.", requestParames);
            //访问库存足够的药店列表以及药店下的药品的信息
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HdHttpUrlEnum httpUrl;
            try {
                httpUrl = HdHttpUrlEnum.fromMethodName(methodName);
                CloseableHttpResponse response = sendHttpRequest(httpClient, requestParames, httpUrl, drugsEnterprise);
                //当相应状态为200时返回json
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                JSONObject jsonObject = JSONObject.parseObject(responseStr);
                List datas = (List)jsonObject.get("data");
                LOGGER.info("responseStr :{}.", responseStr);
                if (CollectionUtils.isNotEmpty(datas)) {
                    for (Object data : datas) {
                        Map<String, Object> drugMap = (Map<String, Object>)data;
                        List drugInvs = (List)drugMap.get("drugInvs");
                        for (Object drugs : drugInvs) {
                            Map<String, Object> drugResult = (Map<String, Object>) drugs;
                            try{
                                Double availableSumQty = Double.parseDouble((String)drugResult.get("invQty"));
                                String drugCode = (String)drugResult.get("drugCode");
                                String drugValue = drugData.get(drugCode);
                                if (StringUtils.isNotEmpty(drugValue) && drugValue.contains("&&")) {
                                    String[] values = drugValue.split("&&");
                                    double useTotalDose = Double.parseDouble(values[0]);
                                    if (availableSumQty > useTotalDose) {
                                        result.add(values[1]);
                                    }
                                }
                            }catch(Exception e){
                                LOGGER.info("getDrugInventoryForApp 药店解析数据 error msg:{}.", e.getMessage(), e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("getDrugInventoryForApp 药店处理数据 error msg:{}.", e.getMessage(), e);
            } finally {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private static CloseableHttpResponse sendHttpRequest(CloseableHttpClient httpClient, String requestStr, HdHttpUrlEnum httpUrl, DrugsEnterprise drugsEnterprise) throws IOException {
        HttpPost httpPost = new HttpPost(drugsEnterprise.getBusinessUrl() + httpUrl.getUrl());
        //组装请求参数(组装权限验证部分)
        httpPost.setHeader(requestHeadJsonKey, requestHeadJsonValue);
        httpPost.setHeader(requestHeadAuthorizationKey, requestAuthorizationValueHead + drugsEnterprise.getToken());

        StringEntity requestEntry = new StringEntity(requestStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntry);
        //获取响应消息
        return httpClient.execute(httpPost);
    }

    private boolean hasOrgan(String organ, String args){
        if (StringUtils.isNotEmpty(args)) {
            String[] organs = args.split(",");
            List<String> organList = Arrays.asList(organs);
            return organList.contains(organ);
        }
        return false;
    }

    private static Integer getInventoryNum(Object obj){
        Integer result = 0;
        try {
            String clazzName = obj.getClass().getName();
            String type = clazzName.substring(clazzName.lastIndexOf(".")+1);
            switch (type) {
                case "Integer" :
                    result = Integer.parseInt(obj.toString());
                    break;
                case "Double" :
                    Double data = (Double)obj;
                    result = data.intValue();
                    break;
                default :
                    break;
            }
        } catch (NumberFormatException e) {
            LOGGER.info("getInventoryNum error ", e);
        }
        return result;
    }

}