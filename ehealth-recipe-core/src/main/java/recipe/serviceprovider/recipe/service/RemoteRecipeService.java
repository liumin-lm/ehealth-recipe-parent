package recipe.serviceprovider.recipe.service;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.ca.api.vo.*;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.his.recipe.mode.QueryRecipeRequestTO;
import com.ngari.his.recipe.mode.QueryRecipeResponseTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.ca.mode.CaSignResultTo;
import com.ngari.platform.recipe.mode.HospitalReqTo;
import com.ngari.platform.recipe.mode.ReadjustDrugDTO;
import com.ngari.recipe.ca.CaSignResultUpgradeBean;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.model.StandardResultBean;
import com.ngari.recipe.drugsenterprise.model.ThirdResultBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.hisprescription.model.SyncEinvoiceNumberDTO;
import com.ngari.recipe.recipe.constant.RecipePayTextEnum;
import com.ngari.recipe.recipe.constant.RecipeSendTypeEnum;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipe.model.CaSignResultBean;
import com.ngari.recipe.recipe.service.IRecipeService;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipereportform.model.*;
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
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.RecipeUtil;
import recipe.ca.CAInterface;
import recipe.ca.factory.CommonCAFactory;
import recipe.ca.vo.CaSignResultVo;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.*;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.StandardEnterpriseCallService;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.drugsenterprise.TmdyfRemoteService;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.medicationguide.service.WinningMedicationGuideService;
import recipe.service.*;
import recipe.service.recipereportforms.RecipeReportFormsService;
import recipe.serviceprovider.BaseService;
import recipe.thread.*;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


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
//    @Autowired
//    private CommonCAFactory commonCAFactory;

    @RpcService
    @Override
    public void sendSuccess(RecipeBussReqTO request) {
        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
        if (null != request.getData()) {
            HisSendResTO response = (HisSendResTO) request.getData();
//            service.sendSuccess(response);
            //异步处理回调方法，避免超时
            RecipeBusiThreadPool.execute(new RecipeSendSuccessRunnable(response));
        }
    }

    @RpcService
    @Override
    public void sendFail(RecipeBussReqTO request) {
        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
        if (null != request.getData()) {
            HisSendResTO response = (HisSendResTO) request.getData();
//            service.sendFail(response);
            //异步处理回调方法，避免超时
            RecipeBusiThreadPool.execute(new RecipeSendFailRunnable(response));
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

    @Override
    public boolean updateRecipeInfoByRecipeIdAndAfterStatus(int recipeId, int afterStatus, Map<String, Object> changeAttr) {
        return recipeDAO.updateRecipeInfoByRecipeId(recipeId, afterStatus, changeAttr);
    }

    @RpcService
    @Override
    public RecipeBean getByRecipeId(int recipeId) {
        RecipeBean recipeBean = get(recipeId);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (recipeBean != null && recipeExtend != null) {
            recipeBean.setMainDieaseDescribe(recipeExtend.getMainDieaseDescribe());
            recipeBean.setRecipeCostNumber(recipeExtend.getRecipeCostNumber());
        }
        return recipeBean;
    }

    @RpcService
    @Override
    public long getUncheckedRecipeNum(int doctorId) {
        List<Integer> organIds = findAPOrganIdsByDoctorId(doctorId);
        if (CollectionUtils.isEmpty(organIds)) {
            return 0;
        }
        //flag = 0 查询待药师审核的条数
        Long num = this.getRecipeCountByFlag(organIds, 0);
        return null == num ? 0 : num;
    }

    private List<Integer> findAPOrganIdsByDoctorId(Integer doctorId) {
        List<Integer> organIds = null;
        if (null != doctorId) {
            DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
            organIds = doctorService.findAPOrganIdsByDoctorId(doctorId);
        }

        return organIds;
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
    @Deprecated
    public QueryResult<Map> findRecipesByInfo(Integer organId, Integer status,
                                              Integer doctor, String patientName, Date bDate, Date eDate, Integer dateType,
                                              Integer depart, int start, int limit, List<Integer> organIds, Integer giveMode, Integer sendType, Integer fromflag, Integer recipeId,
                                              Integer enterpriseId, Integer checkStatus, Integer payFlag, Integer orderType) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findRecipesByInfo(organId, status, doctor, patientName, bDate, eDate, dateType, depart, start, limit, organIds, giveMode, sendType, fromflag, recipeId, enterpriseId, checkStatus, payFlag, orderType);
    }

    @RpcService
    @Override
    public QueryResult<Map> findRecipesByInfo2(RecipesQueryVO recipesQueryVO) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findRecipesByInfo(recipesQueryVO.getOrganId(), recipesQueryVO.getStatus(), recipesQueryVO.getDoctor()
                , recipesQueryVO.getPatientName(), recipesQueryVO.getBDate(), recipesQueryVO.getEDate(), recipesQueryVO.getDateType()
                , recipesQueryVO.getDepart(), recipesQueryVO.getStart(), recipesQueryVO.getLimit(), recipesQueryVO.getOrganIds()
                , recipesQueryVO.getGiveMode(), recipesQueryVO.getSendType(), recipesQueryVO.getFromFlag(), recipesQueryVO.getRecipeId()
                , recipesQueryVO.getEnterpriseId(), recipesQueryVO.getCheckStatus(), recipesQueryVO.getPayFlag(), recipesQueryVO.getOrderType());

    }

    @RpcService
    @Override
    public Map<String, Integer> getStatisticsByStatus(Integer organId,
                                                      Integer status, Integer doctor, String mpiid,
                                                      Date bDate, Date eDate, Integer dateType,
                                                      Integer depart, int start, int limit, List<Integer> organIds, Integer giveMode, Integer fromflag, Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getStatisticsByStatus(organId, status, doctor, mpiid, bDate, eDate, dateType, depart, start, limit, organIds, giveMode, fromflag, recipeId);
    }

    @RpcService
    @Override
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId) {
        IRecipeAuditService recipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
        //代码已迁移 ehealth-recipeaudi 修改在ehealth-recipeaudi的对应相同的方法修改
        return recipeAuditService.findRecipeAndDetailsAndCheckById(recipeId, null);
    }

    @RpcService
    @Override
    public List<Map> queryRecipesByMobile(List<String> mpis) {
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
    public List<String> findPatientMpiIdForOp(List<String> mpiIds, List<Integer> organIds) {
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
        return recipeDAO.findHistoryMpiIdsByDoctorId(doctorId, start, limit);
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
                recipedetails.add(getBean(recipeDetailBean, Recipedetail.class));
            }
        }
        if (StringUtils.isEmpty(recipeBean.getRecipeMode())) {
            recipeBean.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
        }
        if (recipeBean.getReviewType() == null) {
            recipeBean.setReviewType(ReviewTypeConstant.Postposition_Check);
        }
        //date 20200601
        //设置处方详情字符类型
        RecipeServiceSub.setUseDaysBToDetali(recipedetails);
        recipeDAO.updateOrSaveRecipeAndDetail(getBean(recipeBean, Recipe.class), recipedetails, false);
    }


    /**
     * 根据日期范围，机构归类的业务量(天，月)
     *
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
     *
     * @param startDate
     * @param endDate
     * @return
     */
    @RpcService
    @Override
    public HashMap<Object, Integer> getCountByHourAreaGroupByOrgan(final Date startDate, final Date endDate) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountByHourAreaGroupByOrgan(startDate, endDate);
    }

    /**
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
    @RpcService(timeout = 600000)
    @Override
    @Deprecated
    public List<Object[]> findRecipesByInfoForExcel(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate,
                                                    final Date eDate, final Integer dateType, final Integer depart, List<Integer> organIds, Integer giveMode,
                                                    Integer fromflag, Integer recipeId, Integer enterpriseId, Integer checkStatus, Integer payFlag, Integer orderType) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipesQueryVO recipesQueryVO = new RecipesQueryVO();
        recipesQueryVO.setOrganIds(organIds);
        recipesQueryVO.setOrganId(organId);
        recipesQueryVO.setBDate(bDate);
        recipesQueryVO.setCheckStatus(checkStatus);
        recipesQueryVO.setDateType(dateType);
        recipesQueryVO.setDepart(depart);
        recipesQueryVO.setDoctor(doctor);
        recipesQueryVO.setEDate(eDate);
        recipesQueryVO.setEnterpriseId(enterpriseId);
        recipesQueryVO.setFromFlag(fromflag);
        recipesQueryVO.setGiveMode(giveMode);
        recipesQueryVO.setRecipeId(recipeId);
        recipesQueryVO.setPayFlag(payFlag);
        recipesQueryVO.setOrderType(orderType);
        recipesQueryVO.setStatus(status);
        recipesQueryVO.setPatientName(patientName);
        List<Object[]> result = recipeDAO.findRecipesByInfoForExcel(recipesQueryVO);
        return result;
    }

    @RpcService(timeout = 600000)
    @Override
    public List<Object[]> findRecipesByInfoForExcel2(RecipesQueryVO recipesQueryVO) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Object[]> result = recipeDAO.findRecipesByInfoForExcel(recipesQueryVO);
        return result;
    }

    /**
     * 春节2月17版本 JRK
     * 查询
     *
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
    @Deprecated
    public List<Map> findRecipeOrdersByInfoForExcel(Integer organId, List<Integer> organIds, Integer status, Integer doctor, String patientName, Date bDate,
                                                    Date eDate, Integer dateType, Integer depart, Integer giveMode, Integer fromflag, Integer recipeId) {
        LOGGER.info("findRecipeOrdersByInfoForExcel查询处方订单导出信息入参:{},{},{},{},{},{},{},{},{},{},{},{}", organId, organIds, status, doctor, patientName, bDate, eDate, dateType, depart, giveMode, fromflag, recipeId);
        RecipesQueryVO recipesQueryVO = new RecipesQueryVO();
        recipesQueryVO.setOrganIds(organIds);
        recipesQueryVO.setOrganId(organId);
        recipesQueryVO.setBDate(bDate);
        recipesQueryVO.setDateType(dateType);
        recipesQueryVO.setDepart(depart);
        recipesQueryVO.setDoctor(doctor);
        recipesQueryVO.setEDate(eDate);
        recipesQueryVO.setFromFlag(fromflag);
        recipesQueryVO.setGiveMode(giveMode);
        recipesQueryVO.setRecipeId(recipeId);
        recipesQueryVO.setStatus(status);
        recipesQueryVO.setPatientName(patientName);
        return findRecipeOrdersByInfoForExcel2(recipesQueryVO);
    }


    @RpcService(timeout = 600000)
    @Override
    public List<Map> findRecipeOrdersByInfoForExcel2(RecipesQueryVO recipesQueryVO) {
        LOGGER.info("findRecipeOrdersByInfoForExcel查询处方订单导出信息入参:{}", JSONUtils.toString(recipesQueryVO));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Map> recipeMap = recipeDAO.findRecipesByInfoForExcelN(recipesQueryVO);

        //组装数据准备
        List<Map> newRecipeMap = new ArrayList<>();
        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);

        //组装处方相关联的数据
        for (Map<String, Object> recipeMsg : recipeMap) {
            Object nowRecipeId = recipeMsg.get("recipeId");
            if (null == nowRecipeId) {
                continue;
            }
            try {
                //订单数据
                recipeAndOrderMsg(commonRemoteService, recipeMsg);
                recipeMsg.put("recipeOrder", null);
                newRecipeMap.add(recipeMsg);
            } catch (Exception e) {
                LOGGER.error("查询关联信息异常，对应的处方id{}", nowRecipeId, e);
                e.printStackTrace();
                throw new DAOException("查询处方信息异常！");
            }

        }
        LOGGER.info("findRecipeOrdersByInfoForExcel查询处方订单导出信息结果:{}", newRecipeMap);
        return newRecipeMap;
    }

    private void recipeAndOrderMsg(CommonRemoteService commonRemoteService, Map<String, Object> recipeMsg) throws ControllerException {
        //地址
        RecipeOrder order = (RecipeOrder) recipeMsg.get("recipeOrder");
        recipeMsg.put("completeAddress", commonRemoteService.getCompleteAddress(order));
        if (null != order) {
            //收货人
            recipeMsg.put("receiver", order.getReceiver());
            recipeMsg.put("sendType", RecipeSendTypeEnum.getSendText(order.getSendType()));
            //收货人联系方式
            recipeMsg.put("recMobile", order.getRecMobile());
            //下单时间
            recipeMsg.put("orderTime", order.getCreateTime());
            //配送费
            recipeMsg.put("expressFee", order.getExpressFee());
            //订单号
            recipeMsg.put("orderCode", order.getOrderCode());
            //订单状态
            if (null != order.getStatus()) {
                recipeMsg.put("orderStatus", DictionaryController.instance().get("eh.cdr.dictionary.RecipeOrderStatus").getText(order.getStatus()));
            }
            //支付金额
            recipeMsg.put("payMoney", order.getActualPrice());
            recipeMsg.put("totalMoney", order.getTotalFee());
            //date 20200303
            //添加药企信息和期望配送时间
            if (null != order.getEnterpriseId()) {
                //匹配上药企，获取药企名
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (null != enterprise && null != enterprise.getName()) {
                    recipeMsg.put("enterpriseName", enterprise.getName());
                } else {
                    LOGGER.warn("findRecipeOrdersByInfoForExcel 当前处方{}关联的药企id:{}信息不全", order.getRecipeIdList(), order.getEnterpriseId());
                }
            }
            //date 20200303
            //添加期望配送时间
            if (StringUtils.isNotEmpty(order.getExpectSendDate()) && StringUtils.isNotEmpty(order.getExpectSendTime())) {
                recipeMsg.put("expectSendDate", order.getExpectSendDate() + " " + order.getExpectSendTime());
            }
            //date 20200305
            //添加支付状态
            if (null != order.getPayFlag()) {
                recipeMsg.put("payStatusText", RecipePayTextEnum.getByPayFlag(order.getPayFlag()).getPayText());
            } else {
                LOGGER.info("findRecipeOrdersByInfoForExcel 当前处方{}的订单支付状态{}", order.getRecipeIdList(), order.getPayFlag());
                recipeMsg.put("payStatusText", RecipePayTextEnum.Default.getPayText());
            }
            recipeMsg.put("payTime", order.getPayTime());
            recipeMsg.put("tradeNo", order.getTradeNo());

        } else {
            //没有订单说明没有支付
            recipeMsg.put("payStatusText", RecipePayTextEnum.Default.getPayText());
        }
    }

//
//    @RpcService(timeout = 600000)
//    public List<Map> findRecipeOrdersByInfoForExcelNT(Integer organId, List<Integer> organIds, Integer status, Integer doctor, String patientName, Date bDate,
//                                                      Date eDate, Integer dateType, Integer depart, Integer giveMode,
//                                                      Integer fromflag,Integer recipeId){
//        LOGGER.info("findRecipeOrdersByInfoForExcelNT查询处方订单导出信息入参:{},{},{},{},{},{},{},{},{},{},{},{}",organId, organIds, status, doctor, patientName, bDate, eDate, dateType, depart, giveMode, fromflag, recipeId);
//        IRecipeService recipeService = RecipeAPI.getService(IRecipeService.class);
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        List<Map> recipeMap = recipeDAO.findRecipesByInfoForExcelN(organId, status, doctor, patientName, bDate, eDate, dateType, depart, organIds, giveMode, fromflag, recipeId);
//
//        //组装数据准备
//        Object nowRecipeId;
//        RecipeOrder order;
//        List<Map> newRecipeMap = new ArrayList<>();
//        Map<String, Object> recipeMsgMap;
//        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
//
//        //组装处方相关联的数据
//
//        LOGGER.info("当前查询出来条数：{}", recipeMap.size());
//        for(Map<String, Object> recipeMsg: recipeMap){
//            nowRecipeId = recipeMsg.get("recipeId");
//            if(null != nowRecipeId){
//                try {
//                    //订单数据
//                    order = (RecipeOrder)recipeMsg.get("recipeOrder");
//
//                    recipeMsgMap = new HashMap();
//                    recipeMsgMap.putAll(recipeMsg);
//                    recipeAndOrderMsg(order, commonRemoteService, recipeMsgMap);
//                    recipeMsgMap.put("recipeOrder",null);
//                    newRecipeMap.add(recipeMsgMap);
//
//                } catch (Exception e) {
//                    LOGGER.error("查询关联信息异常{}，对应的处方id{}", e, nowRecipeId);
//                    e.printStackTrace();
//                    throw new DAOException("查询处方信息异常！");
//                }
//            }
//        }
//        LOGGER.info("findRecipeOrdersByInfoForExcelNT查询处方订单导出信息结果:{}", newRecipeMap);
//        return newRecipeMap;
//    }

    @RpcService
    @Override
    public HashMap<Integer, Long> getCountGroupByOrgan() {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountGroupByOrgan();
    }


    @RpcService
    @Override
    public HashMap<Integer, Long> getRecipeRequestCountGroupByDoctor() {
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
    public List<Integer> findDrugIdByRecipeId(Integer recipeId) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Integer> recipedetails = recipeDetailDAO.findDrugIdByRecipeId(recipeId);
        return recipedetails;
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
    public boolean saveOrUpdateRecipeExtend(RecipeExtendBean recipeExtendBean) {
        try {
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeExtendBean, RecipeExtend.class);
            recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
            return true;
        } catch (Exception e) {
            LOGGER.error("saveOrUpdateRecipeExtend error", e);
            return false;
        }
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
        if (recipes != null && recipes.size() > 0) {
            Recipe recipe = recipes.get(0);
            return ObjectCopyUtils.convert(recipe, RecipeBean.class);
        }
        return null;
    }

    @Override
    @RpcService
    public Map<String, Object> noticePlatRecipeFlowInfo(NoticePlatRecipeFlowInfoDTO req) {
        TmdyfRemoteService service = ApplicationUtils.getRecipeService(TmdyfRemoteService.class);
        LOGGER.info("noticePlatRecipeFlowInfo req={}", JSONUtils.toString(req));
        Map<String, Object> map = Maps.newHashMap();
        if (req != null && StringUtils.isNotEmpty(req.getPutOnRecordID()) && StringUtils.isNotEmpty(req.getRecipeID())) {
            try {
                DrugEnterpriseResult result = service.updateMedicalInsuranceRecord(req.getRecipeID(), req.getPutOnRecordID());
                if (StringUtils.isNotEmpty(result.getMsg())) {
                    map.put("msg", result.getMsg());
                }
                LOGGER.info("noticePlatRecipeFlowInfo res={}", JSONUtils.toString(result));
            } catch (Exception e) {
                LOGGER.error("noticePlatRecipeFlowInfo error.", e);
                map.put("msg", "处理异常");
            }
        }
        return map;

    }

    @Override
    @RpcService
    public void noticePlatRecipeMedicalInsuranceInfo(NoticePlatRecipeMedicalInfoDTO req) {
        LOGGER.info("noticePlatRecipeMedicalInsuranceInfo req={}", JSONUtils.toString(req));
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
            if ("1".equals(uploadStatus)) {
                //上传成功
                if (RecipeStatusConstant.READY_CHECK_YS != dbRecipe.getStatus()) {
                    status = RecipeStatusConstant.READY_CHECK_YS;
                    memo = "His医保信息上传成功";
                }
                //保存医保返回数据
                saveMedicalInfoForRecipe(req, dbRecipe.getRecipeId());
            } else {
                //上传失败
                //失败原因
                String failureInfo = req.getFailureInfo();
                status = RecipeStatusConstant.RECIPE_MEDICAL_FAIL;
                memo = StringUtils.isEmpty(failureInfo) ? "His医保信息上传失败" : "His医保信息上传失败,原因:" + failureInfo;
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
        Map<String, String> updateMap = Maps.newHashMap();
        updateMap.put("hospOrgCodeFromMedical", hospOrgCodeFromMedical);
        updateMap.put("insuredArea", insuredArea);
        updateMap.put("medicalSettleData", medicalSettleData);
        recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId, updateMap);
    }


    /**
     * 获取处方类型的参数接口对像
     * 区别 中药、西药、膏方
     *
     * @param paramMapType
     * @param recipe
     * @param details
     * @param fileName
     * @return
     */
    @Override
    @RpcService
    public Map<String, Object> createRecipeParamMapForPDF(Integer paramMapType, RecipeBean recipe, List<RecipeDetailBean> details, String fileName) {
        LOGGER.info("createParamMapForChineseMedicine start in  paramMapType={} recipe={} details={} fileName={}"
                , paramMapType, JSONObject.toJSONString(recipe), JSONObject.toJSONString(details), fileName);

        Map<String, Object> map;
        List<Recipedetail> recipeDetails = new ArrayList<>();
        for (RecipeDetailBean recipeDetailBean : details) {
            recipeDetails.add(getBean(recipeDetailBean, Recipedetail.class));
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
    public Boolean updateRecipeInfoByRecipeId(int recipeId, final Map<String, Object> changeAttr) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        try {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, changeAttr);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("updateRecipeInfoByRecipeId -{},error.", recipeId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getHtml5LinkInfo(PatientInfoDTO patient, RecipeBean recipeBean, List<RecipeDetailBean> recipeDetails, Integer reqType) {
        WinningMedicationGuideService winningMedicationGuideService = ApplicationUtils.getRecipeService(WinningMedicationGuideService.class);
        recipe.medicationguide.bean.PatientInfoDTO patientInfoDTO = ObjectCopyUtils.convert(patient, recipe.medicationguide.bean.PatientInfoDTO.class);
        Map<String, Object> resultMap = winningMedicationGuideService.getHtml5LinkInfo(patientInfoDTO, recipeBean, recipeDetails, reqType);
        return resultMap;
    }

    @Override
    public Map<String, String> getEnterpriseCodeByRecipeId(Integer orderId) {
        Map<String, String> map = new HashMap<String, String>();
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        if (recipeOrder != null) {
            map.put("orderType", recipeOrder.getOrderType() == null ? null : recipeOrder.getOrderType() + "");
        } else {
            LOGGER.info("getEnterpriseCodeByRecipeId 获取订单为null orderId = {}", orderId);
        }
        Integer depId = recipeOrder.getEnterpriseId();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (depId != null) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByOrderCode(recipeOrder.getOrderCode());
            if (recipe != null) {
                //货到付款不走卫宁付。。。药店取药可以走卫宁付了
                if (RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode())) {
                    return map;
                }
            }
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
            map.put("enterpriseCode", drugsEnterprise.getEnterpriseCode());
        }
        return map;
    }

    @Override
    public Boolean canRequestConsultForRecipe(String mpiId, Integer depId, Integer organId) {
        LOGGER.info("canRequestConsultForRecipe organId={},mpiId={},depId={}", organId, mpiId, depId);
        //先查3天内未处理的线上处方-平台
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(3), DateConversion.DEFAULT_DATE_TIME);
        //前置没考虑
        List<Recipe> recipeList = recipeDAO.findRecipeListByDeptAndPatient(depId, mpiId, startDt, endDt);
        if (CollectionUtils.isEmpty(recipeList)) {
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
            if (null == response) {
                return true;
            }
            List<RecipeInfoTO> data = response.getData();
            if (CollectionUtils.isEmpty(data)) {
                return true;
            } else {
                return false;
            }
        } else {
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
        return commonRemoteService.getCompleteAddress(getBean(orderBean, RecipeOrder.class));
    }

    @Override
    public String getRecipeOrderCompleteAddressByRecipeId(Integer recipeId) {
        RecipeService recipeService = AppContextHolder.getBean("recipeService", RecipeService.class);
        return recipeService.getCompleteAddress(recipeId);
    }

    @RpcService
    @Override
    public Map<String, Object> noticePlatRecipeAuditResult(NoticeNgariAuditResDTO req) {
        LOGGER.info("noticePlatRecipeAuditResult，req = {}", JSONUtils.toString(req));
        Map<String, Object> resMap = Maps.newHashMap();
        if (null == req) {
            LOGGER.warn("当前处方更新审核结果接口入参为空！");
            resMap.put("msg", "当前处方更新审核结果接口入参为空");
            return resMap;
        }
        try {
            IRecipeAuditService iRecipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
            RecipeDAO dao = DAOFactory.getDAO(RecipeDAO.class);

            //date 20200519
            //当调用处方审核失败接口记录日志，不走有审核结果的逻辑
            Recipe recipe = null;
            //添加处方审核接受结果，recipeId的字段，优先使用recipeId
            if (StringUtils.isNotEmpty(req.getRecipeId())) {
                recipe = dao.getByRecipeId(Integer.parseInt(req.getRecipeId()));
            } else {
                recipe = dao.getByRecipeCodeAndClinicOrgan(req.getRecipeCode(), req.getOrganId());
            }
            if ("2".equals(req.getAuditResult())) {
                LOGGER.warn("当前处方{}调用审核接口失败！", JSONUtils.toString(req));
                RecipeLogDAO logDAO = DAOFactory.getDAO(RecipeLogDAO.class);
                if (null != recipe) {
                    logDAO.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "当前处方调用审核接口失败");
                } else {
                    LOGGER.warn("当前处方code的处方{}不存在！", req.getRecipeCode());
                }
                resMap.put("msg", "当前处方调用审核接口失败");
                return resMap;
            }
            if (recipe == null) {
                resMap.put("msg", "查询不到处方信息");
            }
            Map<String, Object> paramMap = Maps.newHashMap();
            paramMap.put("recipeId", recipe.getRecipeId());
            //1:审核通过 0-通过失败
            paramMap.put("result", req.getAuditResult());
            //审核机构
            paramMap.put("checkOrgan", req.getOrganId());
            //审核药师工号
            paramMap.put("auditDoctorCode", req.getAuditDoctorCode());
            //审核药师姓名
            paramMap.put("auditDoctorName", req.getAuditDoctorName());
            //审核不通过原因备注
            paramMap.put("failMemo", req.getMemo());
            //审核时间
            paramMap.put("auditTime", req.getAuditTime());
            Map<String, Object> result = iRecipeAuditService.saveCheckResult(paramMap);
            //错误消息返回
            if (result != null && result.get("msg") != null) {
                resMap.put("msg", result.get("msg"));
            }
            LOGGER.info("noticePlatRecipeAuditResult，res = {}", JSONUtils.toString(result));
        } catch (Exception e) {
            resMap.put("msg", e.getMessage());
            LOGGER.error("noticePlatRecipeAuditResult，error= {}", e);
        }
        return resMap;
    }

    @Override
    public long getCountByOrganAndDeptIds(Integer organId, List<Integer> deptIds, Integer plusDays) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getCountByOrganAndDeptIds(organId, deptIds, plusDays);
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
        return ObjectCopyUtils.convert(recipeDAO.findByClinicId(consultId), RecipeBean.class);
    }

    @Override
    public List<RecipeBean> findByMpiId(String mpiId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return ObjectCopyUtils.convert(recipeDAO.findByMpiId(mpiId), RecipeBean.class);
    }

    @Override
    @RpcService
    public BigDecimal getRecipeCostCountByOrganIdAndDepartIds(Integer organId, Date startDate, Date endDate, List<Integer> deptIds) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.getRecipeIncome(organId, startDate, endDate, deptIds);
    }


    /**
     * 医院在复诊/处方结算完成的时候将电子票据号同步到结算上
     */
    @Override
    @RpcService
    public HisResponseTO syncEinvoiceNumberToPay(SyncEinvoiceNumberDTO syncEinvoiceNumberDTO) {
        //判断当前传入的信息是否满足定位更新电子票据号
        //满足则更新支付的电子票据号
        HisResponseTO result = new HisResponseTO();
        result.setMsgCode("0");
        if (!valiSyncEinvoiceNumber(syncEinvoiceNumberDTO, result)) {
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

        if (null != hosrelation) {
            hosrelationService.updateEinvoiceNumberById(hosrelation.getId(), syncEinvoiceNumberDTO.getEinvoiceNumber());
            result.setSuccess();
            return result;

        }
        if (null != recipeId) {
            Boolean updateResult = recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId, ImmutableMap.of("einvoiceNumber", syncEinvoiceNumberDTO.getEinvoiceNumber()));
            if (updateResult) {
                result.setSuccess();
                return result;
            } else {
                result.setMsg("更新电子票据号失败！");
            }
        }
        result.setMsg("当前无支付订单与支付单号对应，更新电子票据号失败！");
        return result;
    }

    @Override
    @RpcService
    public Map<String, String> findMsgByparameters(Date startTime, Date endTime, Integer organId) {
        List<Object[]> list = DAOFactory.getDAO(RecipeDAO.class).findMsgByparameters(startTime, endTime, organId);
        Map<String, String> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (Object[] obj : list) {
                if (obj[0] == null) {
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
        if (StringUtils.isEmpty(syncEinvoiceNumberDTO.getOrganId()) || StringUtils.isEmpty(syncEinvoiceNumberDTO.getInvoiceNo())) {
            result.setMsg("当前医院更新电子票据号，传入的机构id或者HIS结算单据号无法更新！");
            flag = false;
        }
        if (StringUtils.isEmpty(syncEinvoiceNumberDTO.getEinvoiceNumber())) {
            result.setMsg("当前医院更新电子票据号，传入更新的电子票据号为空无法更新！");
            flag = false;
        }
        return flag;
    }

    @Override
    public DrugsEnterpriseBean getDrugsEnterpriseBeanById(Integer depId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        LOGGER.info("getDrugsEnterpriseBeanById:{}.", JSONUtils.toString(drugsEnterprise));
        return ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
    }

    @Override
    public ThirdResultBean readyToSend(Map<String, Object> paramMap) {
        LOGGER.info("readyToSend:{}", JSONUtils.toString(paramMap));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        ThirdResultBean thirdResultBean = new ThirdResultBean();
        recipe.bean.ThirdResultBean resultBean = callService.readyToSend(paramMap);
        LOGGER.info("readyToSend resultBean:{}", JSONUtils.toString(resultBean));
        getResultMsg(thirdResultBean, resultBean);
        LOGGER.info("readyToSend thirdResultBean:{}", JSONUtils.toString(thirdResultBean));
        return thirdResultBean;
    }

    @Override
    public ThirdResultBean toSend(Map<String, Object> paramMap) {
        LOGGER.info("toSend:{}", JSONUtils.toString(paramMap));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        ThirdResultBean thirdResultBean = new ThirdResultBean();
        recipe.bean.ThirdResultBean resultBean = callService.toSend(paramMap);
        LOGGER.info("toSend resultBean:{}", JSONUtils.toString(resultBean));
        getResultMsg(thirdResultBean, resultBean);
        LOGGER.info("toSend thirdResultBean:{}", JSONUtils.toString(thirdResultBean));
        return thirdResultBean;
    }

    @Override
    public ThirdResultBean finishRecipe(Map<String, Object> paramMap) {
        LOGGER.info("finishRecipe:{}", JSONUtils.toString(paramMap));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        ThirdResultBean thirdResultBean = new ThirdResultBean();
        recipe.bean.ThirdResultBean resultBean = callService.finishRecipe(paramMap);
        LOGGER.info("finishRecipe resultBean:{}", JSONUtils.toString(resultBean));
        getResultMsg(thirdResultBean, resultBean);
        LOGGER.info("finishRecipe thirdResultBean:{}", JSONUtils.toString(thirdResultBean));
        return thirdResultBean;
    }

    @Override
    public StandardResultBean downLoadRecipes(Map<String, Object> parames) {
        LOGGER.info("downLoadRecipes:{}", JSONUtils.toString(parames));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        return ObjectCopyUtils.convert(callService.downLoadRecipes(parames), StandardResultBean.class);
    }

    @Override
    public StandardResultBean recipeDownloadConfirmation(String appKey, List<Integer> recipeIds) {
        LOGGER.info("recipeDownloadConfirmation:{},{}.", appKey, JSONUtils.toString(recipeIds));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        return ObjectCopyUtils.convert(callService.recipeDownloadConfirmation(appKey, recipeIds), StandardResultBean.class);
    }

    @Override
    public StandardResultBean synchronizeInventory(Map<String, Object> parames) {
        LOGGER.info("synchronizeInventory:{}.", JSONUtils.toString(parames));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        return ObjectCopyUtils.convert(callService.synchronizeInventory(parames), StandardResultBean.class);
    }

    @Override
    public ThirdResultBean recordDrugStoreResult(Map<String, Object> paramMap) {
        LOGGER.info("recordDrugStoreResult:{}.", JSONUtils.toString(paramMap));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        ThirdResultBean thirdResultBean = new ThirdResultBean();
        recipe.bean.ThirdResultBean resultBean = callService.recordDrugStoreResult(paramMap);
        getResultMsg(thirdResultBean, resultBean);
        return thirdResultBean;
    }

    @Override
    public List<StandardResultBean> readjustDrugPrice(List<ReadjustDrugDTO> readjustDrugDTOS) {
        LOGGER.info("readjustDrugPrice:{}.", JSONUtils.toString(readjustDrugDTOS));
        StandardEnterpriseCallService callService = ApplicationUtils.getRecipeService(StandardEnterpriseCallService.class, "distributionService");
        return ObjectCopyUtils.convert(callService.readjustDrugPrice(ObjectCopyUtils.convert(readjustDrugDTOS, com.ngari.recipe.drugsenterprise.model.ReadjustDrugDTO.class)), StandardResultBean.class);
    }

    @Override
    public Integer scanStockEnterpriseForHis(Map<String, Object> paramMap) {
        LOGGER.info("scanStockEnterpriseForHis:{}.", JSONUtils.toString(paramMap));
        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
        return callService.scanStockEnterpriseForHis(paramMap);
    }

    private void getResultMsg(ThirdResultBean thirdResultBean, recipe.bean.ThirdResultBean resultBean) {
        thirdResultBean.setCode(resultBean.getCode());
        thirdResultBean.setBusId(resultBean.getBusId());
        thirdResultBean.setMsg(resultBean.getMsg());
    }

    @Autowired
    RecipeService recipeService;

    @Autowired
    RecipeDAO recipeDAO;

    @Override
    public Boolean saveSignRecipePDF(CaSignResultTo caSignResultTo) {
        LOGGER.info("saveSignRecipePDF caSignResultTo:{}", JSONUtils.toString(caSignResultTo));
        CaSignResultVo signResultVo = new CaSignResultVo();
        Integer recipeId = caSignResultTo.getRecipeId();
        String errorMsg = caSignResultTo.getMsg();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        signResultVo.setRecipeId(recipeId);
        boolean isDoctor = false;
        // 注意这里根据是否有药师的信息，有则为药师签章，没有则为医生签章
        if (null == recipe.getChecker()) {
            isDoctor = true;
        }

        try {
            if (null == recipe) {
                signResultVo.setCode(0);
                signResultVo.setResultCode(0);
                signResultVo.setMsg("未找到处方信息");
                return false;
            }
            if (!StringUtils.isEmpty(errorMsg)) {
                LOGGER.info("当前审核处方{}签名失败！errorMsg: {}", recipeId, errorMsg);
                signResultVo.setCode(0);
                signResultVo.setResultCode(0);
                signResultVo.setMsg(errorMsg);
                /*RecipeLogDAO recipeLogDAO = getDAO(RecipeLogDAO.class);
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.SIGN_ERROR_CODE_PHA, null);
                recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), errorMsg);*/
                return true;
            }

            signResultVo.setPdfBase64(caSignResultTo.getPdfBase64());
            signResultVo.setSignRecipeCode(caSignResultTo.getSignRecipeCode());
            signResultVo.setCode(200);
            // 如果签章数据为空，则表示CA未结束。目前只有上海胸科有签名签章，
            // 如果有异步的签名签章不全的，可以另外实现一个只有签名或只要签章的回调接口
            if (StringUtils.isEmpty(caSignResultTo.getPdfBase64())) {
                signResultVo.setResultCode(-1);
            } else {
                signResultVo.setResultCode(1);
            }

            /*DoctorService doctorService = AppDomainContext
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
            recipeService.signRecipeInfoSave(recipeId, isDoctor, resultVo, recipe.getClinicOrgan());*/
        } catch (Exception e) {
            LOGGER.error("saveSignRecipePDF error", e);
            return false;
        } finally {
            LOGGER.error("saveSignRecipePDF finally callback signResultVo={}", JSONUtils.toString(signResultVo));
            if (isDoctor) {
                recipeService.retryCaDoctorCallBackToRecipe(signResultVo);
            } else {
                recipeService.retryCaPharmacistCallBackToRecipe(signResultVo);
            }
        }
        return true;
    }

    @Override
    public void saveRecipeInfoForBjCa(CaSignResultTo caSignResultTo) {
        LOGGER.info("saveRecipeInfoForBjCa caSignResultTo=[{}]", JSONUtils.toString(caSignResultTo));
        // 保存ca相关信息即可
        if (caSignResultTo != null) {

            SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO = DAOFactory.getDAO(SignDoctorRecipeInfoDAO.class);

            SignDoctorRecipeInfo signDoctorRecipeInfo = new SignDoctorRecipeInfo();
            signDoctorRecipeInfo.setSignCodeDoc(caSignResultTo.getSignCADate());
            signDoctorRecipeInfo.setCaSerCodeDoc(caSignResultTo.getUserAccount());
            signDoctorRecipeInfo.setSignBefText(caSignResultTo.getPdfBase64());
            signDoctorRecipeInfo.setUniqueId(caSignResultTo.getUniqueId());

            signDoctorRecipeInfo.setType("BeijingYwxCa");

            signDoctorRecipeInfoDAO.save(signDoctorRecipeInfo);
        }
    }

    @Override
    public CaSealRequestTO signCreateRecipePDF(Integer recipeId, boolean isDoctor) {
        CaSealRequestTO caSealRequestTO = RecipeServiceEsignExt.signCreateRecipePDF(recipeId, isDoctor);
        return caSealRequestTO;
    }

    @Override
    public CaSignResultBean commonCASignAndSealOrganId(CaSealRequestTO requestSealTO, RecipeBean recipe, Integer organId, String userAccount, String caPassword) {
        CommonCAFactory commonCAFactory = ApplicationUtils.getRecipeService(CommonCAFactory.class);
        CAInterface caInterface = commonCAFactory.useCAFunction(organId);
        Recipe recipe1 = ObjectCopyUtils.convert(recipe, Recipe.class);
        CaSignResultVo resultVo = caInterface.commonCASignAndSeal(requestSealTO, recipe1, organId, userAccount, caPassword);
        return ObjectCopyUtils.convert(resultVo, CaSignResultBean.class);
    }

    @Override
    public void generateSignetRecipePdf(Integer recipeId, Integer organId) {
        RecipeBusiThreadPool.execute(new GenerateSignetRecipePdfRunable(recipeId, organId));
    }

    @Override
    public void pushRecipeToRegulation(Integer recipeId, Integer status) {
        //推送处方到监管平台(审核后数据)
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipeId, status));
    }

    @RpcService
    @Override
    public List<RecipeBean> findRecipeByFlag(List<Integer> organ, List<Integer> recipeIds, List<Integer> recipeTypes, int flag, int start, int limit) {
        List<Recipe> recipes = recipeDAO.findRecipeByFlag(organ, recipeIds, recipeTypes, flag, start, limit);
        //转换前端的展示实体类
        List<RecipeBean> recipeBeans = changBean(recipes, RecipeBean.class);
        return recipeBeans;
    }


    @Override
    public void doAfterCheckNotPassYs(RecipeBean recipeBean) {
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        recipeService.doAfterCheckNotPassYs(recipe);
    }

    @Override
    public void afterCheckPassYs(Integer auditMode, RecipeBean recipeBean) {
        AuditModeContext auditModeContext = AppContextHolder.getBean("auditModeContext", AuditModeContext.class);
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        auditModeContext.getAuditModes(auditMode).afterCheckPassYs(recipe);
    }

    @Override
    public void afterCheckNotPassYs(Integer auditMode, RecipeBean recipeBean) {
        AuditModeContext auditModeContext = AppContextHolder.getBean("auditModeContext", AuditModeContext.class);
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        auditModeContext.getAuditModes(auditMode).afterCheckNotPassYs(recipe);
    }

    @Override
    public int getAuditStatusByReviewType(int reviewType) {
        AuditModeContext auditModeContext = AppContextHolder.getBean("auditModeContext", AuditModeContext.class);
        return auditModeContext.getAuditModes(reviewType).afterAuditRecipeChange();
    }


    @Override
    public void batchSendMsg(RecipeBean recipe, int afterStatus) {
        Recipe recipe1 = ObjectCopyUtils.convert(recipe, Recipe.class);
        RecipeMsgService.batchSendMsg(recipe1, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
    }

    @Override
    public List<RecipeBean> searchRecipe(Set<Integer> organs, Integer searchFlag, String searchString, Integer start, Integer limit) {
        List<Recipe> recipes = recipeDAO.searchRecipe(organs, searchFlag, searchString, start, limit);
        //转换前端的展示实体类
        List<RecipeBean> recipeBeans = changBean(recipes, RecipeBean.class);
        return recipeBeans;
    }

    @Override
    public List<RecipeBean> findByRecipeAndOrganId(List<Integer> recipeIds, Set<Integer> organIds) {
        List<Recipe> recipes = null;
        if (CollectionUtils.isNotEmpty(organIds)) {
            recipes = recipeDAO.findByRecipeAndOrganId(recipeIds, organIds);
        } else {
            recipes =recipeDAO.findByRecipeIds(recipeIds);
        }
        //转换前端的展示实体类
        List<RecipeBean> recipeBeans = changBean(recipes, RecipeBean.class);
        return recipeBeans;
    }

    @Override
    public long getRecipeCountByFlag(List<Integer> organ, int flag) {
        return recipeDAO.getRecipeCountByFlag(organ, flag);
    }


    /**
     * 转换对象
     *
     * @param dataList
     * @param toClass
     * @param <T>      转换前
     * @param <T1>     转换后
     * @return
     */
    private <T, T1> List<T1> changBean(List<T> dataList, Class<T1> toClass) {
        List<T1> list = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(dataList)) {
            list = dataList.stream().map(t -> {
                T1 o = null;
                try {
                    o = toClass.getDeclaredConstructor().newInstance();
                    BeanUtils.copyProperties(t, o);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return o;
            }).collect(Collectors.toList());
        }
        return list;
    }

    @Override
    public List<EnterpriseRecipeDetailResponse> findRecipesPharmaceuticalDetailsByInfoForExcel(EnterpriseRecipeDetailExcelRequest req) {
        RecipeReportFormsService reportFormsService = ApplicationUtils.getRecipeService(RecipeReportFormsService.class);
        RecipeReportFormsRequest request = ObjectCopyUtils.convert(req, RecipeReportFormsRequest.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Long sum = recipeDAO.getCountByAll();
        request.setStart(0);
        request.setLimit(null != sum ? sum.intValue() : 0);
        Map<String, Object> resultMap = reportFormsService.enterpriseRecipeDetailList(request);
        return (null != resultMap && !resultMap.isEmpty()) ? (List<EnterpriseRecipeDetailResponse>) resultMap.get("data") : new ArrayList<EnterpriseRecipeDetailResponse>();
    }

    @Override
    public List<RecipeAccountCheckDetailResponse> findRecipesAccountCheckDetailsByInfoForExcel(RecipeAccountCheckDetailExcelRequest req) {
        RecipeReportFormsService reportFormsService = ApplicationUtils.getRecipeService(RecipeReportFormsService.class);
        RecipeReportFormsRequest request = ObjectCopyUtils.convert(req, RecipeReportFormsRequest.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Long sum = recipeDAO.getCountByAll();
        request.setStart(0);
        request.setLimit(null != sum ? sum.intValue() : 0);
        Map<String, Object> resultMap = reportFormsService.recipeAccountCheckDetailList(request);
        return (null != resultMap && !resultMap.isEmpty()) ? (List<RecipeAccountCheckDetailResponse>) resultMap.get("data") : new ArrayList<RecipeAccountCheckDetailResponse>();
    }

    @Override
    public List<RecipeHisAccountCheckResponse> recipeHisAccountCheckList(RecipeReportFormsRequest request) {
        RecipeReportFormsService reportFormsService = ApplicationUtils.getRecipeService(RecipeReportFormsService.class);
        Map<String, Object> result = reportFormsService.recipeHisAccountCheckList(request);
        return null != result ? (List<RecipeHisAccountCheckResponse>) result.get("data") : new ArrayList<RecipeHisAccountCheckResponse>();
    }

    @Override
    public Integer recipeStatusNotice(Map<String, Object> paramMap) {
        LOGGER.info("recipeStatusNotice paramMap:{}.", JSONUtils.toString(paramMap));
        String result = MapValueUtil.getString(paramMap, "result");
        if (StringUtils.isNotEmpty(result) && "success".equals(result)) {
            String prescriptionNo = MapValueUtil.getString(paramMap, "prescriptionNo"); //处方编码
            String orgCode = MapValueUtil.getString(paramMap, "orgCode"); //医疗机构编码
            String hosCode = MapValueUtil.getString(paramMap, "hosCode"); //院区编码
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(Integer.parseInt(prescriptionNo));
            if (recipe == null) {
                return 0;
            }

            //表示回调成功,需要查询处方状态并开始更新处方信息
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            HospitalReqTo hospitalReqTo = new HospitalReqTo();
            hospitalReqTo.setOrganId(recipe.getClinicOrgan());
            hospitalReqTo.setPrescriptionNo(prescriptionNo);
            hospitalReqTo.setOrgCode(orgCode);
            hospitalReqTo.setHosCode(hosCode);
            LOGGER.info("recipeStatusNotice hospitalReqTo:{}.", JSONUtils.toString(hospitalReqTo));
            HisResponseTO hisResponseTO = recipeEnterpriseService.queryRecipeStatus(hospitalReqTo);
            LOGGER.info("recipeStatusNotice hisResponseTO:{}.", JSONUtils.toString(hisResponseTO));
            if (hisResponseTO != null && hisResponseTO.isSuccess()) {
                Map map = hisResponseTO.getExtend();
                String payStatus = (String) map.get("payStatus");
                String orderStatus = (String) map.get("orderStatus");
                String writeoffStatus = (String) map.get("writeoffStatus");
                StringBuilder stringBuilder = new StringBuilder();
                //如果处方没有下单,则payStatus = null,由于不产生订单,现只将订单信息记录日志
                if (StringUtils.isNotEmpty(payStatus)) {
                    switch (payStatus) {
                        case "0":
                            stringBuilder.append("[支付状态]该订单未支付;");
                            break;
                        case "1":
                            stringBuilder.append("[支付状态]该订单已支付;");
                            break;
                        case "2":
                            stringBuilder.append("[支付状态]该订单已退款;");
                            break;
                        default:
                            stringBuilder.append("[支付状态]该处方未下单;");
                            break;
                    }
                }
                if (StringUtils.isNotEmpty(orderStatus)) {
                    switch (orderStatus) {
                        case "2":
                            stringBuilder.append("[订单状态]该订单已经被接单;");
                            break;
                        case "3":
                            stringBuilder.append("[订单状态]该订单已发货/已取药;");
                            break;
                        case "4":
                            Map<String, Object> attrMap = Maps.newHashMap();
                            attrMap.put("giveFlag", 1);
                            attrMap.put("payFlag", 1);
                            attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_SEND_TO_HOME);
                            attrMap.put("chooseFlag", 1);
                            attrMap.put("payDate", new Date());
                            //更新处方信息
                            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), RecipeStatusConstant.FINISH, attrMap);
                            stringBuilder.append("[订单状态]该订单已完成;");
                            break;
                        case "5":
                            stringBuilder.append("[订单状态]该订单已被取消;");
                            break;
                        case "6":
                            stringBuilder.append("[订单状态]该订单已退回;");
                            break;
                        default:
                            stringBuilder.append("[订单状态]该处方未下单;");
                            break;
                    }
                }

                if (StringUtils.isNotEmpty(writeoffStatus)) {
                    switch (writeoffStatus) {
                        case "0":
                            stringBuilder.append("[处方状态]该处方已审核;");
                            break;
                        case "1":
                            //处方已核销
                            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("status", RecipeStatusConstant.FINISH));
                            stringBuilder.append("[处方状态]该处方已核销;");
                            break;
                        case "2":
                            //该处方已失效
                            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("status", RecipeStatusConstant.NO_PAY));
                            stringBuilder.append("[处方状态]该处方已失效;");
                            break;
                        case "3":
                            //该处方已撤销
                            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("status", RecipeStatusConstant.REVOKE));
                            stringBuilder.append("[处方状态]该处方已撤销;");
                            break;
                        default:
                            break;
                    }
                }
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), stringBuilder.toString());
            }
        }
        return 1;
    }

    /**
     * 根据clinicId 查询复诊处方能否退费
     * select clinicid,count(*),group_concat(status) from cdr_recipe  c group by clinicid
     *
     * @return Map<String, Object>
     */
    @Override
    @RpcService
    public Map<String, Object> findRecipeCanRefundByClinicId(Map<String, String> params) {
        LOGGER.info("findRecipeCanRefundByClinicId 参数{}", JSONUtils.toString(params));
        if (StringUtils.isEmpty(params.get("clinicId"))) {
            throw new DAOException("findRecipeCanRefundByClinicId clinicId不允许为空");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findByClinicId(Integer.parseInt(params.get("clinicId")));
        Map<String, Object> map = Maps.newHashMap();
        String msg = "";
        String recipeStatusText = "";
        boolean canRefund = false;//默认不能申请退款
        //只有已取消状态或已撤销或审核不通过的处方才能申请退款 返回true  其余返回false
        try {
            if (recipes != null && recipes.size() > 0) {
                for (Recipe recipe : recipes) {
                    LOGGER.info("findRecipeCanRefundByClinicId status:[{}]", recipe.getStatus());
                    if (!(recipe.getStatus() == RecipeStatusConstant.HIS_FAIL          //11
                            || recipe.getStatus() == RecipeStatusConstant.NO_DRUG      //12
                            || recipe.getStatus() == RecipeStatusConstant.NO_PAY       //13
                            || recipe.getStatus() == RecipeStatusConstant.NO_OPERATOR  //14
                            || recipe.getStatus() == RecipeStatusConstant.EXPIRED      //20
                            || recipe.getStatus() == RecipeStatusConstant.RECIPE_FAIL  //17
                            || recipe.getStatus() == RecipeStatusConstant.RECIPE_MEDICAL_FAIL//19
                            || recipe.getStatus() == RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN//25
                            || recipe.getStatus() == RecipeStatusConstant.REVOKE      //9
                            || recipe.getStatus() == RecipeStatusConstant.CHECK_NOT_PASS//-1
                            || recipe.getStatus() == RecipeStatusConstant.CHECK_NOT_PASS_YS//15
                            || recipe.getStatus() == RecipeStatusConstant.UNSIGN)//0
                    ) {
                        String recipeStatusTextTmp = DictionaryController.instance().get("eh.cdr.dictionary.RecipeStatus").getText(recipe.getStatus());
                        if (StringUtils.isEmpty(recipeStatusText) || (!StringUtils.isEmpty(recipeStatusText) && !recipeStatusText.contains(recipeStatusTextTmp))) {
                            recipeStatusText += recipeStatusTextTmp + "、";
                        }
                    }
                }
                if (!StringUtils.isEmpty(recipeStatusText)) {
                    msg += "当前有处方处于" + recipeStatusText.substring(0, recipeStatusText.length() - 1) + "状态，不能退费";
                }
            }
        } catch (ControllerException e) {
            LOGGER.info("findRecipeCanRefundByClinicId {}", e);
        }
        if (StringUtils.isEmpty(msg)) {
            canRefund = true;
        }
        map.put("canRefund", canRefund);
        map.put("msg", msg);
        return map;
    }

    @RpcService
    @Override
    public List<RecipeRefundBean> findRefundListByRecipeId(Integer recipeId) {

        RecipeRefundDAO drugsEnterpriseDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        List<RecipeRefund> recipeRefunds = drugsEnterpriseDAO.findRefundListByRecipeId(recipeId);
        return changBean(recipeRefunds, RecipeRefundBean.class);
    }

    @Override
    @RpcService
    public List<RecipeBean> findReadyCheckRecipeByCheckMode(Integer checkMode) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findReadyCheckRecipeByCheckMode(checkMode);
        List<RecipeBean> recipeBeans = changBean(recipes, RecipeBean.class);
        if (CollectionUtils.isNotEmpty(recipeBeans)) {
            List<Integer> recipeIds = recipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
            Map<Integer, String> map = recipeExtends.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, RecipeExtend::getRegisterID));
            for (RecipeBean recipeBean : recipeBeans) {
                Integer recipeId = recipeBean.getRecipeId();
                recipeBean.setRegisterId(map.get(recipeId));
            }
        }
        return recipeBeans;
    }

    @RpcService
    @Override
    public List<Integer> queryRecipeIdByOrgan(List<Integer> organIds, List<Integer> recipeTypes, Integer type) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.queryRecipeIdByOrgan(organIds, recipeTypes, type);
    }

    @Override
    public List<RecipeBean> queryRecipeInfoByOrganAndRecipeType(List<Integer> organIds, List<Integer> recipeTypes) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.queryRecipeInfoByOrganAndRecipeType(organIds, recipeTypes);
        return ObjectCopyUtils.convert(recipes, RecipeBean.class);
    }

    @Override
    public List<RecipeDetailBean> findRecipeDetailsByRecipeIds(List<Integer> recipeIds) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        return ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);
    }

    @Override
    public String getRecipeParameterValue(String paramName) {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        return recipeParameterDao.getByName(paramName);
    }

    @RpcService
    @Override
    public void retryCaDoctorCallBackToRecipe(CaSignResultUpgradeBean resultVo) {
        CaSignResultVo caSignResultVo = makeCaSignResultVoFromCABean(resultVo);
        RecipeService service = ApplicationUtils.getRecipeService(RecipeService.class);
        service.retryCaDoctorCallBackToRecipe(caSignResultVo);
    }

    @RpcService
    @Override
    public void retryCaPharmacistCallBackToRecipe(CaSignResultUpgradeBean resultVo) {
        CaSignResultVo caSignResultVo = makeCaSignResultVoFromCABean(resultVo);
        RecipeService service = ApplicationUtils.getRecipeService(RecipeService.class);
        service.retryCaPharmacistCallBackToRecipe(caSignResultVo);
    }


    private CaSignResultVo makeCaSignResultVoFromCABean(CaSignResultUpgradeBean resultVo) {
        CaSignResultVo caSignResultVo = new CaSignResultVo();
        caSignResultVo.setResultCode(resultVo.getResultStatus());
        caSignResultVo.setSignPicture(resultVo.getSignPicture());
        caSignResultVo.setCertificate(resultVo.getCertificate());
        caSignResultVo.setSignCADate(resultVo.getSignDate());
        caSignResultVo.setSignRecipeCode(resultVo.getSignCode());
        caSignResultVo.setCode(resultVo.getMsgCode());
        caSignResultVo.setMsg(resultVo.getMsg());
        caSignResultVo.setEsignResponseMap(resultVo.getEsignResponseMap());
        caSignResultVo.setRecipeId(resultVo.getBussId());
        caSignResultVo.setBussType(resultVo.getBusstype());
        return caSignResultVo;
    }

}
