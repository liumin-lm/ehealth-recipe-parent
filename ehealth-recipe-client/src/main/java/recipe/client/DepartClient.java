package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.revisit.common.service.IRevisitService;
import com.ngari.revisit.dto.response.RevisitBeanVO;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.util.ValidateUtil;


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
    @Autowired
    private IRevisitService revisitService;

    /**
     * 通过机构和行政科室获取挂号科室
     *
     * @param organId
     * @param depart  处方表depart字段
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Integer organId, Integer depart) {
        logger.info("DepartClient getAppointDepartByOrganIdAndDepart organId:{},depart:{}", organId, depart);
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

    /**
     * 获取挂号科室
     *
     * @param appointId
     * @return
     */
    public AppointDepartDTO getAppointDepartById(Integer appointId) {
        if (ValidateUtil.integerIsEmpty(appointId)) {
            return null;
        }
        AppointDepartDTO appointDepartDTO = appointDepartService.getById(appointId);
        logger.info("DepartClient getAppointDepartById appointDepartDTO ={}", JSON.toJSONString(appointDepartDTO));
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
        AppointDepartDTO appointDepart = this.getAppointDepart(clinicId, organId, departId);
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
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    public void setRecipe(Recipe recipe) {
        // 根据咨询单特殊来源标识设置处方单特殊来源标识
        if (BussSourceTypeEnum.BUSSSOURCE_OUTPATIENT.getType().equals(recipe.getBussSource())) {
            return;
        }
        AppointDepartDTO appointDepart = this.getAppointDepartDTO(recipe.getClinicId(), recipe.getClinicOrgan(), recipe.getDepart());
        if (null == appointDepart) {
            return;
        }
        recipe.setAppointDepart(appointDepart.getAppointDepartCode());
        recipe.setAppointDepartName(appointDepart.getAppointDepartName());
    }

    /**
     * 获取挂号科室,如果获取不到挂号科室 ，则使用行政科室
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @param departId 行政科室id
     * @return
     */
    private AppointDepartDTO getAppointDepart(Integer clinicId, Integer organId, Integer departId) {
        if (null == clinicId) {
            return this.getAppointDepartByOrganIdAndDepart(organId, departId);
        }
        RevisitBeanVO revisitBeanVO = revisitService.getRevisitBeanVOByConsultId(clinicId);
        if (null == revisitBeanVO || null == revisitBeanVO.getAppointDepartId()) {
            return this.getAppointDepartByOrganIdAndDepart(organId, departId);
        }
        AppointDepartDTO appointDepartDTO = this.getAppointDepartById(revisitBeanVO.getAppointDepartId());
        if (null != appointDepartDTO) {
            return appointDepartDTO;
        }
        return this.getAppointDepartByOrganIdAndDepart(organId, departId);
    }
}
