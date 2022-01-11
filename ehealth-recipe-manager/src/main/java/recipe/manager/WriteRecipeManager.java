package recipe.manager;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.Consult;
import com.ngari.his.recipe.mode.WriteDrugRecipeTO;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipe.model.WriteDrugRecipeBean;
import com.ngari.recipe.recipe.model.WriteDrugRecipeDTO;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DepartClient;
import recipe.client.PatientClient;
import recipe.client.WriteRecipeClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zgy
 * @date 2022/1/10 18:07
 */
@Service
public class WriteRecipeManager extends BaseManager{

    @Autowired
    private WriteRecipeClient writeRecipeClient;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private DepartClient departClient;

    /**
     * 获取院内门诊
     * @param mpiId 患者唯一标识
     * @param organId  机构ID
     * @param doctorId  医生ID
     * @return 院内门诊
     */
    public List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) throws Exception {
        List<String> namesList = Arrays.asList( "1", "2", "3", "6");
        ArrayList<String> cardTypes = new ArrayList<>(namesList);
        List<HealthCardDTO> healthCardDTOS = patientClient.queryCardsByParam(organId, mpiId, cardTypes);
        PatientDTO patient = patientClient.getPatientBeanByMpiId(mpiId);
        //组装获取院内门诊请求参数
        WriteDrugRecipeReqTO writeDrugRecipeReqTO = new WriteDrugRecipeReqTO();
        writeDrugRecipeReqTO.setPatId(patient.getPatId());
        writeDrugRecipeReqTO.setOrgId(organId);
        writeDrugRecipeReqTO.setDoctorId(doctorId);
        PatientDTO requestPatient = new PatientDTO();
        requestPatient.setPatientName(patient.getPatientName());
        logger.info("WriteRecipeManager findWriteDrugRecipeByRevisitFromHis writeDrugRecipeReqTO={}", JSONUtils.toString(writeDrugRecipeReqTO));
        HisResponseTO<List<WriteDrugRecipeTO>> writeDrugRecipeList= writeRecipeClient.findWriteDrugRecipeByRevisitFromHis(writeDrugRecipeReqTO);
        //组装院内门诊返回数据
        List<WriteDrugRecipeDTO> writeDrugRecipeDTOList = new ArrayList<>();
        List<WriteDrugRecipeTO> dataList = writeDrugRecipeList.getData();
        try {
            for(WriteDrugRecipeTO writeDrugRecipeTO : dataList){
                WriteDrugRecipeDTO writeDrugRecipeDTO = new WriteDrugRecipeDTO();
                WriteDrugRecipeBean writeDrugRecipeBean = new WriteDrugRecipeBean();
                Consult consult = writeDrugRecipeTO.getConsult();
                String appointDepartCode = consult.getAppointDepartCode();
                AppointDepartDTO appointDepartDTO = departClient.getAppointDepartByOrganIdAndAppointDepartCode(organId, appointDepartCode);
                if(null != appointDepartDTO.getDepartId()){
                    writeDrugRecipeBean.setAppointDepartInDepartId(appointDepartDTO.getDepartId());
                }
                writeDrugRecipeDTO.setPatient(patient);
                writeDrugRecipeDTO.setRequestPatient(requestPatient);
                writeDrugRecipeDTO.setConsult(consult);
                writeDrugRecipeDTO.setType(writeDrugRecipeTO.getType());
                writeDrugRecipeDTO.setWriteDrugRecipeBean(writeDrugRecipeBean);
                logger.info("WriteRecipeManager findWriteDrugRecipeByRevisitFromHis writeDrugRecipeDTO={}", JSONUtils.toString(writeDrugRecipeDTO));
                writeDrugRecipeDTOList.add(writeDrugRecipeDTO);
            }
        }catch (Exception e){
            logger.error("WriteRecipeManager findWriteDrugRecipeByRevisitFromHis error={}", JSONUtils.toString(e));
        }

        return writeDrugRecipeDTOList;
    }

}
