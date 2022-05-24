package recipe.atop.patient;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.patient.mode.QueryHospMedListRequestVO;
import com.ngari.his.visit.service.IRevisitOtherHosRecordService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.vo.MedicalInsuranceAuthInfoVO;
import com.ngari.recipe.vo.MedicalInsuranceAuthResVO;
import com.ngari.recipe.vo.OutPatientReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.patient.IPatientBusinessService;

import java.util.List;
import java.util.Map;

/**
 * 患者相关服务
 *
 * @author yinsheng
 * @date 2021\7\29 0029 19:57
 */
@RpcBean(value = "patientAtop")
public class PatientAtop extends BaseAtop {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientAtop.class);


    @Autowired
    private IPatientBusinessService recipePatientService;
    @Autowired
    private IRevisitOtherHosRecordService revisitOtherHosRecordService;
    @Autowired
    private PatientService patientService;

    /**
     * 校验当前就诊人是否有效 是否实名认证 就诊卡是否有效
     *
     * @param outPatientReqVO 当前就诊人信息
     * @return 枚举值
     */
    @RpcService
    public Integer checkCurrentPatient(OutPatientReqVO outPatientReqVO) {
        validateAtop(outPatientReqVO, outPatientReqVO.getMpiId());
        return recipePatientService.checkCurrentPatient(outPatientReqVO);
    }

    /**
     * 医保授权
     *
     * @param medicalInsuranceAuthInfoVO
     * @return
     */
    @RpcService
    public MedicalInsuranceAuthResVO medicalInsuranceAuth(MedicalInsuranceAuthInfoVO medicalInsuranceAuthInfoVO) {
        validateAtop(medicalInsuranceAuthInfoVO, medicalInsuranceAuthInfoVO.getMpiId());
        return recipePatientService.medicalInsuranceAuth(medicalInsuranceAuthInfoVO);
    }

    /**
     * 获取药品清单列表
     * @param requestVO
     * @return
     */
    @RpcService
    public List<Map<String,Object>> queryHospMedList(QueryHospMedListRequestVO requestVO){
        LOGGER.info("queryHospMedList requestVO[{}]",JSONUtils.toString(requestVO));
        PatientDTO patientDTO = patientService.get(requestVO.getMpiId());
        if(ValidateUtil.blankString(patientDTO.getCertificate())){
            LOGGER.error("queryHospMedList certificate is null, requestVO[{}]",JSONUtils.toString(requestVO));
        }
        requestVO.setIdentity(patientDTO.getCertificate());
        HisResponseTO response = revisitOtherHosRecordService.queryHospMedList(requestVO);
        LOGGER.info("queryMedUseList response[{}]",JSONUtils.toString(response));
        if (response == null || !"200".equals(response.getMsgCode())){
            LOGGER.error("queryHospMedList error requestVO[{}],hisResponseTO{}", JSONUtils.toString(requestVO),JSONUtils.toString(response));
            throw new DAOException("健康云错误");
        }
        return (List<Map<String,Object>>)response.getData();
    }

    /**
     * 获取药品详情
     * @param requestVO
     * @return
     */
    @RpcService
    public List<Map<String,Object>> queryMedUseList(QueryHospMedListRequestVO requestVO){
        LOGGER.info("queryMedUseList requestVO[{}]",JSONUtils.toString(requestVO));
        PatientDTO patientDTO = patientService.get(requestVO.getMpiId());
        if(ValidateUtil.blankString(patientDTO.getCertificate())){
            LOGGER.error("queryMedUseList certificate is null, requestVO[{}]",JSONUtils.toString(requestVO));
        }
        requestVO.setIdentity(patientDTO.getCertificate());
        HisResponseTO response = revisitOtherHosRecordService.queryMedUseList(requestVO);
        LOGGER.info("queryMedUseList response[{}]",JSONUtils.toString(response));
        if (response == null || !"200".equals(response.getMsgCode())){
            LOGGER.error("queryMedUseList error requestVO[{}],hisResponseTO{}", JSONUtils.toString(requestVO),JSONUtils.toString(response));
            throw new DAOException("健康云错误");
        }
        return (List<Map<String,Object>>)response.getData();
    }

}
