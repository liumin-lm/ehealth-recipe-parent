package recipe.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.CommonRecipe;
import com.ngari.recipe.entity.CommonRecipeDrug;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
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
import recipe.bussutil.RecipeUtil;
import recipe.constant.ErrorCode;
import recipe.dao.CommonRecipeDAO;
import recipe.dao.CommonRecipeDrugDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.serviceprovider.BaseService;
import recipe.util.ByteUtils;

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

    /**
     * 新增或更新常用方  选好药品后将药品加入到常用处方
     *
     * @param commonRecipeDTO
     * @param drugListDTO
     */
    @RpcService
    public void addCommonRecipe(CommonRecipeDTO commonRecipeDTO, List<CommonRecipeDrugDTO> drugListDTO) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        CommonRecipeDrugDAO commonRecipeDrugDAO = DAOFactory.getDAO(CommonRecipeDrugDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
//        LOGGER.info("addCommonRecipe param. commonRecipe={}, drugList={}", JSONUtils.toString(commonRecipe),
//                JSONUtils.toString(drugList));
        if (null != commonRecipeDTO && CollectionUtils.isNotEmpty(drugListDTO)) {
            CommonRecipe commonRecipe = ObjectCopyUtils.convert(commonRecipeDTO, CommonRecipe.class);
            List<CommonRecipeDrug> drugList = ObjectCopyUtils.convert(drugListDTO, CommonRecipeDrug.class);

            Integer commonRecipeId = commonRecipe.getCommonRecipeId();
            LOGGER.info("addCommonRecipe commonRecipeId={} ", commonRecipeId);
            validateParam(commonRecipe, drugList);
            List<Integer> drugIdList = drugListDTO.stream().map(CommonRecipeDrugDTO::getDrugId).distinct().collect(Collectors.toList());
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugIdList(commonRecipe.getOrganId(), drugIdList);
            if (CollectionUtils.isEmpty(organDrugLists)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品为空");
            }
            Map<String, OrganDrugList> organDrugMap = organDrugLists.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
            drugListDTO.forEach(a -> {
                OrganDrugList organDrugList = organDrugMap.get(a.getDrugId() + a.getOrganDrugCode());
                if (null == organDrugList) {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品错误");
                }
                if (null != a.getPharmacyId() && StringUtils.isNotEmpty(organDrugList.getPharmacy())
                        && !Arrays.asList(organDrugList.getPharmacy().split(ByteUtils.COMMA)).contains(String.valueOf(a.getPharmacyId()))) {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "机构药品药房错误");
                }
            });
            try {
                commonRecipe.setCommonRecipeId(null);
                //如何查看对应的hql语句，
                commonRecipeDAO.save(commonRecipe);
                for (CommonRecipeDrug commonRecipeDrug : drugList) {
                    commonRecipeDrug.setCommonRecipeId(commonRecipe.getCommonRecipeId());
                    if (StringUtils.isEmpty(commonRecipeDrug.getOrganDrugCode())) {
                        //通过机构id和药品id获取
                        List<OrganDrugList> organDrugs = organDrugListDAO.findOrganDrugs(commonRecipeDrug.getDrugId(), commonRecipe.getOrganId(), 1);
                        if (CollectionUtils.isNotEmpty(organDrugs)) {
                            //设置机构药品编码，编码通过药品id和机构id去进行获取
                            commonRecipeDrug.setOrganDrugCode(organDrugs.get(0).getOrganDrugCode());
                        }
                    }
                    commonRecipeDrugDAO.save(commonRecipeDrug);
                }
                if (null != commonRecipeId) {
                    commonRecipeDAO.remove(commonRecipeId);
                }
            } catch (DAOException e) {
                LOGGER.error("addCommonRecipe add to db error. commonRecipe={}, drugList={}", JSONUtils.toString(commonRecipe),
                        JSONUtils.toString(drugList), e);
                throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方添加出错");
            }
        } else {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方数据不完整，请重试");
        }
    }


    /**
     * 删除常用方
     *
     * @param commonRecipeId
     */
    @RpcService
    public void deleteCommonRecipe(Integer commonRecipeId) {
        LOGGER.info("CommonRecipeService.deleteCommonRecipe  commonRecipeId = " + commonRecipeId);
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        // 删除常用方
        commonRecipeDAO.remove(commonRecipeId);
    }

    /**
     * 获取常用方列表
     *
     * @param doctorId
     * @param recipeType 0：获取全部常用方  其他：按类型获取
     * @param start
     * @param limit
     * @return
     */
    @Deprecated
    @RpcService
    public List<CommonRecipeDTO> getCommonRecipeList(Integer organId, Integer doctorId, String recipeType, int start, int limit) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        LOGGER.info("getCommonRecipeList  recipeType={}, doctorId={}, organId={} ", recipeType, doctorId, organId);

        if (null != doctorId) {
            if (StringUtils.isNotEmpty(recipeType) && !"0".equals(recipeType)) {
                List<CommonRecipe> list = commonRecipeDAO.findByRecipeType(Arrays.asList(Integer.valueOf(recipeType)), doctorId, start, limit);
                return getList(list, CommonRecipeDTO.class);
            } else {
                List<CommonRecipe> list = commonRecipeDAO.findByDoctorId(doctorId, start, limit);
                return getList(list, CommonRecipeDTO.class);
            }
        }
        return null;
    }

    /**
     * 获取常用方扩展 获取常用方列表
     *
     * @param organId
     * @param doctorId
     * @param recipeType
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<CommonRecipeDTO> findCommonRecipeListExt(Integer organId, Integer doctorId, List<Integer> recipeType,
                                                         int start, int limit) {

        LOGGER.info("getCommonRecipeListExt  organId={}, doctorId={}, recipeType={}", organId, doctorId, JSONUtils.toString(recipeType));
        if (null == doctorId && null == organId) {
            return null;
        }

        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        CommonRecipeDrugDAO commonRecipeDrugDAO = DAOFactory.getDAO(CommonRecipeDrugDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<CommonRecipeDTO> commonRecipeDTOList;
        if (CollectionUtils.isNotEmpty(recipeType)) {
            //通过处方类型获取常用处方
            List<CommonRecipe> list = commonRecipeDAO.findByRecipeTypeAndOrganId(recipeType, doctorId, organId, start, limit);
            commonRecipeDTOList = getList(list, CommonRecipeDTO.class);
        } else {
            //通过医生id查询常用处方
            List<CommonRecipe> list = commonRecipeDAO.findByDoctorIdAndOrganId(doctorId, organId, start, limit);
            commonRecipeDTOList = getList(list, CommonRecipeDTO.class);
        }

        if (CollectionUtils.isEmpty(commonRecipeDTOList)) {
            return null;
        }

        //获取运营平台是否适量的配置
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        Boolean isAppropriate = (Boolean) configurationService.getConfiguration(organId, "isShowUseDose");

        //获取机构的用药频率和用药方式
        com.ngari.patient.service.IUsingRateService usingRateService = AppDomainContext.getBean("basic.usingRateService", com.ngari.patient.service.IUsingRateService.class);
        List<com.ngari.patient.dto.UsingRateDTO> usingRates = usingRateService.findAllusingRateByOrganId(organId);
        if (CollectionUtils.isEmpty(usingRates)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "CommonRecipeService findCommonRecipeListExt : usingRates is null");
        }
        com.ngari.patient.service.IUsePathwaysService usePathwaysService = AppDomainContext.getBean("basic.usePathwaysService", com.ngari.patient.service.IUsePathwaysService.class);
        List<com.ngari.patient.dto.UsePathwaysDTO> usePathways = usePathwaysService.findAllUsePathwaysByOrganId(organId);
        if (CollectionUtils.isEmpty(usePathways)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "CommonRecipeService findCommonRecipeListExt : usePathways is null");
        }

        Map<Integer, com.ngari.patient.dto.UsingRateDTO> usingRateMap = usingRates.stream().collect(Collectors.toMap(com.ngari.patient.dto.UsingRateDTO::getId, b -> b));
        Map<Integer, com.ngari.patient.dto.UsePathwaysDTO> pathwaysMap = usePathways.stream().collect(Collectors.toMap(com.ngari.patient.dto.UsePathwaysDTO::getId, b -> b));

        //获取到常用方中的药品信息
        List<Integer> commonRecipeIdList = commonRecipeDTOList.stream().map(CommonRecipeDTO::getCommonRecipeId).collect(Collectors.toList());
        List<CommonRecipeDrug> commonRecipeDrugList = commonRecipeDrugDAO.findByCommonRecipeIdList(commonRecipeIdList);
        if (CollectionUtils.isEmpty(commonRecipeDrugList)) {
            return commonRecipeDTOList;
        }
        //获取drugId
        List<Integer> drugIdList = commonRecipeDrugList.stream().map(CommonRecipeDrug::getDrugId).collect(Collectors.toList());

        //通过drugIdList进行判断药品是否在对应的机构id下且是否被禁用
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugIdList(organId, drugIdList);
        Map<Integer, List<Integer>> failureDrugIdAndCommonRecipeId = new HashMap<>();
        if (CollectionUtils.isEmpty(organDrugLists)) {
            organDrugLists = new ArrayList<>();
        }

        Map<Integer, OrganDrugList> organDrugListMap = organDrugLists.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, a -> a, (k1, k2) -> k1));
        Map<Integer, List<CommonRecipeDrug>> groupMapByCommonRecipeId = commonRecipeDrugList.stream().collect(Collectors.groupingBy(CommonRecipeDrug::getCommonRecipeId));
        //判断药品是否在对应的药房下 k:commonRecipeId v:List<CommonRecipeDrug> a:CommonRecipeDrug
        LOGGER.info("getCommonRecipeListExt groupMapByCommonRecipeId={}", JSONUtils.toString(groupMapByCommonRecipeId));
        List<OrganDrugList> finalOrganDrugLists = organDrugLists;
        groupMapByCommonRecipeId.forEach((k, v) -> {
            List<Integer> drugIds = new LinkedList<>();
            v.forEach(a -> {
                if (null == organDrugListMap.get(a.getDrugId())) {
                    drugIds.add(a.getDrugId());
                    return;
                }
                //看看是commonRecipeDrug在organDrugList中否存在
                List<OrganDrugList> organDrugLisFindByDrugIdAndOrganDrugCode = finalOrganDrugLists.stream()
                        .filter(o -> o.getDrugId().equals(a.getDrugId()) && o.getOrganDrugCode().equals(a.getOrganDrugCode()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isEmpty(organDrugLisFindByDrugIdAndOrganDrugCode)) {
                    drugIds.add(a.getDrugId());
                    return;
                }
                OrganDrugList organDrugList = organDrugLisFindByDrugIdAndOrganDrugCode.get(0);
                //判断是否在药房下
                if (null != a.getPharmacyId() && StringUtils.isNotEmpty(organDrugList.getPharmacy()) && !Arrays.asList(organDrugList.getPharmacy().split(ByteUtils.COMMA)).contains(String.valueOf(a.getPharmacyId()))) {
                    drugIds.add(a.getDrugId());
                    return;
                }
                if (null != a.getPharmacyId() && StringUtils.isEmpty(organDrugList.getPharmacy())) {
                    drugIds.add(a.getDrugId());
                    return;
                }

                //运行平台是否适量关闭和药品中useDose为空则失效
                if (StringUtils.isNotEmpty(a.getUseDoseStr()) && !isAppropriate) {
                    drugIds.add(a.getDrugId());
                    return;
                }
                LOGGER.info("getCommonRecipeListExt groupMapByCommonRecipeId isAppropriate");
                //判断药品用药频率和方式是否合理
                if (null == a.getUsingRateId() || null == a.getUsePathwaysId()) {
                    drugIds.add(a.getDrugId());
                    return;
                }
                if (null != a.getUsingRateId() && null == usingRateMap.get(Integer.parseInt(a.getUsingRateId()))) {
                    LOGGER.info("getCommonRecipeListExt groupMapByCommonRecipeId usingRateMap {}", JSON.toJSONString(usingRateMap));
                    drugIds.add(a.getDrugId());
                    return;
                }
                if (null != a.getUsePathwaysId() && null == pathwaysMap.get(Integer.parseInt(a.getUsePathwaysId()))) {
                    LOGGER.info("getCommonRecipeListExt groupMapByCommonRecipeId pathwaysMap {} ", JSON.toJSONString(pathwaysMap));
                    drugIds.add(a.getDrugId());
                }

            });
            if (CollectionUtils.isNotEmpty(drugIds)) {
                failureDrugIdAndCommonRecipeId.put(k, drugIds);
            }
        });
        //设置常用方的状态
        commonRecipeDTOList.forEach(a -> {
            List<Integer> drugIds = failureDrugIdAndCommonRecipeId.get(a.getCommonRecipeId());
            if (CollectionUtils.isNotEmpty(drugIds)) {
                a.setCommonRecipeStatus(1);
                a.setDrugIdList(failureDrugIdAndCommonRecipeId.get(a.getCommonRecipeId()));
            } else {
                a.setCommonRecipeStatus(0);
            }
        });
        LOGGER.info(" getCommonRecipeListExt commonRecipeDTOList={}", JSONUtils.toString(commonRecipeDTOList));
        return commonRecipeDTOList;
    }

    public static void main(String[] args) {
        Integer a = 20;
        if (null != a && StringUtils.isNotEmpty("20") && !Arrays.asList("20".split(ByteUtils.COMMA)).contains(String.valueOf(a))) {
            System.out.println("1");
            return;
        }
    }

    /**
     * 根据常用方类型检查是否存在常用方
     *
     * @param doctorId
     * @param recipeType
     * @return
     */
    @RpcService
    public Boolean checkCommonRecipeExist(Integer doctorId, String recipeType) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        if (null != doctorId && StringUtils.isNotEmpty(recipeType)) {
            List<CommonRecipe> list = commonRecipeDAO.findByRecipeType(Arrays.asList(Integer.valueOf(recipeType)), doctorId, 0, 1);
            LOGGER.info("checkCommonRecipeExist the doctorId={}, recipeType={} ", doctorId, recipeType);
            if (CollectionUtils.isEmpty(list)) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * 查询常用方和常用方下的药品列表信息  查询常用方的详细信息
     *
     * @param commonRecipeId
     * @return
     */
    @RpcService
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
    public static void validateParam(CommonRecipe commonRecipe, List<CommonRecipeDrug> drugList) {
        Integer doctorId = commonRecipe.getDoctorId();
        Integer recipeType = commonRecipe.getRecipeType();
        String commonRecipeName = commonRecipe.getCommonRecipeName();

        if (null == doctorId || null == recipeType || StringUtils.isEmpty(commonRecipeName)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方必填参数为空");
        }

        // 常用方名称校验
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        CommonRecipe dbCommonRecipe = commonRecipeDAO.getByDoctorIdAndName(commonRecipe.getDoctorId(), commonRecipeName);
        if (null != dbCommonRecipe && !dbCommonRecipe.getCommonRecipeId().equals(commonRecipe.getCommonRecipeId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "已存在相同常用方名称");
        }

        Date now = DateTime.now().toDate();
        commonRecipe.setCreateDt(now);
        commonRecipe.setLastModify(now);

        for (CommonRecipeDrug drug : drugList) {
            drug.setSalePrice(null);
            drug.setDrugCost(null);
            drug.setCreateDt(now);
            drug.setLastModify(now);
            if (RecipeUtil.isTcmType(recipeType)) {
                drug.setUsePathways(null);
                drug.setUsingRate(null);
                drug.setUseTotalDose(drug.getUseDose());
            }
        }
    }

}
