package recipe.service;

import com.google.common.collect.Maps;
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
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.RecipeRefund;
import com.ngari.recipe.recipe.model.RecipeRefundBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipePatientRefundVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.dao.RecipeRefundDAO;
import recipe.dao.RecipeExtendDAO;

import java.text.SimpleDateFormat;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;


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
    public void applyForRecipeRefund(Integer recipeId, String applyReason) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if(recipe == null || recipe.getOrderCode() == null){
            LOGGER.error("applyForRecipeRefund-未获取到处方单信息. recipeId={}", recipeId.toString());
            throw new DAOException("未获取到处方订单信息！");
        }
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if(recipeOrder == null){
            LOGGER.error("applyForRecipeRefund-未获取到处方单信息. recipeId={}", recipeId.toString());
            throw new DAOException("未获取到处方订单信息！");
        }
        ApplicationForRefundVisitReqTO request = new ApplicationForRefundVisitReqTO();
        request.setOrganId(recipe.getClinicOrgan());
        request.setBusNo(recipeOrder.getTradeNo());
        request.setPatientId(recipe.getPatientID());
        request.setPatientName(recipe.getPatientName());
        request.setApplyReason(applyReason);

        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
        HisResponseTO<String> hisResult = service.applicationForRefundVisit(request);
        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
            LOGGER.info("applyForRecipeRefund-处方退费申请成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            //退费申请记录保存
            RecipeRefund recipeRefund = new RecipeRefund();
            recipeRefund.setTradeNo(recipeOrder.getTradeNo());
            recipeRefund.setPrice(recipeOrder.getActualPrice());
            recipeRefund.setNode(-1);
            recipeRefund.setApplyNo(hisResult.getData());
            recipeRefund.setReason(applyReason);
            recipeReFundSave(recipe, recipeRefund);
            RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.RECIPE_REFUND_APPLY);
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

        RecipeRefundDAO recipeRefundDao = DAOFactory.getDAO(RecipeRefundDAO.class);
        List<RecipeRefund> list = recipeRefundDao.findRefundListByRecipeId(recipeId);
        if(list == null && list.size() == 0){
            LOGGER.error("checkForRecipeRefund-未获取到处方退费信息. recipeId={}", recipeId);
            throw new DAOException("未获取到处方退费信息！");
        }

        CheckForRefundVisitReqTO request = new CheckForRefundVisitReqTO();
        request.setOrganId(recipe.getClinicOrgan());
        request.setApplyNoHis(list.get(0).getApplyNo());
        request.setBusNo(list.get(0).getTradeNo());
        request.setPatientId(recipe.getPatientID());
        request.setPatientName(recipe.getPatientName());
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
        if (null != doctorDTO) {
            request.setChecker(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(doctorDTO.getDoctorId(), recipe.getClinicOrgan(), recipe.getDepart()));
            request.setCheckerName(doctorDTO.getName());
        }
        request.setCheckStatus(checkStatus);
        request.setCheckNode("0");
        request.setCheckReason(checkReason);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        request.setCheckTime(formatter.format(new Date()));

        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
        HisResponseTO<String> hisResult = service.checkForRefundVisit(request);
        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
            LOGGER.info("checkForRecipeRefund-处方退费审核成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            //退费审核记录保存
            RecipeRefund recipeRefund = new RecipeRefund();
            recipeRefund.setNode(0);
            recipeRefund.setStatus(Integer.valueOf(checkStatus));
            recipeRefund.setReason(checkReason);
            recipeRefund.setTradeNo(list.get(0).getTradeNo());
            recipeRefund.setPrice(list.get(0).getPrice());
            recipeRefund.setApplyNo(hisResult.getData());
            recipeReFundSave(recipe, recipeRefund);
            if(2 == Integer.valueOf(checkStatus)){
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.RECIPE_REFUND_AUDIT_FAIL);
            }
        } else {
            LOGGER.error("checkForRecipeRefund-处方退费审核失败-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
            throw new DAOException("处方退费审核失败！" + hisResult.getMsg());
        }

    }


    /*
     * @description 退费记录保存
     * @author gmw
     * @date 2020/7/15
     * @param recipe 处方
     * @param recipeRefund 退费
     */
    @RpcService
    public void recipeReFundSave(Recipe recipe, RecipeRefund recipeRefund) {
        RecipeRefundDAO recipeRefundDao = DAOFactory.getDAO(RecipeRefundDAO.class);
        recipeRefund.setBusId(recipe.getRecipeId());
        recipeRefund.setOrganId(recipe.getClinicOrgan());
        recipeRefund.setMpiid(recipe.getMpiid());
        recipeRefund.setPatientName(recipe.getPatientName());
        recipeRefund.setDoctorId(recipe.getDoctor());
        String memo = null;
        try {
            memo = DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundNode").getText(recipeRefund.getNode()) +
                DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(recipeRefund.getStatus());
        } catch (ControllerException e) {
            LOGGER.error("recipeReFundSave-未获取到处方单信息. recipeId={}, node={}, recipeRefund={}", recipe, JSONUtils.toString(recipeRefund));
            throw new DAOException("退费相关字典获取失败");
        }
        recipeRefund.setMemo(memo);
//        switch(recipeRefund.getNode()){
//            case -1:
//                recipeRefund.setStatus(0);
//                recipeRefund.setMemo("患者发起退费申请");
//                break;
//            case 0:
//                break;
//            default:
//                break;
//        }
        if(recipeRefund.getNode() == -1){
            recipeRefund.setStatus(0);
            recipeRefund.setMemo("患者发起退费申请");
        }
        recipeRefund.setNode(recipeRefund.getNode());
        recipeRefund.setStatus(recipeRefund.getStatus());
        recipeRefund.setCheckTime(new Date());
        //保存记录
        recipeRefundDao.saveRefund(recipeRefund);

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
        request.setBusNo(applyNo);
        request.setPatientId(recipe.getPatientID());
        request.setPatientName(recipe.getPatientName());

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
    public List<RecipeRefundBean> findRecipeReFundRate(Integer recipeId) {
        RecipeRefundDAO recipeRefundDao = DAOFactory.getDAO(RecipeRefundDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<RecipeRefund> list = recipeRefundDao.findRefundListByRecipeId(recipeId);
        if(list == null || list.size() == 0){
            LOGGER.error("findRecipeReFundRate-未获取到处方退费信息. recipeId={}", recipeId);
            throw new DAOException("未获取到处方退费信息！");
        }
        List<RecipeRefundBean> result = new ArrayList<>();
        //医生审核后还需要获取医院his的审核状态
        if(list.get(0).getNode() >= 0 && list.get(0).getNode() != 9){
            RecipeRefund recipeRefund = null;
            try {
                FindRefundRecordResponseTO record = findRefundRecordfromHis(recipeId, list.get(0).getApplyNo());
                //当his的审核记录发生变更时才做记录
                if(null != record && !(list.get(0).getNode().equals(Integer.valueOf(record.getCheckNode()))
                                        && list.get(0).getStatus().equals(Integer.valueOf(record.getCheckStatus())))){
                    recipeRefund = ObjectCopyUtils.convert(list.get(0), RecipeRefund.class);
                    recipeRefund.setNode(Integer.valueOf(record.getCheckNode()));
                    recipeRefund.setStatus(Integer.valueOf(record.getCheckStatus()));
                    recipeRefund.setReason(record.getReason());
                    String memo = DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundNode").getText(record.getCheckNode()) +
                        DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(record.getCheckStatus());
                    recipeRefund.setMemo(memo);
                    recipeRefund.setCheckTime(null);
                    //保存记录
                    recipeRefundDao.saveRefund(recipeRefund);
                    //date 20200717
                    //添加推送逻辑
//                    if(9 == Integer.valueOf(record.getCheckNode())){
//                        if(1 == Integer.valueOf(record.getCheckStatus())){
//                            RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.RECIPE_REFUND_SUCC);
//                            //修改处方单状态
//                            recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.REVOKE, null);
//                        }
//                        if(2 == Integer.valueOf(record.getCheckStatus())){
//                            RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.RECIPE_REFUND_FAIL);
//
//                        }
//                    }
                    //将最新记录返回到前端
                    result.add(ObjectCopyUtils.convert(recipeRefund, RecipeRefundBean.class));
                }
            } catch (Exception e) {
                throw new DAOException(e);
            }
        }

        for(int i=0; i<list.size(); i++){
            if(list.get(i).getNode().equals(-1)){
                RecipeRefundBean recipeRefundBean = new RecipeRefundBean();
                recipeRefundBean.setBusId(list.get(i).getBusId());
                recipeRefundBean.setMemo("等待审核");
                result.add(recipeRefundBean);
            }
            RecipeRefundBean recipeRefundBean2 =ObjectCopyUtils.convert(list.get(i), RecipeRefundBean.class);
            recipeRefundBean2.setReason(null);
            result.add(recipeRefundBean2);
        }
        return result;

    }

    /*
     * @description 是否展示查看进度按钮
     * @author gmw
     * @date 2020/7/15
     * @param recipeId 处方序号
     * @return 是否展示
     */
    @RpcService
    public boolean refundRateShow(Integer recipeId) {
        RecipeRefundDAO recipeRefundDao = DAOFactory.getDAO(RecipeRefundDAO.class);
        List<RecipeRefund> list = recipeRefundDao.findRefundListByRecipeId(recipeId);
        if(list == null || list.size() == 0){
            return false;
        } else {
            return true;
        }
    }

    @RpcService
    public List<RecipePatientRefundVO> findPatientRefundRecipesByDoctorId(Integer doctorId, Integer refundType, int start, int limit) {
        List<RecipePatientRefundVO> result = new ArrayList<RecipePatientRefundVO>();
        //获取当前医生的退费处方列表，根据当前处方的开方医生审核列表获取当前退费最新的一条记录
        RecipeRefundDAO recipeRefundDAO = getDAO(RecipeRefundDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        List<Integer> recipeIds = recipeRefundDAO.findDoctorPatientRefundListByRefundType(doctorId, refundType, start, limit);
        List<Integer> noteList = new ArrayList<>();
        noteList.add(0);
        noteList.add(-1);
        Recipe recipe;
        RecipeOrder recipeOrder;
        for (Integer recipeId : recipeIds) {
            recipe = recipeDAO.getByRecipeId(recipeId);
            recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if(null == recipe || null == recipeOrder){
                LOGGER.warn("当前处方id{}，处方或者订单不存在！", recipeId);
                continue;
            }
            RecipePatientRefundVO recipePatientRefundVO = new RecipePatientRefundVO();

            //初始化对象
            initRecipeRefundVo(noteList, recipe, recipeOrder, recipeId, recipePatientRefundVO);


            result.add(recipePatientRefundVO);
        }
        return result;
    }

    private void initRecipeRefundVo(List<Integer> noteList, Recipe recipe, RecipeOrder recipeOrder, Integer recipeId, RecipePatientRefundVO recipePatientRefundVO) {
        RecipeRefundDAO recipeRefundDAO = getDAO(RecipeRefundDAO.class);
        recipePatientRefundVO.setBusId(recipeId);
        List<RecipeRefund> nodes = recipeRefundDAO.findRefundListByRecipeIdAndNodes(recipeId, noteList);
        for(RecipeRefund recipeRefund : nodes){
            recipePatientRefundVO.setDoctorId(recipeRefund.getDoctorId());
            if(0 == recipeRefund.getNode()){
                recipePatientRefundVO.setDoctorNoPassReason(recipeRefund.getReason());
            }
            recipePatientRefundVO.setPatientMpiid(recipe.getMpiid());
            recipePatientRefundVO.setPatientName(recipe.getPatientName());
            recipePatientRefundVO.setRefundPrice(recipeOrder.getActualPrice());
            if(-1 == recipeRefund.getNode()){
                recipePatientRefundVO.setRefundReason(recipeRefund.getReason());
                recipePatientRefundVO.setRefundDate(recipeRefund.getCheckTime());
            }

        }
        if(CollectionUtils.isNotEmpty(nodes)){
            try {
                recipePatientRefundVO.setRefundStatusMsg(DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(nodes.get(0).getStatus()));
            } catch (Exception e) {
                throw new DAOException(e);
            }
        }
        recipePatientRefundVO.setRefundStatus(nodes.get(0).getStatus());
    }

    @RpcService
    public RecipePatientRefundVO getPatientRefundRecipeByRecipeId(Integer busId) {

        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipePatientRefundVO recipePatientRefundVO = new RecipePatientRefundVO();
        List<Integer> noteList = new ArrayList<>();
        noteList.add(0);
        noteList.add(-1);
        Recipe recipe = recipeDAO.getByRecipeId(busId);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if(null == recipe || null == recipeOrder){
            LOGGER.warn("当前处方id{}，处方或者订单不存在！", busId);
            return null;
        }
        initRecipeRefundVo(noteList, recipe, recipeOrder, busId, recipePatientRefundVO);

        return recipePatientRefundVO;
    }

    //用户提交退费申请给医生
    @RpcService
    public Map<String, Object> startRefundRecipeToDoctor(Integer recipeId, String patientRefundReason){
        Map<String, Object> result = Maps.newHashMap();

        try {
            applyForRecipeRefund(recipeId, patientRefundReason);
        } catch (Exception e) {
            throw new DAOException(609,e.getMessage());
        }

        result.put("result", true);
        result.put("code", 200);
        return result;
    }

    //医生端审核患者退费，通过不通过的
    @RpcService
    public Map<String, Object> doctorCheckRefundRecipe(Integer busId, Boolean checkResult, String doctorNoPassReason){
        Map<String, Object> result = Maps.newHashMap();
        try {
            checkForRecipeRefund(busId,checkResult ? "1" : "2",doctorNoPassReason);
        } catch (Exception e) {
            throw new DAOException(609,e.getMessage());
        }
        result.put("result", true);
        result.put("code", 200);
        return result;
    }
}
