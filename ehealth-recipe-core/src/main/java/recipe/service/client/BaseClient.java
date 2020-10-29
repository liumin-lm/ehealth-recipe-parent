package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;

/**
 * his调用基类
 *
 * @author fuzi
 */
public class BaseClient {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * hsi返回成功代码
     */
    private final static String CODE_SUCCEED = "200";

    @Autowired
    protected IRecipeHisService recipeHisService;
    @Autowired
    protected RecipeDAO recipeDAO;
    @Autowired
    protected RecipeDetailDAO recipeDetailDAO;


    protected <T> T getResponse(HisResponseTO<T> hisResponse) throws Exception {
        logger.info("BaseClient getResponse  hisResponse= {}", JSON.toJSONString(hisResponse));
        if (null == hisResponse) {
            throw new DAOException(609, "his返回出错");
        }
        if (!CODE_SUCCEED.equals(hisResponse.getMsgCode())) {
            throw new DAOException(609, "his代码返回出错");
        }
        if (null == hisResponse.getData()) {
            throw new DAOException(609, "his出参为空");
        }
        T result = hisResponse.getData();
        logger.info("BaseClient getResponse request= {}", JSON.toJSONString(result));
        return result;
    }
}
