package recipe.client;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 科室相关服务
 *
 * @Author liumin
 * @Date 2021/11/19 下午2:26
 * @Description
 */
@Service
public class DepartClient extends BaseClient {

    @Autowired
    private AppointDepartService appointDepartService;

    @Autowired
    private DepartmentService departmentService;


    /**
     * 通过机构和行政科室获取挂号科室
     *
     * @param organId
     * @param depart  处方表depart字段
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Integer organId, Integer depart) {
        return appointDepartService.findByOrganIDAndDepartIDAndCancleFlag(organId, depart);
    }

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
            appointDepartDTO = appointDepartService.findByOrganIDAndDepartIDAndCancleFlag(recipe.getClinicOrgan(), recipe.getDepart());
        }
        logger.info("getAppointDepartByOrganIdAndDepart res:{}", JSONUtils.toString(appointDepartDTO));
        return appointDepartDTO;
    }

    /**
     * 通过挂号科室编码、机构获取挂号科室
     *
     * @param organId
     * @param appointDepartCode
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndAppointDepartCode(Integer organId, String appointDepartCode) {
        return appointDepartService.getByOrganIDAndAppointDepartCode(organId, appointDepartCode);
    }

    /**
     * 通过行政科室id获取行政科室
     *
     * @param depart
     * @return
     */
    public DepartmentDTO getDepartmentByDepart(Integer depart) {
        DepartmentDTO departmentDTO = departmentService.getById(depart);
        return departmentDTO;
    }


}
