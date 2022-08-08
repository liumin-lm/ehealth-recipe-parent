package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.PharmacyTcmDAO;
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
     * @param pharmacyId    预比对药房id
     * @param organPharmacy 机构药房id
     * @return true 不一致
     */
    public Boolean pharmacyVariationV1(Integer pharmacyId, String organPharmacy) {
        if (ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isNotEmpty(organPharmacy)) {
            return true;
        }
        if (!ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isEmpty(organPharmacy)) {
            return true;
        }
        if (!ValidateUtil.integerIsEmpty(pharmacyId) && StringUtils.isNotEmpty(organPharmacy) &&
                !Arrays.asList(organPharmacy.split(ByteUtils.COMMA)).contains(String.valueOf(pharmacyId))) {
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
    public Integer organDrugPharmacyId(Integer organId, List<String> organDrugCodeList) {
        Integer pharmacyId = 0;
        Map<Integer, PharmacyTcm> pharmacyIdMap = this.pharmacyIdMap(organId);
        if (null == pharmacyIdMap) {
            return null;
        }
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodeList);
        List<String> pharmacyList = organDrugList.stream().map(OrganDrugList::getPharmacy).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pharmacyList)) {
            return pharmacyId;
        }
        List<String> pharmacyIds = pharmacyList.stream().map(a -> Arrays.asList(a.split(ByteUtils.COMMA))).flatMap(Collection::stream).collect(Collectors.toList());
        Map<String, List<String>> pharmacyMap = pharmacyIds.stream().collect(Collectors.groupingBy(String::valueOf));

        int i = 0;
        for (String key : pharmacyMap.keySet()) {
            if (null == pharmacyIdMap.get(Integer.valueOf(key))) {
                continue;
            }
            if (pharmacyMap.get(key).size() > i) {
                i = pharmacyMap.get(key).size();
                pharmacyId = Integer.valueOf(key);
            }
        }
        return pharmacyId;
    }
}
