package recipe.audit.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import eh.recipeaudit.model.recipe.RecipeDTO;
import eh.recipeaudit.model.recipe.RecipeDetailDTO;
import eh.recipeaudit.model.recipe.RecipeExtendDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;

import java.util.List;

/**
 * 合理用药服务入口
 *
 * @author jiangtingfeng
 */
@RpcBean("prescriptionService")
public class PrescriptionService {
    @Autowired
    private IRecipeAuditService recipeAuditService;

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);

    /**
     * 互联网医院使用返回格式
     *
     * @param recipe        处方信息
     * @param recipedetails 处方详情
     * @return 结果
     */
    @RpcService
    public AutoAuditResultBean analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        if (recipe == null) {
            throw new DAOException("处方不存在");
        }
        RecipeDTO recipeDTO = ObjectCopyUtils.convert(recipe, RecipeDTO.class);
        RecipeExtendBean recipeExtend = recipe.getRecipeExtend();
        RecipeExtendDTO recipeExtendDTO = ObjectCopyUtils.convert(recipeExtend, RecipeExtendDTO.class);
        recipeDTO.setRecipeExtend(recipeExtendDTO);
        List<RecipeDetailDTO> recipeDetailDTOS = ObjectCopyUtils.convert(recipedetails, RecipeDetailDTO.class);
        AutoAuditResultBean resultBean = recipeAuditService.analysis(recipeDTO, recipeDetailDTOS);
        return JSON.parseObject(JSON.toJSONString(resultBean), AutoAuditResultBean.class);
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
