package recipe.dao;

import com.ngari.recipe.entity.RecipeRulesDrugCorrelation;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author maoze
 * @description
 * @date 2021年10月12日 14:14
 */
@RpcSupportDAO
public abstract class RecipeRulesDrugCorrelationDAO extends HibernateSupportDelegateDAO<RecipeRulesDrugCorrelation> {

    public RecipeRulesDrugCorrelationDAO() {
        super();
        this.setEntityName(RecipeRulesDrugCorrelation.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "From RecipeRulesDrugcorrelation where drugId in (:drugIds) and medicationRulesId=:ruleId ")
    public abstract List<RecipeRulesDrugCorrelation> findListRules(@DAOParam("drugIds") List<Integer> drugIds,
                                                                   @DAOParam("ruleId") Integer ruleId);


    @DAOMethod(sql = "From RecipeRulesDrugcorrelation where drugId = :drugId and medicationRulesId = :ruleId ")
    public abstract List<RecipeRulesDrugCorrelation> findRulesByDrugIdAndRuleId(@DAOParam("drugIds") Integer drugId,
                                                                                @DAOParam("ruleId") Integer ruleId);
}
