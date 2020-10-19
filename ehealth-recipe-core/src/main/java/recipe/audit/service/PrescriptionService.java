package recipe.audit.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.JudicialOrgan;
import com.ngari.recipe.entity.OrganJudicialRelation;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import eh.recipeaudit.model.recipe.RecipeDTO;
import eh.recipeaudit.model.recipe.RecipeDetailDTO;
import eh.recipeaudit.model.recipe.RecipeExtendDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.dao.JudicialOrganDAO;
import recipe.dao.OrganJudicialRelationDAO;

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
     * 早期使用接口，不能删除
     *
     * @param recipe        处方信息
     * @param recipedetails 处方详情
     * @return 结果
     */
    @RpcService
    @Deprecated
    public String getPAAnalysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        return null;
    }

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
        List<RecipeDetailDTO>recipeDetailDTOS = ObjectCopyUtils.convert(recipedetails, RecipeDetailDTO.class);
        AutoAuditResultBean resultBean = recipeAuditService.analysis(recipeDTO, recipeDetailDTOS);
        return JSON.parseObject(JSON.toJSONString(resultBean), AutoAuditResultBean.class);
    }

    /**
     * 获取药品说明页面URL
     *
     * @param drugId 药品Id
     * @return 药品说明页面URL
     */
    @RpcService
    public String getDrugSpecification(Integer drugId, Integer organId) {
        LOGGER.info("PrescriptionService getDrugSpecification parames:{},{}", drugId, organId);
        IntellectJudicialService judicialService = getService(organId);
        return judicialService.getDrugSpecification(drugId);
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

    private IntellectJudicialService getService(Integer organId) {
        OrganJudicialRelationDAO organJudicialRelationDAO = DAOFactory.getDAO(OrganJudicialRelationDAO.class);
        OrganJudicialRelation organJudicialRelation = organJudicialRelationDAO.getOrganJudicialRelationByOrganId(organId);
        JudicialOrganDAO judicialOrganDAO = DAOFactory.getDAO(JudicialOrganDAO.class);
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer intellectJudicialFlag = (Integer) configurationCenterUtilsService.getConfiguration(organId, "intellectJudicialFlag");
        String account = StringUtils.EMPTY;
        if (intellectJudicialFlag == 1) { //卫宁审方
            account = "winning";
        } else if (intellectJudicialFlag == 2) { //逸曜
            account = "hangzhouyiyao";
        } else if (intellectJudicialFlag == 3) {
            //第三方
            account = "thirdParty";
        }
        if (organJudicialRelation == null) {
            LOGGER.info("PrescriptionService getService 没有维护智能审方关系");
            OrganJudicialRelation judicialRelation = new OrganJudicialRelation();
            judicialRelation.setOrganId(organId);
            JudicialOrgan judicialOrgan = judicialOrganDAO.getByAccount(account);
            judicialRelation.setJudicialorganId(judicialOrgan.getJudicialorganId());
            organJudicialRelationDAO.save(judicialRelation);
            organJudicialRelation = organJudicialRelationDAO.getOrganJudicialRelationByOrganId(organId);
        } else {
            JudicialOrgan judicialOrgan = judicialOrganDAO.get(organJudicialRelation.getJudicialorganId());
            if (!judicialOrgan.getAccount().equals(account)) {
                JudicialOrgan judicialOrganNew = judicialOrganDAO.getByAccount(account);
                organJudicialRelation.setJudicialorganId(judicialOrganNew.getJudicialorganId());
                organJudicialRelationDAO.update(organJudicialRelation);
            }
        }
        LOGGER.info("PrescriptionService getService organJudicialRelation:{}.", JSONUtils.toString(organJudicialRelation));
        JudicialOrgan judicialOrgan = judicialOrganDAO.getByJudicialorganId(organJudicialRelation.getJudicialorganId());
        if (judicialOrgan == null) {
            throw new DAOException("审方机构不存在");
        }
        String serviceName = judicialOrgan.getCallSys() + "PrescriptionService";
        LOGGER.info("PrescriptionService getService serviceName:{}.", serviceName);
        return AppContextHolder.getBean(serviceName, IntellectJudicialService.class);
    }

}
