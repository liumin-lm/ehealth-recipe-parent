package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
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
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.StandardResultDTO;
import recipe.drugsenterprise.bean.StandardStateDTO;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeLogService;
import recipe.service.common.RecipeCacheService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;
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
//        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
//        //1物流配送
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
//        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
//        OrganService organService = BasicAPI.getService(OrganService.class);
//
//
//        UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
//        updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
//        //平台处方号
//        updateTakeDrugWayReqTO.setNgarRecipeId(recipe.getRecipeId()+"");
//        //医院处方号
//        //流转到这里来的属于物流配送
//        updateTakeDrugWayReqTO.setDeliveryType("1");
//        updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
//        updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
//
//        updateTakeDrugWayReqTO.setPayMode("1");
//        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
//        if(recipeExtend != null && recipeExtend.getDeliveryCode() != null){
//            updateTakeDrugWayReqTO.setDeliveryCode(recipeExtend.getDeliveryCode());
//            updateTakeDrugWayReqTO.setDeliveryName(recipeExtend.getDeliveryName());
//        } else {
//            LOGGER.info("杭州互联网虚拟药企-未获取his返回的配送药-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
//            result.setMsg("未获取his返回的配送药");
//            result.setCode(DrugEnterpriseResult.FAIL);
//        }
//        if (StringUtils.isNotEmpty(recipe.getOrderCode())){
//            RecipeOrderDAO dao = DAOFactory.getDAO(RecipeOrderDAO.class);
//            RecipeOrder order = dao.getByOrderCode(recipe.getOrderCode());
//            if (order!=null){
//                //收货人
//                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
//                //联系电话
//                updateTakeDrugWayReqTO.setContactTel(order.getRecTel());
//                //详细收货地址
//                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
//                updateTakeDrugWayReqTO.setAddress(commonRemoteService.getCompleteAddress(order));
//
//                //收货地址代码
//                updateTakeDrugWayReqTO.setReceiveAddrCode(order.getAddress3());
//                String address3 = null;
//                try {
//                    address3 = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getAddress3());
//                } catch (ControllerException e) {
//                    LOGGER.warn("杭州互联网虚拟药企-未获取收货地址名称-add={}", JSONUtils.toString(order.getAddress3()));
//
//                }
//                //收货地址名称
//                updateTakeDrugWayReqTO.setReceiveAddress(address3);
//                //期望配送日期
//                updateTakeDrugWayReqTO.setConsignee(order.getExpectSendDate());
//                //期望配送时间
//                updateTakeDrugWayReqTO.setContactTel(order.getExpectSendTime());
//            }else{
//                LOGGER.info("杭州互联网虚拟药企-未获取有效订单-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
//                result.setMsg("未获取有效订单");
//                result.setCode(DrugEnterpriseResult.FAIL);
//            }
//        }
//
//        HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
//        if("200".equals(hisResult.getMsgCode())){
//            LOGGER.info("杭州互联网虚拟药企-更新取药信息成功-his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));
//            result.setCode(DrugEnterpriseResult.SUCCESS);
//        }else{
//            LOGGER.error("杭州互联网虚拟药企-更新取药信息失败-his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));
//
//            result.setMsg(hisResult.getMsg());
//            result.setCode(DrugEnterpriseResult.FAIL);
//        }
        //虚拟药企推送，修改配送信息的逻辑调整到前面确认订单
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    /*
     * @description 处方预结算
     * @author gaomw
     * @date 2019/12/13
     * @param [recipeId]
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    public DrugEnterpriseResult recipeMedicalPreSettleO(Integer recipeId) {
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
        //HisResponseTO hisResult = service.recipeMedicalPreSettle(medicalPreSettleReqTO);
        HisResponseTO hisResult = null;
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


    /*
     * @description 处方预结算(新)
     * @author gaomw
     * @date 2019/12/13
     * @param [recipeId]
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    public DrugEnterpriseResult recipeMedicalPreSettle(Integer recipeId, Integer depId) {

        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if(recipeId == null || depId == null){
            LOGGER.info("recipeMedicalPreSettle-未获取处方或药企ID,处方ID={},药企ID：{}",recipeId,depId);
        } else {
            LOGGER.info("recipeMedicalPreSettle-杭州互联网医保预结算开始,处方号={},药企ID：{}",recipeId,depId);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);

            DrugsEnterpriseDAO drugEnterpriseDao = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugEnterprise = drugEnterpriseDao.get(depId);
            if(drugEnterprise != null && "hzInternet".equals(drugEnterprise.getAccount())){
                HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
                //杭州市互联网医院监管中心 管理单元eh3301
                OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
                OrganDTO organDTO = organService.getByManageUnit("eh3301");
                String bxh = null;
                if (organDTO!=null) {
                    bxh = healthCardService.getMedicareCardId(recipe.getMpiid(), organDTO.getOrganId());

                }
                //有市名卡才走预结算
                if (StringUtils.isNotEmpty(bxh)){
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    PatientDTO patientBean = patientService.get(recipe.getMpiid());

                    MedicalPreSettleReqNTO request = new MedicalPreSettleReqNTO();
                    request.setClinicOrgan(recipe.getClinicOrgan());
                    request.setPatientName(patientBean.getPatientName());
                    request.setIdcard(patientBean.getIdcard());
                    request.setBirthday(patientBean.getBirthday());
                    request.setAddress(patientBean.getAddress());
                    request.setMobile(patientBean.getMobile());
                    request.setGuardianName(patientBean.getGuardianName());
                    request.setGuardianTel(patientBean.getLinkTel());
                    request.setGuardianCertificate(patientBean.getGuardianCertificate());
                    request.setRecipeId(recipeId + "");

                    request.setDoctorId(recipe.getDoctor() + "");
                    request.setDoctorName(recipe.getDoctorName());
                    request.setDepartId(recipe.getDepart() + "");

                    request.setBxh(bxh);

                    try {
                        request.setSex(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patientBean.getPatientSex()));
                        request.setDepartName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart()));
                    } catch (ControllerException e) {
                        LOGGER.error("DictionaryController 字典转化异常,{}",e);
                    }
                    RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                    LOGGER.info("recipeMedicalPreSettle. recipeId={},req={}", recipeId, JSONUtils.toString(request));
                    HisResponseTO<RecipeMedicalPreSettleInfo> hisResult = service.recipeMedicalPreSettleN(request);
                    if(hisResult != null && "200".equals(hisResult.getMsgCode())){
                        LOGGER.info("recipeMedicalPreSettle-success. recipeId={},result={}", recipeId, JSONUtils.toString(hisResult));
                        if(hisResult.getData() != null){
                            RecipeExtend ext = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                            if(ext != null){
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("registerNo", hisResult.getData().getGhxh());
                                map.put("hisSettlementNo", hisResult.getData().getSjh());
                                map.put("preSettleTotalAmount", hisResult.getData().getZje());
                                map.put("fundAmount", hisResult.getData().getYbzf());
                                map.put("cashAmount", hisResult.getData().getYfje());
                                recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), map);
                            } else {
                                ext = new RecipeExtend();
                                ext.setRecipeId(recipe.getRecipeId());
                                ext.setRegisterNo(hisResult.getData().getGhxh());
                                ext.setHisSettlementNo(hisResult.getData().getSjh());
                                ext.setPreSettletotalAmount(hisResult.getData().getZje());
                                ext.setFundAmount(hisResult.getData().getYbzf());
                                ext.setCashAmount(hisResult.getData().getYfje());
                                recipeExtendDAO.save(ext);
                            }
                        }
                        result.setCode(DrugEnterpriseResult.SUCCESS);
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "杭州市医保预结算成功");
                    }else{
                        LOGGER.error("recipeMedicalPreSettle fail. recipeId={},result={}", recipeId, JSONUtils.toString(hisResult));
                        String msg;
                        if(hisResult != null){
                            msg = hisResult.getMsg();
                        }else {
                            msg = "前置机返回结果null";
                        }
                        result.setCode(DrugEnterpriseResult.FAIL);
                        result.setMsg(msg);
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "杭州市医保预结算失败，原因："+msg);
                    }
                } else{
                    LOGGER.error("recipeMedicalPreSettle-患者医保卡号为null,recipeId{}，患者：{}", recipe.getRecipeId(), recipe.getPatientName());
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "由于获取不到杭州市医保卡,无法进行预结算");
                }
            }  else{
                LOGGER.info("recipeMedicalPreSettle-非杭州互联网药企不走杭州医保预结算,recipeId={} 药企ID：{}",recipeId,depId);
            }
        }

        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        //date 20200311
        //查询库存通过his预校验的返回判断库存是否足够
        LOGGER.info("scanStock 虚拟药企库存入参为：{}，{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if(!valiScanStock(recipeId, drugsEnterprise, result)){
            return result;
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        if(null != extend){
            //获取当前his返回的药企信息，以及价格信息
            String deliveryRecipeFees = extend.getDeliveryRecipeFee();
            String deliveryCodes = extend.getDeliveryCode();
            String deliveryNames = extend.getDeliveryName();
            if(StringUtils.isNotEmpty(deliveryRecipeFees) &&
                    StringUtils.isNotEmpty(deliveryCodes) && StringUtils.isNotEmpty(deliveryNames)){
                //只有杭州是互联网医院返回的是库存足够
                result.setCode(DrugEnterpriseResult.SUCCESS);
                result.setMsg("调用[" + drugsEnterprise.getName() + "][ scanStock ]结果返回成功,有库存,处方单ID:"+recipeId+".");
                return result;
            }
        }
        return result;
    }

    private boolean valiScanStock(Integer recipeId, DrugsEnterprise drugsEnterprise, DrugEnterpriseResult result) {
        if(null == recipeId){
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("传入的处方id为空！");
            return false;
        }
        return true;
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
        LOGGER.info("findSupportDep 虚拟药企导出入参为：{}，{}，{}", JSONUtils.toString(recipeIds), JSONUtils.toString(ext), JSONUtils.toString(enterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        //校验入参
        if(!valiRequestDate(recipeIds, ext, result)){
            return result;
        }
        //date 20200311
        //修改逻辑：将his返回的药企列表信息回传成
        Integer recipeId = recipeIds.get(0);

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        if(null == recipe){
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("当前处方" + recipeIds.get(0) + "不存在！");
            return result;
        }
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
        DrugsEnterprise drugsEnterprise;
        if(CollectionUtils.isNotEmpty(drugsEnterprises)){
            //这里杭州市互联网医院只配置一个虚拟药企
            drugsEnterprise = drugsEnterprises.get(0);
        }else{
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("当前医院"+ recipe.getClinicOrgan() +"没有设置关联药企！");
            return result;
        }

        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        List<DepDetailBean> depDetailList = new ArrayList<>();
        if(null != extend){
            //获取当前his返回的药企信息，以及价格信息
            String deliveryRecipeFees = extend.getDeliveryRecipeFee();
            String deliveryCodes = extend.getDeliveryCode();
            String deliveryNames = extend.getDeliveryName();
            DepDetailBean depDetailBean;
            if(StringUtils.isNotEmpty(deliveryRecipeFees) &&
                    StringUtils.isNotEmpty(deliveryCodes) && StringUtils.isNotEmpty(deliveryNames)){
                LOGGER.info("findSupportDepList 当前处方{}的药企信息为his预校验返回信息：{}", recipeId, JSONUtils.toString(extend));
                String[] deliveryRecipeFeeList = deliveryRecipeFees.split("\\|");
                String[] deliveryCodeList = deliveryCodes.split("\\|");
                String[] deliveryNameList = deliveryNames.split("\\|");

                for(int i = 1; i < deliveryRecipeFeeList.length ; i++){
                    depDetailBean = new DepDetailBean();
                    //标识选择的药企是his推过来的
                    depDetailBean.setDepId(drugsEnterprise.getId());
                    depDetailBean.setDepName(deliveryNameList[i]);
                    depDetailBean.setRecipeFee(new BigDecimal(deliveryRecipeFeeList[i]));
                    depDetailBean.setBelongDepName(deliveryNameList[i]);
                    depDetailBean.setOrderType(1);
                    depDetailBean.setPayModeText("在线支付");
                    depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
                    //预留字段标识是医院推送给过来的
                    depDetailBean.setHisDep(true);
                    depDetailBean.setHisDepCode(deliveryCodeList[i]);
                    //date 20200311
                    //医院返回的药企处方金额
                    depDetailBean.setHisDepFee(new BigDecimal(deliveryRecipeFeeList[i]));

                    depDetailList.add(depDetailBean);
                }



            }

        }
        LOGGER.info("findSupportDepList 虚拟药企处方{}查询his药企列表展示信息：{}", recipeId, JSONUtils.toString(depDetailList));
        result.setObject(depDetailList);
        return result;
    }

    private Boolean valiRequestDate(List<Integer> recipeIds, Map ext, DrugEnterpriseResult result) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("传入的处方id为空！");
            return false;
        }

        return true;
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

    @Override
    public boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {

        DrugEnterpriseResult result = scanStock(dbRecipe.getRecipeId(), dep);
        LOGGER.info("scanStock recipeId:{}, result:{}", dbRecipe.getRecipeId(), JSONUtils.toString(result));
        boolean equals = result.getCode().equals(DrugEnterpriseResult.SUCCESS);
        LOGGER.info("scanStock 请求虚拟药企返回：{}", equals);
        return equals;
    }

    @Override
    public String appEnterprise(RecipeOrder order) {
        String hisEnterpriseName = null;
        if (null != order) {

            hisEnterpriseName = order.getHisEnterpriseName();
        }
        LOGGER.info("appEnterprise 请求虚拟药企返回：{}", hisEnterpriseName);
        return hisEnterpriseName;
    }

    @Override
    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo) {
        BigDecimal depFee = recipeFee;
        String hisDepFee = extInfo.get("hisDepFee");
        if(StringUtils.isNotEmpty(hisDepFee)){
            depFee = new BigDecimal(hisDepFee);
        }
        LOGGER.info("orderToRecipeFee 请求虚拟药企返回：{}", depFee);
        return depFee;
    }

    @Override
    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order) {
        if(null != extInfo){
            //date 20200312
            //设置患者选中his回传信息
            order.setHisEnterpriseName(extInfo.get("depName"));
            order.setHisEnterpriseCode(extInfo.get("hisDepCode"));
        }
        LOGGER.info("当前虚拟药企组装的订单：{}", JSONUtils.toString(order));
    }

    @Override
    public void checkRecipeGiveDeliveryMsg(RecipeBean recipeBean, Map<String, Object> map) {
        String giveMode = null != map.get("giveMode") ? map.get("giveMode").toString() : null;
        Object deliveryList = map.get("deliveryList");
        if(null != deliveryList && null != giveMode){

            List<Map> deliveryLists = (List<Map>)deliveryList;
            //暂时按照逻辑只保存展示返回的第一个药企
            DeliveryList nowDeliveryList = JSON.parseObject(JSON.toJSONString(deliveryLists.get(0)), DeliveryList.class);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            if (null != nowDeliveryList){
                Map<String,String> updateMap = Maps.newHashMap();
                updateMap.put("deliveryCode", nowDeliveryList.getDeliveryCode());
                updateMap.put("deliveryName", nowDeliveryList.getDeliveryName());
                //存放处方金额
                updateMap.put("deliveryRecipeFee", null != nowDeliveryList.getRecipeFee() ? nowDeliveryList.getRecipeFee().toString() : null);
                recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeBean.getRecipeId(), updateMap);
            }
            //date 20200311
            //将his返回的批量药企信息存储下来，将信息分成|分割
            DeliveryList deliveryListNow;
            Map<String,String> updateMap = Maps.newHashMap();
            StringBuffer deliveryCodes = new StringBuffer().append("|");
            StringBuffer deliveryNames = new StringBuffer().append("|");
            StringBuffer deliveryRecipeFees = new StringBuffer().append("|");
            for(Map<String,String> delivery : deliveryLists){
                deliveryListNow = JSON.parseObject(JSON.toJSONString(delivery), DeliveryList.class);
                deliveryCodes.append(deliveryListNow.getDeliveryCode()).append("|");
                deliveryNames.append(deliveryListNow.getDeliveryName()).append("|");
                deliveryRecipeFees.append(deliveryListNow.getRecipeFee()).append("|");
            }
            updateMap.put("deliveryCode", "|".equals(deliveryCodes) ? null : deliveryCodes.toString());
            updateMap.put("deliveryName", "|".equals(deliveryNames) ? null : deliveryNames.toString());
            //存放处方金额
            updateMap.put("deliveryRecipeFee", "|".equals(deliveryRecipeFees) ? null : deliveryRecipeFees.toString());
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeBean.getRecipeId(), updateMap);
            LOGGER.info("hisRecipeCheck 当前处方{}预校验，配送方式存储成功:{}！", recipeBean.getRecipeId(), JSONUtils.toString(updateMap));

        }else{
            LOGGER.info("hisRecipeCheck 当前处方{}预校验，配送方式没有返回药企信息！", recipeBean.getRecipeId());
        }
    }

    @Override
    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo) {
        order.setEnterpriseId(depId);
        if(null != extInfo){
            //date 20200312
            //设置患者选中his回传信息
            order.setHisEnterpriseName(extInfo.get("depName"));
            order.setHisEnterpriseCode(extInfo.get("hisDepCode"));
        }
        LOGGER.info("当前虚拟药企组装的订单：{}", JSONUtils.toString(order));
    }
}
