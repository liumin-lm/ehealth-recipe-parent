package recipe.client;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.OrganService;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.ObjectCopyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构相关服务
 *
 * @author fuzi
 */
@Service
public class OrganClient extends BaseClient {
    @Autowired
    private AppointDepartService appointDepartService;
    @Autowired
    private OrganService organService;
    @Autowired
    private DepartmentService departmentService;

    /**
     * 查询当前区域公众号下所有归属机构
     *
     * @return 机构列表
     */
    public List<Integer> findOrganIdsByCurrentClient() {
        List<Integer> organIds = currentUserInfoService.getCurrentOrganIds();
        logger.info("OrganClient getOrganForWeb getCurrentOrganIds organIds:{}", JSONUtils.toString(organIds));
        return organIds;
    }

    /**
     * 获取所有机构列表
     *
     * @return 机构列表
     */
    public List<Integer> findAllOrganIds() {
        List<Integer> organIds = new ArrayList<>();
        List<OrganDTO> organs = organService.findOrgans();
        if (CollectionUtils.isNotEmpty(organs)) {
            organIds = organs.stream().map(OrganDTO::getOrganId).collect(Collectors.toList());
        }
        logger.info("OrganClient getOrganForWeb findOrgans organIds:{}", JSONUtils.toString(organIds));
        return organIds;

    }

    /**
     * 获取机构信息
     *
     * @param organId 机构id
     * @return
     */
    public com.ngari.recipe.dto.OrganDTO organDTO(Integer organId) {
        OrganDTO organDTO = organService.getByOrganId(organId);
        logger.info("OrganClient organDTO organDTO {}", JSONUtils.toString(organDTO));
        return ObjectCopyUtils.convert(organDTO, com.ngari.recipe.dto.OrganDTO.class);
    }

    /**
     * 获取可是科室信息
     *
     * @param organId  机构id
     * @param departId 开方可是id
     * @return 通用departClient类
     */
    @Deprecated
    public AppointDepartDTO departDTO(Integer organId, Integer departId) {
        return appointDepartService.findByOrganIDAndDepartID(organId, departId);
    }

    /**
     * 获取可是信息
     *
     * @param departId 开方可是id
     * @return 通用departClient类
     */
    @Deprecated
    public DepartmentDTO departmentDTO(Integer departId) {
        return departmentService.get(departId);
    }
}
