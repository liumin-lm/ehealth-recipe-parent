package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeExtDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.constant.ErrorCode;
import recipe.dao.*;
import recipe.service.manager.CommonRecipeManager;
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
public class CommonRecipeService extends BaseService<CommonRecipeDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonRecipeService.class);

    @Autowired
    private CommonRecipeManager commonRecipeManager;

    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;

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
        addCommonRecipe(commonRecipeDTO, commonRecipeDTO.getCommonRecipeExt(), drugListDTO);
    }

    /**
     * 新增或更新常用方
     *
     * @param commonRecipeDTO 常用方
     * @param commonRecipeExt 中药扩展信息
     * @param drugListDTO     常用方药品
     * @return
     */
    public void addCommonRecipe(CommonRecipeDTO commonRecipeDTO, CommonRecipeExtDTO commonRecipeExt, List<CommonRecipeDrugDTO> drugListDTO) {
        //id不为空则删除 重新add数据
        Integer commonRecipeId = commonRecipeDTO.getCommonRecipeId();
        //常用方参数校验
        CommonRecipe commonRecipe = ObjectCopyUtils.convert(commonRecipeDTO, CommonRecipe.class);
        List<CommonRecipeDrug> drugList = ObjectCopyUtils.convert(drugListDTO, CommonRecipeDrug.class);
        validateParam(commonRecipe, drugList);
        try {
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
    @RpcService
    @Deprecated
    public void deleteCommonRecipe(Integer commonRecipeId) {
        LOGGER.info("CommonRecipeService.deleteCommonRecipe  commonRecipeId = " + commonRecipeId);
        commonRecipeManager.removeCommonRecipe(commonRecipeId);
    }

    /**
     * 获取常用方列表
     *
     * @param organId
     * @param doctorId
     * @param recipeType
     * @param start
     * @param limit
     * @return
     */
    public List<CommonRecipeDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        //获取常用方
        List<CommonRecipeDTO> commonRecipeList = commonRecipeManager.commonRecipeList(organId, doctorId, recipeType, start, limit);
        if (CollectionUtils.isEmpty(commonRecipeList)) {
            return null;
        }
        //获取到常用方中的扩展信息
        List<Integer> commonRecipeIdList = commonRecipeList.stream().map(CommonRecipeDTO::getCommonRecipeId).collect(Collectors.toList());
        Map<Integer, CommonRecipeExtDTO> commonRecipeExtMap = commonRecipeManager.commonRecipeExtDTOMap(commonRecipeIdList);
        //获取到常用方中的药品信息
        Map<Integer, List<CommonRecipeDrugDTO>> commonDrugGroup = commonRecipeManager.commonDrugGroup(organId, commonRecipeIdList);
        if (null == commonDrugGroup) {
            return commonRecipeList;
        }
        //药房信息
        List<PharmacyTcm> pharmacyList = pharmacyTcmDAO.findByOrganId(organId);
        Map<Integer, PharmacyTcm> pharmacyMap = Optional.ofNullable(pharmacyList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
        //组织出参
        commonRecipeList.forEach(a -> {
            List<CommonRecipeDrugDTO> commonDrugList = commonDrugGroup.get(a.getCommonRecipeId());
            if (CollectionUtils.isNotEmpty(commonDrugList)) {
                //药品名拼接配置
                Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, a.getRecipeType()));
                //药品商品名拼接配置
                Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(organId, a.getRecipeType()));
                commonDrugList.forEach(item -> {
                    //药品名历史数据处理---取实时的
                    item.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(item, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(a.getRecipeType())));
                    item.setDrugDisplaySplicedSaleName(DrugDisplayNameProducer.getDrugName(item, configSaleNameMap, DrugNameDisplayUtil.getSaleNameConfigKey(a.getRecipeType())));
                });
                a.setCommonDrugList(commonDrugList);
            }
            CommonRecipeExtDTO commonRecipeExt = commonRecipeExtMap.get(a.getCommonRecipeId());
            if (null != commonRecipeExt) {
                a.setCommonRecipeExt(commonRecipeExt);
            }
            if (null == a.getPharmacyId()) {
                return;
            }
            PharmacyTcm pharmacyTcm = pharmacyMap.get(a.getPharmacyId());
            if (null == pharmacyTcm) {
                return;
            }
            a.setPharmacyCode(pharmacyTcm.getPharmacyCode());
            a.setPharmacyName(pharmacyTcm.getPharmacyName());
        });
        return commonRecipeList;
    }
    
    /**
     * 查询常用方和常用方下的药品列表信息  查询常用方的详细信息
     * 新版废弃/保留兼容老app版本
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

    /**
     * 参数校验
     *
     * @param commonRecipe
     */
    private void validateParam(CommonRecipe commonRecipe, List<CommonRecipeDrug> drugList) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        Integer doctorId = commonRecipe.getDoctorId();
        Integer recipeType = commonRecipe.getRecipeType();
        String commonRecipeName = commonRecipe.getCommonRecipeName();

        if (null == doctorId || null == recipeType || StringUtils.isEmpty(commonRecipeName)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方必填参数为空");
        }

        // 常用方名称校验
        CommonRecipe dbCommonRecipe = commonRecipeManager.getByDoctorIdAndName(commonRecipe.getDoctorId(), commonRecipeName);
        if (null != dbCommonRecipe && !dbCommonRecipe.getCommonRecipeId().equals(commonRecipe.getCommonRecipeId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "已存在相同常用方名称");
        }

        Date now = DateTime.now().toDate();
        commonRecipe.setCreateDt(now);
        commonRecipe.setLastModify(now);

        List<Integer> drugIdList = drugList.stream().map(CommonRecipeDrug::getDrugId).distinct().collect(Collectors.toList());
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugIdList(commonRecipe.getOrganId(), drugIdList);
        LOGGER.info("validateParam organDrugLists:{}", JSONUtils.toString(organDrugLists));
        if (CollectionUtils.isEmpty(organDrugLists)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品为空");
        }
        Map<Integer, List<OrganDrugList>> organDrugListsGroup = organDrugLists.stream().collect(Collectors.groupingBy(OrganDrugList::getDrugId));

        //常用方药品校验
        drugList.forEach(a -> {
            OrganDrugList organDrugList = null;
            List<OrganDrugList> organDrugs = organDrugListsGroup.get(a.getDrugId());
            if (CollectionUtils.isEmpty(organDrugs)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品错误");
            }
            for (OrganDrugList organDrug : organDrugs) {
                if (organDrug.getOrganDrugCode().equals(a.getOrganDrugCode())) {
                    organDrugList = organDrug;
                    break;
                }
            }
            if (null == organDrugList) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品错误");
            }
            if (null != a.getPharmacyId() && StringUtils.isNotEmpty(organDrugList.getPharmacy())
                    && !Arrays.asList(organDrugList.getPharmacy().split(ByteUtils.COMMA)).contains(String.valueOf(a.getPharmacyId()))) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品药房错误");
            }
            if (StringUtils.isEmpty(a.getOrganDrugCode())) {
                a.setOrganDrugCode(organDrugs.get(0).getOrganDrugCode());
            }
            if (StringUtils.isAnyEmpty(a.getUsingRate(), a.getUsePathways(), a.getUsingRateId(), a.getUsePathwaysId())) {
                LOGGER.info("validateParam usingRate:{},usePathways:{},usingRateId:{},usePathwaysId:{}", a.getUsingRate(), a.getUsePathways(), a.getUsingRateId(), a.getUsePathwaysId());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "用药频次和用药方式不能为空");
            }
            a.setSalePrice(null);
            a.setDrugCost(null);
            a.setCreateDt(now);
            a.setLastModify(now);
        });

    }

}
