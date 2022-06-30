package recipe.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.RulesDrugCorrelationDTO;
import com.ngari.recipe.drug.service.IRulesDrugCorrelationService;
import com.ngari.recipe.entity.RecipeRulesDrugCorrelation;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.dao.RecipeRulesDrugCorrelationDAO;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 合理用药规则 药品关系 服务类
 *
 * @author renfuhao
 */
@RpcBean("rulesDrugCorrelationService")
public class RecipeRulesDrugCorrelationService implements IRulesDrugCorrelationService {

    @Autowired
    private RecipeRulesDrugCorrelationDAO recipeRulesDrugCorrelationDAO;

    private final static Integer OVER = 3;

    /**
     * 合理用药规则,关联药品数据查询接口 （运营平台调用）
     *
     * @param input
     * @return
     */
    @Override
    @RpcService
    public QueryResult<RulesDrugCorrelationDTO> queryRulesDrugCorrelationByDrugCodeOrname(Integer drugId, String input, Integer rulesId, int start, int limit) {
        return recipeRulesDrugCorrelationDAO.queryMedicationRulesByNameAndRecipeType(drugId, input, rulesId, start, limit);
    }


    /**
     * 数据 删除
     *
     * @param drugCorrelationId
     */
    @Override
    @RpcService
    public void deleteRulesDrugCorrelationById(Integer drugCorrelationId) {
        if (ObjectUtils.isEmpty(drugCorrelationId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugCorrelationId is required!");
        }
        RecipeRulesDrugCorrelation recipeRulesDrugCorrelation = recipeRulesDrugCorrelationDAO.get(drugCorrelationId);
        if (ObjectUtils.isEmpty(recipeRulesDrugCorrelation)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该规则关联药品关系数据!");
        }
        recipeRulesDrugCorrelationDAO.remove(drugCorrelationId);
    }

    /**
     * 合理用药规则 关联药品数据新增接口
     *
     * @param
     * @return
     */
    @Override
    @RpcService
    public void addRulesDrugCorrelation(List<RulesDrugCorrelationDTO> list, Integer rulesId) {
        if (Objects.isNull(rulesId) || CollectionUtils.isEmpty(list)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "param is required!");
        }
        //前端目前空的RulesDrugCorrelationDTO对象也会传过来，每个字段都是"", 临时处理下
        List<RulesDrugCorrelationDTO> rulesList = list.stream().filter(x -> Objects.nonNull(x.getDrugId())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        if (OVER.equals(rulesId)) {
            //超量
            for (RulesDrugCorrelationDTO rule : rulesList) {
                List<RecipeRulesDrugCorrelation> recipeRulesDrugCorrelationList = recipeRulesDrugCorrelationDAO.findRulesByDrugIdAndRuleId(rule.getDrugId(), rulesId);
                if (CollectionUtils.isNotEmpty(recipeRulesDrugCorrelationList)) {
                    throw new DAOException(DAOException.VALUE_NEEDED, "保存数据" + rule.getDrugName() + "此药品已存在，请重新填写!");
                }
                RecipeRulesDrugCorrelation recipeRulesDrugCorrelation = ObjectCopyUtils.convert(rule, RecipeRulesDrugCorrelation.class);
                recipeRulesDrugCorrelation.setCreateDt(new Date());
                recipeRulesDrugCorrelation.setLastModify(new Date());
                recipeRulesDrugCorrelationDAO.save(recipeRulesDrugCorrelation);
            }
        } else {
            //十八反、十九畏
            for (RulesDrugCorrelationDTO ruleDTO : rulesList) {
                RecipeRulesDrugCorrelation rule = ObjectCopyUtils.convert(ruleDTO, RecipeRulesDrugCorrelation.class);
                RecipeRulesDrugCorrelation drugCorrelation = recipeRulesDrugCorrelationDAO.getDrugCorrelationByCodeAndRulesId(rulesId, rule.getDrugId(), rule.getCorrelationDrugId());
                if (Objects.nonNull(drugCorrelation)) {
                    throw new DAOException(DAOException.VALUE_NEEDED, "保存数据【" + rule.getDrugName() + "】规则关联【" + rule.getCorrelationDrugName() + "】关联关系数据已存在!");
                }
                rule.setCreateDt(new Date());
                rule.setLastModify(new Date());
                recipeRulesDrugCorrelationDAO.save(rule);
            }
        }

    }

    /**
     * 添加十八反、十九畏、超量时校验该规则是否已存在
     * 前端每添加一条校验一次
     *
     * @param correlationDTO
     */
    @Override
    @RpcService
    public Boolean checkRulesDrugCorrelations(RulesDrugCorrelationDTO correlationDTO, Integer rulesId) {
        if (Objects.isNull(correlationDTO) || Objects.isNull(correlationDTO.getDrugId()) || Objects.isNull(rulesId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "param is required!");
        }

        if (OVER.equals(rulesId)) {
            List<RecipeRulesDrugCorrelation> drugCorrelationList = recipeRulesDrugCorrelationDAO.findRulesByDrugIdAndRuleId(correlationDTO.getDrugId(), rulesId);
            return CollectionUtils.isNotEmpty(drugCorrelationList);
        } else {
            RecipeRulesDrugCorrelation drugCorrelation = recipeRulesDrugCorrelationDAO.getDrugCorrelationByCodeAndRulesId(rulesId, correlationDTO.getDrugId(), correlationDTO.getCorrelationDrugId());
            return Objects.nonNull(drugCorrelation);
        }
    }

}
