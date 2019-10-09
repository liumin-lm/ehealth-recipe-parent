package recipe.audit.service;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.JudicialOrgan;
import com.ngari.recipe.entity.OrganJudicialRelation;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.bean.*;
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

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);

    /**
     * 早期使用接口，不能删除
     * @param recipe          处方信息
     * @param recipedetails   处方详情
     * @return                结果
     */
    @RpcService
    @Deprecated
    public String getPAAnalysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails){
        return null;
    }

    /**
     * 互联网医院使用返回格式
     * @param recipe          处方信息
     * @param recipedetails   处方详情
     * @return                结果
     */
    @RpcService
    public AutoAuditResult analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails){
        if (recipe == null) {
            throw new DAOException("处方不存在");
        }
        Integer organId = recipe.getClinicOrgan();
        IntellectJudicialService judicialService = getService(organId);
        return judicialService.analysis(recipe, recipedetails);
    }

    /**
     * 获取药品说明页面URL
     * @param drugId   药品Id
     * @return         药品说明页面URL
     */
    @RpcService
    public String getDrugSpecification(Integer drugId, Integer organId){
        LOGGER.info("PrescriptionService getDrugSpecification parames:{},{}", drugId, organId);
        IntellectJudicialService judicialService = getService(organId);
       return judicialService.getDrugSpecification(drugId);
    }

    /**
     * 获取智能审方开关配置
     * @param organId  机构id
     * @return         0 关闭 1 打开
     */
    @RpcService
    public Integer getIntellectJudicialFlag(Integer organId){
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (Integer) configurationCenterUtilsService.getConfiguration(organId, "intellectJudicialFlag");
    }

    private IntellectJudicialService getService(Integer organId){
        OrganJudicialRelationDAO organJudicialRelationDAO = DAOFactory.getDAO(OrganJudicialRelationDAO.class);
        OrganJudicialRelation organJudicialRelation = organJudicialRelationDAO.getOrganJudicialRelationByOrganId(organId);
        if (organJudicialRelation == null) {
            throw new DAOException("请为该机构配置审方机构");
        }
        JudicialOrganDAO judicialOrganDAO = DAOFactory.getDAO(JudicialOrganDAO.class);
        JudicialOrgan judicialOrgan = judicialOrganDAO.getByJudicialorganId(organJudicialRelation.getJudicialorganId());
        if (judicialOrgan == null) {
            throw new DAOException("审方机构不存在");
        }
        String serviceName = judicialOrgan.getCallSys() + "PrescriptionService";
        LOGGER.info("PrescriptionService getService serviceName:{}.", serviceName);
        return AppContextHolder.getBean(serviceName, IntellectJudicialService.class);
    }
    
}
