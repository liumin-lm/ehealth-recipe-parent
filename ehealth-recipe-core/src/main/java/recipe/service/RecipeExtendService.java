package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrderBill;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderBillDAO;

import java.util.Map;

/**
 * created by shiyuping on 2020/4/14
 */
@RpcBean("recipeExtendService")
public class RecipeExtendService {
    private final Logger LOGGER = LoggerFactory.getLogger(RecipeExtendService.class);
    /**
     * 更新处方扩展信息人脸识别认证成功的bizToken
     * @param recipeId
     * @param bizToken 人脸识别认证成功bizToken
     * @return
     */
    @RpcService
    public Boolean updateBizTokenByRecipeId(int recipeId,String bizToken){
        LOGGER.info("updateBizTokenByRecipeId recipeId={},bizToken={}",recipeId, bizToken);
        if (StringUtils.isEmpty(bizToken)){
            throw new DAOException(DAOException.VALUE_NEEDED, "bizToken is require");
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        Map<String, Object> changeAttr = Maps.newHashMap();
        changeAttr.put("medicalSettleData",bizToken);
        return recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId,changeAttr);
    }

    /**
     * 根据处方id查询发票号
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public String queryEinvoiceNumberByRecipeId(Integer recipeId){

        String einvoiceNumber = "";
        if (null != recipeId){
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if (null != recipeExtend && StringUtils.isNotBlank(recipeExtend.getEinvoiceNumber())) {
                einvoiceNumber = recipeExtend.getEinvoiceNumber();
            }else {
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                Recipe recipe = recipeDAO.getByRecipeId(recipeId);
                if (recipe != null && StringUtils.isNotBlank(recipe.getOrderCode())){
                    RecipeOrderBillDAO recipeOrderBillDAO = DAOFactory.getDAO(RecipeOrderBillDAO.class);
                    RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
                    if (null != recipeOrderBill){
                        einvoiceNumber = recipeOrderBill.getBillNumber();
                    }
                }
            }
        }
        return einvoiceNumber;
    }
}
