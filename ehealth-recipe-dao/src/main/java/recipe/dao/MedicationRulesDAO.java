package recipe.dao;

import com.ngari.recipe.commonrecipe.model.MedicationRulesDTO;
import com.ngari.recipe.commonrecipe.model.RulesDrugCorrelationDTO;
import com.ngari.recipe.entity.MedicationRules;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 合理用药规则表dao
 *
 * @author renfuhao
 */
@RpcSupportDAO
public class MedicationRulesDAO extends HibernateSupportDelegateDAO<MedicationRules> {
    private static Logger logger = Logger.getLogger(MedicationRulesDAO.class);

    public MedicationRulesDAO() {
        super();
        this.setEntityName(MedicationRules.class.getName());
        this.setKeyField("medicationRulesId");
    }

    public List<MedicationRulesDTO> queryMedicationRulesBynameAndRecipeType(final String name,
                                                                            final int recipeType) {
        HibernateStatelessResultAction<List<MedicationRulesDTO>> action = new AbstractHibernateStatelessResultAction<List<MedicationRulesDTO>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select *  from  MedicationRules  where 1=1  ");
                if (!ObjectUtils.isEmpty(name)) {
                    hql.append(" and medicationRulesName like:name ");
                }
                if (!ObjectUtils.isEmpty(recipeType)) {
                    hql.append("and recipeType =:recipeType) ");
                }
                Query q = ss.createQuery(hql.toString());
                if (!ObjectUtils.isEmpty(name)) {
                    q.setParameter("name", "%" + name + "%");
                }
                if (!ObjectUtils.isEmpty(recipeType)) {
                    q.setParameter("recipeType", recipeType);
                }
                hql.append("  order by createDt desc");
                List<MedicationRulesDTO> lists = q.list();
                setResult(lists);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
