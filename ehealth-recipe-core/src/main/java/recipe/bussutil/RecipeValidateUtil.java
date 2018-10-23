package recipe.bussutil;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 处方校验类
 *
 * @author yu_yun
 */
public class RecipeValidateUtil {

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
        if (detail.getUseDose() == null || detail.getUseDose() <= d) {
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

        if (StringUtils.isEmpty(recipe.getOrganDiseaseName())) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                    "organDiseaseName is required!");
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

        //判断诊断备注是否超过50字
        int i = 50;
        if (StringUtils.isNotEmpty(recipe.getMemo()) && recipe.getMemo().length() > i) {
            throw new DAOException("备注内容字数限制50字");
        }

        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
            if (recipe.getTcmUsePathways() == null) {
                throw new DAOException(DAOException.VALUE_NEEDED,
                        "tcmUsePathways is required!");
            }

            if (recipe.getTcmUsingRate() == null) {
                throw new DAOException(DAOException.VALUE_NEEDED,
                        "tcmUsingRate is required!");
            }
        }

        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patient = patientService.get(recipe.getMpiid());
        //解决旧版本因为wx2.6患者身份证为null，而业务申请不成功
        if (patient == null || StringUtils.isEmpty(patient.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该患者还未填写身份证信息，不能开处方");
        }

    }

    public static List<Recipedetail> validateDrugsImpl(Recipe recipe) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

        Integer organId = recipe.getClinicOrgan();
        Integer recipeId = recipe.getRecipeId();
        List<Recipedetail> backDetailList = new ArrayList<>();
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(details)) {
            return backDetailList;
        }
        List<Integer> drugIdList = FluentIterable.from(details).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();

        //校验当前机构可开具药品是否满足
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(organId, drugIdList);
        if (CollectionUtils.isEmpty(organDrugList)) {
            return backDetailList;
        }

        //2边长度一致直接返回
        if (organDrugList.size() == drugIdList.size()) {
            return details;
        }

        Map<Integer, Recipedetail> drugIdAndDetailMap = Maps.uniqueIndex(details, new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        });

        Recipedetail mapDetail;
        for (OrganDrugList organDrug : organDrugList) {
            mapDetail = drugIdAndDetailMap.get(organDrug.getDrugId());
            if (null != mapDetail) {
                backDetailList.add(mapDetail);
            }
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
}
