package recipe.caNew.pdf.service;

import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.exception.DAOException;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.WordToPdfBean;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.constant.CacheConstant;
import recipe.constant.ErrorCode;
import recipe.service.client.IConfigurationClient;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

/**
 * 自定义创建pdf
 * 根据自定义工具画模版 方式生成的业务处理代码类
 *
 * @author fuzi
 */
@Service
public class CustomCreatePdfServiceImpl implements CreatePdfService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private IConfigurationClient configurationClient;

    @Override
    public SignRecipePdfVO queryPdfOssId(Recipe recipe) {
        return null;
    }

    @Override
    public CaSealRequestTO queryPdfByte(Recipe recipe) {
        String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), "xxxxxxpdf", "");
        String pdfBase64Str;
        try {
            //需要记录坐标的的字段
            List<CoOrdinateVO> ordinateList = new LinkedList<>();
            //替换模版pdf的字段
            List<WordToPdfBean> generatePdfList = new LinkedList<>();
            
            byte[] data = CreateRecipePdfUtil.generateTemplatePdf(recipe.getRecipeId(), organSealId, generatePdfList, ordinateList);
            pdfBase64Str = new String(Base64.encode(data));
            redisClient.addList(CacheConstant.KEY_RECIPE_LABEL + recipe.getRecipeId().toString(), ordinateList, 3 * 24 * 60 * 60L);
        } catch (Exception e) {
            logger.error("CustomCreatePdfServiceImpl queryPdfByte error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "CustomCreatePdfServiceImpl queryPdfByte error");
        }
        return CreatePdfFactory.caSealRequestTO(55, 76, recipe.getRecipeId().toString(), pdfBase64Str);
    }

    @Override
    public CaSealRequestTO queryCheckPdfByte(Recipe recipe) {
        return null;
    }

    @Override
    public void updateTotalPdf(Integer recipeId, BigDecimal recipeFee) {
    }

    @Override
    public String updateCheckNamePdf(Recipe recipe) {
        return null;
    }

    @Override
    public String updateDoctorNamePdf(Recipe recipe) {
        return null;
    }

    @Override
    public String updateCodePdf(Recipe recipe) {
        return null;
    }

    @Override
    public void updateAddressPdf(Recipe recipe) {

    }

    @Override
    public Recipe updateGiveUser(Recipe recipe) {
        return null;
    }
}
