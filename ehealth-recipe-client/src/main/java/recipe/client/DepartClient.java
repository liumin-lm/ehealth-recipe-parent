package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.revisit.common.service.IRevisitService;
import com.ngari.revisit.dto.response.RevisitBeanVO;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.util.ValidateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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
     * 类加载排序
     *
     * @return
     */
    @Override
    public Integer getSort() {
        return 16;
    }

    /**
     * 通过机构和行政科室获取挂号科室
     *
     * @param organId
     * @param depart  处方表depart字段
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Integer organId, Integer depart) {
        if (ValidateUtil.validateObjects(organId, depart)) {
            return null;
        }
        logger.info("DepartClient getAppointDepartByOrganIdAndDepart organId:{},depart:{}", organId, depart);
        AppointDepartDTO appointDepartDTO = appointDepartService.findByOrganIDAndDepartIDAndCancleFlag(organId, depart);
        logger.info("DepartClient getAppointDepartByOrganIdAndDepart appointDepartDTO:{}", JSON.toJSONString(appointDepartDTO));
        return appointDepartDTO;
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
     * 获取挂号科室
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @param departId 行政科室id
     * @return
     */
    public AppointDepartDTO getAppointDepart(Integer clinicId, Integer organId, Integer departId) {
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

    /**
     * 获取当前科室的药房
     *
     * @param clinicId
     * @param organId
     * @param departId
     * @param symptomQueryResult
     * @return
     */
    @LogRecord
    public List<PharmacyTcm> appointDepartPharmacy(Integer clinicId, Integer organId, Integer departId, List<PharmacyTcm> symptomQueryResult) {
        if (CollectionUtils.isEmpty(symptomQueryResult)) {
            return new ArrayList<>();
        }
        AppointDepartDTO appointDepartDTO = this.getAppointDepart(clinicId, organId, departId);
        return symptomQueryResult.stream().filter(a -> {
            if (StringUtils.isEmpty(a.getAppointDepartId())) {
                return true;
            }
            try {
                List<Integer> appointDepartIds = JSONUtils.parse(a.getAppointDepartId(), List.class);
                if (CollectionUtils.isEmpty(appointDepartIds)) {
                    return true;
                }
                if (null == appointDepartDTO) {
                    return CollectionUtils.isEmpty(appointDepartIds);
                }
                if (appointDepartIds.contains(appointDepartDTO.getAppointDepartId())) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("DepartClient appointDepartPharmacy error a:{}", JSON.toJSONString(a), e);
                return false;
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipe(Recipe recipe) {
        // 根据咨询单特殊来源标识设置处方单特殊来源标识
        if (BussSourceTypeEnum.BUSSSOURCE_OUTPATIENT.getType().equals(recipe.getBussSource())) {
            return;
        }
        if (ValidateUtil.integerIsEmpty(recipe.getDepart())) {
            return;
        }
        AppointDepartDTO appointDepart = this.getAppointDepartDTO(recipe.getClinicId(), recipe.getClinicOrgan(), recipe.getDepart());
        if (null == appointDepart) {
            return;
        }
        recipe.setAppointDepart(appointDepart.getAppointDepartCode());
        recipe.setAppointDepartName(appointDepart.getAppointDepartName());
    }

}
