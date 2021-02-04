package recipe.service.client;

import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 药品数据 交互处理类
 *
 * @author fuzi
 */
@Service
public class DrugClient extends BaseClient {
    @Autowired
    private IUsingRateService usingRateService;
    @Autowired
    private IUsePathwaysService usePathwaysService;

    /**
     * 获取机构 药物使用频率
     *
     * @param organId        机构id
     * @param organUsingRate 机构药物使用频率代码
     * @return
     */
    public UsingRateDTO usingRate(Integer organId, String organUsingRate) {
        if (null == organId || StringUtils.isEmpty(organUsingRate)) {
            return null;
        }
        try {
            UsingRateDTO usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(organId, organUsingRate);
            if (null == usingRateDTO) {
                return null;
            }
            return usingRateDTO;
        } catch (Exception e) {
            logger.warn("DrugClient usingRate usingRateDTO error", e);
            return null;
        }
    }

    /**
     * 获取机构 药物使用途径
     *
     * @param organId          机构id
     * @param organUsePathways 机构药物使用途径代码
     * @return
     */
    public UsePathwaysDTO usePathways(Integer organId, String organUsePathways) {
        if (null == organId || StringUtils.isEmpty(organUsePathways)) {
            return null;
        }
        try {
            UsePathwaysDTO usePathwaysDTO = usePathwaysService.findUsePathwaysByOrganAndKey(organId, organUsePathways);
            if (null == usePathwaysDTO) {
                return null;
            }
            return usePathwaysDTO;
        } catch (Exception e) {
            logger.warn("DrugClient usePathways usePathwaysDTO error", e);
            return null;
        }
    }
}
