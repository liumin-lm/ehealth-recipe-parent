package recipe.serviceprovider.recipe.service;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.NoticePlatRecipeMedicalInfoReq;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipe.service.IRecipeService;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.RecipeUtil;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.drugsenterprise.TmdyfRemoteService;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.service.*;
import recipe.serviceprovider.BaseService;
import recipe.util.MapValueUtil;

import java.util.*;

/**
 * company: ngarihealth
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
        return get(recipeId);
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
                                              Integer depart, int start, int limit, List<Integer> organIds, Integer giveMode,Integer fromflag,Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findRecipesByInfo(organId, status, doctor, patientName, bDate, eDate, dateType, depart, start, limit, organIds, giveMode,fromflag,recipeId);
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
        return service.findRecipeAndDetailsAndCheckById(recipeId);
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
    @Override
    public List<Map> findRecipesByInfoForExcel(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate,
                                        final Date eDate, final Integer dateType, final Integer depart, List<Integer> organIds, Integer giveMode,
                                        Integer fromflag,Integer recipeId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findRecipesByInfoForExcel(organId,status,doctor,patientName,bDate,eDate,dateType,depart,organIds,giveMode,fromflag,recipeId);
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
}
