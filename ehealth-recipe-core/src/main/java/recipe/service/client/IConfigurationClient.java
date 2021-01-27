package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.util.ByteUtils;

/**
 * 获取配置项 交互处理类
 *
 * @author fuzi
 */
@Service
public class IConfigurationClient extends BaseClient {
    @Autowired
    private IConfigurationCenterUtilsService configService;

    /**
     * 获取用药天数
     *
     * @param organId 机构id
     * @return
     */
    public String[] recipeDay(Integer organId) {
        logger.info("IConfigurationClient recipeDay organId= {}", organId);
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "organId is null");
        }
        String[] recipeDay;
        Object isCanOpenLongRecipe = configService.getConfiguration(organId, "isCanOpenLongRecipe");
        if (null == isCanOpenLongRecipe || (boolean) isCanOpenLongRecipe) {
            Object noLongRecipe = configService.getConfiguration(organId, "noLongRecipe");
            if (null == noLongRecipe) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "noLongRecipe is null");
            }
            recipeDay = noLongRecipe.toString().split(ByteUtils.COMMA);
        } else {
            Object yesLongRecipe = configService.getConfiguration(organId, "yesLongRecipe");
            if (null == yesLongRecipe) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "yesLongRecipe is null");
            }
            recipeDay = yesLongRecipe.toString().split(ByteUtils.COMMA);
        }
        if (2 != recipeDay.length) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeDay is error");
        }
        logger.info("IConfigurationClient recipeDay recipeDay= {}", JSON.toJSONString(recipeDay));
        return recipeDay;
    }


}
