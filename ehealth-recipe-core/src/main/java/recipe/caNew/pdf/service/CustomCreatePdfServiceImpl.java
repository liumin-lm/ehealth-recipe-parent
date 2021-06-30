package recipe.caNew.pdf.service;

import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import org.springframework.stereotype.Service;
import recipe.caNew.pdf.CreatePdfService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 自定义创建pdf
 * 根据自定义工具画模版 方式生成的业务处理代码类
 *
 * @author fuzi
 */
@Service
public class CustomCreatePdfServiceImpl implements CreatePdfService {
    @Override
    public Map<String, Object> queryPdfOssId(Recipe recipe) {
        return null;
    }

    @Override
    public CaSealRequestTO queryPdfByte(Recipe recipe) {
        return null;
    }

    @Override
    public CaSealRequestTO queryCheckPdfByte(Recipe recipe) {
        return null;
    }

    @Override
    public Recipe updateTotalPdf(Integer recipeId, BigDecimal recipeFee) {
        return null;
    }
}
