package recipe.service;

import com.google.common.collect.Maps;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeExtendDAO;

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
}
