package recipe.core.api;

import com.ngari.his.recipe.mode.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.RecipeRulesDrugcorrelation;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.SearchDrugReqVo;

import java.util.List;

/**
 * @description： 药品service 接口
 * @author： whf
 * @date： 2021-08-23 19:02
 */
public interface IDrugBusinessService {
    /**
     * 患者端搜索药品信息
     *
     * @param searchDrugReqVo
     * @return
     */
    List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVo searchDrugReqVo);

    /**
     * 获取药品说明书
     *
     * @param organId       机构id
     * @param organDrugCode 机构药品编码
     * @return
     */
    Dispensatory getDrugBook(Integer organId, String organDrugCode);


    /**
     * 十八反十九畏的规则
     *
     * @param list
     * @param ruleId
     * @return
     */
    List<RecipeRulesDrugcorrelation> getListDrugRules(List<Integer> list, Integer ruleId);

    /**
     * 查询his 药品说明书
     *
     * @param organId      机构id
     * @param recipeDetail 药品数据
     * @return
     */
    DrugSpecificationInfoDTO hisDrugBook(Integer organId, Recipedetail recipeDetail);
}
