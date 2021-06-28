package recipe.caNew.pdf;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;

import java.util.Map;

/**
 * pdf 创建业务接口
 *
 * @author fuzi
 */
public interface CreatePdfService {
    /**
     * 获取pdf oss id
     *
     * @param recipe 处方信息
     * @return
     */
    Map<String, Object> queryPdfOssId(Recipe recipe);

    /**
     * 获取pdf Byte字节 给前端SDK
     *
     * @param recipe 处方信息
     * @return
     */
    String queryPdfByte(RecipeBean recipe);
}
