package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.recipe.model.ValidateOrganDrugVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.OrganDrugListDAO;
import recipe.util.ValidateUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 机构药品处理
 *
 * @author fuzi
 */
@Service
public class OrganDrugListManager {
    private static final Logger logger = LoggerFactory.getLogger(OrganDrugListManager.class);
    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    /**
     * 根据code获取机构药品 分组
     *
     * @param organId      机构id
     * @param drugCodeList 机构药品code
     * @return 机构code = key对象
     */
    public Map<String, List<OrganDrugList>> getOrganDrugCode(int organId, List<String> drugCodeList) {
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, drugCodeList);
        logger.info("RecipeDetailService validateDrug organDrugList= {}", JSON.toJSONString(organDrugList));
        if (CollectionUtils.isEmpty(organDrugList)) {
            return new HashMap<>();
        }
        return organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
    }

    /**
     * 校验比对药品
     *
     * @param validateOrganDrugVO 校验机构药品对象
     * @param organDrugGroup      机构药品组
     * @return 返回机构药品
     */
    public static OrganDrugList validateOrganDrug(ValidateOrganDrugVO validateOrganDrugVO, Map<String, List<OrganDrugList>> organDrugGroup) {
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
