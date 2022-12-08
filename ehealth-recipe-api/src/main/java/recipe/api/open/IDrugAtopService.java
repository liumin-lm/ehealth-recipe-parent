package recipe.api.open;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.MedicationInfoResTO;
import com.ngari.platform.recipe.mode.ListOrganDrugReq;
import com.ngari.platform.recipe.mode.ListOrganDrugRes;
import com.ngari.recipe.drug.model.DrugListBean;
import ctd.util.annotation.RpcService;
import recipe.vo.doctor.DrugBookVo;
import recipe.vo.second.RecipeRulesDrugCorrelationVO;


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
    @RpcService(mvcDisabled = true)
    DrugBookVo getDrugBook(Integer organId, String organDrugCode);

    /**
     * 获取药品规则列表
     *
     * @param list
     * @param ruleId
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeRulesDrugCorrelationVO> getListDrugRules(List<Integer> list, Integer ruleId);

    /**
     * 获取机构药品目录
     *
     * @param listOrganDrugReq
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<ListOrganDrugRes> listOrganDrug(ListOrganDrugReq listOrganDrugReq);

    /**
     * 根据drugId获取药品规则
     *
     * @param drugId
     * @param ruleId
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeRulesDrugCorrelationVO> findRulesByDrugIdAndRuleId(Integer drugId, Integer ruleId);


    /**
     * 根据correlationDrugId获取药品规则
     *
     * @param correlationDrugId
     * @param ruleId
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeRulesDrugCorrelationVO> findRulesByCorrelationDrugIdAndRuleId(Integer correlationDrugId, Integer ruleId);

    /**
     * his调用，同步机构数据字典中用药频次、用药途径
     * @param medicationInfoResTOList
     * @return
     */
    @RpcService
    HisResponseTO medicationInfoSyncTaskForHis(List<MedicationInfoResTO> medicationInfoResTOList);

    /**
     * 根据药品名称、规格、生产厂家、药品单位、包装数量查询平台药品目录
     * @param drugListBean
     * @return
     */
    @RpcService
    List<DrugListBean> findDrugListByInfo(DrugListBean drugListBean);


}
