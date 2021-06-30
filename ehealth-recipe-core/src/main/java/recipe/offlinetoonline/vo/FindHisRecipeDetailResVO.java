package recipe.offlinetoonline.vo;

import com.ngari.recipe.entity.HisRecipeDetail;
import com.ngari.recipe.entity.HisRecipeExt;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情返回参数
 */
@SuppressWarnings("ALL")
@Data
public class FindHisRecipeDetailResVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    //
    private Map<String, Object> recipeDetailMap;

    //线下处方详情
    private List<HisRecipeDetail> hisRecipeDetails;

    //线下处方拓展数据
    private List<HisRecipeExt> hisRecipeExts;

    //线下处方显示文本
    private String showText;







}


