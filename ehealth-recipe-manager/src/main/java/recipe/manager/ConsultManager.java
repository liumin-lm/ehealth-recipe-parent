package recipe.manager;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.OutPatientRecordResTO;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.recipe.dto.OutPatientRecordResDTO;
import com.ngari.recipe.dto.PatientDTO;
import ctd.dictionary.DictionaryController;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.ConsultClient;
import recipe.client.DepartClient;
import recipe.client.PatientClient;

/**
 * 咨询管理类
 *
 * @author zgy
 * @date 2022/3/2 14:57
 */
@Service
public class ConsultManager extends BaseManager {
    @Autowired
    private PatientClient patientClient;
    /**
     * todo revisitManager？代码下移
     */
    @Autowired
    private RevisitManager revisitManager;
    @Autowired
    private ConsultClient consultClient;
    @Autowired
    private DepartClient departClient;

    /**
     * 获取有效门诊记录
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 门诊记录
     */
    public OutPatientRecordResDTO findOutPatientRecordFromHis(String mpiId, Integer organId, Integer doctorId) {
        logger.info("ConsultManager findOutPatientRecordFromHis mpiId={}",mpiId);
        OutPatientRecordResDTO outPatientRecordResDTO = new OutPatientRecordResDTO();
        PatientDTO patient = patientClient.getPatientDTO(mpiId);
        if (null != patient) {
            WriteDrugRecipeReqTO writeDrugRecipeReqTO = revisitManager.getWriteDrugRecipeReqTO(patient, organId, doctorId);
            if (null != writeDrugRecipeReqTO) {
                HisResponseTO<OutPatientRecordResTO> hisResponseTO = consultClient.findOutPatientRecordFromHis(writeDrugRecipeReqTO);
                outPatientRecordResDTO = assembleOutPatientRecord(hisResponseTO, organId);
            }
        }
        return outPatientRecordResDTO;
    }

    /**
     * 组装有效门诊记录返回数据
     *
     * @param hisResponseTO
     * @param organId
     * @return
     */
    public OutPatientRecordResDTO assembleOutPatientRecord(HisResponseTO<OutPatientRecordResTO> hisResponseTO, Integer organId) {
        logger.info("ConsultManager assembleOutPatientRecord hisResponseTO={}", JSONUtils.toString(hisResponseTO));
        OutPatientRecordResDTO outPatientRecordResDTO = new OutPatientRecordResDTO();
        try {
            if (null != hisResponseTO) {
                OutPatientRecordResTO response = hisResponseTO.getData();
                if(new Integer(1).equals(response.getMsgCode())){
                    outPatientRecordResDTO.setMsgCode(609);
                    outPatientRecordResDTO.setMsg("患者线下未建档，无法开具处方");
                    return outPatientRecordResDTO;
                }
                else if(new Integer(2).equals(response.getMsgCode())){
                    outPatientRecordResDTO.setMsgCode(609);
                    outPatientRecordResDTO.setMsg("未获取到门诊记录");
                    return outPatientRecordResDTO;
                }
                else if(new Integer(3).equals(response.getMsgCode())){
                    outPatientRecordResDTO.setMsgCode(609);
                    outPatientRecordResDTO.setMsg("未完成线下系统对接，无法获取门诊记录");
                    return outPatientRecordResDTO;
                }
                else if(new Integer(4).equals(response.getMsgCode())){
                    outPatientRecordResDTO.setMsgCode(609);
                    outPatientRecordResDTO.setMsg("其他未知错误");
                    return outPatientRecordResDTO;
                }
                String appointDepartCode = response.getAppointDepartCode();
                AppointDepartDTO appointDepartDTO = departClient.getAppointDepartByOrganIdAndAppointDepartCode(organId, appointDepartCode);
                if (null != appointDepartDTO) {
                    outPatientRecordResDTO.setAppointDepartId(appointDepartDTO.getAppointDepartId());
                    outPatientRecordResDTO.setAppointDepartCode(appointDepartCode);
                    outPatientRecordResDTO.setAppointDepartName(appointDepartDTO.getAppointDepartName());
                    String consultDepartText = DictionaryController.instance().get("eh.base.dictionary.Depart").getText(appointDepartDTO.getDepartId());
                    outPatientRecordResDTO.setConsultDepart(appointDepartDTO.getDepartId());
                    outPatientRecordResDTO.setConsultDepartText(consultDepartText);
                    outPatientRecordResDTO.setAppointDepartInDepartId(appointDepartDTO.getDepartId());
                }
                outPatientRecordResDTO.setCardId(response.getCardId());
                outPatientRecordResDTO.setCardType(response.getCardType());
                outPatientRecordResDTO.setRequestMode(1);
                outPatientRecordResDTO.setRecipeBusinessType(1);
                outPatientRecordResDTO.setRegisterNo(response.getRegisterNo());
                outPatientRecordResDTO.setMsgCode(200);
                logger.info("ConsultManager assembleOutPatientRecord outPatientRecordResDTO={}", JSONUtils.toString(outPatientRecordResDTO));
            }
        } catch (Exception e) {
            logger.error("ConsultManager assembleOutPatientRecord error",e);
        }
        return outPatientRecordResDTO;
    }

    /**
     * 处方开成功回写咨询更改处方id
     *
     * @param recipeId
     * @param clinicId
     */
    public void updateRecipeIdByConsultId(Integer recipeId, Integer clinicId) {
        consultClient.updateRecipeIdByConsultId(recipeId, clinicId);
    }
}
