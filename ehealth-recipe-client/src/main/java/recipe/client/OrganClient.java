package recipe.client;

import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.dto.OrganConfigDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.HealthCardService;
import com.ngari.patient.service.OrganConfigService;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;

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
    private OrganService organService;
    @Autowired
    private IOrganConfigService iOrganConfigService;
    @Autowired
    private HealthCardService healthCardService;
    @Autowired
    private OrganConfigService organConfigService;
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
     * 机构账号可以配置其它机构
     *
     * @param manageUnit
     * @return
     */
    public List<Integer> findOrganIdsByManageUnit(String manageUnit) {
        return organService.findOrganIdsByManageUnit(manageUnit + "%");
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
     * 获取机构配置
     * @param organId 机构ID
     * @return 机构配置
     */
    public OrganConfigBean getOrganConfigBean(Integer organId){
        OrganConfigBean organConfigBean = iOrganConfigService.get(organId);
        logger.info("OrganClient organConfigBean:{}", JSONUtils.toString(organConfigBean));
        return organConfigBean;
    }

    /**
     * 根据地区获取机构
     * @param manageUnit
     * @return
     */
    @LogRecord
    public OrganDTO getByManageUnit(String manageUnit){
        return organService.getByManageUnit(manageUnit);
    }

    @LogRecord
    public List<HealthCardDTO> findByCardOrganAndMpiId(String mpiId, Integer organId){
        return healthCardService.findByCardOrganAndMpiId(organId,mpiId);
    }

    /**
     * 获取机构配置
     * @param organId
     * @return
     */
    public OrganConfigDTO getOrganConfigByOrganId(Integer organId){
        return organConfigService.getByOrganId(organId);
    }

    /**
     * 更新机构配置
     *
     * @param param
     * @return
     */
    public OrganConfigDTO updateOrganConfig(OrganConfigDTO param) {
        return organConfigService.update(param);
    }

    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    public void setRecipe(Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipe.getClinicOrgan())) {
            return;
        }
        com.ngari.recipe.dto.OrganDTO organDTO = this.organDTO(recipe.getClinicOrgan());
        recipe.setOrganName(organDTO.getShortName());
    }

}
