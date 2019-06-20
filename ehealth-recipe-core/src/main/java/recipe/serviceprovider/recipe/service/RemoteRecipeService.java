package recipe.serviceprovider.recipe.service;


import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.HisSendResTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeRollingInfoBean;
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
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.service.RecipeCheckService;
import recipe.service.RecipeListService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeService;
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
    public List<Integer> findReadyAuditRecipeIdsByOrganIds(List<Integer> organIds) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findReadyAuditRecipeIdsByOrganIds(organIds);
    }
}