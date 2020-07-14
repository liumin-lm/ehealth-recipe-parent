package recipe.service;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.visit.mode.ApplicationForRefundVisitReqTO;
import com.ngari.his.visit.mode.CheckForRefundVisitReqTO;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.PatientService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
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


    @RpcService
    public void applicationForRefundVisit(Integer consultId, String applyReason) {
//        ConsultDAO consultDAO = DAOFactory.getDAO(ConsultDAO.class);
//        ConsultExDAO consultExDAO = DAOFactory.getDAO(ConsultExDAO.class);
//        Consult consult = consultDAO.getById(consultId);
//        ConsultEx consultEx = consultExDAO.getByConsultId(consultId);
//        if(consult == null){
//            LOGGER.error("applicationForRefundVisit-未获取到复诊单信息. consultId={}", consultId.toString());
//            throw new DAOException("未获取到复诊单信息！");
//        }
//        ApplicationForRefundVisitReqTO request = new ApplicationForRefundVisitReqTO();
//        request.setOrganId(consult.getConsultOrgan());
//        //        request.setBusNo(consult.getOutTradeNo());
//        if(consultEx != null){
//            request.setBusNo(consultEx.getHisSettlementNo());
//            request.setCardNo(consultEx.getCardId());
//            request.setCardType(consultEx.getCardType());
//        } else{
//            LOGGER.error("applicationForRefundVisit-未获取到复诊单扩展信息. consultId={}", consultId.toString());
//        }
//        PatientService patientService = BasicAPI.getService(PatientService.class);
//        PatientDTO patientBean = patientService.get(consult.getMpiid());
//        request.setIdCard(patientBean.getCardId());
//        request.setMobile(patientBean.getMobile());
//        request.setPatientName(consult.getMpiName());
//        request.setApplyReason(applyReason);
//
//        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
//        HisResponseTO<String> hisResult = service.applicationForRefundVisit(request);
//        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
//            LOGGER.info("applicationForRefundVisit-复诊退费申请成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
//            if (hisResult.getData() != null) {
//                consultExDAO.updateHisRefundApplicationNoByConsultId(hisResult.getData(), consultId);
//            }
//        } else {
//            LOGGER.error("applicationForRefundVisit-复诊退费申请失败-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
//            String msg = "";
//            if(hisResult != null && hisResult.getMsg() != null){
//                msg = hisResult.getMsg();
//            }
//            throw new DAOException("复诊退费申请失败！" + msg);
//        }
    }

    @RpcService
    public void checkForRefundVisit(Integer consultId, String checkStatus, String checkReason) {
//        ConsultDAO consultDAO = DAOFactory.getDAO(ConsultDAO.class);
//        ConsultExDAO consultExDAO = DAOFactory.getDAO(ConsultExDAO.class);
//        Consult consult = consultDAO.getById(consultId);
//        ConsultEx consultEx = consultExDAO.getByConsultId(consultId);
//        if(consult == null){
//            log.error("checkForRefundVisit-未获取到复诊单信息. consultId={}", consultId.toString());
//            throw new DAOException("未获取到复诊单信息！");
//        }
//        CheckForRefundVisitReqTO request = new CheckForRefundVisitReqTO();
//        request.setOrganId(consult.getConsultOrgan());
//        request.setApplyNoHis(consultEx.getHisRefundApplicationNo());
//        request.setPatientName(consult.getMpiName());
//        if(consultEx != null){
//            request.setBusNo(consultEx.getHisSettlementNo());
//            request.setCardNo(consultEx.getCardId());
//            request.setCardType(consultEx.getCardType());
//        } else{
//            log.error("checkForRefundVisit-未获取到复诊单扩展信息. consultId={}", consultId.toString());
//        }
//        PatientService patientService = BasicAPI.getService(PatientService.class);
//        PatientDTO patientBean = patientService.get(consult.getMpiid());
//        request.setIdCard(patientBean.getCardId());
//        request.setMobile(patientBean.getMobile());
//        request.setPatientName(consult.getMpiName());
//        Integer doctorId = null;
//        Integer departId = null;
//        if(consult.getExeDoctor() == null){
//            doctorId = consult.getConsultDoctor();
//            departId = consult.getConsultDepart();
//        } else {
//            doctorId = consult.getExeDoctor();
//            departId = consult.getExeDepart();
//        }
//        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
//        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
//        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
//        if (null != doctorDTO) {
//            request.setChecker(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(doctorDTO.getDoctorId(), consult.getConsultOrgan(), departId));
//        }
//        request.setCheckerName(doctorDTO.getName());
//        request.setCheckStatus(checkStatus);
//        request.setCheckNode("0");
//        request.setCheckReason(checkReason);
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH:mm:ss");
//        request.setCheckTime(formatter.format(new Date()));
//
//        IVisitService service = AppContextHolder.getBean("his.visitService", IVisitService.class);
//        HisResponseTO<String> hisResult = service.checkForRefundVisit(request);
//        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
//            log.info("checkForRefundVisit-复诊退费审核成功-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
//        } else {
//            log.error("checkForRefundVisit-复诊退费审核失败-his. param={},result={}", JSONUtils.toString(request), JSONUtils.toString(hisResult));
//            throw new DAOException("复诊退费审核失败！" + hisResult.getMsg());
//        }

    }
}
