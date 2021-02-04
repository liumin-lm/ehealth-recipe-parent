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
     * @param organId   机构id
     * @param usingRate 药物使用频率代码
     * @return
     */
    public UsingRateDTO usingRate(Integer organId, String usingRate) {
        if (null == organId || StringUtils.isEmpty(usingRate)) {
            return null;
        }
        try {
            UsingRateDTO usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(organId, usingRate);
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
     * @param organId     机构id
     * @param usePathways 药物使用途径代码
     * @return
     */
    public UsePathwaysDTO usePathways(Integer organId, String usePathways) {
        if (null == organId || StringUtils.isEmpty(usePathways)) {
            return null;
        }
        try {
            UsePathwaysDTO usePathwaysDTO = usePathwaysService.findUsePathwaysByOrganAndKey(organId, usePathways);
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
