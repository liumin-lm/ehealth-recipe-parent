package recipe.caNew.pdf;

import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.ESignDTO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.comment.DictionaryUtil;
import recipe.constant.CacheConstant;
import recipe.constant.ErrorCode;
import recipe.service.RecipeLogService;
import recipe.service.client.IConfigurationClient;
import recipe.service.manager.RecipeLabelManager;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static recipe.service.RecipeServiceSub.getRecipeAndDetailByIdImpl;

/**
 * 平台创建pdf实现类
 * 根据运营平台配置模版方式生成的 业务处理代码类
 *
 * @author fuzi
 */
@Service
public class PlatformCreatePdfServiceImpl implements CreatePdfService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IConfigurationClient configurationClient;
    @Resource
    private IESignBaseService esignService;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private RecipeLabelManager recipeLabelManager;


    @Override
    public Map<String, Object> queryPdfOssId(Recipe recipe) {
        int recipeId = recipe.getRecipeId();
        Map<String, Object> recipeMap = getRecipeAndDetailByIdImpl(recipeId, false);
        Map<String, List<RecipeLabelVO>> result = recipeLabelManager.queryRecipeLabelById(recipe.getClinicOrgan(), recipeMap);
        try {
            PatientDTO patientDTO = (PatientDTO) recipeMap.get("patient");
            //组装生成pdf的参数
            ESignDTO eSignDTO = new ESignDTO();
            if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                //中药pdf参数
                eSignDTO.setTemplateType("tcm");
                createChineMedicinePDF(result, recipeMap, (RecipeBean) recipeMap.get("recipe"));
            } else {
                eSignDTO.setTemplateType("wm");
                eSignDTO.setImgFileId(String.valueOf(recipeId));
                createMedicinePDF(result, (RecipeBean) recipeMap.get("recipe"));
            }
            eSignDTO.setLoginId(patientDTO.getLoginId());
            eSignDTO.setDoctorName(recipe.getDoctorName());
            eSignDTO.setDoctorId(recipe.getDoctor());
            eSignDTO.setOrgan(recipe.getClinicOrgan());
            eSignDTO.setFileName("recipe_" + recipeId + ".pdf");
            eSignDTO.setParamMap(Collections.unmodifiableMap(result));
            eSignDTO.setRp(configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "rptorx", null));
            Map<String, Object> backMap = esignService.signForRecipe2(eSignDTO);
            logger.info("RecipeLabelManager queryPdfRecipeLabelById backMap={},eSignDTO={}", JSONUtils.toString(backMap), JSONUtils.toString(eSignDTO));
            List<CoOrdinateVO> coOrdinateVO = MapValueUtil.getList(backMap, "coOrdinateList");
            coOrdinate(recipeId, coOrdinateVO);
            return backMap;
        } catch (Exception e) {
            logger.error("queryPdfRecipeLabelById error ", e);
            //日志记录
            String memo = "签名上传文件失败！原因：" + e.getMessage();
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), memo);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "pdf error");
        }
    }


    @Override
    public String queryPdfByte(RecipeBean recipe) {
        int recipeId = recipe.getRecipeId();
        Map<String, Object> recipeMap = getRecipeAndDetailByIdImpl(recipeId, false);
        if (org.springframework.util.CollectionUtils.isEmpty(recipeMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is null!");
        }
        Map<String, List<RecipeLabelVO>> result = recipeLabelManager.queryRecipeLabelById(recipe.getClinicOrgan(), recipeMap);
        try {
            //组装生成pdf的参数
            Map<String, Object> map = new HashMap<>();
            if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                //中药pdf参数
                map.put("templateType", "tcm");
                createChineMedicinePDF(result, recipeMap, (RecipeBean) recipeMap.get("recipe"));
                //添加斜线位置 1,中间  2 下面
                String invalidInfoObject = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "diagonalLineLayer", "1");
                map.put("diagonalLineLayer", Integer.valueOf(invalidInfoObject));
            } else {
                map.put("templateType", "wm");
                createMedicinePDF(result, (RecipeBean) recipeMap.get("recipe"));
            }

            map.put("rp", configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "rptorx", null));
            map.put("paramMap", result);
            SignRecipePdfVO signRecipePdfVO = esignService.createSignRecipePDF(map);
            logger.info("RecipeLabelManager queryPdfRecipeLabelById map={},signRecipePdfVO={}", JSONUtils.toString(map), JSONUtils.toString(signRecipePdfVO));
            coOrdinate(recipeId, signRecipePdfVO.getCoOrdinateList());
            return signRecipePdfVO.getDataStr();
        } catch (Exception e) {
            logger.error("queryPdfRecipeLabelById error ", e);
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "获取pdf-byte-格式" + e.getMessage());
            throw new DAOException(ErrorCode.SERVICE_ERROR, "pdf error");
        }
    }


    /**
     * 特殊字段坐标记录
     *
     * @param recipeId
     * @param coOrdinateList
     */
    private void coOrdinate(Integer recipeId, List<CoOrdinateVO> coOrdinateList) {
        if (CollectionUtils.isEmpty(coOrdinateList) || null == recipeId) {
            logger.error("RecipeLabelManager coOrdinate error ");
            return;
        }
        redisClient.addList(CacheConstant.KEY_RECIPE_LABEL + recipeId.toString(), coOrdinateList, 3 * 24 * 60 * 60L);
    }


    /**
     * 西药 pdf 摸版参数
     *
     * @param result
     * @param recipe
     */
    private void createMedicinePDF(Map<String, List<RecipeLabelVO>> result, RecipeBean recipe) {
        List<RecipeLabelVO> list = result.get("moduleThree");
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<RecipeDetailBean> recipeDetailList = (List<RecipeDetailBean>) list.get(0).getValue();
        for (int i = 0; i < recipeDetailList.size(); i++) {
            RecipeDetailBean d = recipeDetailList.get(i);
            //名称+规格+药品单位+开药总量+药品单位
            StringBuilder stringBuilder = new StringBuilder(i + 1);
            stringBuilder.append(i + 1).append("、");
            //药品显示名处理
            if (StringUtils.isNotEmpty(d.getDrugDisplaySplicedName())) {
                stringBuilder.append(d.getDrugDisplaySplicedName());
            } else {
                stringBuilder.append(d.getDrugName()).append(d.getDrugSpec()).append("/").append(d.getDrugUnit());
            }
            stringBuilder.append("   ").append("X").append(d.getUseTotalDose()).append(d.getDrugUnit());
            Boolean canShowDrugCost = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "canShowDrugCost", false);
            if (canShowDrugCost) {
                BigDecimal drugCost = d.getDrugCost().divide(BigDecimal.ONE, 2, RoundingMode.UP);
                stringBuilder.append("   ").append(drugCost).append("元");
            }
            stringBuilder.append(" \n ");
            //每次剂量+剂量单位
            String useDose = null == d.getUseDose() ? "" : d.getUseDose() + d.getUseDoseUnit();
            String uDose = "Sig: 每次" + useDose;

            //用药频次
            String dRateName = d.getUsingRateTextFromHis() != null ? d.getUsingRateTextFromHis() : DictionaryUtil.getDictionary("eh.cdr.dictionary.UsingRate", d.getUsingRate());
            //用法
            String dWay = d.getUsePathwaysTextFromHis() != null ? d.getUsePathwaysTextFromHis() : DictionaryUtil.getDictionary("eh.cdr.dictionary.UsePathways", d.getUsePathways());
            stringBuilder.append(uDose).append("    ").append(dRateName).append("    ").append(dWay).append("    ").append(getUseDays(d.getUseDaysB(), d.getUseDays()));

            if (!StringUtils.isEmpty(d.getMemo())) {
                stringBuilder.append(" \n ").append("嘱托:").append(d.getMemo());
            }
            list.add(new RecipeLabelVO("medicine", "drugInfo" + i, stringBuilder.toString()));
        }
    }

    /**
     * 中药pdf 摸版参数
     *
     * @param result
     * @param recipeMap
     * @param recipe
     */
    private void createChineMedicinePDF(Map<String, List<RecipeLabelVO>> result, Map<String, Object> recipeMap, RecipeBean recipe) {
        List<RecipeLabelVO> list = result.get("moduleThree");
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<RecipeDetailBean> recipeDetailList = (List<RecipeDetailBean>) list.get(0).getValue();
        String drugShowName;
        for (int i = 0; i < recipeDetailList.size(); i++) {
            RecipeDetailBean detail = recipeDetailList.get(i);
            String dTotal;
            if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                dTotal = detail.getUseDoseStr() + detail.getUseDoseUnit();
            } else {
                dTotal = detail.getUseDose() + detail.getUseDoseUnit();
            }
            if (!StringUtils.isEmpty(detail.getMemo())) {
                dTotal = dTotal + "(" + detail.getMemo() + ")";
            }
            drugShowName = detail.getDrugName() + " " + dTotal;
            list.add(new RecipeLabelVO("chineMedicine", "drugInfo" + i, drugShowName));
        }
        RecipeDetailBean detail = recipeDetailList.get(0);
        list.add(new RecipeLabelVO("天数", "tcmUseDay", getUseDays(detail.getUseDaysB(), detail.getUseDays())));
        try {
            list.add(new RecipeLabelVO("用药途径", "tcmUsePathways", DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detail.getUsePathways())));
            list.add(new RecipeLabelVO("用药频次", "tcmUsingRate", DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detail.getUsingRate())));
        } catch (Exception e) {
            logger.error("用药途径 用药频率有误");
        }
        list.add(new RecipeLabelVO("贴数", "copyNum", recipe.getCopyNum() + "贴"));
        RecipeExtend extend = (RecipeExtend) recipeMap.get("recipeExtend");
        if (null != extend) {
            list.add(new RecipeLabelVO("煎法", "tcmDecoction", extend.getDecoctionText() == null ? "" : extend.getDecoctionText()));
            list.add(new RecipeLabelVO("每付取汁", "tcmJuice", extend.getJuice() + extend.getJuiceUnit()));
            list.add(new RecipeLabelVO("次量", "tcmMinor", extend.getMinor() + extend.getMinorUnit()));
            list.add(new RecipeLabelVO("制法", "tcmMakeMethod", extend.getMakeMethodText() == null ? "" : extend.getMakeMethodText()));
        }
        list.add(new RecipeLabelVO("嘱托", "tcmRecipeMemo", recipe.getRecipeMemo() == null ? "" : recipe.getRecipeMemo()));
    }


    /**
     * 获取天数 与 单位字符串展示
     *
     * @param useDaysB
     * @param useDays
     * @return
     */
    private String getUseDays(String useDaysB, Integer useDays) {
        if (StringUtils.isNotEmpty(useDaysB) && !"0".equals(useDaysB)) {
            return useDaysB + "天";
        }
        if (!ValidateUtil.integerIsEmpty(useDays)) {
            return useDays + "天";
        }
        return "";
    }

}
