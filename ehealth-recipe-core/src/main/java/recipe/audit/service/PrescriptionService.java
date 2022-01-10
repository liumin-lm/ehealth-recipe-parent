package recipe.audit.service;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.client.RecipeAuditClient;

import java.util.List;

/**
 * 合理用药服务入口
 *
 * @author jiangtingfeng
 */
@RpcBean("prescriptionService")
public class PrescriptionService {
    @Autowired
    private RecipeAuditClient recipeAuditClient;
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);

    /**
     * 互联网医院使用返回格式
     * RecipeAuditClient
     *
     * @param recipeBean    处方信息
     * @param recipedetails 处方详情
     * @return 结果
     */
    @RpcService
    @Deprecated
    public AutoAuditResultBean analysis(RecipeBean recipeBean, List<RecipeDetailBean> recipedetails) {
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeBean.getRecipeExtend(), RecipeExtend.class);
        List<Recipedetail> recipeDetails = ObjectCopyUtils.convert(recipedetails, Recipedetail.class);
        return recipeAuditClient.analysis(recipe, recipeExtend, recipeDetails);
    }


    /**
     * 获取智能审方开关配置
     *
     * @param organId 机构id
     * @return 0 关闭 1 打开
     */
    @RpcService
    public Integer getIntellectJudicialFlag(Integer organId) {
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer intellectJudicialFlag = (Integer) configurationCenterUtilsService.getConfiguration(organId, "intellectJudicialFlag");
        if (intellectJudicialFlag == 2 || intellectJudicialFlag == 3) {
            intellectJudicialFlag = 1;
        }
        LOGGER.info("PrescriptionService getIntellectJudicialFlag  organId = {} , intellectJudicialFlag={}", organId, intellectJudicialFlag);
        return intellectJudicialFlag;
    }


}
