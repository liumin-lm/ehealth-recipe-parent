package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.PharmacyTcmDAO;
import recipe.dao.RecipeParameterDao;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 药房处理
 *
 * @author fuzi
 */
@Service
public class PharmacyManager extends BaseManager {
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private RecipeParameterDao recipeParameterDao;

    /**
     * 校验药品药房是否变动
     *
     * @param pharmacyId      预比对药房id
     * @param pharmacyCode    预比对药房code
     * @param organPharmacy   机构药房id
     * @param pharmacyCodeMap 药房表信息
     * @return true 不一致
     */
    public static Integer pharmacyVariation(Integer pharmacyId, String pharmacyCode, String organPharmacy, Map<String, PharmacyTcm> pharmacyCodeMap) {
        if (ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isEmpty(pharmacyCode) && StringUtils.isNotEmpty(organPharmacy)) {
            return null;
        }
        if (ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isNotEmpty(pharmacyCode)) {
            PharmacyTcm pharmacyTcm = pharmacyCodeMap.get(pharmacyCode);
            if (null == pharmacyTcm) {
                return null;
            }
            pharmacyId = pharmacyTcm.getPharmacyId();
        }
        if (ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isNotEmpty(organPharmacy)) {
            return null;
        }
        if (!ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isEmpty(organPharmacy)) {
            return null;
        }
        if (!ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isNotEmpty(organPharmacy) &&
                !Arrays.asList(organPharmacy.split(ByteUtils.COMMA)).contains(String.valueOf(pharmacyId))) {
            return null;
        }
        if (ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isEmpty(organPharmacy)) {
            pharmacyId = 0;
        }
        return pharmacyId;
    }


    /**
     * 校验药品药房是否变动
     *
     * @param pharmacy      最优药房
     * @param organPharmacy 当前药品 机构药房id
     * @return true 不一致
     */
    public Boolean pharmacyVariationV1(PharmacyTcm pharmacy, String organPharmacy) {
        //机构没药房
        if (null == pharmacy) {
            return false;
        }
        //全部机构药品没药房
        if (ValidateUtil.integerIsEmpty(pharmacy.getPharmacyId())) {
            return true;
        }
        //当前机构药品没药房
        if (StringUtils.isEmpty(organPharmacy)) {
            return true;
        }
        //当前药品药房不包含最优药房
        if (!Arrays.asList(organPharmacy.split(ByteUtils.COMMA)).contains(String.valueOf(pharmacy.getPharmacyId()))) {
            return true;
        }
        return false;
    }


    /**
     * 药房信息
     *
     * @param organId 机构id
     * @return 机构code = key对象
     */
    public Map<String, PharmacyTcm> pharmacyCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        //药房信息
        List<PharmacyTcm> pharmacyList = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("RecipeDetailService pharmacyCodeMap organId:{}, pharmacyList= {}", organId, JSON.toJSONString(pharmacyList));
        return Optional.ofNullable(pharmacyList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyCode, a -> a, (k1, k2) -> k1));
    }

    /**
     * 药房信息
     *
     * @param organId 机构id
     * @return id = key对象
     */
    public Map<Integer, PharmacyTcm> pharmacyIdMap(Integer organId) {
        //药房信息
        List<PharmacyTcm> pharmacyList = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("RecipeDetailService pharmacyIdMap pharmacyList= {}", JSON.toJSONString(pharmacyList));
        return Optional.ofNullable(pharmacyList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
    }

    /**
     * 根据药房 key==id 的 Map 获取对象
     *
     * @param pharmacyId    药房Id
     * @param pharmacyIdMap key==id 的 Map
     * @return 药房对象
     */
    public static PharmacyTcm pharmacyById(Integer pharmacyId, Map<Integer, PharmacyTcm> pharmacyIdMap) {
        if (ValidateUtil.integerIsEmpty(pharmacyId)) {
            return null;
        }
        PharmacyTcm pharmacyTcm = pharmacyIdMap.get(pharmacyId);
        if (null == pharmacyTcm) {
            return null;
        }
        return pharmacyTcm;
    }

    /**
     * 获取机构药品最大匹配药房id
     *
     * @param organId           机构id
     * @param organDrugCodeList 机构药品code
     * @return
     */
    public PharmacyTcm organDrugPharmacyId(Integer organId, Integer recipeType, List<String> organDrugCodeList, Integer pharmacy) {
        //判断机构药房
        List<PharmacyTcm> pharmacys = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("PharmacyManager organDrugPharmacyId pharmacys:{}", JSON.toJSONString(pharmacys));
        if (CollectionUtils.isEmpty(pharmacys)) {
            return null;
        }
        //判断机构药房-药房支持的处方类型
        String recipeTypeText = RecipeTypeEnum.getRecipeType(recipeType);
        List<PharmacyTcm> pharmacyTypes = pharmacys.stream().filter(a -> Arrays.asList(a.getPharmacyCategray().split(ByteUtils.COMMA)).contains(recipeTypeText)).collect(Collectors.toList());
        logger.info("PharmacyManager organDrugPharmacyId pharmacyTypes = {},recipeTypeText={}", JSON.toJSONString(pharmacyTypes), recipeTypeText);
        if (CollectionUtils.isEmpty(pharmacyTypes)) {
            return null;
        }
        //获取机构药品药房
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodeList);
        List<String> pharmacyList = organDrugList.stream().map(OrganDrugList::getPharmacy).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pharmacyList)) {
            return new PharmacyTcm();
        }
        String organDrugPharmacyId = recipeParameterDao.getByName("organDrugPharmacyId");
        if (StringUtils.isEmpty(organDrugPharmacyId)) {
            //优先使用线上药房
            List<Integer> pharmacyIdList = pharmacys.stream().map(PharmacyTcm::getPharmacyId).collect(Collectors.toList());
            if (Objects.nonNull(pharmacy) && pharmacyIdList.contains(pharmacy)) {
                Map<Integer, PharmacyTcm> currentPharmacyMap = pharmacys.stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
                return currentPharmacyMap.get(pharmacy);
            }
        }
        //计算最优药房
        List<String> pharmacyIds = pharmacyList.stream().map(a -> Arrays.asList(a.split(ByteUtils.COMMA))).flatMap(Collection::stream).collect(Collectors.toList());
        Map<String, List<String>> pharmacyMap = pharmacyIds.stream().collect(Collectors.groupingBy(String::valueOf));
        Map<Integer, PharmacyTcm> pharmacyIdMap = pharmacyTypes.stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
        int i = 0;
        Integer pharmacyId = 0;
        for (String key : pharmacyMap.keySet()) {
            if (null == pharmacyIdMap.get(Integer.valueOf(key))) {
                continue;
            }
            if (pharmacyMap.get(key).size() > i) {
                i = pharmacyMap.get(key).size();
                pharmacyId = Integer.valueOf(key);
            }
        }
        if (0 == pharmacyId) {
            return new PharmacyTcm();
        }
        return pharmacyIdMap.get(pharmacyId);
    }
}
