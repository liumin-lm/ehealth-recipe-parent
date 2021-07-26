package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeExtDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.dto.ValidateOrganDrugDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ICommonRecipeService;
import recipe.dao.CommonRecipeDAO;
import recipe.dao.CommonRecipeDrugDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.manager.CommonRecipeManager;
import recipe.manager.DrugManeger;
import recipe.manager.OrganDrugListManager;
import recipe.manager.PharmacyManager;
import recipe.serviceprovider.BaseService;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 常用方服务
 * Created by jiangtingfeng on 2017/5/23.
 *
 * @author jiangtingfeng
 */
@RpcBean("commonRecipeService")
public class CommonRecipeService extends BaseService<CommonRecipeDTO> implements ICommonRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonRecipeService.class);

    @Autowired
    private CommonRecipeManager commonRecipeManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private DrugManeger drugManeger;


    /**
     * 新增或更新常用方  选好药品后将药品加入到常用处方
     *
     * @param commonRecipeDTO 常用方
     * @param drugListDTO     常用方药品
     */
    @RpcService
    @Deprecated
    public void addCommonRecipe(CommonRecipeDTO commonRecipeDTO, List<CommonRecipeDrugDTO> drugListDTO) {
        LOGGER.info("CommonRecipeService addCommonRecipe commonRecipe:{},drugList:{}", JSONUtils.toString(commonRecipeDTO), JSONUtils.toString(drugListDTO));
        if (null == commonRecipeDTO || CollectionUtils.isEmpty(drugListDTO)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方数据不完整，请重试");
        }
        CommonDTO common = new CommonDTO(commonRecipeDTO, commonRecipeDTO.getCommonRecipeExt(), drugListDTO);
        saveCommonRecipe(common);
    }


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
     * todo 需要删除 RpcService标签
     *
     * @param commonRecipeId
     */
    @Override
    @RpcService
    @Deprecated
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
        Map<String, DecoctionWay> decoctionWayCodeMap = drugManeger.decoctionWayCodeMap(organId);
        Map<String, DrugMakingMethod> makingMethodCodeMap = drugManeger.drugMakingMethodCodeMap(organId);
        Map<String, UsingRate> usingRateCodeMap = drugManeger.usingRateMapCode(organId);
        Map<String, UsePathways> usePathwaysCodeMap = drugManeger.usePathwaysCodeMap(organId);
        Map<String, DrugEntrust> drugEntrustNameMap = drugManeger.drugEntrustNameMap(organId);
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
            ValidateOrganDrugDTO validateOrganDrugDTO = new ValidateOrganDrugDTO(a.getOrganDrugCode(), null, null);
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


    /**
     * 查询常用方和常用方下的药品列表信息  查询常用方的详细信息
     * 新版废弃/保留兼容老app版本
     * todo 三个月后删除
     *
     * @param commonRecipeId
     * @return
     */
    @RpcService
    @Deprecated
    public Map getCommonRecipeDetails(Integer commonRecipeId) {
        if (null == commonRecipeId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "commonRecipeId is null");
        }
        LOGGER.info("getCommonRecipeDetails commonRecipeId={}", commonRecipeId);

        CommonRecipeDrugDAO commonRecipeDrugDAO = DAOFactory.getDAO(CommonRecipeDrugDAO.class);
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        Map map = Maps.newHashMap();
        //通过常用方Id获取常用方
        CommonRecipe commonRecipe = commonRecipeDAO.get(commonRecipeId);
        if (null == commonRecipe) {
            map.put("commonRecipe", null);
            return map;
        }
        CommonRecipeDTO commonRecipeDTO = getBean(commonRecipe, CommonRecipeDTO.class);
        //通过常用方id去获取常用方中的药品信息
        List<CommonRecipeDrug> drugList = commonRecipeDrugDAO.findByCommonRecipeId(commonRecipeId);
        List<CommonRecipeDrugDTO> drugDtoList = ObjectCopyUtils.convert(drugList, CommonRecipeDrugDTO.class);

        List<String> organDrugCodeList = new ArrayList<>(drugDtoList.size());
        List<Integer> drugIdList = new ArrayList<>(drugDtoList.size());
        for (CommonRecipeDrugDTO commonRecipeDrug : drugDtoList) {
            if (null != commonRecipeDrug && null != commonRecipeDrug.getDrugId()) {
                organDrugCodeList.add(commonRecipeDrug.getOrganDrugCode());
                drugIdList.add(commonRecipeDrug.getDrugId());
            }
        }
        if (CollectionUtils.isNotEmpty(drugIdList)) {
            // 查询机构药品表，同步药品状态
            //是否为老的药品兼容方式，老的药品传入方式没有organDrugCode
            boolean oldFlag = organDrugCodeList.isEmpty() ? true : false;
            List<OrganDrugList> organDrugList = Lists.newArrayList();
            List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList;
            IUsingRateService usingRateService = AppDomainContext.getBean("eh.usingRateService", IUsingRateService.class);
            IUsePathwaysService usePathwaysService = AppDomainContext.getBean("eh.usePathwaysService", IUsePathwaysService.class);
            UsingRateDTO usingRateDTO;
            UsePathwaysDTO usePathwaysDTO;
            if (oldFlag) {
                organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(commonRecipeDTO.getOrganId(), drugIdList);
                for (CommonRecipeDrugDTO commonRecipeDrug : drugDtoList) {
                    Integer durgId = commonRecipeDrug.getDrugId();
                    for (OrganDrugList organDrug : organDrugList) {
                        if ((durgId.equals(organDrug.getDrugId()))) {
                            commonRecipeDrug.setDrugStatus(organDrug.getStatus());
                            commonRecipeDrug.setSalePrice(organDrug.getSalePrice());
                            commonRecipeDrug.setPrice1(organDrug.getSalePrice().doubleValue());
                            commonRecipeDrug.setDrugForm(organDrug.getDrugForm());
                            //添加平台药品ID
                            if (null != commonRecipeDrug.getUseTotalDose()) {
                                commonRecipeDrug.setDrugCost(organDrug.getSalePrice().multiply(
                                        new BigDecimal(commonRecipeDrug.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
                            }
                            //设置医生端每次剂量和剂量单位联动关系
                            useDoseAndUnitRelationList = Lists.newArrayList();
                            //用药单位不为空时才返回给前端
                            if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
                            }
                            if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
                            }
                            try {
                                commonRecipeDrug.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
                                usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(organDrug.getOrganId(), commonRecipeDrug.getUsingRate());
                                if (usingRateDTO != null) {
                                    commonRecipeDrug.setUsingRateId(String.valueOf(usingRateDTO.getId()));
                                }
                                usePathwaysDTO = usePathwaysService.getUsePathwaysByOrganAndPlatformKey(organDrug.getOrganId(), commonRecipeDrug.getUsePathways());
                                if (usePathwaysDTO != null) {
                                    commonRecipeDrug.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
                                }
                            } catch (Exception e) {
                                LOGGER.info("getCommonRecipeDetails error,commonRecipeId={}", commonRecipeId, e);
                            }
                            break;
                        }
                    }
                }
            } else {
                //药品名拼接配置
                Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(commonRecipeDTO.getOrganId(), commonRecipeDTO.getRecipeType()));
                organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(commonRecipeDTO.getOrganId(), organDrugCodeList);
                for (CommonRecipeDrugDTO commonRecipeDrug : drugDtoList) {
                    Integer durgId = commonRecipeDrug.getDrugId();
                    String drugCode = commonRecipeDrug.getOrganDrugCode();
                    DrugList drug = drugListDAO.getById(durgId);
                    for (OrganDrugList organDrug : organDrugList) {
                        if ((durgId.equals(organDrug.getDrugId()) && drugCode.equals(organDrug.getOrganDrugCode()))) {
                            commonRecipeDrug.setDrugStatus(organDrug.getStatus());
                            commonRecipeDrug.setSalePrice(organDrug.getSalePrice());
                            commonRecipeDrug.setPrice1(organDrug.getSalePrice().doubleValue());
                            commonRecipeDrug.setDrugForm(organDrug.getDrugForm());
                            //添加平台药品ID
                            //平台药品商品名
                            if (drug != null) {
                                commonRecipeDrug.setPlatformSaleName(drug.getSaleName());
                            }
                            if (null != commonRecipeDrug.getUseTotalDose()) {
                                commonRecipeDrug.setDrugCost(organDrug.getSalePrice().multiply(
                                        new BigDecimal(commonRecipeDrug.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
                            }
                            //设置药品拼接名
                            //要用药品名实时配置
                            commonRecipeDrug.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(commonRecipeDrug, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(commonRecipeDTO.getRecipeType())));
                            //设置医生端每次剂量和剂量单位联动关系
                            useDoseAndUnitRelationList = Lists.newArrayList();
                            //用药单位不为空时才返回给前端
                            if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
                            }
                            if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
                            }
                            commonRecipeDrug.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
                            try {
                                commonRecipeDrug.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
                                usingRateDTO = usingRateService.getUsingRateDTOByOrganAndPlatformKey(organDrug.getOrganId(), commonRecipeDrug.getUsingRate());
                                if (usingRateDTO != null) {
                                    commonRecipeDrug.setUsingRateId(String.valueOf(usingRateDTO.getId()));
                                }
                                usePathwaysDTO = usePathwaysService.getUsePathwaysByOrganAndPlatformKey(organDrug.getOrganId(), commonRecipeDrug.getUsePathways());
                                if (usePathwaysDTO != null) {
                                    commonRecipeDrug.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
                                }
                            } catch (Exception e) {
                                LOGGER.info("getCommonRecipeDetails error,commonRecipeId={}", commonRecipeId, e);
                            }
                            break;
                        }
                    }
                }
            }
        }
        map.put("drugList", drugDtoList);
        map.put("commonRecipe", commonRecipeDTO);
        return map;
    }

}
