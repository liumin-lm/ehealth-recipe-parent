package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.patient.mode.PatientQueryRequestTO;
import com.ngari.jgpt.zjs.service.IMinkeOrganService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.PatientDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.util.DictionaryUtil;
import recipe.util.LocalStringUtil;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者信息处理类
 *
 * @author fuzi
 */
@Service
public class PatientClient extends BaseClient {
    @Resource
    private PatientService patientService;
    @Resource
    private OrganService organService;
    @Resource
    private IMinkeOrganService minkeOrganService;

    /**
     * 获取 脱敏后的 患者对象
     *
     * @param mpiId
     * @return
     */
    public PatientDTO getPatientEncipher(String mpiId) {
        com.ngari.patient.dto.PatientDTO patient = patientService.get(mpiId);
        return getPatientEncipher(patient);
    }

    /**
     * 获取患者信息
     *
     * @param mpiId
     * @return
     */
    public PatientDTO getPatientDTO(String mpiId) {
        com.ngari.patient.dto.PatientDTO patient = patientService.get(mpiId);
        PatientDTO p = new PatientDTO();
        BeanUtils.copyProperties(patient, p);
        return p;
    }

    /**
     * 获取脱敏患者对象
     *
     * @param mpiIds
     * @return
     */
    public Map<String, PatientDTO> findPatientMap(List<String> mpiIds) {
        List<com.ngari.patient.dto.PatientDTO> patientList = patientService.findByMpiIdIn(mpiIds);
        logger.info("PatientClient findPatientMap patientList:{}", JSON.toJSONString(patientList));
        if (CollectionUtils.isEmpty(patientList)) {
            return null;
        }
        List<PatientDTO> patientDTOList = new LinkedList<>();
        patientList.forEach(a -> patientDTOList.add(getPatientEncipher(a)));
        return patientDTOList.stream().collect(Collectors.toMap(PatientDTO::getMpiId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 根据平台机构id获取民科机构登记号
     *
     * @param organId 机构id
     * @return
     */
    public String getMinkeOrganCodeByOrganId(Integer organId) {
        if (null == organId) {
            return null;
        }
        try {
            //获取民科机构登记号
            OrganDTO organDTO = organService.getByOrganId(organId);
            if (organDTO != null && StringUtils.isNotEmpty(organDTO.getMinkeUnitID())) {
                return minkeOrganService.getRegisterNumberByUnitId(organDTO.getMinkeUnitID());
            }
        } catch (Exception e) {
            logger.error("PatientClient getMinkeOrganCodeByOrganId error", e);
        }
        return null;
    }

    /**
     * 根据mpiid获取患者信息
     *
     * @param mpiId
     * @return
     */
    public com.ngari.patient.dto.PatientDTO getPatientBeanByMpiId(String mpiId) {
        if (StringUtils.isEmpty(mpiId)) {
            return null;
        }
        return patientService.getPatientBeanByMpiId(mpiId);
    }

    /**
     * 查询线下患者信息
     * @param patientQueryRequestTO
     * @return
     */
    public PatientQueryRequestTO queryPatient(PatientQueryRequestTO patientQueryRequestTO){
        logger.info("PatientClient queryPatient patientQueryRequestTO:{}." , JSON.toJSONString(patientQueryRequestTO));
        try {
            HisResponseTO<PatientQueryRequestTO> response = patientHisService.queryPatient(patientQueryRequestTO);
            PatientQueryRequestTO result = getResponse(response);
            if (result == null){
                return null;
            }
            result.setCardID(null);
            result.setCertificate(null);
            result.setGuardianCertificate(null);
            result.setMobile(null);
            return result;
        } catch (Exception e) {
            logger.error("PatientClient queryPatient error", e);
            return null;
        }
    }

    /**
     * 患者信息脱敏
     *
     * @param patient
     * @return
     */
    private PatientDTO getPatientEncipher(com.ngari.patient.dto.PatientDTO patient) {
        PatientDTO p = new PatientDTO();
        BeanUtils.copyProperties(patient, p);
        if (StringUtils.isNotEmpty(p.getMobile())) {
            p.setMobile(LocalStringUtil.coverMobile(p.getMobile()));
        }
        if (StringUtils.isNotEmpty(p.getIdcard())) {
            p.setIdcard(ChinaIDNumberUtil.hideIdCard(p.getIdcard()));
        }
        p.setAge(null == p.getBirthday() ? 0 : DateConversion.getAge(p.getBirthday()));
        p.setIdcard2(null);
        p.setCertificate(null);
        if (StringUtils.isNotEmpty(p.getPatientSex())) {
            p.setPatientSex(DictionaryUtil.getDictionary("eh.base.dictionary.Gender", String.valueOf(p.getPatientSex())));
        }
        return p;
    }

}
