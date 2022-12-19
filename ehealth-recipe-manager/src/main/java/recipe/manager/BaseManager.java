package recipe.manager;

import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeLog;
import com.ngari.recipe.entity.Recipedetail;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.*;
import recipe.dao.*;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.DictionaryUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.List;

/**
 * his调用基类
 *
 * @author fuzi
 */
public class BaseManager {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected RecipeDAO recipeDAO;
    @Autowired
    protected RecipeExtendDAO recipeExtendDAO;
    @Autowired
    protected RecipeDetailDAO recipeDetailDAO;
    @Autowired
    protected RecipeOrderDAO recipeOrderDAO;
    @Autowired
    protected OrganDrugListDAO organDrugListDAO;
    @Autowired
    protected SaleDrugListDAO saleDrugListDAO;
    @Autowired
    protected DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    protected HisRecipeDAO hisRecipeDAO;
    @Autowired
    protected RecipeLogDAO recipeLogDAO;
    @Autowired
    protected PatientClient patientClient;
    @Autowired
    protected DoctorClient doctorClient;
    @Autowired
    protected IConfigurationClient configurationClient;
    @Autowired
    protected RevisitClient revisitClient;
    @Autowired
    protected OrganClient organClient;
    @Autowired
    protected DepartClient departClient;
    @Autowired
    protected EnterpriseClient enterpriseClient;
    @Autowired
    protected DrugClient drugClient;
    @Autowired
    protected ConsultClient consultClient;
    @Resource
    protected OfflineRecipeClient offlineRecipeClient;
    @Autowired
    protected DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    protected DrugListDAO drugListDAO;
    @Autowired
    protected RecipeRefundDAO recipeRefundDAO;
    @Autowired
    protected RecipeParameterDao parameterDao;
    @Autowired
    protected MedicationSyncConfigDAO medicationSyncConfigDAO;
    @Autowired
    protected RecipeBeforeOrderDAO recipeBeforeOrderDAO;
    @Autowired
    protected DefaultValueClient defaultValueClientUtil;

    /**
     * 获取处方相关信息
     *
     * @param recipeId 处方id
     * @return
     */
    protected RecipeDTO getRecipeDTO(Integer recipeId) {
        logger.info("BaseManager getRecipeDTO recipeId:{}", recipeId);
        RecipeDTO recipeDTO = new RecipeDTO();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipe(recipe);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        recipeDTO.setRecipeDetails(recipeDetails);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipeExtend(recipeExtend);
        return recipeDTO;
    }

    /**
     * 获取地址枚举
     *
     * @param address
     * @param area
     */
    protected void getAddressDic(StringBuilder address, String area) {
        address.append(getAddress(area));
    }

    /**
     * 获取地址枚举
     *
     * @param area
     */
    protected String getAddress(String area) {
        return DictionaryUtil.getDictionary("eh.base.dictionary.AddrArea", area);
    }


    /**
     * 保存处方操作记录
     */
    public void saveRecipeLog(Integer recipeId, RecipeStatusEnum beforeStatus, RecipeStatusEnum afterStatus, String memo) {
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            return;
        }
        beforeStatus = null == beforeStatus ? RecipeStatusEnum.NONE : beforeStatus;
        afterStatus = null == afterStatus ? RecipeStatusEnum.NONE : afterStatus;
        saveRecipeLog(recipeId, beforeStatus.getType(), afterStatus.getType(), memo);
    }

    protected void saveRecipeLog(Integer recipeId, Integer beforeStatus, Integer afterStatus, String memo) {
        try {
            memo = StringUtils.defaultString(memo, "");
            if (StringUtils.isNotEmpty(memo) && memo.length() > 250) {
                memo = memo.substring(0, 250);
            }
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(recipeId);
            recipeLog.setModifyDate(DateTime.now().toDate());
            recipeLog.setBeforeStatus(beforeStatus);
            recipeLog.setAfterStatus(afterStatus);
            recipeLog.setMemo(memo);
            recipeLog.setExpand("");
            recipeLogDAO.saveRecipeLog(recipeLog);
        } catch (Exception e) {
            logger.error("BaseManager saveRecipeLog 保存日志出错", e);
        }
    }
}
