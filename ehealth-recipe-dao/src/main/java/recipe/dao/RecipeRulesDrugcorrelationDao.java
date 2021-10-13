package recipe.dao;

import com.ngari.recipe.entity.RecipeRulesDrugcorrelation;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author maoze
 * @description
 * @date 2021年10月12日 14:14
 */
@RpcSupportDAO
public abstract class RecipeRulesDrugcorrelationDao extends HibernateSupportDelegateDAO<RecipeRulesDrugcorrelation> {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    public RecipeRulesDrugcorrelationDao() {
        super();
        this.setEntityName(RecipeRulesDrugcorrelation.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "From RecipeRulesDrugcorrelation where drugId in (:drugIds) and medicationRulesId=:ruleId ")
    public abstract List<RecipeRulesDrugcorrelation> findListRules(@DAOParam("drugIds") List<Integer> drugIds, @DAOParam("ruleId") Integer ruleId);





}