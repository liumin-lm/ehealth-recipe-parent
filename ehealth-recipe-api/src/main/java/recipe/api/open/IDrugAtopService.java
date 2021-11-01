package recipe.api.open;

import ctd.util.annotation.RpcService;
import recipe.vo.doctor.DrugBookVo;
import recipe.vo.drug.RecipeRulesDrugcorrelationVo;

import java.util.List;

/**
 * @description： 二方药品请求入口
 * @author： whf
 * @date： 2021-08-26 9:36
 */
public interface IDrugAtopService {

    /**
     * 获取药品说明书
     *
     * @param organId       机构id
     * @param organDrugCode 机构药品编码
     * @returno
     */
    @RpcService
    DrugBookVo getDrugBook(Integer organId, String organDrugCode);

    /**
     * 获取药品规则
     * @param list
     * @param ruleId
     * @return
     */
    @RpcService
    List<RecipeRulesDrugcorrelationVo> getListDrugRules(List<Integer> list, Integer ruleId);

}
