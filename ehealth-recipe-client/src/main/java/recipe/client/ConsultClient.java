package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.OutPatientRecordResTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.his.visit.mode.NeedPaymentRecipeReqTo;
import com.ngari.his.visit.mode.NeedPaymentRecipeResTo;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
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
