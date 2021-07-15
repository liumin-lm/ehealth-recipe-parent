package recipe.caNew.pdf.service;

import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import recipe.bussutil.SignImgNode;

import java.math.BigDecimal;
import java.util.List;

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
    SignRecipePdfVO queryPdfOssId(Recipe recipe) throws Exception;

    /**
     * 获取pdf Byte字节 给前端SDK
     *
     * @param recipe 处方信息
     * @return
     */
    CaSealRequestTO queryPdfByte(Recipe recipe) throws Exception;

    /**
     * 在pdf中添加 医生签名
     *
     * @param recipe
     */
    String updateDoctorNamePdf(Recipe recipe, SignImgNode signImgNode) throws Exception;

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
     * @param recipe      处方信息
     * @param signImageId 药师签名
     * @return 文件 fileId
     * @throws Exception
     */
    String updateCheckNamePdf(Recipe recipe, String signImageId) throws Exception;

    /**
     * 在pdf中添加 药品金额
     *
     * @param recipeId
     * @param recipeFee
     * @return
     */
    CoOrdinateVO updateTotalPdf(Recipe recipe, BigDecimal recipeFee);

    /**
     * pdf 处方号和患者病历号
     *
     * @param recipeId
     */
    String updateCodePdf(Recipe recipeId) throws Exception;

    /**
     * 支付成功后修改pdf 添加收货人信息/煎法
     *
     * @param recipeId
     */
    List<CoOrdinateVO> updateAddressPdf(Recipe recipeId, RecipeOrder order, String address);

    /**
     * pdf 核对发药
     *
     * @param recipe 处方
     * @return
     */
    SignImgNode updateGiveUser(Recipe recipe);
}
