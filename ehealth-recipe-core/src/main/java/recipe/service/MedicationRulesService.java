package recipe.service;

import com.ngari.recipe.commonrecipe.model.MedicationRulesDTO;
import com.ngari.recipe.drug.model.DrugListAndOrganDrugListDTO;
import com.ngari.recipe.entity.MedicationRules;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.MedicationRulesDAO;

import java.util.List;

/**
 * 合理用药规则服务类
 *  @author renfuhao
 */
@RpcBean("medicationRulesService")
public class MedicationRulesService {

    @Autowired
    private MedicationRulesDAO medicationRulesDAO;

    /**
     * 合理用药规则查询接口  （运营平台调用）
     * @param name
     * @param recipeType
     * @return
     */
    @RpcService
    public List<MedicationRulesDTO> queryMedicationRulesBynameAndRecipeType(final String name, final Integer recipeType) {
        List<MedicationRulesDTO> medicationRules = medicationRulesDAO.queryMedicationRulesBynameAndRecipeType(name, recipeType);
        return medicationRules;
    }


}
