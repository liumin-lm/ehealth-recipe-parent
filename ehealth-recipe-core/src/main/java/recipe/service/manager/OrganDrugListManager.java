package recipe.service.manager;

import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.recipe.model.ValidateOrganDrugVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.util.ValidateUtil;

import java.util.List;
import java.util.Map;

/**
 * 机构药品处理
 *
 * @author fuzi
 */
@Service
public class OrganDrugListManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * 校验比对药品
     *
     * @param validateOrganDrugVO 校验机构药品对象
     * @param organDrugGroup      机构药品组
     * @return 返回机构药品
     */
    public OrganDrugList validateOrganDrug(ValidateOrganDrugVO validateOrganDrugVO, Map<String, List<OrganDrugList>> organDrugGroup) {
        validateOrganDrugVO.setValidateStatus(true);
        //校验药品存在
        if (StringUtils.isEmpty(validateOrganDrugVO.getOrganDrugCode())) {
            validateOrganDrugVO.setValidateStatus(false);
            return null;
        }
        List<OrganDrugList> organDrugs = organDrugGroup.get(validateOrganDrugVO.getOrganDrugCode());
        if (CollectionUtils.isEmpty(organDrugs)) {
            validateOrganDrugVO.setValidateStatus(false);
            return null;
        }
        //校验比对药品
        OrganDrugList organDrug = null;
        if (ValidateUtil.integerIsEmpty(validateOrganDrugVO.getDrugId()) && 1 == organDrugs.size()) {
            organDrug = organDrugs.get(0);
        }
        if (!ValidateUtil.integerIsEmpty(validateOrganDrugVO.getDrugId())) {
            for (OrganDrugList drug : organDrugs) {
                if (drug.getDrugId().equals(validateOrganDrugVO.getDrugId())) {
                    organDrug = drug;
                    break;
                }
            }
        }
        if (null == organDrug) {
            validateOrganDrugVO.setValidateStatus(false);
            logger.info("RecipeDetailService validateDrug organDrug is null OrganDrugCode ：  {}", validateOrganDrugVO.getOrganDrugCode());
            return null;
        }
        return organDrug;
    }

}
