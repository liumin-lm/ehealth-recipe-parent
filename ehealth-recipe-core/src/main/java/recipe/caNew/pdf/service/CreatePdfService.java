package recipe.caNew.pdf.service;

import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;

import java.math.BigDecimal;

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
    SignRecipePdfVO queryPdfOssId(Recipe recipe);

    /**
     * 获取pdf Byte字节 给前端SDK
     *
     * @param recipe 处方信息
     * @return
     */
    CaSealRequestTO queryPdfByte(Recipe recipe);

    /**
     * 在pdf中添加 医生签名
     *
     * @param recipe
     */
    void updateDoctorNamePdf(Recipe recipe);

    /**
     * 获取药师签名 pdf Byte字节 给前端SDK
     *
     * @param recipe 处方信息
     * @return
     */
    CaSealRequestTO queryCheckPdfByte(Recipe recipe);

    /**
     * 在pdf中添加 药师签名
     *
     * @param recipeId
     */
    void updateCheckNamePdf(Integer recipeId);

    /**
     * 在pdf中添加 药品金额
     *
     * @param recipeId
     * @param recipeFee
     * @return
     */
    void updateTotalPdf(Integer recipeId, BigDecimal recipeFee);

    /**
     * pdf 处方号和患者病历号
     *
     * @param recipeId
     */
    void updateCodePdf(Integer recipeId);

    /**
     * 支付成功后修改pdf 添加收货人信息/煎法
     *
     * @param recipeId
     */
    void updateAddressPdf(Integer recipeId);

    /**
     * pdf 核对发药
     *
     * @param recipe 处方
     * @return
     */
    Recipe updateGiveUser(Recipe recipe);
}
