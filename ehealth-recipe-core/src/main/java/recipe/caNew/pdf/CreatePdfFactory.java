package recipe.caNew.pdf;

import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.exception.DAOException;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author fuzi
 */
@Service
public class CreatePdfFactory {

    @Resource(name = "platformCreatePdfServiceImpl")
    private CreatePdfService createPdfService;

    /**
     * 获取pdf oss id
     *
     * @param recipe
     * @return
     */
    public Map<String, Object> queryPdfOssId(Recipe recipe) {
        if (ValidateUtil.validateObjects(recipe, recipe.getRecipeId(), recipe.getClinicOrgan())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        //判断自定义有就调用 CustomCreatePdfServiceImpl
        return createPdfService.queryPdfOssId(recipe);
    }

    /**
     * 获取pdf byte 格式
     *
     * @param recipe
     * @return
     */
    public CaSealRequestTO queryPdfByte(RecipeBean recipe) {
        if (ValidateUtil.validateObjects(recipe, recipe.getRecipeId(), recipe.getClinicOrgan())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        //判断自定义有就调用 CustomCreatePdfServiceImpl
        return createPdfService.queryPdfByte(recipe);
    }


    /**
     * 获取药师 pdf byte 格式
     *
     * @param recipe
     * @return
     */
    public CaSealRequestTO queryCheckPdfByte(RecipeBean recipe) {
        if (ValidateUtil.validateObjects(recipe, recipe.getRecipeId(), recipe.getSignFile())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        //判断自定义有就调用 CustomCreatePdfServiceImpl
        return createPdfService.queryCheckPdfByte(recipe);
    }

}
