package recipe.manager;

import com.ngari.common.dto.RevisitTracesMsg;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.Consult;
import com.ngari.his.recipe.mode.WriteDrugRecipeTO;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.ConsultDTO;
import com.ngari.recipe.dto.WriteDrugRecipeBean;
import com.ngari.recipe.dto.WriteDrugRecipeDTO;
import com.ngari.recipe.entity.Recipe;
import ctd.dictionary.DictionaryController;
import ctd.net.broadcast.MQHelper;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DepartClient;
import recipe.client.PatientClient;
import recipe.client.RevisitClient;
import recipe.common.OnsConfig;
import recipe.constant.RecipeSystemConstant;
import recipe.util.ObjectCopyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 复诊处理通用类
 *
 * @author fuzi
 */
@Service
public class RevisitManager extends BaseManager {
    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private DepartClient departClient;

    /**
     * 通知复诊——添加处方追溯数据
     *
     * @param recipe
     */
    public void saveRevisitTracesList(Recipe recipe) {
        try {
            if (recipe == null) {
                logger.info("saveRevisitTracesList recipe is null ");
                return;
            }
            if (recipe.getClinicId() == null || 2 != recipe.getBussSource()) {
                logger.info("saveRevisitTracesList return param:{}", JSONUtils.toString(recipe));
                return;
            }
            RevisitTracesMsg revisitTracesMsg = new RevisitTracesMsg();
            revisitTracesMsg.setOrganId(recipe.getClinicOrgan());
            revisitTracesMsg.setConsultId(recipe.getClinicId());
            revisitTracesMsg.setBusId(recipe.getRecipeId().toString());
            revisitTracesMsg.setBusType(1);
            revisitTracesMsg.setBusNumOrder(10);
            revisitTracesMsg.setBusOccurredTime(recipe.getCreateDate());
            try {
                logger.info("saveRevisitTracesList sendMsgToMq send to MQ start, busId:{}，revisitTracesMsg:{}", recipe.getRecipeId(), JSONUtils.toString(revisitTracesMsg));
                MQHelper.getMqPublisher().publish(OnsConfig.revisitTraceTopic, revisitTracesMsg, null);
                logger.info("saveRevisitTracesList sendMsgToMq send to MQ end, busId:{}", recipe.getRecipeId());
            } catch (Exception e) {
                logger.error("saveRevisitTracesList sendMsgToMq can't send to MQ,  busId:{}", recipe.getRecipeId(), e);
            }
        } catch (Exception e) {
            logger.error("RevisitClient saveRevisitTracesList error recipeId:{}", recipe.getRecipeId(), e);
            e.printStackTrace();
        }
    }

    /**
     * 获取医生下同一个患者 最新 复诊的id
     *
     * @param mpiId        患者id
     * @param doctorId     医生id
     * @param isRegisterNo 是否存在挂号序号
     * @return 复诊id
     */
    public Integer getRevisitId(String mpiId, Integer doctorId, Boolean isRegisterNo) {
        if (isRegisterNo) {
            //获取存在挂号序号的复诊id
            return revisitClient.getRevisitIdByRegisterNo(mpiId, doctorId, RecipeSystemConstant.CONSULT_TYPE_RECIPE, true);
        }
        //获取最新的复诊id
        return revisitClient.getRevisitIdByRegisterNo(mpiId, doctorId, null, null);
    }

