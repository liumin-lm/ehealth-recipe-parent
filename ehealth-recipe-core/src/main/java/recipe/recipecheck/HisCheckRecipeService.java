//package recipe.recipecheck;
//
//import com.google.common.collect.Maps;
//import com.ngari.his.recipe.mode.NoticeHisRecipeInfoReq;
//import com.ngari.patient.dto.EmploymentDTO;
//import com.ngari.patient.dto.OrganDTO;
//import com.ngari.patient.service.BasicAPI;
//import com.ngari.patient.service.EmploymentService;
//import com.ngari.patient.service.OrganService;
//import com.ngari.recipe.entity.Recipe;
//import com.ngari.recipe.entity.RecipeCheck;
//import com.ngari.recipe.entity.RecipeCheckDetail;
//import com.ngari.recipe.entity.RecipeExtend;
//import ctd.persistence.DAOFactory;
//import ctd.persistence.exception.DAOException;
//import ctd.util.JSONUtils;
//import ctd.util.annotation.RpcBean;
//import ctd.util.annotation.RpcService;
//import eh.base.constant.ErrorCode;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import recipe.ApplicationUtils;
//import recipe.audit.auditmode.AuditModeContext;
//import recipe.constant.RecipeStatusConstant;
//import recipe.constant.RecipecCheckStatusConstant;
//import recipe.dao.RecipeDAO;
//import recipe.dao.RecipeExtendDAO;
//import recipe.hisservice.RecipeToHisMqService;
//import recipe.service.RecipeLogService;
//import recipe.service.RecipeMsgService;
//import recipe.service.RecipeService;
//import recipe.thread.PushRecipeToRegulationCallable;
//import recipe.thread.RecipeBusiThreadPool;
//import recipe.util.MapValueUtil;
//
//import javax.annotation.Resource;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
//import static ctd.persistence.DAOFactory.getDAO;
//
///**
// * @author yinsheng
// * @date 2020\3\17 0017 11:51
// * 第三方处方审核结果通知接口
// */
//@RpcBean("hisCheckRecipeService")
//public class HisCheckRecipeService implements IRecipeCheckService {
//    /**
//     * LOGGER
//     */
//    private static final Logger LOGGER = LoggerFactory.getLogger(HisCheckRecipeService.class);
//
//    @Resource
//    private AuditModeContext auditModeContext;
//
//    @RpcService
//    public void sendCheckRecipeInfo(Recipe recipe) {
//        LOGGER.info("HisCheckRecipeService.sendCheckRecipeInfo recipeId:{}.", recipe.getRecipeId());
//        //审方途径开处方的时候已经获取
//        if (new Integer(2).equals(recipe.getCheckMode())) {
//            OrganService organService = BasicAPI.getService(OrganService.class);
//            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
//            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
//            //表示为HIS审方
//            RecipeToHisMqService recipeToHisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
//            NoticeHisRecipeInfoReq notice = new NoticeHisRecipeInfoReq();
//            notice.setOrganId(recipe.getClinicOrgan());
//            OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
//            notice.setOrganizeCode(organDTO.getOrganizeCode());
//            notice.setRecipeID(recipe.getRecipeCode());
//            notice.setPlatRecipeID(recipe.getRecipeId() + "");
//            notice.setRegisterId(recipeExtend.getRegisterID());
//            notice.setPatientId(recipe.getPatientID());
//            notice.setRecipeStatus("5");
//            recipeToHisMqService.recipeStatusToHis(notice);
//        }
//    }
//
//    @Override
//    public Map<String, Object> saveCheckResult(Map<String, Object> paramMap) {
//        LOGGER.error("saveCheckResult paramMap : {}", JSONUtils.toString(paramMap));
//        Integer recipeId = MapValueUtil.getInteger(paramMap, "recipeId");
//        String result = MapValueUtil.getString(paramMap, "result");
//        Integer checkOrgan = MapValueUtil.getInteger(paramMap, "checkOrgan");
//        String auditDoctorCode = MapValueUtil.getString(paramMap, "auditDoctorCode");
//        String auditDoctorName = MapValueUtil.getString(paramMap, "auditDoctorName");
//        Date auditTime = MapValueUtil.getDate(paramMap, "auditTime");
//        String memo = MapValueUtil.getString(paramMap, "failMemo");
//        if (null == recipeId || StringUtils.isEmpty(result) || checkOrgan == null) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "params are needed");
//        }
//        Integer checker = 0;
//        if (StringUtils.isNotEmpty(auditDoctorCode)){
//            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
//            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(
//                    auditDoctorCode, checkOrgan);
//            if (employmentDTO != null){
//                checker = employmentDTO.getDoctorId();
//            }
//        }
//        Map<String, Object> resMap = Maps.newHashMap();
//        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
//        //更新保存审核结果
//        if (null == recipe.getStatus() || recipe.getStatus() != RecipeStatusConstant.READY_CHECK_YS) {
//            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方已被审核");
//        }
//
//        Map<String, Object> attrMap = Maps.newHashMap();
//        attrMap.put("checkDateYs", auditTime);
//        attrMap.put("checkOrgan", checkOrgan);
//        attrMap.put("checker", checker);
//        attrMap.put("checkFailMemo", (StringUtils.isEmpty(memo)) ? "" : memo);
//        attrMap.put("checkStatus", RecipecCheckStatusConstant.Check_Normal);
//
//        //保存审核记录和详情审核记录
//        RecipeCheck recipeCheck = new RecipeCheck();
//        //暂时设置成0
//        recipeCheck.setChecker(checker);
//        recipeCheck.setRecipeId(recipeId);
//        recipeCheck.setCheckOrgan(checkOrgan);
//        recipeCheck.setCheckDate(auditTime);
//        recipeCheck.setMemo((StringUtils.isEmpty(memo)) ? "" : memo);
//        recipeCheck.setCheckStatus(Integer.valueOf(result));
//        recipeCheck.setCheckerName(auditDoctorName);
//        List<RecipeCheckDetail> recipeCheckDetails = null;
//        RecipeCheckDAO recipeCheckDAO = getDAO(RecipeCheckDAO.class);
//
//        RecipeCheck oldRecipeCheck = recipeCheckDAO.getByRecipeId(recipeCheck.getRecipeId());
//        if (oldRecipeCheck != null) {
//            recipeCheck.setCheckId(oldRecipeCheck.getCheckId());
//            recipeCheckDAO.update(recipeCheck);
//        } else {
//            recipeCheckDAO.saveRecipeCheckAndDetail(recipeCheck,recipeCheckDetails);
//        }
//        int beforeStatus = recipe.getStatus();
//        String logMemo = "审核不通过(第三方平台，药师：" + auditDoctorName + "):" + memo;
//        int recipeStatus = RecipeStatusConstant.CHECK_NOT_PASS_YS;
//        if ("1".equals(result)) {
//            //根据审方模式改变状态
//            recipeStatus = auditModeContext.getAuditModes(recipe.getReviewType()).afterAuditRecipeChange();
//            logMemo = "审核通过(第三方平台，药师：" + auditDoctorName + ")";
//        }
//
//        boolean bl = recipeDAO.updateRecipeInfoByRecipeId(recipeId, recipeStatus, attrMap);
//        if (!bl) {
//            LOGGER.error("saveCheckResult update recipe[" + recipeId + "] error!");
//            resMap.put("msg", "更新处方审核信息失败");
//            return resMap;
//        }
//        //记录日志
//        RecipeLogService.saveRecipeLog(recipeId, beforeStatus, recipeStatus, logMemo);
//        //todo--这里没有做药师签名
//
//        //审方做异步处理
//        RecipeBusiThreadPool.submit(() -> {
//            if ("1".equals(result)) {
//                //审方成功
//                auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckPassYs(recipe);
//            } else {
//                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
//                //审核不通过后处理
//                auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckNotPassYs(recipe);
//                //记录日志
//                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核不通过处理完成");
//            }
//            return null;
//        });
//        //推送处方到监管平台(审核后数据)
//        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(), 2));
//        return resMap;
//    }
//}
