package recipe.manager;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.dto.DoctorPermissionDTO;
import com.ngari.recipe.entity.DoctorDefault;
import org.springframework.stereotype.Service;
import recipe.dao.DoctorDefaultDAO;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 医生信息通用层
 * @author fuzi
 */
@Service
public class DoctorManager extends BaseManager{

   @Resource
   private DoctorDefaultDAO doctorDefaultDAO;

    /**
     * 获取医生 默认数据
     * @param organId 机构id
     * @param doctorId 医生id
     * @param category 类别
     * @return 医生默认数据
     */
    public List<DoctorDefault> doctorDefaultList(Integer organId, Integer doctorId, Integer category) {
        if(ValidateUtil.integerIsEmpty(category)){
          return doctorDefaultDAO.findByOrganAndDoctor(organId,doctorId);
        }
        return doctorDefaultDAO.findByOrganAndDoctorAndCategory(organId,doctorId,category);
    }

    /**
     * 保存医生 默认数据
     * @param doctorDefault
     */
    public void saveDoctorDefault(DoctorDefault doctorDefault) {
        if (Integer.valueOf(2).equals(doctorDefault.getCategory())) {
            DoctorDefault dbDefault = doctorDefaultDAO.getByOrganAndDoctorAndCategory(doctorDefault.getOrganId(), doctorDefault.getDoctorId(), doctorDefault.getCategory());
            if (null != dbDefault) {
                dbDefault.setIdKey(doctorDefault.getIdKey());
                dbDefault.setType(doctorDefault.getType());
                doctorDefaultDAO.updateNonNullFieldByPrimaryKey(dbDefault);
            } else {
                doctorDefaultDAO.save(doctorDefault);
            }
            return;
        }
        DoctorDefault dbDefault = doctorDefaultDAO.getByOrganAndDoctorAndCategoryAndType(doctorDefault.getOrganId(), doctorDefault.getDoctorId(), doctorDefault.getCategory(), doctorDefault.getType());
        if (null != dbDefault) {
            dbDefault.setIdKey(doctorDefault.getIdKey());
            doctorDefaultDAO.updateNonNullFieldByPrimaryKey(dbDefault);
            return;
        }
        doctorDefaultDAO.save(doctorDefault);
    }

    /**
     * 平台医生权限
     *
     * @param doctorPermissionVO
     */
    public DoctorPermissionDTO doctorRecipePermission(DoctorPermissionDTO doctorPermissionVO) {
        Integer organId = doctorPermissionVO.getOrganId();

        int listNum = organDrugListDAO.getCountByOrganIdAndStatus(Collections.singletonList(organId));
        boolean haveDrug = listNum > 0;
        //获取中药数量
        boolean isDrugNum = false;
        if (haveDrug) {
            Long zhongDrugNum = drugListDAO.getSpecifyNum(organId, 3);
            isDrugNum = zhongDrugNum > 0;
        }
        DoctorPermissionDTO doctorPermission = consultClient.doctorPermissionSetting(doctorPermissionVO.getDoctorId(), isDrugNum);
        //处方权限结果
        doctorPermission.setResult(doctorPermission.getPrescription() && haveDrug);
        //提示内容
        String tips = configurationClient.getValueCatch(organId, "DoctorRecipeNoPermissionText", "");
        doctorPermission.setTips(tips);
        //开处方页顶部文案配置
        String openRecipeTopText = configurationClient.getValueCatch(organId, "openRecipeTopTextConfig", "");
        doctorPermission.setUnSendTitle(openRecipeTopText);
        return doctorPermission;
    }

    /**
     * his医生权限
     *
     * @param doctorPermission
     */
    public DoctorPermissionDTO doctorHisRecipePermission(DoctorPermissionDTO doctorPermission) {
        DoctorDTO doctorDTO = doctorClient.jobNumber(doctorPermission.getOrganId(), doctorPermission.getDoctorId(), doctorPermission.getDepartId());
        doctorDTO.getJobNumber();
        doctorDTO.getName();
        doctorDTO.getIdNumber();
        AppointDepartDTO appointDepartDTO = departClient.getAppointDepartById(doctorPermission.getAppointId());
        appointDepartDTO.getAppointDepartName();
        appointDepartDTO.getAppointDepartCode();

        return new DoctorPermissionDTO();
    }
}
