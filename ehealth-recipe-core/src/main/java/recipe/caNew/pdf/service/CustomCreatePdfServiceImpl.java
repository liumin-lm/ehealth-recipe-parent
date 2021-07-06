package recipe.caNew.pdf.service;

import com.alibaba.fastjson.JSON;
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
import recipe.constant.ErrorCode;
import recipe.service.client.IConfigurationClient;
import recipe.service.manager.RedisManager;

import java.math.BigDecimal;
import java.util.Arrays;
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
    /**
     * 需要记录坐标的的字段
     */
    private final List<String> ADDITIONAL_FIELDS = Arrays.asList("doctorSignImg,doctorSignImgToken", "recipe.check", "recipe.actualPrice", "recipe.patientID", "recipe.recipeCode", "address", "tcmDecoction", "recipe.giveUser");
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private IConfigurationClient configurationClient;

    @Override
    public SignRecipePdfVO queryPdfOssId(Recipe recipe) {
        return null;
    }

    @Override
    public CaSealRequestTO queryPdfByte(Recipe recipe) {
        logger.info("CustomCreatePdfServiceImpl queryPdfByte recipe = {}", recipe.getRecipeId());
        String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), "xxxxxxpdf", "");
        try {
            //需要记录坐标的的字段
            List<CoOrdinateVO> ordinateList = ordinateList();
            //替换模版pdf的字段
            List<WordToPdfBean> generatePdfList = generatePdfList(recipe);
            byte[] data = CreateRecipePdfUtil.generateTemplatePdf(recipe.getRecipeId(), organSealId, generatePdfList, ordinateList);
            if (null == data) {
                return null;
            }
            redisManager.coOrdinate(recipe.getRecipeId(), ordinateList);
            String pdfBase64Str = new String(Base64.encode(data));
            CoOrdinateVO ordinateVO = redisManager.getPdfCoords(recipe.getRecipeId(), "doctorSignImg,doctorSignImgToken");
            return CreatePdfFactory.caSealRequestTO(ordinateVO.getX(), ordinateVO.getY(), recipe.getRecipeId().toString(), pdfBase64Str);
        } catch (Exception e) {
            logger.error("CustomCreatePdfServiceImpl queryPdfByte error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "CustomCreatePdfServiceImpl queryPdfByte error");
        }
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

    /**
     * 需要记录坐标的的字段
     *
     * @return
     */
    private List<CoOrdinateVO> ordinateList() {
        List<CoOrdinateVO> ordinateList = new LinkedList<>();
        ADDITIONAL_FIELDS.forEach(a -> {
            CoOrdinateVO coOrdinateVO = new CoOrdinateVO();
            coOrdinateVO.setName(a);
            ordinateList.add(coOrdinateVO);
        });
        logger.info("CustomCreatePdfServiceImpl ordinateList ordinateList = {}", JSON.toJSONString(ordinateList));
        return ordinateList;
    }

    private List<WordToPdfBean> generatePdfList(Recipe recipe) {
        List<WordToPdfBean> generatePdfList = new LinkedList<>();


        logger.info("CustomCreatePdfServiceImpl generatePdfList generatePdfList = {}", JSON.toJSONString(generatePdfList));
        return generatePdfList;
    }

}
