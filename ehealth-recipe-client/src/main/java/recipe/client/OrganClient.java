package recipe.client;

import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.patient.dto.OrganDTO;
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
public class OrganClient extends BaseClient{

    @Autowired
    private ICurrentUserInfoService currentUserInfoService;

    @Autowired
    private OrganService organService;

    /**
     * 获取当前区域公众号下机构
     * @return 机构列表
     */
    public List<Integer> findOrganIdsByCurrentClient(){
        //查询当前区域公众号下所有归属机构
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
}
