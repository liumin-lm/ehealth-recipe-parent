package recipe.caNew.pdf.service;

import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 自定义创建pdf
 * 根据自定义工具画模版 方式生成的业务处理代码类
 *
 * @author fuzi
 */
@Service
public class CustomCreatePdfServiceImpl implements CreatePdfService {
    @Override
    public SignRecipePdfVO queryPdfOssId(Recipe recipe) {
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
    public void updateTotalPdf(Integer recipeId, BigDecimal recipeFee) {
    }

    @Override
    public void updateCheckNamePdf(Recipe recipe) {

    }

    @Override
    public void updateDoctorNamePdf(Recipe recipe) {

    }

    @Override
    public void updateCodePdf(Recipe recipe) {

    }

    @Override
    public void updateAddressPdf(Recipe recipe) {

    }

    @Override
    public Recipe updateGiveUser(Recipe recipe) {
        return null;
    }
}
