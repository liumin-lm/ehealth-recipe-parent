package recipe.service.client;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.basic.ds.PatientVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;

import javax.annotation.Resource;

/**
 * @author fuzi
 */
@Service
public class PatientClient extends BaseClient {
    @Resource
    private PatientService patientService;

    /**
     * 获取 脱敏后的 患者对象
     *
     * @param mpiid
     * @return
     */
    public PatientDTO getPatientDTO(String mpiid) {
        PatientDTO patient = patientService.get(mpiid);
        PatientVO p = new PatientVO();
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
        PatientDTO patientDTO = new PatientDTO();
        BeanUtils.copyProperties(p, patientDTO);
        return patientDTO;
    }
}
