package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.collect.ImmutableMap;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.MedicalPreSettleReqTO;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.*;
import com.qimencloud.api.sceneqimen.request.AlibabaAlihealthPrescriptionStatusSyncRequest;
import com.qimencloud.api.sceneqimen.response.AlibabaAlihealthPrescriptionStatusSyncResponse;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.*;
import com.taobao.api.response.*;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import eh.utils.DateConversion;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseResponse;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.StandardResultDTO;
import recipe.drugsenterprise.bean.StandardStateDTO;
import recipe.hisservice.RecipeToHisService;
import recipe.service.common.RecipeCacheService;
import java.text.SimpleDateFormat;
import java.util.*;

import static ctd.util.AppContextHolder.getBean;

/**
 * @description 杭州互联网（金投）对接服务
 * @author gmw
 * @date 2019/9/11
 */
@RpcBean("hzInternetRemoteService")
public class HzInternetRemoteService extends AccessDrugEnterpriseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(HzInternetRemoteService.class);

    private static final String EXPIRE_TIP = "请重新授权";

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private TaobaoConf taobaoConf;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("HzInternetRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("杭州互联网虚拟药企-更新取药信息至处方流转平台开始，处方ID：{}.", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        //1物流配送
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);


        UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
        updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
        //平台处方号
        updateTakeDrugWayReqTO.setNgarRecipeId(recipe.getRecipeId()+"");
        //医院处方号
        //流转到这里来的属于物流配送
        updateTakeDrugWayReqTO.setDeliveryType("1");
        updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
        updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));

        updateTakeDrugWayReqTO.setPayMode("1");
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if(recipeExtend != null && recipeExtend.getDeliveryCode() != null){
            updateTakeDrugWayReqTO.setDeliveryCode(recipeExtend.getDeliveryCode());
            updateTakeDrugWayReqTO.setDeliveryName(recipeExtend.getDeliveryName());
        } else {
            LOGGER.info("杭州互联网虚拟药企-未获取his返回的配送药-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
            result.setMsg("未获取his返回的配送药");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        if (StringUtils.isNotEmpty(recipe.getOrderCode())){
            RecipeOrderDAO dao = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = dao.getByOrderCode(recipe.getOrderCode());
            if (order!=null){
                //收货人
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                //联系电话
                updateTakeDrugWayReqTO.setContactTel(order.getRecTel());
                //详细收货地址
                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                updateTakeDrugWayReqTO.setAddress(commonRemoteService.getCompleteAddress(order));

                //收货地址代码
                updateTakeDrugWayReqTO.setReceiveAddrCode(order.getAddress3());
                String address3 = null;
                try {
                    address3 = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getAddress3());
                } catch (ControllerException e) {
                    LOGGER.warn("杭州互联网虚拟药企-未获取收货地址名称-add={}", JSONUtils.toString(order.getAddress3()));

                }
                //收货地址名称
                updateTakeDrugWayReqTO.setReceiveAddress(address3);
                //期望配送日期
                updateTakeDrugWayReqTO.setConsignee(order.getExpectSendDate());
                //期望配送时间
                updateTakeDrugWayReqTO.setContactTel(order.getExpectSendTime());
            }else{
                LOGGER.info("杭州互联网虚拟药企-未获取有效订单-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
                result.setMsg("未获取有效订单");
                result.setCode(DrugEnterpriseResult.FAIL);
            }
        }

        HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
        if("200".equals(hisResult.getMsgCode())){
            LOGGER.info("杭州互联网虚拟药企-更新取药信息成功-his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));
            result.setCode(DrugEnterpriseResult.SUCCESS);
        }else{
            LOGGER.error("杭州互联网虚拟药企-更新取药信息失败-his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));

            result.setMsg(hisResult.getMsg());
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        return result;
    }

    /*
     * @description 处方预结算
     * @author gaomw
     * @date 2019/12/13
     * @param [recipeId]
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    public DrugEnterpriseResult recipeMedicalPreSettle(Integer recipeId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        MedicalPreSettleReqTO medicalPreSettleReqTO = new MedicalPreSettleReqTO();
        medicalPreSettleReqTO.setClinicOrgan(recipe.getClinicOrgan());

        //封装医保信息
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if(recipeExtend != null && recipeExtend.getMedicalSettleData() != null){
            medicalPreSettleReqTO.setHospOrgCode(recipeExtend.getHospOrgCodeFromMedical());
            medicalPreSettleReqTO.setInsuredArea(recipeExtend.getInsuredArea());
            medicalPreSettleReqTO.setMedicalSettleData(recipeExtend.getMedicalSettleData());
        } else {
            LOGGER.info("杭州互联网虚拟药企-未获取处方医保结算请求串-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
            result.setMsg("未获取处方医保结算请求串");
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        HisResponseTO hisResult = service.recipeMedicalPreSettle(medicalPreSettleReqTO);
        if(hisResult != null && "200".equals(hisResult.getMsgCode())){
            LOGGER.info("杭州互联网虚拟药企-处方预结算成功-his. param={},result={}", JSONUtils.toString(medicalPreSettleReqTO), JSONUtils.toString(hisResult));
            result.setCode(DrugEnterpriseResult.SUCCESS);
        }else{
            LOGGER.error("杭州互联网虚拟药企-处方预结算失败-his. param={},result={}", JSONUtils.toString(medicalPreSettleReqTO), JSONUtils.toString(hisResult));
            if(hisResult != null){
                result.setMsg(hisResult.getMsg());
            }
            result.setCode(DrugEnterpriseResult.FAIL);
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
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_ALDYF;
    }

    /*
     * @description 推送药企处方状态，由于只是个别药企需要实现，故有默认实现
     * @author gmw
     * @date 2019/9/18
     * @param rxId  recipeCode
     * @param status  status
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    @Override
    public DrugEnterpriseResult updatePrescriptionStatus(String rxId, int status) {
        LOGGER.info("更新处方状态");
        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);

        return drugEnterpriseResult;
    }

    /*
     * @description 淘宝处方状态变更
     * @author gmw
     * @date 2019/9/19
     * @param requestParam
     * @return java.lang.String
     */
    @RpcService
    public String changeState(String requestParam) {

        LOGGER.info("收到天猫更新处方请求，开始--{}" + requestParam);
        //获取入参
        AlibabaAlihealthPrescriptionStatusSyncRequest aRequest = JSON.parseObject(
            requestParam, AlibabaAlihealthPrescriptionStatusSyncRequest.class);
        //出参对象
        AlibabaAlihealthPrescriptionStatusSyncResponse response = new AlibabaAlihealthPrescriptionStatusSyncResponse();
        AlibabaAlihealthPrescriptionStatusSyncResponse.ResultDo resultDo = new AlibabaAlihealthPrescriptionStatusSyncResponse.ResultDo();

        //业务逻辑
        StandardStateDTO state = new StandardStateDTO ();

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        if(null == aRequest.getRxNo()){
            resultDo.setSuccess(false);
            resultDo.setErrorMessage("rnNO can not be null");
            resultDo.setErrorCode("500");
            response.setResult(resultDo);
            LOGGER.warn("参数异常--{}",JSON.toJSONString(response));
            return JSON.toJSONString(response);
        }
        List<Integer> recipeIds = recipeExtendDAO.findRecipeIdsByRxNo(aRequest.getRxNo());
        Recipe recipe = null;
        if(null != recipeIds && recipeIds.size() != 0){
            recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
            state.setRecipeCode(recipe.getRecipeCode());
            //天猫的机构编码和纳里不一致,直接用纳里的机构编码
            state.setClinicOrgan(null == recipe.getClinicOrgan() ? null : recipe.getClinicOrgan() +"");
        } else {
            resultDo.setSuccess(true);
            resultDo.setErrorMessage("invalid rnNO: "+aRequest.getRxNo()+"，can not get recipeInfo");
            resultDo.setErrorCode("500");
            response.setResult(resultDo);
            LOGGER.warn("参数异常--{}",JSON.toJSONString(response));
            return JSON.toJSONString(response);
        }

        if(null != aRequest.getStatus()){
            state.setStatus(RecipeStatusEnum.getKey(aRequest.getStatus()));
        } else {
            resultDo.setSuccess(false);
            resultDo.setErrorMessage("status can not be null");
            resultDo.setErrorCode("500");
            response.setResult(resultDo);
            LOGGER.warn("参数异常--{}",JSON.toJSONString(response));
            return JSON.toJSONString(response);
        }

        //没有业务含义，仅满足校验用
        state.setOrganId(aRequest.getHospitalId());
        state.setAccount("tmdyf");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        state.setDate(simpleDateFormat.format(new Date()));


        StandardEnterpriseCallService distributionService = getBean("distributionService", StandardEnterpriseCallService.class);
        StandardResultDTO resulta = distributionService.changeState(Collections.singletonList(state));

        if(StandardResultDTO.SUCCESS == resulta.getCode()){
            resultDo.setSuccess(true);
        } else {
            resultDo.setSuccess(true);
            resultDo.setErrorMessage(resulta.getMsg());
            resultDo.setErrorCode("500");
        }

        LOGGER.info("天猫更新处方请求执行结束");
        response.setResult(resultDo);
        return JSON.toJSONString(response);
    }

    /**
     * 更新处方医保备案号
     * @param recipeCode 处方号
     * @param medicalInsuranceRecord 医保备案号
     * @return
     */
    @RpcService
    public DrugEnterpriseResult updateMedicalInsuranceRecord(String recipeCode, String medicalInsuranceRecord) {
        LOGGER.info("更新处方医保备案号 start");
        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);

        return drugEnterpriseResult;
    }

//    /**
//     *
//     * @param rxId  处⽅Id
//     * @param queryOrder  是否查询订单
//     * @return 处方单
//     */
//    @Override
//    public DrugEnterpriseResult queryPrescription(String rxId, Boolean queryOrder) {
//        PatientService patientService = BasicAPI.getService(PatientService.class);
//        OrganService organService = BasicAPI.getService(OrganService.class);
//        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe dbRecipe = recipeDAO.getByRecipeCode(rxId);
//        if (ObjectUtils.isEmpty(dbRecipe)) {
//            return getDrugEnterpriseResult(drugEnterpriseResult, "处方不存在");
//        }
//        String outHospitalId = organService.getOrganizeCodeByOrganId(dbRecipe.getClinicOrgan());
//        if (StringUtils.isEmpty(outHospitalId)) {
//            return getDrugEnterpriseResult(drugEnterpriseResult, "医院的外部编码不能为空");
//        }
//        String loginId = patientService.getLoginIdByMpiId(dbRecipe.getRequestMpiId());
//        String accessToken = aldyfRedisService.getTaobaoAccessToken(loginId);
//        if (ObjectUtils.isEmpty(accessToken)) {
//            return getDrugEnterpriseResult(drugEnterpriseResult, EXPIRE_TIP);
//        }
//        LOGGER.info("获取到accessToken:{}, loginId:{},{},{}", accessToken, loginId, rxId, outHospitalId);
//        alihealthHospitalService.setTopSessionKey(accessToken);
//        AlibabaAlihealthRxPrescriptionGetRequest prescriptionGetRequest = new AlibabaAlihealthRxPrescriptionGetRequest();
//        prescriptionGetRequest.setRxId(rxId);
//        prescriptionGetRequest.setOutHospitalId(outHospitalId);
//        BaseResult<AlibabaAlihealthRxPrescriptionGetResponse> responseBaseResult = alihealthHospitalService.queryPrescription(prescriptionGetRequest);
//        LOGGER.info("查询处方，{}", getJsonLog(responseBaseResult));
//        getAldyfResult(drugEnterpriseResult, responseBaseResult);
//        return drugEnterpriseResult;
//    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        LOGGER.info("HzInternetRemoteService-getDrugEnterpriseResult提示信息：{}.", msg);
        return result;
    }

    private Integer getClinicOrganByOrganId(String organId, String clinicOrgan) throws Exception {
        Integer co = null;
        if (StringUtils.isEmpty(clinicOrgan)) {
            IOrganService organService = BaseAPI.getService(IOrganService.class);

            List<OrganBean> organList = organService.findByOrganizeCode(organId);
            if (CollectionUtils.isNotEmpty(organList)) {
                co = organList.get(0).getOrganId();
            }
        } else {
            co = Integer.parseInt(clinicOrgan);
        }
        return co;
    }

    private static String getJsonLog(Object object) {
        return JSONUtils.toString(object);
    }
}
