package recipe.caNew.pdf.service;

import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.Recipe;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.IConfigurationClient;
import recipe.client.OperationClient;
import recipe.constant.OperationConstant;
import recipe.manager.RecipeManager;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

/**
 * @author fuzi
 */
public class BaseCreatePdf {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected OperationClient operationClient;
    @Autowired
    protected RecipeManager recipeManager;
    @Autowired
    protected IConfigurationClient configurationClient;

    /**
     * 条形码
     *
     * @param recipe
     * @return
     */
    protected String barcode(Recipe recipe) {
        String barCode = configurationClient.getValueCatch(recipe.getClinicOrgan(), OperationConstant.OP_BARCODE, "");
        logger.info("BaseCreatePdf barcode barCode:{}", barCode);
        if (OperationConstant.OP_BARCODE_DEFAULT.equals(barCode)) {
            return null;
        }
        String[] keySplit = barCode.trim().split(ByteUtils.DOT);
        if (2 != keySplit.length) {
            return null;
        }
        String objectName = keySplit[0];
        String fieldName = keySplit[1];
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDictionary(recipe.getRecipeId());
        String barcode = operationClient.invokeFieldName(objectName, fieldName, recipePdfDTO);
        logger.info("BaseCreatePdf barcode barcode:{}", barcode);
        return barcode;
    }


    /**
     * 组织pdf Byte字节 给前端SDK 出餐
     *
     * @param leftX        行坐标
     * @param pdfName      文件名
     * @param pdfBase64Str 文件
     * @return
     */
    protected CaSealRequestTO caSealRequestTO(int leftX, int leftY, String pdfName, String pdfBase64Str) {
        CaSealRequestTO caSealRequest = new CaSealRequestTO();
        caSealRequest.setPdfBase64Str(pdfBase64Str);
        //这个赋值后端没在用 可能前端在使用,所以沿用老代码写法
        caSealRequest.setLeftX(leftX);
        caSealRequest.setLeftY(leftY);
        caSealRequest.setPdfName("recipe" + pdfName + ".pdf");
        caSealRequest.setSealHeight(40);
        caSealRequest.setSealWidth(40);
        caSealRequest.setPage(1);
        caSealRequest.setPdfMd5("");
        caSealRequest.setMode(1);
        return caSealRequest;
    }


    /**
     * 获取天数 与 单位字符串展示
     *
     * @param useDaysB
     * @param useDays
     * @return
     */
    protected String getUseDays(String useDaysB, Integer useDays) {
        if (StringUtils.isNotEmpty(useDaysB) && !"0".equals(useDaysB)) {
            return useDaysB + "天";
        }
        if (!ValidateUtil.integerIsEmpty(useDays)) {
            return useDays + "天";
        }
        return "";
    }

}
