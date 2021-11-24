package recipe.manager;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.recipe.entity.Recipe;
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
        if (depart == null) {
            return departmentDTO;
        }
        departmentDTO = departClient.getDepartmentByDepart(depart);
        return departmentDTO;
    }

    /**
     * 通过挂号科室编码、机构获取挂号科室
     *
     * @param organId  机构ID
     * @param appointDepartCode 挂号科室编码
     * @return 挂号科室
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndAppointDepartCode(Integer organId, String appointDepartCode) {
        logger.info("getAppointDepartByOrganIdAndAppointDepartCode organId:{},appointDepartCode:{}.", organId, appointDepartCode);
        AppointDepartDTO appointDepartDTO =  departClient.getAppointDepartByOrganIdAndAppointDepartCode(organId, appointDepartCode);
        logger.info("getAppointDepartByOrganIdAndAppointDepartCode appointDepartDTO:{}.", JSONUtils.toString(appointDepartDTO));
        return appointDepartDTO;
    }

}
