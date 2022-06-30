package recipe.dao;

import com.ngari.recipe.commonrecipe.model.RulesDrugCorrelationDTO;
import com.ngari.recipe.entity.RecipeRulesDrugCorrelation;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.List;
import java.util.Objects;

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

    @DAOMethod(sql = "FROM RecipeRulesDrugCorrelation WHERE drugId in (:drugIds) AND medicationRulesId=:ruleId ")
    public abstract List<RecipeRulesDrugCorrelation> findListRules(@DAOParam("drugIds") List<Integer> drugIds,
                                                                   @DAOParam("ruleId") Integer ruleId);


    @DAOMethod(sql = "FROM RecipeRulesDrugCorrelation WHERE drugId = :drugId AND medicationRulesId = :ruleId ")
    public abstract List<RecipeRulesDrugCorrelation> findRulesByDrugIdAndRuleId(@DAOParam("drugId") Integer drugId,
                                                                                @DAOParam("ruleId") Integer ruleId);

    @DAOMethod(sql = "FROM RecipeRulesDrugCorrelation WHERE correlationDrugId = :correlationDrugId AND medicationRulesId = :ruleId ")
    public abstract List<RecipeRulesDrugCorrelation> findRulesByCorrelationDrugIdAndRuleId(@DAOParam("correlationDrugId") Integer correlationDrugId,
                                                                                           @DAOParam("ruleId") Integer ruleId);

    @DAOMethod(sql = "FROM RecipeRulesDrugCorrelation WHERE medicationRulesId = :medicationRulesId " +
            "AND drugId = :drugId AND correlationDrugId = :correlationDrugId ", limit = 0)
    public abstract RecipeRulesDrugCorrelation getDrugCorrelationByCodeAndRulesId(@DAOParam("medicationRulesId") Integer medicationRulesId,
                                                                                  @DAOParam("drugId") Integer drugId,
                                                                                  @DAOParam("correlationDrugId") Integer correlationDrugId);


    public QueryResult<RulesDrugCorrelationDTO> queryMedicationRulesByNameAndRecipeType(Integer drugId, String input, Integer rulesId, int start, int limit) {
        HibernateStatelessResultAction<QueryResult<RulesDrugCorrelationDTO>> action = new AbstractHibernateStatelessResultAction<QueryResult<RulesDrugCorrelationDTO>>() {
            @Override
            public void execute(StatelessSession ss) {
                StringBuilder hql = new StringBuilder("FROM RecipeRulesDrugCorrelation  WHERE 1=1 ");
                if (Objects.nonNull(drugId)) {
                    hql.append(" AND (drugId = :drugId or correlationDrugId = :drugId) ");
                }
                if (Objects.nonNull(input)) {
                    hql.append(" AND (drugName LIKE :input or correlationDrugName LIKE :input) ");
                }
                if (Objects.nonNull(rulesId)) {
                    hql.append(" AND medicationRulesId = :rulesId ");
                }
                hql.append(" order by createDt desc ");

                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (Objects.nonNull(input)) {
                    countQuery.setParameter("input", "%" + input + "%");
                }
                if (Objects.nonNull(drugId)) {
                    countQuery.setParameter("drugId", drugId);
                }
                if (Objects.nonNull(rulesId)) {
                    countQuery.setParameter("rulesId", rulesId);
                }
                Long total = (Long) countQuery.uniqueResult();

                Query q = ss.createQuery(hql.toString());
                if (Objects.nonNull(input)) {
                    countQuery.setParameter("input", "%" + input + "%");
                }
                if (Objects.nonNull(drugId)) {
                    countQuery.setParameter("drugId", drugId);
                }
                if (Objects.nonNull(rulesId)) {
                    countQuery.setParameter("rulesId", rulesId);
                }
                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<RulesDrugCorrelationDTO> lists = q.list();
                setResult(new QueryResult<>(total, q.getFirstResult(), q.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
