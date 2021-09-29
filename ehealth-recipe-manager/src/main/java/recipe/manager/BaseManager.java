package recipe.manager;

import com.alibaba.fastjson.JSON;
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
    protected RecipeLogDAO recipeLogDao;
    @Autowired
    private RecipeLogDAO recipeLogDAO;
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
        logger.info("BaseManager getRecipeDTO recipeDTO:{}", JSON.toJSONString(recipeDTO));
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
    protected void saveRecipeLog(Integer recipeId, RecipeStatusEnum beforeStatus, RecipeStatusEnum afterStatus, String memo) {
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            return;
        }
        beforeStatus = null == beforeStatus ? RecipeStatusEnum.NONE : beforeStatus;
        afterStatus = null == afterStatus ? RecipeStatusEnum.NONE : afterStatus;
        memo = StringUtils.defaultString(memo, "");
        if (StringUtils.isNotEmpty(memo) && memo.length() > 250) {
            memo = memo.substring(0, 250);
        }
        try {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(recipeId);
            recipeLog.setModifyDate(DateTime.now().toDate());
            recipeLog.setBeforeStatus(beforeStatus.getType());
            recipeLog.setAfterStatus(afterStatus.getType());
            recipeLog.setMemo(memo);
            recipeLog.setExpand("");
            recipeLogDAO.saveRecipeLog(recipeLog);
        } catch (Exception e) {
            logger.error("BaseManager saveRecipeLog 保存日志出错", e);
        }
    }
}
