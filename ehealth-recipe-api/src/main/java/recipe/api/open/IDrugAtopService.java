package recipe.api.open;

import com.ngari.platform.recipe.mode.ListOrganDrugReq;
import com.ngari.platform.recipe.mode.ListOrganDrugRes;
import ctd.util.annotation.RpcService;
import recipe.vo.doctor.DrugBookVo;
import recipe.vo.second.RecipeDrugRelationRuleVO;

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
     * 获取药品规则列表
     *
     * @param list
     * @param ruleId
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeDrugRelationRuleVO> getListDrugRules(List<Integer> list, Integer ruleId);

    /**
     * 获取机构药品目录
     *
     * @param listOrganDrugReq
     * @return
     */
    @RpcService
    List<ListOrganDrugRes> listOrganDrug(ListOrganDrugReq listOrganDrugReq);

    /**
     * 根据drugId获取药品规则
     *
     * @param drugId
     * @param ruleId
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeDrugRelationRuleVO> findRulesByDrugIdAndRuleId(Integer drugId, Integer ruleId);


    /**
     * 根据correlationDrugId获取药品规则
     *
     * @param correlationDrugId
     * @param ruleId
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeDrugRelationRuleVO> findRulesByCorrelationDrugIdAndRuleId(Integer correlationDrugId, Integer ruleId);
}
