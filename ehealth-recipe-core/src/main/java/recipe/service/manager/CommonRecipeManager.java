package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeExtDTO;
import com.ngari.recipe.entity.CommonRecipe;
import com.ngari.recipe.entity.CommonRecipeDrug;
import com.ngari.recipe.entity.CommonRecipeExt;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.exception.DAOException;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.dao.CommonRecipeDAO;
import recipe.dao.CommonRecipeDrugDAO;
import recipe.dao.CommonRecipeExtDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.service.client.DoctorClient;
import recipe.service.client.DrugClient;
import recipe.service.client.OfflineRecipeClient;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private CommonRecipeExtDAO commonRecipeExtDAO;
    @Autowired
    private CommonRecipeDrugDAO commonRecipeDrugDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private DrugClient drugClient;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private OfflineRecipeClient offlineRecipeClient;

    /**
     * 新增常用方信息
     *
     * @param commonRecipe       常用方
     * @param commonRecipeExtDTO 常用方扩展
     * @param commonDrugList     常用方药品
     */
    public void saveCommonRecipe(CommonRecipe commonRecipe, CommonRecipeExtDTO commonRecipeExtDTO, List<CommonRecipeDrug> commonDrugList) {
        LOGGER.info("CommonRecipeManager saveCommonRecipe commonRecipe={},commonRecipeExtDTO={},commonDrugList={}"
                , JSON.toJSONString(commonRecipe), JSON.toJSONString(commonRecipeExtDTO), JSON.toJSONString(commonDrugList));
        commonRecipe.setCommonRecipeId(null);
        commonRecipeDAO.save(commonRecipe);
        if (null != commonRecipeExtDTO) {
            CommonRecipeExt commonRecipeExt = ObjectCopyUtils.convert(commonRecipeExtDTO, CommonRecipeExt.class);
            commonRecipeExt.setCommonRecipeId(commonRecipe.getCommonRecipeId());
            commonRecipeExt.setStatus(0);
            commonRecipeExtDAO.save(commonRecipeExt);
        }

        commonDrugList.forEach(a -> {
            a.setCommonRecipeId(commonRecipe.getCommonRecipeId());
            commonRecipeDrugDAO.save(a);
        });
        LOGGER.info("CommonRecipeManager saveCommonRecipe commonRecipeId={}", commonRecipe.getCommonRecipeId());
    }

    /**
     * 删除常用方信息
     *
     * @param commonRecipeId 常用方id
     */
    public void removeCommonRecipe(Integer commonRecipeId) {
        if (ValidateUtil.integerIsEmpty(commonRecipeId)) {
            return;
        }
        commonRecipeDAO.remove(commonRecipeId);
        commonRecipeDrugDAO.deleteByCommonRecipeId(commonRecipeId);
        commonRecipeExtDAO.deleteByCommonRecipeId(commonRecipeId);
        LOGGER.info("CommonRecipeManager removeCommonRecipe commonRecipeId={}", commonRecipeId);
    }

    /**
     * 查询常用方扩展数据
     *
     * @param commonRecipeIdList
     * @return
     */
    public Map<Integer, CommonRecipeExtDTO> commonRecipeExtDTOMap(List<Integer> commonRecipeIdList) {
        List<CommonRecipeExt> list = commonRecipeExtDAO.findByCommonRecipeIds(commonRecipeIdList);
        LOGGER.info("CommonRecipeManager commonRecipeExtDTOMap list={},commonRecipeIdList={}", JSON.toJSONString(list), JSON.toJSONString(commonRecipeIdList));
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        List<CommonRecipeExtDTO> commonRecipeExtList = ObjectCopyUtils.convert(list, CommonRecipeExtDTO.class);
        return commonRecipeExtList.stream().collect(Collectors.toMap(CommonRecipeExtDTO::getCommonRecipeId, a -> a, (k1, k2) -> k1));
    }

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
     * 查询线下常用方列表
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @return
     */
    public List<CommonRecipe> offlineCommonRecipeList(Integer organId, Integer doctorId) {
        List<CommonRecipe> commonRecipeList = commonRecipeDAO.findByOrganIdAndDoctorIdAndType(organId, doctorId, 2);
        LOGGER.info("CommonRecipeManager commonRecipeList commonRecipeList={}，organId={}，doctorId={}", JSON.toJSONString(commonRecipeList), organId, doctorId);
        return commonRecipeList;
    }

    /**
     * 查询常用方药品，与机构药品关联返回
     *
     * @param organId            机构id
     * @param commonRecipeIdList 常用方id
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
        //用药途径 用药频次
        Map<Integer, UsePathways> usePathwaysMap = drugClient.usePathwaysMap(organId);
        Map<Integer, UsingRate> usingRateMap = drugClient.usingRateMap(organId);

        Map<Integer, List<CommonRecipeDrugDTO>> commonDrugGroup = new HashMap<>();
        commonRecipeDrugList.forEach(a -> {
            CommonRecipeDrugDTO commonDrugDTO = new CommonRecipeDrugDTO();
            BeanUtils.copyProperties(a, commonDrugDTO);
            OrganDrugList organDrug = organDrugMap.get(commonDrugDTO.getDrugId() + commonDrugDTO.getOrganDrugCode());
            if (null == organDrug) {
                commonDrugDTO.setDrugStatus(0);
            } else {
                commonDrugDTO.setOrganPharmacyId(organDrug.getPharmacy());
                if (null != commonDrugDTO.getUseTotalDose()) {
                    commonDrugDTO.setDrugCost(organDrug.getSalePrice().multiply(new BigDecimal(commonDrugDTO.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
                }
                commonDrugDTO.setDrugStatus(organDrug.getStatus());
                commonDrugDTO.setSalePrice(organDrug.getSalePrice());
                commonDrugDTO.setPrice1(organDrug.getSalePrice().doubleValue());
                commonDrugDTO.setDrugForm(organDrug.getDrugForm());
                //用药单位不为空时才返回给前端
                commonDrugDTO.setUseDoseAndUnitRelation(OrganDrugListManager.defaultUseDose(organDrug));
                commonDrugDTO.setPlatformSaleName(organDrug.getSaleName());
            }
            if (StringUtils.isNotEmpty(commonDrugDTO.getUsingRateId())) {
                UsingRate usingRate = usingRateMap.get(Integer.valueOf(commonDrugDTO.getUsingRateId()));
                if (null != usingRate) {
                    commonDrugDTO.setUsingRateEnglishNames(usingRate.getEnglishNames());
                }
            }
            if (StringUtils.isNotEmpty(commonDrugDTO.getUsePathwaysId())) {
                UsePathways usePathways = usePathwaysMap.get(Integer.valueOf(commonDrugDTO.getUsePathwaysId()));
                if (null != usePathways) {
                    commonDrugDTO.setUsePathEnglishNames(usePathways.getEnglishNames());
                }
            }

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
     * 根据医生id和常用方名查找
     *
     * @param doctorId         医生id
     * @param commonRecipeName 常用方名
     * @return
     */
    public Integer getByDoctorIdAndName(Integer doctorId, String commonRecipeName) {
        LOGGER.info("CommonRecipeManager validateParam getByDoctorIdAndName doctorId:{},commonRecipeName:{}", doctorId, commonRecipeName);
        try {
            List<CommonRecipe> list = commonRecipeDAO.findByName(doctorId, commonRecipeName);
            if (CollectionUtils.isNotEmpty(list)) {
                return list.size();
            } else {
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("CommonRecipeManager validateParam error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "已存在相同常用方名称");
        }
    }

    /**
     * 获取线下常用方
     *
     * @param doctorId 医生id
     * @return
     */
    public List<CommonDTO> offlineCommon(Integer organId, Integer doctorId) {
        DoctorDTO doctorDTO = doctorClient.getDoctor(doctorId);
        return offlineRecipeClient.offlineCommonRecipe(organId, doctorDTO);
    }


}
