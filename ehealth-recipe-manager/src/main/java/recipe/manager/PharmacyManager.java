package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.DoctorDefault;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DepartClient;
import recipe.dao.PharmacyTcmDAO;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.ByteUtils;
import recipe.util.RecipeUtil;
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
    private DepartClient departClient;

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
     * @param pharmacyCode      前端指定药房code
     * @param pharmacy          前端指定药房id
     * @return 药房对象
     */
    public PharmacyTcm organDrugPharmacyId(Integer organId, Integer recipeType, List<String> organDrugCodeList, String pharmacyCode,
                                           Integer pharmacy, Integer recipeDrugForm, Integer clinicId, Integer departId) {
        //判断机构药房
        List<PharmacyTcm> pharmacys = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("PharmacyManager organDrugPharmacyId pharmacys:{}，pharmacy:{}", JSON.toJSONString(pharmacys), pharmacyCode);
        pharmacys = departClient.appointDepartPharmacy(clinicId, organId, departId, pharmacys);
        if (CollectionUtils.isEmpty(pharmacys)) {
            return null;
        }
        //判断机构药房-药房支持的处方类型
        String recipeTypeText = RecipeTypeEnum.getRecipeType(recipeType);
        List<PharmacyTcm> pharmacyTypes = pharmacys.stream().filter(a -> Arrays.asList(a.getPharmacyCategray().split(ByteUtils.COMMA)).contains(recipeTypeText)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pharmacyTypes)) {
            return null;
        }
        //中药剂型过滤
        if (RecipeUtil.isTcmType(recipeType)) {
            pharmacyTypes = pharmacyTypes.stream().filter(a -> a.getDrugFormType().contains(recipeDrugForm.toString())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(pharmacyTypes)) {
                return new PharmacyTcm();
            }
        }

        Map<Integer, PharmacyTcm> pharmacyIdMap = pharmacyTypes.stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
        //返回指定药房
        if (StringUtils.isNotEmpty(pharmacyCode)) {
            Integer pharmacyId = pharmacyTypes.stream().filter(a -> a.getPharmacyCode().equals(pharmacyCode)).findFirst().map(PharmacyTcm::getPharmacyId).orElse(null);
            if (null != pharmacyIdMap.get(pharmacyId)) {
                return pharmacyIdMap.get(pharmacyId);
            }
        }
        if (null != pharmacyIdMap.get(pharmacy)) {
            return pharmacyIdMap.get(pharmacy);
        }
        //获取机构药品药房
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodeList);
        List<String> pharmacyList = organDrugList.stream().map(OrganDrugList::getPharmacy).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pharmacyList)) {
            return new PharmacyTcm();
        }
        //计算最优药房
        List<String> pharmacyIds = pharmacyList.stream().map(a -> Arrays.asList(a.split(ByteUtils.COMMA))).flatMap(Collection::stream).collect(Collectors.toList());
        Map<String, List<String>> pharmacyMap = pharmacyIds.stream().collect(Collectors.groupingBy(String::valueOf));
        logger.info("PharmacyManager organDrugPharmacyId pharmacyMap:{}", JSON.toJSONString(pharmacyMap));
        int i = 0;
        int pharmacyId = 0;
        for (String key : pharmacyMap.keySet()) {
            if (null == pharmacyIdMap.get(Integer.valueOf(key))) {
                continue;
            }
            if (pharmacyMap.get(key).size() > i) {
                i = pharmacyMap.get(key).size();
                pharmacyId = Integer.parseInt(key);
            }
        }

        if (0 == pharmacyId) {
            return new PharmacyTcm();
        }
        return pharmacyIdMap.get(pharmacyId);
    }

    /**
     * 获取当前科室的药房 - 医生默认数据实体返回
     *
     * @param organId
     * @param departId
     * @param clinicId
     * @param doctorDefaultList
     * @return
     */
    public List<DoctorDefault> appointDepartPharmacy(Integer organId, Integer departId, Integer clinicId, List<DoctorDefault> doctorDefaultList) {
        List<DoctorDefault> doctorDefaultPharmacy = doctorDefaultList.stream().filter(a -> a.getCategory().equals(1)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(doctorDefaultPharmacy)) {
            return new ArrayList<>();
        }
        List<Integer> pharmacyIds = doctorDefaultPharmacy.stream().map(DoctorDefault::getIdKey).distinct().collect(Collectors.toList());
        List<PharmacyTcm> pharmacyTcmList = pharmacyTcmDAO.findPharmacyTcmByIds(pharmacyIds);
        List<PharmacyTcm> appointDepartPharmacy = departClient.appointDepartPharmacy(clinicId, organId, departId, pharmacyTcmList);
        if (CollectionUtils.isEmpty(appointDepartPharmacy)) {
            return new ArrayList<>();
        }
        List<Integer> appointDepartPharmacyIds = appointDepartPharmacy.stream().map(PharmacyTcm::getPharmacyId).collect(Collectors.toList());
        return doctorDefaultPharmacy.stream().filter(a -> appointDepartPharmacyIds.contains(a.getIdKey())).collect(Collectors.toList());
    }
}
