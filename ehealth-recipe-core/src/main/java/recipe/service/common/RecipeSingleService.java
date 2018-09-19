package recipe.service.common;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.util.MapValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 获取处方单个数据服务
 * @version： 1.0
 */
@RpcBean("recipeSingleService")
public class RecipeSingleService {

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeDetailDAO detailDAO;

    @RpcService
    public RecipeStandardResTO<Map> getRecipeByConditions(RecipeStandardReqTO request) {
        RecipeStandardResTO<Map> response = RecipeStandardResTO.getRequest(Map.class);
        response.setCode(RecipeCommonBaseTO.FAIL);
        if (request.isNotEmpty()) {
            Map<String, Object> conditions = request.getConditions();
            Integer recipeId = MapValueUtil.getInteger(conditions, "recipeId");
            Recipe dbRecipe;
            if (null != recipeId) {
                dbRecipe = recipeDAO.get(recipeId);
            } else {
                Integer organId = MapValueUtil.getInteger(conditions, "organId");
                String recipeCode = MapValueUtil.getString(conditions, "recipeCode");
                dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, organId);
            }

            if (null != dbRecipe) {
                //组装处方数据
                Map<String, Object> recipeInfo = new HashMap<>();
                List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
                recipeInfo.put("recipe", ObjectCopyUtils.convert(dbRecipe, RecipeBean.class));
                recipeInfo.put("detailList", ObjectCopyUtils.convert(detailList, RecipeBean.class));
                response.setCode(RecipeCommonBaseTO.SUCCESS);
                response.setData(recipeInfo);
            } else {
                response.setMsg("没有处方匹配");
            }

        } else {
            response.setMsg("请求对象为空");
        }

        return response;
    }
}
