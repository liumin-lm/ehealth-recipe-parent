package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeExtDTO;
import com.ngari.recipe.dto.ValidateOrganDrugDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ICommonRecipeBusinessService;
import recipe.manager.CommonRecipeManager;
import recipe.manager.DrugManager;
import recipe.manager.OrganDrugListManager;
import recipe.manager.PharmacyManager;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 常用方服务
 * Created by jiangtingfeng on 2017/5/23.
 *
 * @author jiangtingfeng
 */
@Service
public class CommonRecipeBusinessService extends BaseService implements ICommonRecipeBusinessService {
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CommonRecipeManager commonRecipeManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private DrugManager drugManager;


    @Override
    public void saveCommonRecipe(CommonDTO common) {
        //id不为空则删除 重新add数据
        Integer commonRecipeId = common.getCommonRecipeDTO().getCommonRecipeId();
        //常用方参数校验
        CommonRecipe commonRecipe = ObjectCopyUtils.convert(common.getCommonRecipeDTO(), CommonRecipe.class);
        List<CommonRecipeDrug> drugList = ObjectCopyUtils.convert(common.getCommonRecipeDrugList(), CommonRecipeDrug.class);
        validateParam(commonRecipe, drugList);
        try {
            CommonRecipeExt commonRecipeExt = ObjectCopyUtils.convert(common.getCommonRecipeExt(), CommonRecipeExt.class);
            commonRecipeManager.saveCommonRecipe(commonRecipe, commonRecipeExt, drugList);
            commonRecipeManager.removeCommonRecipe(commonRecipeId);
        } catch (DAOException e) {
            LOGGER.error("addCommonRecipe error. commonRecipe={}, drugList={}", JSONUtils.toString(commonRecipe), JSONUtils.toString(drugList), e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方添加出错");
        }
    }

    /**
     * 删除常用方
     *
     * @param commonRecipeId
     */
    @Override
    public void deleteCommonRecipe(Integer commonRecipeId) {
        LOGGER.info("CommonRecipeService.deleteCommonRecipe  commonRecipeId = " + commonRecipeId);
        commonRecipeManager.removeCommonRecipe(commonRecipeId);
    }


    @Override
    public List<CommonDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        List<CommonDTO> commonList = new LinkedList<>();
        //获取常用方
        List<CommonRecipe> commonRecipeList = commonRecipeManager.commonRecipeList(organId, doctorId, recipeType, start, limit);
        if (CollectionUtils.isEmpty(commonRecipeList)) {
            return commonList;
        }
        //获取到常用方中的扩展信息
        List<Integer> commonRecipeIdList = commonRecipeList.stream().map(CommonRecipe::getCommonRecipeId).collect(Collectors.toList());
        Map<Integer, CommonRecipeExt> commonRecipeExtMap = commonRecipeManager.commonRecipeExtDTOMap(commonRecipeIdList);
        //获取到常用方中的药品信息
        Map<Integer, List<com.ngari.recipe.dto.CommonRecipeDrugDTO>> commonDrugGroup = commonRecipeManager.commonDrugGroup(organId, commonRecipeIdList);
        if (null == commonDrugGroup) {
            return commonList;
        }
        //药房信息
        Map<Integer, PharmacyTcm> pharmacyIdMap = pharmacyManager.pharmacyIdMap(organId);
        //组织出参
        commonRecipeList.forEach(a -> {
            CommonDTO commonDTO = new CommonDTO();
            List<com.ngari.recipe.dto.CommonRecipeDrugDTO> commonDrugList = commonDrugGroup.get(a.getCommonRecipeId());
            if (CollectionUtils.isNotEmpty(commonDrugList)) {
                List<CommonRecipeDrugDTO> commonRecipeDrugList = new LinkedList<>();
                //药品名拼接配置
                Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, a.getRecipeType()));
                //药品商品名拼接配置
                Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(organId, a.getRecipeType()));
                commonDrugList.forEach(item -> {
                    //药品名历史数据处理---取实时的
                    item.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(item, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(a.getRecipeType())));
                    item.setDrugDisplaySplicedSaleName(DrugDisplayNameProducer.getDrugName(item, configSaleNameMap, DrugNameDisplayUtil.getSaleNameConfigKey(a.getRecipeType())));
                    CommonRecipeDrugDTO commonRecipeDrugDTO = ObjectCopyUtils.convert(item, CommonRecipeDrugDTO.class);
                    commonRecipeDrugDTO.setUseDoseAndUnitRelation(RecipeUtil.defaultUseDose(item.getOrganDrugList()));
                    commonRecipeDrugList.add(commonRecipeDrugDTO);
                });
                commonDTO.setCommonRecipeDrugList(commonRecipeDrugList);
            }
            //扩展信息
            CommonRecipeExt commonRecipeExt = commonRecipeExtMap.get(a.getCommonRecipeId());
            if (null != commonRecipeExt) {
                CommonRecipeExtDTO commonRecipeExtDTO = ObjectCopyUtils.convert(commonRecipeExt, CommonRecipeExtDTO.class);
                commonDTO.setCommonRecipeExt(commonRecipeExtDTO);
            }
            //药房
            PharmacyTcm pharmacyTcm = PharmacyManager.pharmacyById(a.getPharmacyId(), pharmacyIdMap);
            if (null != pharmacyTcm) {
                a.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                a.setPharmacyName(pharmacyTcm.getPharmacyName());
            }
            commonDTO.setCommonRecipeDTO(ObjectCopyUtils.convert(a, CommonRecipeDTO.class));
            commonList.add(commonDTO);
        });
        return commonList;
    }


    @Override
    public List<CommonDTO> offlineCommon(Integer organId, Integer doctorId) {
        //获取线下常用方
        List<com.ngari.his.recipe.mode.CommonDTO> offlineCommonList = commonRecipeManager.offlineCommon(organId, doctorId);
        List<CommonDTO> result = new LinkedList<>();
        if (CollectionUtils.isEmpty(offlineCommonList)) {
            return result;
        }
        offlineCommonList.forEach(a -> {
            CommonDTO commonDTO = new CommonDTO();
            commonDTO.setCommonRecipeDTO(ObjectCopyUtils.convert(a.getCommonRecipeDTO(), CommonRecipeDTO.class));
            commonDTO.setCommonRecipeExt(ObjectCopyUtils.convert(a.getCommonRecipeExt(), CommonRecipeExtDTO.class));
            commonDTO.setCommonRecipeDrugList(ObjectCopyUtils.convert(a.getCommonRecipeDrugList(), CommonRecipeDrugDTO.class));
            result.add(commonDTO);
        });

        if (CollectionUtils.isEmpty(offlineCommonList)) {
            return result;
        }
        //关联常用方主键
        List<CommonRecipe> commonRecipeList = commonRecipeManager.offlineCommonRecipeList(organId, doctorId);
        if (CollectionUtils.isEmpty(commonRecipeList)) {
            return result;
        }
        Map<String, CommonRecipe> commonRecipeMap = commonRecipeList.stream().collect(Collectors.toMap(CommonRecipe::getCommonRecipeCode, a -> a, (k1, k2) -> k1));
        result.forEach(a -> {
            CommonRecipeDTO offlineCommonRecipeDTO = a.getCommonRecipeDTO();
            if (null == offlineCommonRecipeDTO) {
                return;
            }
            CommonRecipe commonRecipe = commonRecipeMap.get(offlineCommonRecipeDTO.getCommonRecipeCode());
            if (null == commonRecipe) {
                return;
            }
            offlineCommonRecipeDTO.setCommonRecipeId(commonRecipe.getCommonRecipeId());
        });
        return result;
    }


    @Override
    public List<String> addOfflineCommon(Integer organId, List<CommonDTO> commonList) {
        List<String> failNameList = new LinkedList<>();
        //查询字典数据
        Map<String, PharmacyTcm> pharmacyCodeMap = pharmacyManager.pharmacyCodeMap(organId);
        Map<String, DecoctionWay> decoctionWayCodeMap = drugManager.decoctionWayCodeMap(organId);
        Map<String, DrugMakingMethod> makingMethodCodeMap = drugManager.drugMakingMethodCodeMap(organId);
        Map<String, UsingRate> usingRateCodeMap = drugManager.usingRateMapCode(organId);
        Map<String, UsePathways> usePathwaysCodeMap = drugManager.usePathwaysCodeMap(organId);
        Map<String, DrugEntrust> drugEntrustNameMap = drugManager.drugEntrustNameMap(organId);
        //查询机构药品
        List<String> drugCodeList = commonList.stream().filter(a -> CollectionUtils.isNotEmpty(a.getCommonRecipeDrugList()))
                .flatMap(a -> a.getCommonRecipeDrugList().stream().map(CommonRecipeDrugDTO::getOrganDrugCode)).distinct().collect(Collectors.toList());
        Map<String, List<OrganDrugList>> organDrugGroup = organDrugListManager.getOrganDrugCode(organId, drugCodeList);
        //数据比对转线上数据
        commonList.forEach(a -> {
            //常用方药房比对
            CommonRecipeDTO commonRecipeDTO = a.getCommonRecipeDTO();
            if (StringUtils.isNotEmpty(commonRecipeDTO.getPharmacyCode())) {
                PharmacyTcm pharmacyTcm = pharmacyCodeMap.get(commonRecipeDTO.getPharmacyCode());
                if (null == pharmacyTcm) {
                    failNameList.add(commonRecipeDTO.getCommonRecipeName());
                    LOGGER.info("CommonRecipeService addOfflineCommon pharmacyTcm is null ：{}", commonRecipeDTO.getPharmacyCode());
                    return;
                }
                commonRecipeDTO.setPharmacyName(pharmacyTcm.getPharmacyName());
                commonRecipeDTO.setPharmacyId(pharmacyTcm.getPharmacyId());
                commonRecipeDTO.setPharmacyCode(pharmacyTcm.getPharmacyCode());
            }
            CommonRecipe commonRecipe = ObjectCopyUtils.convert(commonRecipeDTO, CommonRecipe.class);
            //药品信息比对
            List<CommonRecipeDrugDTO> commonRecipeDrugList = a.getCommonRecipeDrugList();
            if (CollectionUtils.isEmpty(commonRecipeDrugList)) {
                failNameList.add(commonRecipeDTO.getCommonRecipeName());
                LOGGER.info("CommonRecipeService addOfflineCommon commonRecipeDrugList is null ：{}", commonRecipeDTO.getCommonRecipeName());
                return;
            }
            List<CommonRecipeDrug> drugList = offlineCommonRecipeDrug(commonRecipeDrugList, organDrugGroup, pharmacyCodeMap, usingRateCodeMap, usePathwaysCodeMap, drugEntrustNameMap);
            if (CollectionUtils.isEmpty(drugList)) {
                failNameList.add(commonRecipeDTO.getCommonRecipeName());
                LOGGER.info("CommonRecipeService addOfflineCommon drugList is null ：{}", commonRecipeDTO.getCommonRecipeName());
                return;
            }
            if (drugList.size() != commonRecipeDrugList.size()) {
                failNameList.add(commonRecipeDTO.getCommonRecipeName());
            }
            //扩展信息转换
            offlineCommonRecipeExt(a.getCommonRecipeExt(), decoctionWayCodeMap, makingMethodCodeMap);
            //写入表
            CommonRecipeExt commonRecipeExt = ObjectCopyUtils.convert(a.getCommonRecipeExt(), CommonRecipeExt.class);
            commonRecipeManager.saveCommonRecipe(commonRecipe, commonRecipeExt, drugList);
        });
        return failNameList;
    }

    /**
     * 参数校验
     *
     * @param commonRecipe 常用方头
     * @param drugList     常用方药品
     */
    private void validateParam(CommonRecipe commonRecipe, List<CommonRecipeDrug> drugList) {
        // 常用方名称校验
        Integer commonRecipeNameSize = commonRecipeManager.getByDoctorIdAndName(commonRecipe.getDoctorId(), commonRecipe.getCommonRecipeName(), commonRecipe.getCommonRecipeId());
        if (commonRecipeNameSize > 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "已存在相同常用方名称");
        }

        List<String> drugCodeList = drugList.stream().map(CommonRecipeDrug::getOrganDrugCode).distinct().collect(Collectors.toList());
        Map<String, List<OrganDrugList>> organDrugGroup = organDrugListManager.getOrganDrugCode(commonRecipe.getOrganId(), drugCodeList);
        if (organDrugGroup.isEmpty()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品为空");
        }
        //常用方药品校验
        drugList.forEach(a -> {
            if (StringUtils.isAnyEmpty(a.getUsingRate(), a.getUsePathways(), a.getUsingRateId(), a.getUsePathwaysId())) {
                LOGGER.info("validateParam usingRate:{},usePathways:{},usingRateId:{},usePathwaysId:{}", a.getUsingRate(), a.getUsePathways(), a.getUsingRateId(), a.getUsePathwaysId());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "用药频次和用药方式不能为空");
            }
            //校验比对药品
            ValidateOrganDrugDTO validateOrganDrugDTO = new ValidateOrganDrugDTO(a.getOrganDrugCode(), null, a.getDrugId());
            OrganDrugList organDrug = OrganDrugListManager.validateOrganDrug(validateOrganDrugDTO, organDrugGroup);
            if (null == organDrug) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品错误");
            }
            //校验药品药房变动
            if (null != a.getPharmacyId() && StringUtils.isNotEmpty(organDrug.getPharmacy())
                    && !Arrays.asList(organDrug.getPharmacy().split(ByteUtils.COMMA)).contains(String.valueOf(a.getPharmacyId()))) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品药房错误");
            }
            if (StringUtils.isEmpty(a.getOrganDrugCode())) {
                a.setOrganDrugCode(organDrug.getOrganDrugCode());
            }
            a.setSalePrice(null);
            a.setDrugCost(null);
        });
    }

    /**
     * 线下 扩展信息转换
     *
     * @param commonRecipeExt         常用方线下扩展信息
     * @param decoctionWayCodeMap     煎法字典
     * @param drugMakingMethodCodeMap 制法字典
     * @return 常用方 线下扩展信息 转线上扩展信息数据
     */
    private void offlineCommonRecipeExt(CommonRecipeExtDTO commonRecipeExt, Map<String, DecoctionWay> decoctionWayCodeMap, Map<String, DrugMakingMethod> drugMakingMethodCodeMap) {
        if (null == commonRecipeExt) {
            return;
        }
        //煎法
        DecoctionWay decoctionWay = DrugClient.validateDecoction(commonRecipeExt.getDecoctionCode(), decoctionWayCodeMap);
        commonRecipeExt.setDecoctionId(ByteUtils.objValueOf(decoctionWay.getDecoctionId()));
        commonRecipeExt.setDecoctionText(decoctionWay.getDecoctionText());
        //制法
        DrugMakingMethod drugMakingMethod = DrugClient.validateMakeMethod(commonRecipeExt.getMakeMethod(), drugMakingMethodCodeMap);
        commonRecipeExt.setMakeMethodId(ByteUtils.objValueOf(drugMakingMethod.getMethodId()));
        commonRecipeExt.setMakeMethodText(drugMakingMethod.getMethodText());
    }

    /**
     * 常用方药品信息比对
     *
     * @param commonRecipeDrugList 常用线下药品
     * @param organDrugGroup       机构药品code分组
     * @param pharmacyCodeMap      机构药房信息
     * @param usingRateCodeMap     机构的用药频率
     * @param usePathwaysCodeMap   机构的用药途径
     * @param drugEntrustNameMap   机构嘱托
     * @return 常用方 线下药品 转线上药品数据
     */
    private List<CommonRecipeDrug> offlineCommonRecipeDrug(List<CommonRecipeDrugDTO> commonRecipeDrugList, Map<String, List<OrganDrugList>> organDrugGroup,
                                                           Map<String, PharmacyTcm> pharmacyCodeMap, Map<String, UsingRate> usingRateCodeMap,
                                                           Map<String, UsePathways> usePathwaysCodeMap, Map<String, DrugEntrust> drugEntrustNameMap) {
        List<CommonRecipeDrug> drugList = new LinkedList<>();
        commonRecipeDrugList.forEach(b -> {
            CommonRecipeDrugDTO drug = offlineCommonRecipeDrug(b, organDrugGroup, pharmacyCodeMap, usingRateCodeMap, usePathwaysCodeMap, drugEntrustNameMap);
            if (null == drug) {
                return;
            }
            CommonRecipeDrug commonRecipeDrug = ObjectCopyUtils.convert(drug, CommonRecipeDrug.class);
            drugList.add(commonRecipeDrug);
        });
        return drugList;
    }

    /**
     * 常用方药品信息比对
     *
     * @param drug               常用线下药品
     * @param organDrugGroup     机构药品code分组
     * @param pharmacyCodeMap    机构药房信息
     * @param usingRateCodeMap   机构的用药频率
     * @param usePathwaysCodeMap 机构的用药途径
     * @param drugEntrustNameMap 机构嘱托
     * @return 常用方 线下药品 转线上药品数据
     */
    private CommonRecipeDrugDTO offlineCommonRecipeDrug(CommonRecipeDrugDTO drug, Map<String, List<OrganDrugList>> organDrugGroup,
                                                        Map<String, PharmacyTcm> pharmacyCodeMap, Map<String, UsingRate> usingRateCodeMap,
                                                        Map<String, UsePathways> usePathwaysCodeMap, Map<String, DrugEntrust> drugEntrustNameMap) {
        //校验比对药品
        ValidateOrganDrugDTO validateOrganDrugDTO = new ValidateOrganDrugDTO(drug.getOrganDrugCode(), null, null);
        OrganDrugList organDrug = OrganDrugListManager.validateOrganDrug(validateOrganDrugDTO, organDrugGroup);
        if (null == organDrug) {
            LOGGER.warn("CommonRecipeService offlineCommonRecipeDrug organDrug OrganDrugCode ：{}", drug.getOrganDrugCode());
            return null;
        }
        drug.setDrugId(organDrug.getDrugId());
        //校验药品药房变动
        if (PharmacyManager.pharmacyVariation(null, drug.getPharmacyCode(), organDrug.getPharmacy(), pharmacyCodeMap)) {
            LOGGER.warn("CommonRecipeService offlineCommonRecipeDrug pharmacy OrganDrugCode ：{}", drug.getOrganDrugCode());
            return null;
        }
        drug.setDrugName(organDrug.getDrugName());
        drug.setSaleName(organDrug.getSaleName());
        drug.setPlatformSaleName(organDrug.getSaleName());
        //药品单位
        drug.setUseDoseUnit(OrganDrugListManager.getUseDoseUnit(drug.getUseDoseUnit(), organDrug));
        if (StringUtils.isEmpty(drug.getUseDoseUnit())) {
            drug.setUseDose(null);
        }

        PharmacyTcm pharmacyTcm = pharmacyCodeMap.get(drug.getPharmacyCode());
        if (null == pharmacyTcm) {
            pharmacyTcm = new PharmacyTcm();
        }
        drug.setPharmacyCode(pharmacyTcm.getPharmacyCode());
        drug.setPharmacyId(pharmacyTcm.getPharmacyId());
        drug.setPharmacyName(pharmacyTcm.getPharmacyName());

        //用药频率
        UsingRate usingRate = usingRateCodeMap.get(drug.getUsingRate());
        if (null == usingRate) {
            usingRate = new UsingRate();
        }
        drug.setUsingRate(usingRate.getUsingRateKey());
        drug.setUsingRateId(ByteUtils.objValueOf(usingRate.getId()));
        drug.setUsingRateEnglishNames(usingRate.getEnglishNames());
        //频次
        UsePathways usePathways = usePathwaysCodeMap.get(drug.getUsePathways());
        if (null == usePathways) {
            usePathways = new UsePathways();
        }
        drug.setUsePathways(usePathways.getPathwaysKey());
        drug.setUsePathwaysId(ByteUtils.objValueOf(usePathways.getId()));
        drug.setUsePathEnglishNames(usePathways.getEnglishNames());
        //嘱托
        DrugEntrust drugEntrust = drugEntrustNameMap.get(drug.getMemo());
        if (null == drugEntrust) {
            drugEntrust = new DrugEntrust();
            drugEntrust.setDrugEntrustName(drug.getMemo());
        }
        drug.setDrugEntrustCode(drugEntrust.getDrugEntrustCode());
        drug.setDrugEntrustId(ByteUtils.objValueOf(drugEntrust.getDrugEntrustId()));
        drug.setMemo(drugEntrust.getDrugEntrustName());
        return drug;
    }
}
