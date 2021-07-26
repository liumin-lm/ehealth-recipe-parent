package recipe.client;

import com.ngari.jgpt.zjs.service.IMinkeOrganService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.PatientDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.util.DictionaryUtil;
import recipe.util.LocalStringUtil;

import javax.annotation.Resource;

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
     * @param mpiid
     * @return
     */
    public PatientDTO getPatient(String mpiid) {
        com.ngari.patient.dto.PatientDTO patient = patientService.get(mpiid);
        PatientDTO p = new PatientDTO();
        BeanUtils.copyProperties(patient, p);
        if (StringUtils.isNotEmpty(patient.getMobile())) {
            p.setMobile(LocalStringUtil.coverMobile((patient.getMobile())));
        }
        if (StringUtils.isNotEmpty(patient.getIdcard())) {
            p.setIdcard(ChinaIDNumberUtil.hideIdCard((patient.getIdcard())));
        }
        p.setAge(null == patient.getBirthday() ? 0 : DateConversion.getAge(patient.getBirthday()));
        p.setIdcard2(null);
        p.setCertificate(null);
        if (StringUtils.isNotEmpty(p.getPatientSex())) {
            p.setPatientSex(DictionaryUtil.getDictionary("eh.base.dictionary.Gender", String.valueOf(p.getPatientSex())));
        }
        return p;
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
            logger.error("getMinkeOrganCodeByOrganId error", e);
        }
        return null;
    }

    /**
     * 根据mpiid获取患者信息
     * @param mpiId
     * @return
     */
    public com.ngari.patient.dto.PatientDTO getPatientBeanByMpiId(String mpiId){
        if(StringUtils.isEmpty(mpiId)){
            return  null;
        }
        return patientService.getPatientBeanByMpiId(mpiId);
    }

}
