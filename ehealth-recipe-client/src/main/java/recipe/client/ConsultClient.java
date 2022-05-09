package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.model.ConsultRegistrationNumberResultVO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultRedisService;
import com.ngari.his.recipe.mode.OutPatientRecordResTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.his.visit.mode.NeedPaymentRecipeReqTo;
import com.ngari.his.visit.mode.NeedPaymentRecipeResTo;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
import com.ngari.patient.dto.ConsultSetDTO;
import com.ngari.patient.service.ConsultSetService;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.ValidateUtil;

/**
 * 咨询相关服务
 *
 * @Author liumin
 * @Date 2022/1/10 下午2:26
 * @Description
 */
@Service
public class ConsultClient extends BaseClient {
    @Autowired
    private IConsultExService consultExService;
    @Autowired
    private IRecipeHisService iRecipeHisService;
    @Autowired
    private ConsultSetService consultSetService;
    @Autowired
    private IConsultRedisService iConsultRedisService;

    public void getConsult(Integer consultId) {
        logger.info("ConsultClient getConsult consultId={}", consultId);
        ConsultRegistrationNumberResultVO consult = iConsultRedisService.getConsultRegistrationNumber(consultId);
        logger.info("ConsultClient getConsult consult={}", JSON.toJSONString(consult));
    }

    /**
     * 根据医生id获取开靶向药的权限
     *
     * @param doctorId
     * @return
     */
    public Boolean getTargetedDrugTypeRecipeRight(Integer doctorId) {
        logger.info("ConsultClient getTargetedDrugTypeRecipeRight doctorId={}", JSON.toJSONString(doctorId));
        ConsultSetDTO consultSetDTO = consultSetService.getBeanByDoctorId(doctorId);
        logger.info("ConsultClient getTargetedDrugTypeRecipeRight consultSetDTO={}", JSON.toJSONString(consultSetDTO));
        boolean targetedDrugTypeRecipeRight = null == consultSetDTO.getTargetedDrugTypeRecipeRight() ? false : consultSetDTO.getTargetedDrugTypeRecipeRight();
        return targetedDrugTypeRecipeRight;
    }

    /**
     * 向门诊获取代缴费用
     *
     * @param needPaymentRecipeReqTo
     * @return
     */
    public NeedPaymentRecipeResTo getRecipePaymentFee(NeedPaymentRecipeReqTo needPaymentRecipeReqTo) {
        logger.info("ConsultClient getRecipePaymentFee needPaymentRecipeReqTo={}", JSON.toJSONString(needPaymentRecipeReqTo));
        NeedPaymentRecipeResTo response = null;
        try {
            HisResponseTO<NeedPaymentRecipeResTo> hisResponseTO = iRecipeHisService.getRecipePaymentFee(needPaymentRecipeReqTo);
            response = this.getResponse(hisResponseTO);
        } catch (Exception e) {
            logger.error("ConsultClient getRecipePaymentFee error ", e);
        }
        logger.info("ConsultClient getRecipePaymentFee res={}", JSON.toJSONString(response));

        return response;
    }


    /**
     * 根据单号获取网络门诊信息
     *
     * @param clinicId 业务单号
     * @return 网络门诊信息
     */
    public ConsultExDTO getConsultExByClinicId(Integer clinicId) {
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return null;
        }
        logger.info("ConsultClient getByClinicId param clinicId:{}", clinicId);
        ConsultExDTO consultExDTO = consultExService.getByConsultId(clinicId);
        logger.info("ConsultClient getByClinicId res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }

    /**
     * 获取有效门诊记录
     *
     * @param writeDrugRecipeReqTO 获取有效门诊记录请求入参
     * @return 门诊记录
     */
    public HisResponseTO<OutPatientRecordResTO> findOutPatientRecordFromHis(WriteDrugRecipeReqTO writeDrugRecipeReqTO) {
        logger.info("ConsultClient findOutPatientRecordFromHis writeDrugRecipeReqTO={}", JSON.toJSONString(writeDrugRecipeReqTO));
        HisResponseTO<OutPatientRecordResTO> hisResponseTO = new HisResponseTO<>();
        try {
            hisResponseTO = iRecipeHisService.findOutPatientRecordFromHis(writeDrugRecipeReqTO);
        } catch (Exception e) {
            logger.error("ConsultClient findOutPatientRecordFromHis error ", e);
        }
        return hisResponseTO;
    }
}
