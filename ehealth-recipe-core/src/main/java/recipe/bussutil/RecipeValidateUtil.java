package recipe.bussutil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.constant.ErrorCode;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处方校验类
 *
 * @author yu_yun
 */
public class RecipeValidateUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeValidateUtil.class);

    /**
     * 保存处方前进行校验前段输入数据
     *
     * @param detail 处方明细
     * @author zhangx
     * @date 2015-12-4 下午4:05:34
     */
    public static void validateRecipeDetailData(Recipedetail detail, Recipe recipe) {
        if (detail.getDrugId() == null) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                    "drugId is required!");
        }
        double d = 0d;
        if ((detail.getUseDose() == null && StringUtils.isEmpty(detail.getUseDoseStr()))
                || (detail.getUseDose() != null && StringUtils.isEmpty(detail.getUseDoseStr()) && detail.getUseDose() <= d)) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                    "useDose is required!");
        }
        //西药和中成药必填参数
        if (!RecipeUtil.isTcmType(recipe.getRecipeType())) {
            if (detail.getUseDays() == null || detail.getUseDays() <= 0) {
                throw new DAOException(DAOException.VALUE_NEEDED,
                        "useDays is required!");
            }
            if (detail.getUseTotalDose() == null || detail.getUseTotalDose() <= d) {
                throw new DAOException(DAOException.VALUE_NEEDED,
                        "useTotalDose is required!");
            }
            if (StringUtils.isEmpty(detail.getUsingRate())) {
                throw new DAOException(DAOException.VALUE_NEEDED,
                        "usingRate is required!");
            }
            if (StringUtils.isEmpty(detail.getUsePathways())) {
                throw new DAOException(DAOException.VALUE_NEEDED,
                        "usePathways is required!");
            }
        }

    }

    /**
     * 保存处方单的时候，校验处方单数据
     *
     * @param recipe
     */
    public static void validateSaveRecipeData(Recipe recipe) {
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is required!");
        }

        if (StringUtils.isEmpty(recipe.getMpiid())) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                    "mpiid is required!");
        }

        if (recipe.getClinicOrgan() == null) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                "clinicOrgan is required!");
        }

        if (recipe.getDepart() == null) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                "depart is required!");
        }

        if (recipe.getDoctor() == null) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                "doctor is required!");
        }

        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patient = patientService.get(recipe.getMpiid());
        //解决旧版本因为wx2.6患者身份证为null，而业务申请不成功
        if (patient == null || StringUtils.isEmpty(patient.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该患者还未填写身份证信息，不能开处方");
        }

    }

    public static List<RecipeDetailBean> validateDrugsImpl(Recipe recipe) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

        Integer recipeId = recipe.getRecipeId();
        List<RecipeDetailBean> backDetailList = new ArrayList<>();
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(details)) {
            return backDetailList;
        }
        List<RecipeDetailBean> detailBeans = ObjectCopyUtils.convert(details, RecipeDetailBean.class);
        Map<Integer, RecipeDetailBean> drugIdAndDetailMap = Maps.uniqueIndex(detailBeans, RecipeDetailBean::getDrugId);

        IUsingRateService usingRateService = AppDomainContext.getBean("eh.usingRateService", IUsingRateService.class);
        IUsePathwaysService usePathwaysService = AppDomainContext.getBean("eh.usePathwaysService", IUsePathwaysService.class);
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(recipe.getClinicOrgan(), recipe.getRecipeType()));
        // TODO: 2020/6/19 很多需要返回药品信息的地方可以让前端根据药品id反查具体的药品信息统一展示；后端涉及返回药品信息的接口太多。返回对象也不一样
        for (Recipedetail recipedetail : details) {
            OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
            if (null == organDrug) {
                continue;
            }
            RecipeDetailBean mapDetail = drugIdAndDetailMap.get(organDrug.getDrugId());
            if (null == mapDetail) {
                continue;
            }
            mapDetail.setDrugForm(organDrug.getDrugForm());
            //设置医生端每次剂量和剂量单位联动关系
            List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = Lists.newArrayList();
            //用药单位不为空时才返回给前端
            if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
            }
            if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
            }
            mapDetail.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
            try {
                //重新开具也会走这里但是 暂存要用药品名实时配置
                mapDetail.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(mapDetail, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipe.getRecipeType())));
            } catch (Exception e) {
                LOGGER.error("RecipeServiceSub.validateDrugsImpl 设置药品拼接名error, recipeId:{},{}.", recipeId, e.getMessage(), e);
            }
            try {
                UsingRateDTO usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(organDrug.getOrganId(), mapDetail.getOrganUsingRate());
                if (usingRateDTO != null) {
                    mapDetail.setUsingRateId(String.valueOf(usingRateDTO.getId()));
                }
                UsePathwaysDTO usePathwaysDTO = usePathwaysService.findUsePathwaysByOrganAndKey(organDrug.getOrganId(), mapDetail.getOrganUsePathways());
                if (usePathwaysDTO != null) {
                    mapDetail.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
                }
            } catch (Exception e) {
                LOGGER.info("validateDrugsImpl error,recipeId={}", recipeId, e);
            }
            backDetailList.add(mapDetail);
        }
        return backDetailList;
    }

    /**
     * 处方详情用到返回实际处方明细药品--与删除了的药品无关
     * @param recipe
     * @return
     */
    public static List<RecipeDetailBean> validateDrugsImplForDetail(Recipe recipe,Map<Integer, List<SaleDrugList>> recipeDetailSalePrice) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

        Integer recipeId = recipe.getRecipeId();
        List<RecipeDetailBean> backDetailList = new ArrayList<>();
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(details)) {
            return backDetailList;
        }

        List<RecipeDetailBean> detailBeans = ObjectCopyUtils.convert(details, RecipeDetailBean.class);
        //暂存也会走这里但是 暂存要用药品名实时配置
        Map<String, Integer> configDrugNameMap = null;
        if (RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType().equals(recipe.getStatus())) {
            //药品名拼接配置
            configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(recipe.getClinicOrgan(), recipe.getRecipeType()));
        }
        List<Integer> drugId = detailBeans.stream().map(RecipeDetailBean::getDrugId).collect(Collectors.toList());
        List<DrugList> drugLists = drugListDAO.findByDrugIds(drugId);
        Map<Integer, List<DrugList>> drugListMap = drugLists.stream().collect(Collectors.groupingBy(DrugList::getDrugId));

        // TODO: 2020/6/19 很多需要返回药品信息的地方可以让前端根据药品id反查具体的药品信息统一展示；后端涉及返回药品信息的接口太多。返回对象也不一样
        for (RecipeDetailBean recipeDetail : detailBeans) {
            OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());
            if (organDrug!=null){
                recipeDetail.setDrugForm(organDrug.getDrugForm());
                //设置医生端每次剂量和剂量单位联动关系
                List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = Lists.newArrayList();
                //用药单位不为空时才返回给前端
                if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
                }
                if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
                }
                recipeDetail.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);

                try {
                    //暂存也会走这里但是 暂存要用药品名实时配置
                    if (RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType().equals(recipe.getStatus())) {
                        recipeDetail.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(recipeDetail, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipe.getRecipeType())));
                    } else {
                        //药品名历史数据处理
                        if (StringUtils.isEmpty(recipeDetail.getDrugDisplaySplicedName())) {
                            recipeDetail.setDrugDisplaySplicedName(DrugNameDisplayUtil.dealwithRecipedetailName(Arrays.asList(organDrug), ObjectCopyUtils.convert(recipeDetail, Recipedetail.class), recipe.getRecipeType()));
                        }
                        if (StringUtils.isEmpty(recipeDetail.getDrugDisplaySplicedSaleName())) {
                            recipeDetail.setDrugDisplaySplicedSaleName(DrugNameDisplayUtil.dealwithRecipedetailSaleName(Arrays.asList(organDrug), ObjectCopyUtils.convert(recipeDetail, Recipedetail.class), recipe.getRecipeType()));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("RecipeServiceSub.getRecipeAndDetailByIdImpl 设置药品拼接名error, recipeId:{},{}.", recipeId, e.getMessage(), e);
                }
            }
            DrugClient drugClient = AppContextHolder.getBean("drugClient", DrugClient.class);
            UsingRateDTO usingRateDTO = drugClient.usingRate(recipe.getClinicOrgan(), recipeDetail.getOrganUsingRate());
            if (null != usingRateDTO) {
                recipeDetail.setUsingRateId(String.valueOf(usingRateDTO.getId()));
            }
            UsePathwaysDTO usePathwaysDTO = drugClient.usePathways(recipe.getClinicOrgan(), recipeDetail.getOrganUsePathways());
            if (null != usePathwaysDTO) {
                recipeDetail.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
            }
            List<DrugList> drugList = drugListMap.get(recipeDetail.getDrugId());
            if (CollectionUtils.isNotEmpty(drugList)) {
                recipeDetail.setDrugPic(drugList.get(0).getDrugPic());
            }
            if(MapUtils.isNotEmpty(recipeDetailSalePrice)){
                List<SaleDrugList> saleDrugLists = recipeDetailSalePrice.get(recipeDetail.getDrugId());
                if(CollectionUtils.isNotEmpty(saleDrugLists)){
                    recipeDetail.setSalePrice(saleDrugLists.get(0).getPrice());
                }
            }
            backDetailList.add(recipeDetail);
        }
        return backDetailList;
    }

    /**
     * 校验处方数据
     *
     * @param recipeId
     */
    public static Recipe checkRecipeCommonInfo(Integer recipeId, RecipeResultBean resultBean) {
        if (null == resultBean) {
            resultBean = RecipeResultBean.getSuccess();
        }
        if (null == recipeId) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方ID参数为空");
            return null;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        if (null == dbRecipe) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方未找到");
            return null;
        }

        return dbRecipe;
    }

    /**
     *
     * @param recipeDetailBeans
     * @param isDoctor
     * @param organId
     * @return
     */
    public static List<RecipeDetailBean> covertDrugUnitdoseAndUnit(List<RecipeDetailBean> recipeDetailBeans,boolean isDoctor,Integer organId) {
        LOGGER.info("covertDrugUnitdoseAndUnit 参数 recipeDetailBeans:{}isDoctor:{}organId:{}", JSONUtils.toString(recipeDetailBeans), isDoctor, organId);
        if (recipeDetailBeans != null && recipeDetailBeans.size() > 0) {
            if (isDoctor) {
                return recipeDetailBeans;
            }
            //如果开关关闭：患者处方单每次剂量是否展示最小单位
            IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
            Object useDoseSmallUnit = configService.getConfiguration(organId, "useDoseSmallUnit");
            if (!(boolean) useDoseSmallUnit) {
                return recipeDetailBeans;
            }
            //患者端一次剂量和剂量单位（若医生添加药品使用的是规格单位，患者端展示的处方单和电子处方笺都需要根据以下公式进行转化,显示最小单位。每次剂量(最小单位)/单位剂量(最小单位)=每次剂量(规格单位)/单位剂量(规格单位)）
            for (int i = 0; i < recipeDetailBeans.size(); i++) {
                try{
                    String drugUnitdoseAndUnit = recipeDetailBeans.get(i).getDrugUnitdoseAndUnit();
                    if (StringUtils.isEmpty(drugUnitdoseAndUnit)) {
                        continue;
                    }
                    LOGGER.info("covertDrugUnitdoseAndUnit recipeid:{} drugUnitdoseAndUnit:{}", recipeDetailBeans.get(i).getRecipeId(), drugUnitdoseAndUnit);
                    //单位剂量【规格单位】,单位【规格单位】,单位剂量【最小单位】,单位【最小单位】
                    Map drugUnitdoseAndUnitMap = JSONUtils.parse(drugUnitdoseAndUnit,Map.class);
                    String unitDoseForSpecificationUnit = drugUnitdoseAndUnitMap.get("unitDoseForSpecificationUnit")==null?"":drugUnitdoseAndUnitMap.get("unitDoseForSpecificationUnit").toString();
                    String unitForSpecificationUnit = drugUnitdoseAndUnitMap.get("unitForSpecificationUnit")==null?"":drugUnitdoseAndUnitMap.get("unitForSpecificationUnit").toString();
                    String unitDoseForSmallUnit = drugUnitdoseAndUnitMap.get("unitDoseForSmallUnit")==null?"":drugUnitdoseAndUnitMap.get("unitDoseForSmallUnit").toString();
                    String unitForSmallUnit = drugUnitdoseAndUnitMap.get("unitForSmallUnit")==null?"":drugUnitdoseAndUnitMap.get("unitForSmallUnit").toString();
                    if (StringUtils.isEmpty(unitDoseForSpecificationUnit)
                            || StringUtils.isEmpty(unitDoseForSmallUnit)
                            || StringUtils.isEmpty(unitForSmallUnit)
                            || StringUtils.isEmpty(unitForSpecificationUnit)) {
                        continue;
                    }
                    //如果单位【最小单位】eq 处方详情的剂量单位useDoseUnit 或者单位【规格单位】！eq 处方详情的剂量单位useDoseUnit
                    if (StringUtils.isEmpty(recipeDetailBeans.get(i).getUseDoseUnit()) || unitForSmallUnit.equals(recipeDetailBeans.get(i).getUseDoseUnit())
                            || !unitForSpecificationUnit.equals(recipeDetailBeans.get(i).getUseDoseUnit())) {
                        continue;
                    }
                    //转换
                    try {
                        Double useDose = Double.parseDouble(unitDoseForSmallUnit) * recipeDetailBeans.get(i).getUseDose() / Double.parseDouble(unitDoseForSpecificationUnit);
                        BigDecimal useDoseBigDecimal = new BigDecimal(useDose);
                        useDose = useDoseBigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                        LOGGER.info("covertDrugUnitdoseAndUnit i:{} ,useDose:{} ,计算公式Double.parseDouble(unitDoseForSmallUnit){},*recipeDetailBeans.get(i).getUseDose(){},/Double.parseDouble(unitDoseForSpecificationUnit){} ", i, useDose, Double.parseDouble(unitDoseForSmallUnit), recipeDetailBeans.get(i).getUseDose(), Double.parseDouble(unitDoseForSpecificationUnit));
                        recipeDetailBeans.get(i).setUseDose(useDose);
                        recipeDetailBeans.get(i).setUseDoseUnit(unitForSmallUnit);
                    } catch (Exception e) {
                        LOGGER.error("method covertDrugUnitdoseAndUnit 转换 error " + e.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.error("method covertDrugUnitdoseAndUnit error " + e.getMessage());
                }
            }
        }

        return recipeDetailBeans;
    }
}
