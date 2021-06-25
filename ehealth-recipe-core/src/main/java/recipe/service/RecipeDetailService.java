package recipe.service;

import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.ValidateDetailVO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.dao.RecipeDetailDAO;
import recipe.drugTool.validate.RecipeDetailValidateTool;
import recipe.service.client.DrugClient;
import recipe.service.client.IConfigurationClient;
import recipe.service.manager.OrganDrugListManager;
import recipe.service.manager.PharmacyManager;
import recipe.util.MapValueUtil;

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
public class RecipeDetailService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeDetailValidateTool recipeDetailValidateTool;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private DrugClient drugClient;
    @Autowired
    private OrganDrugListManager organDrugListManager;

    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param validateDetailVO 机构id
     * @return
     */
    public ValidateDetailVO continueRecipeValidateDrug(ValidateDetailVO validateDetailVO) {
        Integer organId = validateDetailVO.getOrganId();
        Integer recipeType = validateDetailVO.getRecipeType();
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(organId, recipeType, validateDetailVO.getLongRecipe());
        //药房信息
        Map<String, PharmacyTcm> pharmacyCodeMap = pharmacyManager.pharmacyCodeMap(organId);
        //查询机构药品
        List<String> organDrugCodeList = validateDetailVO.getRecipeDetails().stream().map(RecipeDetailBean::getOrganDrugCode).distinct().collect(Collectors.toList());
        Map<String, List<OrganDrugList>> organDrugGroup = organDrugListManager.getOrganDrugCode(organId, organDrugCodeList);

        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, recipeType));
        //获取嘱托
        Map<String, DrugEntrustDTO> drugEntrustNameMap = drugClient.drugEntrustNameMap(organId);
        /**校验处方扩展字段*/
        //校验煎法
        recipeDetailValidateTool.validateDecoction(organId, validateDetailVO.getRecipeExtendBean());
        //校验制法
        recipeDetailValidateTool.validateMakeMethod(organId, validateDetailVO.getRecipeExtendBean());
        /**校验药品数据判断状态*/
        validateDetailVO.getRecipeDetails().forEach(a -> {
            //校验机构药品
            OrganDrugList organDrug = recipeDetailValidateTool.validateOrganDrug(a, organDrugGroup);
            if (null == organDrug || RecipeDetailValidateTool.VALIDATE_STATUS_FAILURE.equals(a.getValidateStatus())) {
                return;
            }
            //校验药品药房是否变动
            if (PharmacyManager.pharmacyVariation(a.getPharmacyId(), a.getPharmacyCode(), organDrug.getPharmacy(), pharmacyCodeMap)) {
                a.setValidateStatus(RecipeDetailValidateTool.VALIDATE_STATUS_FAILURE);
                logger.info("RecipeDetailService validateDrug pharmacy OrganDrugCode ：= {}", a.getOrganDrugCode());
                return;
            }
            //校验数据是否完善
            recipeDetailValidateTool.validateDrug(a, recipeDay, organDrug, recipeType, drugEntrustNameMap);
            //返回前端必须字段
            setRecipeDetail(a, organDrug, configDrugNameMap, recipeType);
        });
        return validateDetailVO;
    }

    /**
     * 校验处方药品配置时间
     *
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    public List<RecipeDetailBean> useDayValidate(ValidateDetailVO validateDetailVO) {
        List<RecipeDetailBean> recipeDetails = validateDetailVO.getRecipeDetails();
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getLongRecipe());
        recipeDetails.forEach(a -> recipeDetailValidateTool.useDayValidate(validateDetailVO.getRecipeType(), recipeDay, a));
        return recipeDetails;
    }

    /**
     * 校验中药嘱托
     *
     * @param organId       机构id
     * @param recipeDetails 处方药品明细
     * @return 处方药品明细
     */
    public List<RecipeDetailBean> entrustValidate(Integer organId, List<RecipeDetailBean> recipeDetails) {
        //获取嘱托
        Map<String, DrugEntrustDTO> drugEntrustNameMap = drugClient.drugEntrustNameMap(organId);
        recipeDetails.forEach(a -> {
            if (recipeDetailValidateTool.entrustValidate(a, drugEntrustNameMap)) {
                a.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
        });
        return recipeDetails;
    }

    /**
     * 患者端处方进行中列表查询药品信息
     *
     * @param orderCode 订单code
     * @return
     */
    public String getDrugName(String orderCode) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Recipedetail> recipeDetails = recipeDetailDAO.findDetailByOrderCode(orderCode);
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

    /**
     * 返回前端必须字段
     *
     * @param recipeDetailBean  出参处方明细
     * @param organDrug         机构药品
     * @param configDrugNameMap 药品名拼接配置
     * @param recipeType        处方类型
     */
    private void setRecipeDetail(RecipeDetailBean recipeDetailBean, OrganDrugList organDrug, Map<String, Integer> configDrugNameMap, Integer recipeType) {
        recipeDetailBean.setStatus(organDrug.getStatus());
        recipeDetailBean.setDrugId(organDrug.getDrugId());
        recipeDetailBean.setUseDoseAndUnitRelation(OrganDrugListManager.defaultUseDose(organDrug));
        //续方也会走这里但是 续方要用药品名实时配置
        recipeDetailBean.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(recipeDetailBean, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
    }


}
