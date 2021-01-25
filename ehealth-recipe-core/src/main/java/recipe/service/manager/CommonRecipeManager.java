package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.CommonRecipe;
import com.ngari.recipe.entity.CommonRecipeDrug;
import com.ngari.recipe.entity.OrganDrugList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.CommonRecipeDAO;
import recipe.dao.CommonRecipeDrugDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.ByteUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 常用方通用层
 *
 * @author fuzi
 */
@Service
public class CommonRecipeManager {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private CommonRecipeDAO commonRecipeDAO;
    @Autowired
    private CommonRecipeDrugDAO commonRecipeDrugDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    /**
     * 查询常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return CommonRecipeDTO 常用方列表
     */
    public List<CommonRecipeDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        LOGGER.info("CommonRecipeManager commonRecipeList organId={},doctorId={},recipeType={}", organId, doctorId, JSON.toJSONString(recipeType));
        if (CollectionUtils.isNotEmpty(recipeType)) {
            //通过处方类型获取常用处方
            List<CommonRecipe> commonRecipeList = commonRecipeDAO.findByRecipeTypeAndOrganId(recipeType, doctorId, organId, start, limit);
            return ObjectCopyUtils.convert(commonRecipeList, CommonRecipeDTO.class);
        }
        //通过医生id查询常用处方
        List<CommonRecipe> commonRecipeList = commonRecipeDAO.findByDoctorIdAndOrganId(doctorId, organId, start, limit);
        LOGGER.info("CommonRecipeManager commonRecipeList commonRecipeList={}", JSON.toJSONString(commonRecipeList));
        return ObjectCopyUtils.convert(commonRecipeList, CommonRecipeDTO.class);
    }

    /**
     * 查询常用方药品，与机构药品关联返回
     *
     * @param organId
     * @param commonRecipeIdList
     * @return
     */
    public Map<Integer, List<CommonRecipeDrugDTO>> commonDrugGroup(Integer organId, List<Integer> commonRecipeIdList) {
        LOGGER.info("CommonRecipeManager commonDrugGroup  organId={}, commonRecipeIdList={}", organId, JSON.toJSONString(commonRecipeIdList));
        if (CollectionUtils.isEmpty(commonRecipeIdList)) {
            return null;
        }
        List<CommonRecipeDrug> commonRecipeDrugList = commonRecipeDrugDAO.findByCommonRecipeIdList(commonRecipeIdList);
        LOGGER.info("CommonRecipeManager commonDrugGroup  commonRecipeDrugList={},", JSON.toJSONString(commonRecipeDrugList));
        if (CollectionUtils.isEmpty(commonRecipeDrugList)) {
            return null;
        }
        List<Integer> drugIdList = commonRecipeDrugList.stream().map(CommonRecipeDrug::getDrugId).distinct().collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIdWithoutStatus(organId, drugIdList);
        Map<String, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));

        Map<Integer, List<CommonRecipeDrugDTO>> commonDrugGroup = new HashMap<>();
        commonRecipeDrugList.forEach(a -> {
            CommonRecipeDrugDTO commonDrugDTO = new CommonRecipeDrugDTO();
            BeanUtils.copyProperties(a, commonDrugDTO);
            commonDrugDTO.setVariation(false);
            OrganDrugList organDrug = organDrugMap.get(commonDrugDTO.getDrugId() + commonDrugDTO.getOrganDrugCode());
            if (null == organDrug) {
                commonDrugDTO.setVariation(true);
                commonDrugDTO.setDrugStatus(-1);
                return;
            }
            //判断药品药房是否变动
            commonDrugDTO.setVariation(pharmacyVariation(a.getPharmacyId(), organDrug.getPharmacy()));

            if (null != commonDrugDTO.getUseTotalDose()) {
                commonDrugDTO.setDrugCost(organDrug.getSalePrice().multiply(new BigDecimal(commonDrugDTO.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
            }
            commonDrugDTO.setDrugStatus(organDrug.getStatus());
            commonDrugDTO.setSalePrice(organDrug.getSalePrice());
            commonDrugDTO.setPrice1(organDrug.getSalePrice().doubleValue());
            commonDrugDTO.setDrugForm(organDrug.getDrugForm());
            //用药单位不为空时才返回给前端
            List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = new LinkedList<>();
            if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
            } else if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
            }
            commonDrugDTO.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
            //commonDrugDTO.setPlatformSaleName(drug.getSaleName());
            //commonDrugDTO.setUsingRateId(String.valueOf(usingRateDTO.getId()));
            //commonDrugDTO.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));

            List<CommonRecipeDrugDTO> commonDrugList = commonDrugGroup.get(commonDrugDTO.getCommonRecipeId());
            if (CollectionUtils.isEmpty(commonDrugList)) {
                commonDrugList = new LinkedList<>();
                commonDrugList.add(commonDrugDTO);
                commonDrugGroup.put(commonDrugDTO.getCommonRecipeId(), commonDrugList);
            } else {
                commonDrugList.add(commonDrugDTO);
            }
        });
        LOGGER.info("CommonRecipeManager commonDrugGroup commonDrugGroup={}", JSON.toJSONString(commonDrugGroup));
        return commonDrugGroup;
    }

    /**
     * 判断药品药房是否变动
     *
     * @param commonPharmacyId 常用方药房id
     * @param pharmacy         机构药房id
     * @return true 变动
     */
    private boolean pharmacyVariation(Integer commonPharmacyId, String pharmacy) {
        if (null == commonPharmacyId && StringUtils.isNotEmpty(pharmacy)) {
            return true;
        }
        if (null != commonPharmacyId && StringUtils.isEmpty(pharmacy)) {
            return true;
        }
        if (null != commonPharmacyId && StringUtils.isNotEmpty(pharmacy) &&
                !Arrays.asList(pharmacy.split(ByteUtils.COMMA)).contains(String.valueOf(commonPharmacyId))) {
            return true;
        }
        return false;
    }
}
