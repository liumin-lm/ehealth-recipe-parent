package recipe.service.recipereportforms;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * created by shiyuping on 2020/7/1
 * 处方报表服务
 */
@RpcBean("recipeReportFormsService")
public class RecipeReportFormsService {

    @RpcService
    public void recipeReportFormsService(){

    }

    /**
     * 互联网药品收发存报表
     * @return
     */
    @RpcService
    public Map<String,Object> drugReceivedDispatchedBalanceList(){
        return null;
    }

    /**
     * 互联网月度对账汇总列表
     * @return
     */
    @RpcService
    public Map<String,Object> recipeMonthAccountCheckList(){
        return null;
    }

    /**
     * 处方对账明细列表
     * @return
     */
    @RpcService
    public Map<String,Object> recipeAccountCheckDetailList(){
        return null;
    }

    /**
     * 药企处方月度汇总列表
     * @return
     */
    @RpcService
    public Map<String,Object> enterpriseRecipeMonthSummaryList(){
        return null;
    }

    /**
     * 药企处方明细列表
     * @return
     */
    @RpcService
    public Map<String,Object> enterpriseRecipeDetailList(){
        return null;
    }

    /**
     * 处方his对账表
     * @return
     */
    @RpcService
    public Map<String,Object> recipeHisAccountCheckList(){
        return null;
    }

}
