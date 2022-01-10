package recipe.atop.doctor;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.WriteDrugRecipeTO;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.patient.dto.PatientDTO;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.atop.BaseAtop;
import recipe.core.api.patient.IPatientBusinessService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开处方服务入口类
 *
 * @author fuzi
 */
@RpcBean("writeRecipeDoctorAtop")
public class WriteRecipeDoctorAtop extends BaseAtop {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteRecipeDoctorAtop.class);

    @Autowired
    private IPatientBusinessService recipePatientService;

    @RpcService
    @LogRecord
    public List<Map<String, Object>> findWriteDrugRecipeByRevisitFromHis(String mpid, Integer orgId, Integer doctorId) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<>();
        try {
            PatientDTO patient = recipePatientService.getPatientDTOByMpiID(mpid);
            LOGGER.info("WriteRecipeDoctorAtop findWriteDrugRecipeByRevisitFromHis patient={}", JSONUtils.toString(patient));
            IVisitService iVisitService = AppContextHolder.getBean("his.visitService", IVisitService.class);
            HisResponseTO<List<WriteDrugRecipeTO>> hisResponseTOList = new HisResponseTO<>();
            if (null != patient.getPatId()) {
                hisResponseTOList = iVisitService.findWriteDrugRecipeByRevisitFromHis(patient.getPatId(), orgId, doctorId);
            }
            for (WriteDrugRecipeTO hisResponseTO : hisResponseTOList.getData()) {
                map.put("patient", patient);
                PatientDTO patientDTO = new PatientDTO();
                if (null != patient.getPatientName()) {
                    patientDTO.setPatientName(patient.getPatientName());
                }
                map.put("requestPatient", patientDTO);
                map.put("consult", hisResponseTO.getConsult());
                map.put("type", hisResponseTO.getType());
                mapList.add(map);
            }
        } catch (Exception e) {
            LOGGER.error("WriteRecipeDoctorAtop findWriteDrugRecipeByRevisitFromHis error={}", JSONUtils.toString(e));
        }
        LOGGER.info("WriteRecipeDoctorAtop findWriteDrugRecipeByRevisitFromHis map={}", JSONUtils.toString(map));
        return mapList;
    }
}
