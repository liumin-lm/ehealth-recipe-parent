package recipe.core.api;

import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.RecipeRulesDrugcorrelation;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.SearchDrugReqVO;

import java.util.List;
import java.util.Map;

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
    List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo);

    /**
     * 通过es检索药品
     *
     * @param drugInfo 药品检索条件
     * @param start    开始条数
     * @param limit    每夜条数
     * @return
     */
    List<SearchDrugDetailDTO> searchOrganDrugEs(DrugInfoDTO drugInfo, int start, int limit);

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

    /**
     * 根据ID获取平台药品列表
     *
     * @param drugIds 平台药品ids
     * @return
     */
    Map<Integer, DrugList> drugList(List<Integer> drugIds);
}
