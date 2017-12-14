package recipe.unloginservice;

import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.PathologicalDrug;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.service.DrugListService;
import recipe.service.PathologicalDrugService;
import recipe.util.ApplicationUtils;

import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/8.
 */
@RpcBean(value = "recipeUnloginService", mvc_authentication = false)
public class RecipeUnloginService {

    /**
     * 患者端 获取对症药品
     *
     * @param request 查询请求
     *                conditions={"pathologicalType":1}
     * @return RecipeListResTO<List<DrugListBean>> 药品列表信息
     */
    @RpcService
    public List<DrugList> findPathologicalDrugList(PathologicalDrug pathologicalDrug, int start, int limit) {
        PathologicalDrugService service = ApplicationUtils.getRecipeService(PathologicalDrugService.class);
        return service.findPathologicalDrugList(pathologicalDrug, start, limit);
    }

    /**
     * 患者端 查询某个二级药品目录下的药品列表
     *
     * @param drugClass 药品二级目录
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<DrugList> queryDrugsInDrugClass(String drugClass, int start, int limit) {
        DrugListService service = ApplicationUtils.getRecipeService(DrugListService.class);
        return service.queryDrugsInDrugClass(drugClass, start, limit);
    }

    /**
     * 患者端 推荐药品列表
     *
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<DrugList> recommendDrugList(int start, int limit) {
        DrugListService service = ApplicationUtils.getRecipeService(DrugListService.class);
        return service.recommendDrugList(start, limit);
    }

    /**
     * 患者端 获取对应机构的西药 或者 中药的药品有效全目录（现在目录有二级）
     *
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<Map<String, Object>> queryDrugCatalog() {
        DrugListService service = ApplicationUtils.getRecipeService(DrugListService.class);
        return service.queryDrugCatalog();
    }

    /**
     * 患者端 药品搜索服务 药品名 商品名 拼音 别名
     *
     * @param drugName 搜索的文字或者拼音
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<DrugList> searchDrugByNameOrPyCode(String drugName, String mpiId, int start, int limit) {
        DrugListService service = ApplicationUtils.getRecipeService(DrugListService.class);
        return service.searchDrugByNameOrPyCode(drugName, mpiId, start, limit);
    }


}
