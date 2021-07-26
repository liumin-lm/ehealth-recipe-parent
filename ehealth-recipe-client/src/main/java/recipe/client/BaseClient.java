package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.constant.HisErrorCodeEnum;

/**
 * his调用基类
 *
 * @author fuzi
 */
public class BaseClient {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected IRecipeHisService recipeHisService;


    /**
     * 解析前置机 出参
     *
     * @param hisResponse 前置机出参
     * @param <T>         范型
     * @return 返回封装的data
     * @throws DAOException 自定义前置机异常
     * @throws Exception    运行异常
     */
    protected <T> T getResponse(HisResponseTO<T> hisResponse) throws DAOException, Exception {
        logger.info("BaseClient getResponse  hisResponse= {}", JSON.toJSONString(hisResponse));
        if (null == hisResponse) {
            throw new DAOException(HisErrorCodeEnum.HIS_NULL_ERROR.getCode(), HisErrorCodeEnum.HIS_NULL_ERROR.getMsg());
        }
        if (!String.valueOf(HisErrorCodeEnum.HIS_SUCCEED.getCode()).equals(hisResponse.getMsgCode())) {
            throw new DAOException(HisErrorCodeEnum.HIS_CODE_ERROR.getCode(), HisErrorCodeEnum.HIS_CODE_ERROR.getMsg());
        }
        if (null == hisResponse.getData()) {
            throw new DAOException(HisErrorCodeEnum.HIS_PARAMETER_ERROR.getCode(), HisErrorCodeEnum.HIS_PARAMETER_ERROR.getMsg());
        }
        T result = hisResponse.getData();
        logger.info("BaseClient getResponse result= {}", JSON.toJSONString(result));
        return result;
    }


    /**
     * 扩展 当 前置机没实现接口时特殊处理返回值
     * 不建议使用，只保留特殊处理老代码风格
     *
     * @param hisResponse 前置机出参
     * @param <T>         范型
     * @return 返回封装的data
     */
    protected <T> T getResponseCatch(HisResponseTO<T> hisResponse) {
        try {
            return getResponse(hisResponse);
        } catch (DAOException e) {
            if (HisErrorCodeEnum.HIS_NULL_ERROR.getCode() == e.getCode()) {
                return null;
            }
            throw new DAOException(e);
        } catch (Exception e1) {
            logger.error("BaseClient getResponseCatch hisResponse= {}", JSON.toJSONString(hisResponse));
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        }
    }
}
