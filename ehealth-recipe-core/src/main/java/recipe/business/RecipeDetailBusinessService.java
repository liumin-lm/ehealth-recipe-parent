package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.RecipeSkipVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.IConfigurationClient;
import recipe.core.api.IRecipeDetailBusinessService;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeParameterDao;
import recipe.drugTool.validate.RecipeDetailValidateTool;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.*;
import recipe.util.LocalStringUtil;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;
import recipe.vo.ResultBean;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static recipe.drugTool.validate.RecipeDetailValidateTool.VALIDATE_STATUS_PERFECT;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailBusinessService implements IRecipeDetailBusinessService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 1、不可开重复处方；2、不可开重复药品;3、重复药品提示但可开;4、不需要校验
     */
    private final static String REPEAT_OPEN_RULE_NO = "4";
    private final static String REPEAT_OPEN_RULE_RECIPE = "1";

    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeDetailValidateTool recipeDetailValidateTool;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private DrugManager drugManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private RecipeParameterDao parameterDao;

    @Override
    public ValidateDetailVO continueRecipeValidateDrug(ValidateDetailVO validateDetailVO) {
        Integer organId = validateDetailVO.getOrganId();
        Integer recipeType = validateDetailVO.getRecipeType();
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(organId, recipeType, validateDetailVO.getLongRecipe());
        //查询机构药品
        List<String> organDrugCodeList = validateDetailVO.getRecipeDetails().stream().map(RecipeDetailBean::getOrganDrugCode).distinct().collect(Collectors.toList());
        Map<String, List<OrganDrugList>> organDrugGroup = organDrugListManager.getOrganDrugCode(organId, organDrugCodeList);
        //药房信息
        Map<Integer, PharmacyTcm> pharmacyIdMap = pharmacyManager.pharmacyIdMap(organId);
        Integer pharmacyId = pharmacyManager.organDrugPharmacyId(organId, organDrugCodeList);
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, recipeType));
        //获取嘱托
        Map<String, DrugEntrust> drugEntrustNameMap = drugManager.drugEntrustNameMap(organId);
        /**校验处方扩展字段*/
        //校验煎法
        recipeDetailValidateTool.validateDecoction(organId, validateDetailVO.getRecipeExtendBean());
        //校验制法
        recipeDetailValidateTool.validateMakeMethod(organId, validateDetailVO.getRecipeExtendBean());
        /**校验药品数据判断状态*/
        validateDetailVO.getRecipeDetails().forEach(a -> {
            a.setDrugDisplaySplicedName(a.getDrugName());
            //校验机构药品
            OrganDrugList organDrug = recipeDetailValidateTool.validateOrganDrug(a, organDrugGroup);
            if (null == organDrug || RecipeDetailValidateTool.VALIDATE_STATUS_FAILURE.equals(a.getValidateStatus())) {
                a.setValidateStatusText("机构药品不存在");
                return;
            }
            //校验药品药房是否变动
            boolean pharmacy = pharmacyManager.pharmacyVariationV1(pharmacyId, organDrug.getPharmacy());
            if (null != pharmacyIdMap && pharmacy) {
                a.setValidateStatusText("机构药品药房错误");
                a.setValidateStatus(RecipeDetailValidateTool.VALIDATE_STATUS_FAILURE);
                return;
            }
            //校验数据是否完善
            recipeDetailValidateTool.validateDrug(a, recipeDay, organDrug, recipeType, drugEntrustNameMap, organId, validateDetailVO.getVersion());
            //返回前端必须字段
            setRecipeDetail(a, organDrug, configDrugNameMap, recipeType, pharmacyId, pharmacyIdMap);
        });
        return validateDetailVO;
    }

    @Override
    public List<RecipeDetailBean> useDayValidate(ValidateDetailVO validateDetailVO) {
        List<RecipeDetailBean> recipeDetails = validateDetailVO.getRecipeDetails();
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getLongRecipe());
        recipeDetails.forEach(a -> recipeDetailValidateTool.useDayValidate(validateDetailVO.getRecipeType(), recipeDay, a));
        return recipeDetails;
    }

    @Override
    public List<RecipeDetailBean> drugSuperScalarValidate(ValidateDetailVO validateDetailVO) {
        List<RecipeDetailBean> recipeDetails = validateDetailVO.getRecipeDetails();
        recipeDetails.forEach(a -> {
            if (recipeDetailValidateTool.drugSuperScalarValidate(validateDetailVO.getOrganId(), a)) {
                a.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
        });
        return recipeDetails;
    }

    @Override
    public String getDrugName(String orderCode, Integer orderId) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Recipedetail> recipeDetails;
        if (StringUtils.isNotEmpty(orderCode)) {
            recipeDetails = recipeDetailDAO.findDetailByOrderCode(orderCode);
        } else {
            List<Integer> recipeIds = orderManager.getRecipeIdsByOrderId(orderId);
            recipeDetails = recipeDetailDAO.findByRecipeIds(recipeIds);
        }
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return stringBuilder.toString();
        }
        // 按处方分组,不同处方药品用 ; 分割
        Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetails.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));

        recipeDetailMap.forEach((k, v) -> {
            v.forEach(a -> stringBuilder.append(a.getDrugName()));
            stringBuilder.append(";");
        });

        return stringBuilder.toString();
    }

    @Override
    public ResultBean<String> validateRepeatRecipe(ValidateDetailVO validateDetailVO) {
        ResultBean<String> resultBean = new ResultBean<>();
        resultBean.setBool(true);
        String repeatRecipeOpenRule = configurationClient.getValueEnumCatch(validateDetailVO.getRecipeBean().getClinicOrgan(), "repeatRecipeOpenRule", "4");
        resultBean.setData(repeatRecipeOpenRule);
        //不需要校验
        if (REPEAT_OPEN_RULE_NO.equals(repeatRecipeOpenRule)) {
            return resultBean;
        }

        List<Integer> recipeIds = recipeManager.findRecipeByClinicId(validateDetailVO.getRecipeBean().getClinicId(), validateDetailVO.getRecipeBean().getRecipeId(), RecipeStatusEnum.RECIPE_REPEAT);
        List<Recipedetail> recipeDetails = recipeDetailManager.findRecipeDetails(recipeIds);
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return resultBean;
        }

        //不可开重复处方
        if (REPEAT_OPEN_RULE_RECIPE.equals(repeatRecipeOpenRule)) {
            //需要校验的药品id
            List<Integer> validateDrugIds = validateDetailVO.getRecipeDetails().stream().map(RecipeDetailBean::getDrugId).distinct().sorted().collect(Collectors.toList());
            Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetails.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
            for (List<Recipedetail> recipeDetailList : recipeDetailMap.values()) {
                List<Integer> drugIds = recipeDetailList.stream().map(Recipedetail::getDrugId).distinct().sorted().collect(Collectors.toList());
                if (validateDrugIds.equals(drugIds)) {
                    resultBean.setBool(false);
                    return resultBean;
                }
            }
            return resultBean;
        }
        //不可开重复药品/重复药品提示但可开
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        List<String> drugNames = validateDetailVO.getRecipeDetails().stream().filter(a -> drugIds.contains(a.getDrugId())).map(RecipeDetailBean::getDrugName).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(drugNames)) {
            return resultBean;
        }
        resultBean.setMsgList(drugNames);
        resultBean.setBool(false);
        return resultBean;
    }

    @Override
    public RecipeSkipVO getRecipeSkipUrl(Integer organId, String recipeCode) {
        RecipeSkipVO recipeSkipVO = new RecipeSkipVO();
        recipeSkipVO.setShowFlag(false);
        try {
            String recipeSkipOrgan = parameterDao.getByName("recipeSkipOrgan");
            if (LocalStringUtil.hasOrgan(organId.toString(), recipeSkipOrgan)) {
                //包含机构
                String recipeSkipUrl = parameterDao.getByName("recipeSkipUrl");
                recipeSkipVO.setShowFlag(true);
                recipeSkipVO.setSkipUrl(recipeSkipUrl + recipeCode);
            }
        } catch (Exception e) {
            logger.error("getRecipeSkipUrl error", e);
        }
        return recipeSkipVO;
    }

    @Override
    public List<RecipeDetailDTO> validateHisDrugRule(Recipe recipe, List<RecipeDetailDTO> recipeDetails, String registerId, String dbType) {
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return new ArrayList<>();
        }
        //"1": "大病权限", "2": "靶向药权限"
        List<String> hisDrugRule = configurationClient.getValueListCatch(recipe.getClinicOrgan(), "validateHisDrugRule", null);
        logger.info("RecipeDetailBusinessService validateHisDrugRule hisDrugRule={}", JSON.toJSONString(hisDrugRule));
        if (CollectionUtils.isEmpty(hisDrugRule)) {
            return recipeDetails;
        }
        //"1": "大病权限" ,"3": "机构药品规则（含余量控制）"
        if (hisDrugRule.contains("1") || hisDrugRule.contains("3")) {
            recipeDetailManager.validateHisDrugRule(recipe, recipeDetails, registerId, dbType);
            logger.info("RecipeDetailBusinessService validateHisDrugRule 大病权限 recipeDetails={}", JSON.toJSONString(recipeDetails));
        }
        //"2": "靶向药权限"
        if (hisDrugRule.contains("2")) {
            organDrugListManager.validateHisDrugRule(recipe, recipeDetails);
            logger.info("RecipeDetailBusinessService validateHisDrugRule 靶向药权限 recipeDetails={}", JSON.toJSONString(recipeDetails));
        }
        return recipeDetails;
    }


    /**
     * 返回前端必须字段
     *
     * @param recipeDetailBean  出参处方明细
     * @param organDrug         机构药品
     * @param configDrugNameMap 药品名拼接配置
     * @param recipeType        处方类型
     */
    private void setRecipeDetail(RecipeDetailBean recipeDetailBean, OrganDrugList organDrug, Map<String, Integer> configDrugNameMap,
                                 Integer recipeType, Integer pharmacyId, Map<Integer, PharmacyTcm> pharmacyIdMap) {
        recipeDetailBean.setStatus(organDrug.getStatus());
        recipeDetailBean.setDrugId(organDrug.getDrugId());
        recipeDetailBean.setUseDoseAndUnitRelation(RecipeUtil.defaultUseDose(organDrug));
        recipeDetailBean.setSalePrice(organDrug.getSalePrice());
        //续方也会走这里但是 续方要用药品名实时配置
        recipeDetailBean.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(recipeDetailBean, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
        if (!ValidateUtil.integerIsEmpty(pharmacyId)) {
            PharmacyTcm pharmacyTcm = pharmacyIdMap.get(pharmacyId);
            recipeDetailBean.setPharmacyId(pharmacyId);
            recipeDetailBean.setPharmacyName(pharmacyTcm.getPharmacyName());
            recipeDetailBean.setPharmacyCode(pharmacyTcm.getPharmacyCode());
        }
    }

}
