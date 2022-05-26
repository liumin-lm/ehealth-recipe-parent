package recipe.manager;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.revisit.dto.response.RevisitBeanVO;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DepartClient;

/**
 * 科室通用业务处理类
 *
 * @author liumin
 * @date 2021\11\19 0018 08:57
 */
@Service
public class DepartManager extends BaseManager {

    @Autowired
    private DepartClient departClient;

    /**
     * 通过处方获取挂号科室
     * 如果recipe表存储了挂号科室则返回recipe表挂号科室信息
     * 如果recipe表未存储挂号科室则各接口按原来逻辑查询未取消的挂号科室
     *
     * @param recipe
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Recipe recipe) {
        logger.info("getAppointDepartByOrganIdAndDepart param:{}", JSONUtils.toString(recipe));
        AppointDepartDTO appointDepartDTO = new AppointDepartDTO();
        if (recipe == null) {
            return appointDepartDTO;
        }
        if (StringUtils.isNotEmpty(recipe.getAppointDepart())) {
            appointDepartDTO.setAppointDepartCode(recipe.getAppointDepart());
            appointDepartDTO.setAppointDepartName(recipe.getAppointDepartName());
        } else {
            appointDepartDTO = departClient.getAppointDepartByOrganIdAndDepart(recipe.getClinicOrgan(), recipe.getDepart());
        }
        logger.info("getAppointDepartByOrganIdAndDepart res:{}", JSONUtils.toString(appointDepartDTO));
        return appointDepartDTO;
    }

    /**
     * 获取行政科室
     *
     * @param depart 处方表存的depart字段
     * @return
     */
    public DepartmentDTO getDepartmentByDepart(Integer depart) {
        DepartmentDTO departmentDTO = new DepartmentDTO();
        if (null == depart) {
            return departmentDTO;
        }
        DepartmentDTO department = departClient.getDepartmentByDepart(depart);
        if (null == department) {
            return departmentDTO;
        }
        return department;
    }

    /**
     * 根据执行科室和机构获取挂号科室
     * @param organId
     * @param departId
     * @return
     */
    public AppointDepartDTO getAppointByOrganIdAndDepart(Integer organId, Integer departId){
        AppointDepartDTO appointDepartDTO = departClient.getAppointDepartByOrganIdAndDepart(organId, departId);
        logger.info("getAppointDepartById getAppointByOrganIdAndDepart appointDepartDTO:{}", JSONUtils.toString(appointDepartDTO));
        return appointDepartDTO;
    }

    /**
     * 获取挂号科室
     *
     * @param appointId
     * @return
     */
    public AppointDepartDTO getAppointDepartById(Integer appointId) {
        AppointDepartDTO appointDepartDTO = departClient.getAppointDepartById(appointId);
        logger.info("getAppointDepartById AppointDepartDTO:{}", JSONUtils.toString(appointDepartDTO));
        return appointDepartDTO;
    }

    /**
     * 获取挂号科室,如果获取不到挂号科室 ，则使用行政科室
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @param departId 行政科室id
     * @return
     */
    public AppointDepartDTO getAppointDepartDTO(Integer clinicId, Integer organId, Integer departId) {
        AppointDepartDTO appointDepart = getAppointDepart(clinicId, organId, departId);
        if (null == appointDepart) {
            appointDepart = new AppointDepartDTO();
        }
        if (StringUtils.isNotEmpty(appointDepart.getAppointDepartCode())) {
            return appointDepart;
        }
        DepartmentDTO departmentDTO = this.getDepartmentByDepart(departId);
        appointDepart.setAppointDepartCode(departmentDTO.getCode());
        appointDepart.setAppointDepartName(departmentDTO.getName());
        return appointDepart;
    }

    /**
     * 医生工号
     *
     * @param organId
     * @param doctorId
     * @param departId
     * @return
     */
    public String jobNumber(Integer organId, Integer doctorId, Integer departId) {
        return doctorClient.jobNumber(organId, doctorId, departId).getJobNumber();
    }

    private AppointDepartDTO getAppointDepart(Integer clinicId, Integer organId, Integer departId) {
        if (null == clinicId) {
            return this.getAppointByOrganIdAndDepart(organId, departId);
        }
        RevisitBeanVO revisitBeanVO = revisitClient.revisitBean(clinicId);
        if (null == revisitBeanVO || null == revisitBeanVO.getAppointDepartId()) {
            return this.getAppointByOrganIdAndDepart(organId, departId);
        }
        AppointDepartDTO appointDepartDTO = this.getAppointDepartById(revisitBeanVO.getAppointDepartId());
        if (null != appointDepartDTO) {
            return appointDepartDTO;
        }
        return this.getAppointByOrganIdAndDepart(organId, departId);
    }


}
