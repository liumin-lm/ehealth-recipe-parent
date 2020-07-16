package recipe.service;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.visit.mode.ApplicationForRefundVisitReqTO;
import com.ngari.his.visit.mode.CheckForRefundVisitReqTO;
import com.ngari.his.visit.mode.FindRefundRecordReqTO;
import com.ngari.his.visit.mode.FindRefundRecordResponseTO;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeRefund;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeRefundDAO;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 处方退费
 * company: ngarihealth
 *
 * @author: gaomw
 * @date:20200714
 */
@RpcBean("recipeRefundService")
public class RecipeRefundService extends RecipeBaseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRefundService.class);

    /*
     * @description 向his申请处方退费接口
     * @author gmw
     * @date 2020/7/15
     * @param recipeId 处方序号
     * @param applyReason 申请原因
     * @return 申请序号
     */
    @RpcService
    public String applyForRecipeRefund(Integer recipeId, String applyReason) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if(recipe == null){
            LOGGER.error("applicationForRefundVisit-未获取到处方单信息. recipeId={}", recipeId.toString());
            throw new DAOException("未获取到处方单信息！");
        }
        ApplicationForRefundVisitReqTO request = new ApplicationForRefundVisitReqTO();
        request.setOrganId(recipe.getClinicOrgan());
        request.setBusNo(recipe.getTradeNo());
        request.setPatientName(recipe.getPatientID());
        request.setPatientName(recipe.getPatientName());
        request.setApplyReason(applyReason);

        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
        HisResponseTO<String> hisResult = service.applicationForRefundVisit(request);
        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
            LOGGER.info("applyForRecipeRefund-处方退费申请成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            return hisResult.getData();
        } else {
            LOGGER.error("applyForRecipeRefund-处方退费申请失败-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            String msg = "";
            if(hisResult != null && hisResult.getMsg() != null){
                msg = hisResult.getMsg();
            }
            throw new DAOException("处方退费申请失败！" + msg);
        }
    }

    /*
     * @description 向his提交处方退费医生审核结果接口
     * @author gmw
     * @date 2020/7/15
     * @param recipeId 处方序号
     * @param checkStatus 审核状态
     * @param checkReason 审核原因
     * @return 申请序号
     */
    @RpcService
    public void checkForRecipeRefund(Integer recipeId, String checkStatus, String checkReason) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if(recipe == null){
            LOGGER.error("checkForRecipeRefund-未获取到处方单信息. recipeId={}", recipeId.toString());
            throw new DAOException("未获取到处方单信息！");
        }
        CheckForRefundVisitReqTO request = new CheckForRefundVisitReqTO();
        request.setOrganId(recipe.getClinicOrgan());
//        request.setApplyNoHis(recipeEx.getHisRefundApplicationNo());
        request.setBusNo(recipe.getTradeNo());
        request.setPatientName(recipe.getPatientID());
        request.setPatientName(recipe.getPatientName());
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
        if (null != doctorDTO) {
            request.setChecker(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(doctorDTO.getDoctorId(), recipe.getClinicOrgan(), recipe.getDepart()));
        }
        request.setCheckerName(doctorDTO.getName());
        request.setCheckStatus(checkStatus);
        request.setCheckNode("0");
        request.setCheckReason(checkReason);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        request.setCheckTime(formatter.format(new Date()));

        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
        HisResponseTO<String> hisResult = service.checkForRefundVisit(request);
        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
            LOGGER.info("checkForRecipeRefund-处方退费审核成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
        } else {
            LOGGER.error("checkForRecipeRefund-处方退费审核失败-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            throw new DAOException("处方退费审核失败！" + hisResult.getMsg());
        }

    }

    /*
     * @description 向his查询退费记录
     * @author gmw
     * @date 2020/7/15
     * @param recipeId 处方序号
     * @return 申请序号
     */
    @RpcService
    public FindRefundRecordResponseTO findRefundRecordfromHis(Integer recipeId, String applyNo) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if(recipe == null){
            LOGGER.error("findRefundRecordfromHis-未获取到处方单信息. recipeId={}", recipeId.toString());
            throw new DAOException("未获取到处方单信息！");
        }
        FindRefundRecordReqTO request = new FindRefundRecordReqTO();
        request.setOrganId(recipe.getClinicOrgan());
        request.setBusNo(recipe.getTradeNo());
        request.setPatientName(recipe.getPatientID());
        request.setPatientName(recipe.getPatientName());
        request.setBusNo(recipe.getTradeNo());

        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
        HisResponseTO<FindRefundRecordResponseTO> hisResult = service.findRefundRecord(request);
        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
            LOGGER.info("findRefundRecordfromHis-获取his退费记录成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            return hisResult.getData();
        } else {
            LOGGER.error("findRefundRecordfromHis-获取his退费记录失败-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            String msg = "";
            if(hisResult != null && hisResult.getMsg() != null){
                msg = hisResult.getMsg();
            }
            throw new DAOException("获取医院退费记录失败！" + msg);
        }
    }

    /*
     * @description 查询退费进度
     * @author gmw
     * @date 2020/7/15
     * @param recipeId 处方序号
     * @return 申请序号
     */
    @RpcService
    public List<RecipeRefund> findRecipeReFundRate(Integer recipeId) {
        RecipeRefundDAO recipeRefundDao = DAOFactory.getDAO(RecipeRefundDAO.class);
        List<RecipeRefund> list = recipeRefundDao.findRefundListByRecipeId(recipeId);
        if(list == null && list.size() == 0){
            LOGGER.error("findRecipeReFundRate-未获取到处方退费信息. recipeId={}", recipeId);
            throw new DAOException("未获取到处方退费信息！");
        }
        List<RecipeRefund> result = new ArrayList<>();
        //医生审核后还需要获取医院his的审核状态
        if(list.get(0).getNode() > 0 && list.get(0).getNode() != 9){
            RecipeRefund recipeRefund = null;
            try {
                FindRefundRecordResponseTO record = findRefundRecordfromHis(recipeId, list.get(0).getApplyNo());
                if(null != record){
                    //保存记录
                    recipeRefund = ObjectCopyUtils.convert(list.get(0), RecipeRefund.class);
                    recipeRefund.setNode(Integer.valueOf(record.getCheckNode()));
                    recipeRefund.setStatus(Integer.valueOf(record.getCheckStatus()));
                    recipeRefund.setMemo(record.getReason());
                    recipeRefundDao.saveRefund(recipeRefund);
                }
            } catch (Exception e) {
                throw new DAOException(e.getMessage());
            }

        }
//        for(){
//
//        }
        return result;

    }
}