    /**
     * 获取院内门诊
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 院内门诊
     */
    public List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) {
        PatientDTO patient = patientClient.getPatientBeanByMpiId(mpiId);
        if (null != patient) {
            WriteDrugRecipeReqTO writeDrugRecipeReqTO = getWriteDrugRecipeReqTO(patient, organId, doctorId);
            if (null != writeDrugRecipeReqTO) {
                HisResponseTO<List<WriteDrugRecipeTO>> writeDrugRecipeList = revisitClient.findWriteDrugRecipeByRevisitFromHis(writeDrugRecipeReqTO);
                return convertWriteDrugRecipeDTO(writeDrugRecipeList, patient, organId);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 组装获取院内门诊请求参数
     *
     * @param patient
     * @param organId
     * @param doctorId
     * @return
     */
    public WriteDrugRecipeReqTO getWriteDrugRecipeReqTO(PatientDTO patient, Integer organId, Integer doctorId) {
        logger.info("RevisitManager writeDrugRecipeReqTO patient={},organId={},doctorId={}", JSONUtils.toString(patient), JSONUtils.toString(organId), JSONUtils.toString(doctorId));
        List<HealthCardDTO> healthCardDTOList = new ArrayList<>();
        //出参对象
        WriteDrugRecipeReqTO writeDrugRecipeReqTO = new WriteDrugRecipeReqTO();
        try {
            healthCardDTOList = patientClient.queryCardsByParam(organId, patient.getMpiId(), new ArrayList<>(Arrays.asList("1", "2", "3", "6")));
            logger.info("queryCardsByParam res:{}", JSONUtils.toString(healthCardDTOList));
        } catch (Exception e) {
            logger.error("queryCardsByParam 获取卡号错误", e);
        }
        //组装获取院内门诊请求参数
        if (CollectionUtils.isEmpty(healthCardDTOList)) {
            return writeDrugRecipeReqTO;
        }
        writeDrugRecipeReqTO.setHealthCardDTOList(healthCardDTOList);
        writeDrugRecipeReqTO.setOrganId(organId);
        writeDrugRecipeReqTO.setDoctorId(doctorId);
        writeDrugRecipeReqTO.setPatientName(patient.getPatientName());
        logger.info("RevisitManager writeDrugRecipeReqTO={}", JSONUtils.toString(writeDrugRecipeReqTO));
        return writeDrugRecipeReqTO;
    }

    /**
     * 组装院内门诊返回数据
     *
     * @param writeDrugRecipeList
     * @param patient
     * @param organId
     * @return
     */
    public List<WriteDrugRecipeDTO> convertWriteDrugRecipeDTO(HisResponseTO<List<WriteDrugRecipeTO>> writeDrugRecipeList, PatientDTO patient, Integer organId) {
        com.ngari.recipe.dto.PatientDTO patientDTO = ObjectCopyUtils.convert(patient, com.ngari.recipe.dto.PatientDTO.class);
        PatientDTO requestPatient = new PatientDTO();
        requestPatient.setPatientName(patient.getPatientName());
        com.ngari.recipe.dto.PatientDTO requestPatientDTO = ObjectCopyUtils.convert(requestPatient, com.ngari.recipe.dto.PatientDTO.class);
        //组装院内门诊返回数据
        List<WriteDrugRecipeDTO> writeDrugRecipeDTOList = new ArrayList<>();
        if (null != writeDrugRecipeList) {
            List<WriteDrugRecipeTO> dataList = writeDrugRecipeList.getData();
            if (CollectionUtils.isEmpty(dataList)) {
                return writeDrugRecipeDTOList;
            }
            try {
                for (WriteDrugRecipeTO writeDrugRecipeTO : dataList) {
                    WriteDrugRecipeDTO writeDrugRecipeDTO = new WriteDrugRecipeDTO();
                    WriteDrugRecipeBean writeDrugRecipeBean = new WriteDrugRecipeBean();
                    Consult consult = writeDrugRecipeTO.getConsult();
                    if (consult == null) {
                        continue;
                    }
                    ConsultDTO consultDTO = ObjectCopyUtils.convert(consult, ConsultDTO.class);
                    String appointDepartCode = consult.getAppointDepartCode();
                    AppointDepartDTO appointDepartDTO = departClient.getAppointDepartByOrganIdAndAppointDepartCode(organId, appointDepartCode);
                    if (null != appointDepartDTO) {
                        writeDrugRecipeBean.setAppointDepartInDepartId(appointDepartDTO.getDepartId());
                        String consultDepartText = DictionaryController.instance().get("eh.base.dictionary.Depart").getText(appointDepartDTO.getDepartId());
                        if (null != consultDTO) {
                            consultDTO.setConsultDepart(appointDepartDTO.getDepartId());
                            consultDTO.setConsultDepartText(consultDepartText);
                        }
                    }
                    writeDrugRecipeDTO.setAppointDepartCode(appointDepartCode);
                    writeDrugRecipeDTO.setAppointDepartName(consult.getAppointDepartName());
                    writeDrugRecipeDTO.setPatient(patientDTO);
                    writeDrugRecipeDTO.setRequestPatient(requestPatientDTO);
                    writeDrugRecipeDTO.setConsult(consultDTO);
                    writeDrugRecipeDTO.setType(writeDrugRecipeTO.getType());
                    writeDrugRecipeDTO.setWriteDrugRecipeBean(writeDrugRecipeBean);
                    logger.info("WriteRecipeManager findWriteDrugRecipeByRevisitFromHis writeDrugRecipeDTO={}", JSONUtils.toString(writeDrugRecipeDTO));
                    writeDrugRecipeDTOList.add(writeDrugRecipeDTO);
                }
            } catch (Exception e) {
                logger.error("WriteRecipeManager findWriteDrugRecipeByRevisitFromHis error={}", JSONUtils.toString(e));
            }
        }
        return writeDrugRecipeDTOList;

    }
}
