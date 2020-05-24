package recipe.serviceprovider.recipe.service;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.QueryRecipeRequestTO;
import com.ngari.his.recipe.mode.QueryRecipeResponseTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.ca.mode.CaSignResultTo;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.SyncEinvoiceNumberDTO;
import com.ngari.recipe.recipe.constant.RecipePayTextEnum;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipe.service.IRecipeService;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.RecipeUtil;
import recipe.ca.vo.CaSignResultVo;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.*;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.TmdyfRemoteService;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.medicationguide.service.WinningMedicationGuideService;
import recipe.recipecheck.RecipeCheckService;
import recipe.service.*;
import recipe.serviceprovider.BaseService;
import recipe.sign.SignRecipeInfoService;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/7/31.
 */
@RpcBean("remoteRecipeService")
public class RemoteRecipeService extends BaseService<RecipeBean> implements IRecipeService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRecipeService.class);

    @RpcService
    @Override
    public void sendSuccess(RecipeBussReqTO request) {
        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
        if (null != request.getData()) {
            HisSendResTO response = (HisSendResTO) request.getData();
            service.sendSuccess(response);
        }
    }

    @RpcService
    @Override
    public void sendFail(RecipeBussReqTO request) {
        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
        if (null != request.getData()) {
            HisSendResTO response = (HisSendResTO) request.getData();
            service.sendFail(response);
        }
    }

    @RpcService
    @Override
    public RecipeBean get(Object id) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(id);
        return getBean(recipe, RecipeBean.class);
    }

    @RpcService
    @Override
    public boolean haveRecipeAuthority(int doctorId) {
        RecipeService service = ApplicationUtils.getRecipeService(RecipeService.class);
        Map<String, Object> map = service.openRecipeOrNot(doctorId);
        boolean rs = false;
        try {
            rs = (boolean) map.get("result");
        } catch (Exception e) {
            rs = false;
        }
        return rs;
    }

    @RpcService
    @Override
    public void afterMedicalInsurancePay(int recipeId, boolean success) {
        RecipeMsgService.doAfterMedicalInsurancePaySuccess(recipeId, success);
    }


    @RpcService
    @Override
    public RecipeListResTO<Integer> findDoctorIdSortByCount(RecipeListReqTO request) {
        LOGGER.info("findDoctorIdSortByCount request={}", JSONUtils.toString(request));
        RecipeListService service = ApplicationUtils.getRecipeService(RecipeListService.class);
        List<Integer> organIds = MapValueUtil.getList(request.getConditions(), "organIds");
        List<Integer> doctorIds = service.findDoctorIdSortByCount(request.getStart(), request.getLimit(), organIds);
        return RecipeListResTO.getSuccessResponse(doctorIds);
    }

    @RpcService
    @Override
    public boolean changeRecipeStatus(int recipeId, int afterStatus) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.updateRecipeInfoByRecipeId(recipeId, afterStatus, null);
    }

    @RpcService
    @Override
    public RecipeBean getByRecipeId(int recipeId) {
        RecipeBean recipeBean =  get(recipeId);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if(recipeBean != null && recipeExtend != null){
            recipeBean.setMainDieaseDescribe(recipeExtend.getMainDieaseDescribe());
        }
        return recipeBean;
    }

    @RpcService
    @Override
    public long getUncheckedRecipeNum(int doctorId) {
        RecipeCheckService service = ApplicationUtils.getRecipeService(RecipeCheckService.class);
        return service.getUncheckedRecipeNum(doctorId);
    }

    @RpcService
    @Override
    public RecipeBean getByRecipeCodeAndClinicOrgan(String recipeCode, int clinicOrgan) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
        return getBean(recipe, RecipeBean.class);
    }

    @RpcService
    @Override
    public void changePatientMpiId(String newMpiId, String oldMpiId) {
        RecipeService service = ApplicationUtils.getRecipeService(RecipeService.class);
        service.updatePatientInfoForRecipe(newMpiId, oldMpiId);
    }

    @Override
    public RecipeListResTO<RecipeRollingInfoBean> findLastesRecipeList(RecipeListReqTO request) {
        LOGGER.info("findLastesRecipeList request={}", JSONUtils.toString(request));
        RecipeListService service = ApplicationUtils.getRecipeService(RecipeListService.class);
        List<Integer> organIds = MapValueUtil.getList(request.getConditions(), "organIds");
        List<RecipeRollingInfoBean> recipeList = service.findLastesRecipeList(organIds, request.getStart(), request.getLimit());
        if (CollectionUtils.isEmpty(recipeList)) {
            recipeList = new ArrayList<>(0);
        }
        return RecipeListResTO.getSuccessResponse(recipeList);
    }

    @RpcService
    @Override
    public QueryResult<Map> findRecipesByInfo(Integer organId, Integer status,
                                              Integer doctor, String patientName, Date bDate, Date eDate, Integer dateType,
                                              Integer depart, int start, int limit, List<Integer> organIds, Integer giveMode,Integer fromflag,Integer recipeId,
            Integer enterpriseId,Integer checkStatus,Integer payFlag,Integer orderType) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findRecipesByInfo(organId, status, doctor, patientName, bDate, eDate, dateType, depart, start, limit, organIds, giveMode,fromflag,recipeId,enterpriseId,checkStatus,payFlag,orderType);
    }

    @RpcService
    @Override
    public Map<String, Integer> getStatisticsByStatus(Integer organId,
                                                      Integer status, Integer doctor, String mpiid,
                                                      Date bDate, Date eDate, Integer dateType,
                                                      Integer depart, int start, int limit, List<Integer> organIds, Integer giveMode,Integer fromflag,Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getStatisticsByStatus(organId, status, doctor, mpiid, bDate, eDate, dateType, depart, start, limit, organIds, giveMode,fromflag,recipeId);
    }

    @RpcService
    @Override
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId) {
        RecipeCheckService service = ApplicationUtils.getRecipeService(RecipeCheckService.class);
        return service.findRecipeAndDetailsAndCheckById(recipeId,null);
    }

    @RpcService
    @Override
    public List<Map> queryRecipesByMobile(List<String> mpis){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.queryRecipesByMobile(mpis);
    }

    @RpcService
    @Override
    public List<Integer> findDoctorIdsByRecipeStatus(Integer status) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findDoctorIdsByStatus(status);
    }

    @RpcService
    @Override
    public List<String> findPatientMpiIdForOp(List<String> mpiIds, List<Integer> organIds){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findPatientMpiIdForOp(mpiIds, organIds);
    }

    @RpcService
    @Override
    public List<String> findCommonDiseasByDoctorAndOrganId(int doctorId, int organId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findCommonDiseasByDoctorAndOrganId(doctorId, organId);
    }

    @RpcService
    @Override
    public List<String> findHistoryMpiIdsByDoctorId(int doctorId, Integer start, Integer limit) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findHistoryMpiIdsByDoctorId(doctorId, start,limit);
    }

    @RpcService
    @Override
    public void synPatientStatusToRecipe(String mpiId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        recipeDAO.updatePatientStatusByMpiId(mpiId);
    }

    @RpcService
    @Override
    public void saveRecipeDataFromPayment(RecipeBean recipeBean, List<RecipeDetailBean> recipeDetailBeans) {

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipedetail> recipedetails = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(recipeDetailBeans)) {
            for (RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                recipedetails.add(getBean(recipeDetailBean,Recipedetail.class));
            }
        }
        if (StringUtils.isEmpty(recipeBean.getRecipeMode())){
            recipeBean.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
        }
        if (recipeBean.getReviewType()==null){
            recipeBean.setReviewType(ReviewTypeConstant.Postposition_Check);
        }
        recipeDAO.updateOrSaveRecipeAndDetail(getBean(recipeBean,Recipe.class),recipedetails,false);
    }


    /**
     * 根据日期范围，机构归类的业务量(天，月)
     * @param startDate
     * @param endDate
     * @return
     */
    @RpcService
    @Override
    public HashMap<Integer, Long> getCountByDateAreaGroupByOrgan(final String startDate, final String endDate) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountByDateAreaGroupByOrgan(startDate, endDate);
    }
    /**
     * 根据日期范围，机构归类的业务量(小时)
     * @param startDate
     * @param endDate
     * @return
     */
    @RpcService
    @Override
    public HashMap<Object,Integer> getCountByHourAreaGroupByOrgan(final Date startDate, final Date endDate) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountByHourAreaGroupByOrgan(startDate, endDate);
    }

    /**
     *
     * @param organId
     * @param status
     * @param doctor
     * @param patientName
     * @param bDate
     * @param eDate
     * @param dateType
     * @param depart
     * @param organIds
     * @param giveMode
     * @param fromflag
     * @return
     */
    @RpcService
    public List<Map> findRecipesByInfoForExcel(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate,
                                        final Date eDate, final Integer dateType, final Integer depart, List<Integer> organIds, Integer giveMode,
                                        Integer fromflag,Integer recipeId,Integer enterpriseId,Integer checkStatus,Integer payFlag,Integer orderType){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findRecipesByInfoForExcel(organId,status,doctor,patientName,bDate,eDate,dateType,depart,organIds,giveMode,fromflag,recipeId,enterpriseId,checkStatus,payFlag,orderType);
    }

    /**
     * 春节2月17版本 JRK
     * 查询
     * @param organId
     * @param status
     * @param doctor
     * @param patientName
     * @param bDate
     * @param eDate
     * @param dateType
     * @param depart
     * @param giveMode
     * @param fromflag
     * @return
     */
    @RpcService(timeout = 600000)
    @Override
    public List<Map> findRecipeOrdersByInfoForExcel(Integer organId, List<Integer> organIds, Integer status, Integer doctor, String patientName, Date bDate,
                                               Date eDate, Integer dateType, Integer depart, Integer giveMode,
                                               Integer fromflag,Integer recipeId){
        LOGGER.info("findRecipeOrdersByInfoForExcel查询处方订单导出信息入参:{},{},{},{},{},{},{},{},{},{},{},{}",organId, organIds, status, doctor, patientName, bDate, eDate, dateType, depart, giveMode, fromflag, recipeId);
        IRecipeService recipeService = RecipeAPI.getService(IRecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Map> recipeMap = recipeDAO.findRecipesByInfoForExcelN(organId, status, doctor, patientName, bDate, eDate, dateType, depart, organIds, giveMode, fromflag, recipeId);

        //组装数据准备
        Object nowRecipeId;
        RecipeOrder order;
        List<Map> newRecipeMap = new ArrayList<>();
        Map<String, Object> recipeMsgMap;
        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);

        //组装处方相关联的数据

        for(Map<String, Object> recipeMsg: recipeMap){
            nowRecipeId = recipeMsg.get("recipeId");
            if(null != nowRecipeId){
                try {
                    //订单数据
                    order = (RecipeOrder)recipeMsg.get("recipeOrder");

                    recipeMsgMap = new HashMap();
                    recipeMsgMap.putAll(recipeMsg);
                    recipeAndOrderMsg(order, commonRemoteService, recipeMsgMap);
                    recipeMsgMap.put("recipeOrder",null);
                    newRecipeMap.add(recipeMsgMap);

                } catch (Exception e) {
                    LOGGER.error("查询关联信息异常{}，对应的处方id{}", e, nowRecipeId);
                    e.printStackTrace();
                    throw new DAOException("查询处方信息异常！");
                }
            }
        }
        LOGGER.info("findRecipeOrdersByInfoForExcel查询处方订单导出信息结果:{}", newRecipeMap);
        return newRecipeMap;
    }

    private void recipeAndOrderMsg(RecipeOrder order, CommonRemoteService commonRemoteService, Map<String, Object> recipeMsg) throws ControllerException {
        //地址
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

        recipeMsg.put("completeAddress", commonRemoteService.getCompleteAddress(order));

        if(null != order){
            //收货人
            recipeMsg.put("receiver", order.getReceiver());
            //收货人联系方式
            recipeMsg.put("recMobile", order.getRecMobile());
            //下单时间
            recipeMsg.put("orderTime", order.getCreateTime());
            //配送费
            recipeMsg.put("expressFee", order.getExpressFee());
            //订单号
            recipeMsg.put("orderCode", order.getOrderCode());
            //订单状态
            if(null != order.getStatus()) {
                recipeMsg.put("orderStatus", DictionaryController.instance().get("eh.cdr.dictionary.RecipeOrderStatus").getText(order.getStatus()));
            }
            //支付金额
            recipeMsg.put("payMoney", order.getActualPrice());
            recipeMsg.put("totalMoney", order.getTotalFee());
            //date 20200303
            //添加药企信息和期望配送时间
            if(null != order.getEnterpriseId()){
                //匹配上药企，获取药企名
                //DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if(null != enterprise && null != enterprise.getName()){
                    recipeMsg.put("enterpriseName", enterprise.getName());
                }else{
                    LOGGER.warn("findRecipeOrdersByInfoForExcel 当前处方{}关联的药企id:{}信息不全", order.getRecipeIdList(), order.getEnterpriseId());
                }

            }
            //date 20200303
            //添加期望配送时间
            if(StringUtils.isNotEmpty(order.getExpectSendDate()) && StringUtils.isNotEmpty(order.getExpectSendTime())){
                recipeMsg.put("expectSendDate", order.getExpectSendDate() + " " + order.getExpectSendTime());
            }
            //date 20200305
            //添加支付状态
            if(null != order.getPayFlag()){
                recipeMsg.put("payStatusText", RecipePayTextEnum.getByPayFlag(order.getPayFlag()).getPayText());
            }else{
                LOGGER.info("findRecipeOrdersByInfoForExcel 当前处方{}的订单支付状态{}", order.getRecipeIdList(), order.getPayFlag());
                recipeMsg.put("payStatusText", RecipePayTextEnum.Default.getPayText());
            }
            recipeMsg.put("payTime", order.getPayTime());
            recipeMsg.put("tradeNo", order.getTradeNo());

        }else{
            //没有订单说明没有支付
            recipeMsg.put("payStatusText", RecipePayTextEnum.Default.getPayText());
        }
    }

    @RpcService
    @Override
    public HashMap<Integer, Long> getCountGroupByOrgan(){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountGroupByOrgan();
    }


    @RpcService
    @Override
    public HashMap<Integer, Long> getRecipeRequestCountGroupByDoctor(){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getRecipeRequestCountGroupByDoctor();
    }

    @Override
    public List<RecipeBean> findAllReadyAuditRecipe() {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findAllReadyAuditRecipe();
        return ObjectCopyUtils.convert(recipes, RecipeBean.class);
    }

    @Override
    public List<RecipeDetailBean> findRecipeDetailsByRecipeId(Integer recipeId) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        return ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);
    }

    @Override
    public RecipeDetailBean getRecipeDetailByDetailId(Integer detailId) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        Recipedetail recipedetail = recipeDetailDAO.getByRecipeDetailId(detailId);
        return ObjectCopyUtils.convert(recipedetail, RecipeDetailBean.class);
    }

    @Override
    public RecipeExtendBean findRecipeExtendByRecipeId(Integer recipeId) {
        RecipeExtendDAO RecipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = RecipeExtendDAO.getByRecipeId(recipeId);
        return ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class);
    }

    @Override
    public List<Integer> findReadyAuditRecipeIdsByOrganIds(List<Integer> organIds) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findReadyAuditRecipeIdsByOrganIds(organIds);
    }

    @Override
    public List<String> findSignFileIdByPatientId(String patientId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findSignFileIdByPatientId(patientId);
    }

    @Override
    public List<Integer> findDoctorIdByHistoryRecipe() {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findDoctorIdByHistoryRecipe();
    }

    @Override
    public RecipeBean getRecipeByOrderCode(String orderCode) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findRecipeListByOrderCode(orderCode);
        if (recipes != null && recipes.size() > 0 ){
            Recipe recipe = recipes.get(0);
            return ObjectCopyUtils.convert(recipe, RecipeBean.class);
        }
        return null;
    }

    @Override
    @RpcService
    public Map<String,Object> noticePlatRecipeFlowInfo(NoticePlatRecipeFlowInfoDTO req) {
        TmdyfRemoteService service = ApplicationUtils.getRecipeService(TmdyfRemoteService.class);
        LOGGER.info("noticePlatRecipeFlowInfo req={}",JSONUtils.toString(req));
        Map<String,Object> map = Maps.newHashMap();
        if (req != null && StringUtils.isNotEmpty(req.getPutOnRecordID())&& StringUtils.isNotEmpty(req.getRecipeID())){
            try {
                DrugEnterpriseResult result = service.updateMedicalInsuranceRecord(req.getRecipeID(), req.getPutOnRecordID());
                if (StringUtils.isNotEmpty(result.getMsg())){
                    map.put("msg",result.getMsg());
                }
                LOGGER.info("noticePlatRecipeFlowInfo res={}",JSONUtils.toString(result));
            }catch (Exception e){
                LOGGER.error("noticePlatRecipeFlowInfo error.",e);
                map.put("msg","处理异常");
            }
        }
        return map;

    }

    @Override
    @RpcService
    public void noticePlatRecipeMedicalInsuranceInfo(NoticePlatRecipeMedicalInfoDTO req) {
        LOGGER.info("noticePlatRecipeMedicalInsuranceInfo req={}",JSONUtils.toString(req));
        if (null == req) {
            return;
        }
        //上传状态
        String uploadStatus = req.getUploadStatus();

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeCode(req.getRecipeCode());
        if (null != dbRecipe) {
            //默认 医保上传确认中
            Integer status = RecipeStatusConstant.CHECKING_MEDICAL_INSURANCE;
            String memo = "";
            if ("1".equals(uploadStatus)){
                //上传成功
                if (RecipeStatusConstant.READY_CHECK_YS != dbRecipe.getStatus()){
                    status = RecipeStatusConstant.READY_CHECK_YS;
                    memo = "His医保信息上传成功";
                }
                //保存医保返回数据
                saveMedicalInfoForRecipe(req,dbRecipe.getRecipeId());
            }else {
                //上传失败
                //失败原因
                String failureInfo = req.getFailureInfo();
                status = RecipeStatusConstant.RECIPE_MEDICAL_FAIL;
                memo = StringUtils.isEmpty(failureInfo)?"His医保信息上传失败":"His医保信息上传失败,原因:"+failureInfo;
            }
            recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), status, null);
            //日志记录
            RecipeLogService.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), status, memo);
        }
    }
    private void saveMedicalInfoForRecipe(NoticePlatRecipeMedicalInfoDTO req, Integer recipeId) {
        //医院机构编码
        String hospOrgCodeFromMedical = req.getHospOrgCode();
        //参保地统筹区
        String insuredArea = req.getInsuredArea();
        //医保结算请求串
        String medicalSettleData = req.getMedicalSettleData();
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        Map<String,String> updateMap = Maps.newHashMap();
        updateMap.put("hospOrgCodeFromMedical",hospOrgCodeFromMedical);
        updateMap.put("insuredArea",insuredArea);
        updateMap.put("medicalSettleData",medicalSettleData);
        recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId,updateMap);
    }


    /**
     * 获取处方类型的参数接口对像
     *  区别 中药、西药、膏方
     * @param paramMapType
     * @param recipe
     * @param details
     * @param fileName
     * @return
     */
    @Override
    @RpcService
    public Map<String, Object> createRecipeParamMapForPDF(Integer paramMapType, RecipeBean recipe, List<RecipeDetailBean> details, String fileName){
        LOGGER.info("createParamMapForChineseMedicine start in  paramMapType={} recipe={} details={} fileName={}"
                ,paramMapType,JSONObject.toJSONString(recipe),JSONObject.toJSONString(details),fileName);

        Map<String, Object> map;
        List<Recipedetail> recipeDetails = new ArrayList<>();
        for (RecipeDetailBean recipeDetailBean : details){
            recipeDetails.add(getBean(recipeDetailBean,Recipedetail.class));
        }
        //根据处方类型选择生成参数
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            //处方类型-中药 或 膏方
            map = RecipeServiceSub.createParamMapForChineseMedicine(getBean(recipe, Recipe.class), recipeDetails, fileName);
        } else {
            //处方类型-其他类型
            map = RecipeServiceSub.createParamMap(getBean(recipe, Recipe.class), recipeDetails, fileName);

        }
        return map;
    }

    @RpcService
    @Override
    public Boolean updateRecipeInfoByRecipeId(int recipeId, final Map<String,Object> changeAttr){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        try {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, changeAttr);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Object> getHtml5LinkInfo(PatientInfoDTO patient, RecipeBean recipeBean, List<RecipeDetailBean> recipeDetails, Integer reqType) {
        WinningMedicationGuideService winningMedicationGuideService = ApplicationUtils.getRecipeService(WinningMedicationGuideService.class);
        recipe.medicationguide.bean.PatientInfoDTO patientInfoDTO = ObjectCopyUtils.convert(patient,recipe.medicationguide.bean.PatientInfoDTO.class);
        Map<String,Object> resultMap = winningMedicationGuideService.getHtml5LinkInfo(patientInfoDTO,recipeBean,recipeDetails,reqType);
        return resultMap;
    }

    @Override
    public Map<String, String> getEnterpriseCodeByRecipeId(Integer orderId) {
        Map<String, String> map = new HashMap<String, String>();
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        if(recipeOrder != null){
            map.put("orderType", recipeOrder.getOrderType() == null ? null :recipeOrder.getOrderType() + "");
        } else {
            LOGGER.info("getEnterpriseCodeByRecipeId 获取订单为null orderId = {}",orderId);
        }
        Integer depId = recipeOrder.getEnterpriseId();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (depId != null) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
            map.put("enterpriseCode", drugsEnterprise.getEnterpriseCode());
        }
        return map;
    }

    @Override
    public Boolean canRequestConsultForRecipe(String mpiId, Integer depId, Integer organId) {
        LOGGER.info("canRequestConsultForRecipe organId={},mpiId={},depId={}",organId,mpiId,depId);
        //先查3天内未处理的线上处方-平台
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(3),DateConversion.DEFAULT_DATE_TIME);
        //前置没考虑
        List<Recipe> recipeList = recipeDAO.findRecipeListByDeptAndPatient(depId, mpiId, startDt,endDt);
        if (CollectionUtils.isEmpty(recipeList)){
            //再查3天内线上未缴费的处方-到院取药推送的处方-his
            PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
            PatientDTO patientDTO = patientService.get(mpiId);
            IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
            QueryRecipeRequestTO request = new QueryRecipeRequestTO();
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            patientBaseInfo.setPatientName(patientDTO.getPatientName());
            patientBaseInfo.setCertificate(patientDTO.getCertificate());
            patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
            request.setPatientInfo(patientBaseInfo);
            request.setStartDate(DateConversion.getDateTimeDaysAgo(3));
            request.setEndDate(DateTime.now().toDate());
            request.setOrgan(organId);
            LOGGER.info("canRequestConsultForRecipe-getHosRecipeList req={}", JSONUtils.toString(request));
            QueryRecipeResponseTO response = null;
            try {
                response = hisService.queryRecipeListInfo(request);
            } catch (Exception e) {
                LOGGER.warn("canRequestConsultForRecipe-getHosRecipeList his error. ", e);
            }
            LOGGER.info("canRequestConsultForRecipe-getHosRecipeList res={}", JSONUtils.toString(response));
            if(null == response){
                return true;
            }
            List<RecipeInfoTO> data = response.getData();
            if (CollectionUtils.isEmpty(data)){
                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    @RpcService
    @Override
    public void recipeMedicInsurSettle(MedicInsurSettleSuccNoticNgariReqDTO request) {
        LOGGER.info("省医保结算成功通知平台,param = {}", JSONUtils.toString(request));
        if (null == request.getRecipeId()) {
            return;
        }
        try {
            RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            recipeOrderService.recipeMedicInsurSettleUpdateOrder(request);
        } catch (Exception e) {
            LOGGER.info("recipeMedicInsurSettle error", e);
        }
        return;
    }

    @Override
    @RpcService
    public String getRecipeOrderCompleteAddress(RecipeOrderBean orderBean) {
        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
        return commonRemoteService.getCompleteAddress(getBean(orderBean,RecipeOrder.class));
    }

    @RpcService
    @Override
    public Map<String, Object> noticePlatRecipeAuditResult(NoticeNgariAuditResDTO req) {
        LOGGER.info("noticePlatRecipeAuditResult，req = {}", JSONUtils.toString(req));
        Map<String, Object> resMap = Maps.newHashMap();
        if(null == req){
            LOGGER.warn("当前处方更新审核结果接口入参为空！");
            resMap.put("msg","当前处方更新审核结果接口入参为空");
            return resMap;
        }
        try {
            RecipeCheckService recipeService = ApplicationUtils.getRecipeService(RecipeCheckService.class);
            RecipeDAO dao = DAOFactory.getDAO(RecipeDAO.class);

            //date 20200519
            //当调用处方审核失败接口记录日志，不走有审核结果的逻辑
            Recipe recipe = null;
            //添加处方审核接受结果，recipeId的字段，优先使用recipeId
            if(StringUtils.isNotEmpty(req.getRecipeId())){
                recipe = dao.getByRecipeId(Integer.parseInt(req.getRecipeId()));
            }else{
                recipe = dao.getByRecipeCodeAndClinicOrgan(req.getRecipeCode(), req.getOrganId());
            }
            if("2".equals(req.getAuditResult())){
                LOGGER.warn("当前处方{}调用审核接口失败！", JSONUtils.toString(req));
                RecipeLogDAO logDAO = DAOFactory.getDAO(RecipeLogDAO.class);
                if(null != recipe){
                    logDAO.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "当前处方调用审核接口失败");
                }else{
                    LOGGER.warn("当前处方code的处方{}不存在！", req.getRecipeCode());
                }
                resMap.put("msg","当前处方调用审核接口失败");
                return resMap;
            }
            if (recipe == null){
                resMap.put("msg","查询不到处方信息");
            }
            Map<String, Object> paramMap = Maps.newHashMap();
            paramMap.put("recipeId",recipe.getRecipeId());
            //1:审核通过 0-通过失败
            paramMap.put("result",req.getAuditResult());
            //审核机构
            paramMap.put("checkOrgan",req.getOrganId());
            //审核药师工号
            paramMap.put("auditDoctorCode",req.getAuditDoctorCode());
            //审核药师姓名
            paramMap.put("auditDoctorName",req.getAuditDoctorName());
            //审核不通过原因备注
            paramMap.put("failMemo",req.getMemo());
            //审核时间
            paramMap.put("auditTime",req.getAuditTime());
            Map<String, Object> result = recipeService.saveCheckResult(paramMap);
            //错误消息返回
            if (result!=null && result.get("msg") != null){
                resMap.put("msg",result.get("msg"));
            }
            LOGGER.info("noticePlatRecipeAuditResult，res = {}", JSONUtils.toString(result));
        }catch (Exception e){
            resMap.put("msg",e.getMessage());
            LOGGER.error("noticePlatRecipeAuditResult，error= {}", e);
        }
        return resMap;
    }

    @Override
    public long getCountByOrganAndDeptIds(Integer organId, List<Integer> deptIds,Integer plusDays) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountByOrganAndDeptIds(organId, deptIds,plusDays);
    }

    @RpcService
    @Override
    public List<Object[]> countRecipeIncomeGroupByDeptId(Date startDate, Date endDate, Integer organId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.countRecipeIncomeGroupByDeptId(startDate, endDate, organId);
    }

    @Override
    public List<RecipeBean> findByClinicId(Integer consultId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return ObjectCopyUtils.convert(recipeDAO.findByClinicId(consultId),RecipeBean.class);
    }

    @Override
    public List<RecipeBean> findByMpiId(String mpiId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return ObjectCopyUtils.convert(recipeDAO.findByMpiId(mpiId),RecipeBean.class);
    }

    @Override
    @RpcService
    public BigDecimal getRecipeCostCountByOrganIdAndDepartIds(Integer organId, Date startDate, Date endDate, List<Integer> deptIds) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getRecipeIncome(organId, startDate, endDate, deptIds);
    }


    /**
     * 医院在复诊/处方结算完成的时候将电子票据号同步到结算上
     *
     */
    @Override
    @RpcService
    public HisResponseTO syncEinvoiceNumberToPay(SyncEinvoiceNumberDTO syncEinvoiceNumberDTO) {
        //判断当前传入的信息是否满足定位更新电子票据号
        //满足则更新支付的电子票据号
        HisResponseTO result = new HisResponseTO();
        result.setMsgCode("0");
        if(!valiSyncEinvoiceNumber(syncEinvoiceNumberDTO, result)){
            return result;
        }
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        //判断复诊的支付或者处方的支付能否定位到
        HosrelationBean hosrelation = hosrelationService.getByStatusAndInvoiceNoAndOrganId
                (1, syncEinvoiceNumberDTO.getInvoiceNo(), Integer.parseInt(syncEinvoiceNumberDTO.getOrganId()));

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        Integer recipeId = recipeDetailDAO.getRecipeIdByOrganIdAndInvoiceNo(Integer.parseInt(syncEinvoiceNumberDTO.getOrganId()), syncEinvoiceNumberDTO.getInvoiceNo());

        if(null != hosrelation){
            hosrelationService.updateEinvoiceNumberById(hosrelation.getId(), syncEinvoiceNumberDTO.getEinvoiceNumber());
            result.setSuccess();
            return result;

        }
        if(null != recipeId){
            Boolean updateResult = recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId, ImmutableMap.of("einvoiceNumber", syncEinvoiceNumberDTO.getEinvoiceNumber()));
            if (updateResult) {
                result.setSuccess();
                return result;
            }else{
                result.setMsg("更新电子票据号失败！");
            }
        }
        result.setMsg("当前无支付订单与支付单号对应，更新电子票据号失败！");
        return result;
    }

    @Override
    @RpcService
    public Map<String, String> findMsgByparameters(Date startTime, Date endTime, Integer organId){
        List<Object[]> list = DAOFactory.getDAO(RecipeDAO.class).findMsgByparameters(startTime, endTime, organId);
        Map<String, String> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(list)){
            for (Object[] obj : list) {
                if (obj[0] == null){
                    continue;
                }
                result.put(String.valueOf(obj[0]), String.valueOf(obj[1]));
            }
        }
        return result;
    }

    private boolean valiSyncEinvoiceNumber(SyncEinvoiceNumberDTO syncEinvoiceNumberDTO, HisResponseTO result) {
        boolean flag = true;
        if (null == syncEinvoiceNumberDTO) {
            result.setMsg("当前医院更新电子票据号，请求参数为空！");
            flag = false;
        }
        if(StringUtils.isEmpty(syncEinvoiceNumberDTO.getOrganId()) || StringUtils.isEmpty(syncEinvoiceNumberDTO.getInvoiceNo())){
            result.setMsg("当前医院更新电子票据号，传入的机构id或者HIS结算单据号无法更新！");
            flag = false;
        }
        if(StringUtils.isEmpty(syncEinvoiceNumberDTO.getEinvoiceNumber())){
            result.setMsg("当前医院更新电子票据号，传入更新的电子票据号为空无法更新！");
            flag = false;
        }
        return flag;
    }

    @Override
    public DrugsEnterpriseBean getDrugsEnterpriseBeanById(Integer depId){
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        return ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
    }

    @Autowired
    RecipeService recipeService;

    @Autowired
    RecipeDAO recipeDAO;

    @Override
    public Boolean saveSignRecipePDF(CaSignResultTo caSignResultTo) {
        LOGGER.info("saveSignRecipePDF caSignResultTo:{}", JSONUtils.toString(caSignResultTo));
        Integer recipeId = caSignResultTo.getRecipeId();
        String errorMsg = caSignResultTo.getMsg();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return false;
        }
        if (!StringUtils.isEmpty(errorMsg)){
            LOGGER.info("当前审核处方{}签名失败！errorMsg: {}", recipeId, errorMsg);
            RecipeLogDAO recipeLogDAO = getDAO(RecipeLogDAO.class);
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.SIGN_ERROR_CODE_PHA, null);
            recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), errorMsg);
            return true;
        }

        try {
            boolean isDoctor = true;
            if (null == recipe.getCheckDateYs()) { // 注意这里在RecipeServiceEsignExt.saveSignRecipePDF判断时药师还是医生，那边改动会有影响
                isDoctor = false;
            }

            DoctorService doctorService = AppDomainContext
                    .getBean("basic.doctorService", DoctorService.class);
            DoctorDTO doctor = doctorService.getBeanByDoctorId(recipe.getDoctor());
            String loginId = doctor.getLoginId();

            CaSignResultVo resultVo = new CaSignResultVo();
            resultVo.setPdfBase64(caSignResultTo.getPdfBase64());
            resultVo.setSignRecipeCode(caSignResultTo.getSignRecipeCode());
            String fileId = null;
            //保存签名值、时间戳、电子签章文件
            LOGGER.info("start save PdfBase64 Or SignRecipeCode");
            String result = RecipeServiceEsignExt.saveSignRecipePDF2(resultVo.getPdfBase64(),
                    recipeId, loginId, resultVo.getSignCADate(), resultVo.getSignRecipeCode(), isDoctor, fileId);
            if ("fail".equalsIgnoreCase(result)){
                return false;
            }
            resultVo.setFileId(fileId);
            recipeService.signRecipeInfoSave(recipeId, isDoctor, resultVo, recipe.getClinicOrgan());
        }catch (Exception e) {
            LOGGER.error("saveSignRecipePDF error", e);
            return false;
        }
        return true;
    }

}
