package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.platform.recipe.mode.AdvanceInfoReqTO;
import com.ngari.platform.recipe.mode.AdvanceInfoResTO;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

/**
 * @author zgy
 * @date 2022/6/28 18:50
 */
@Service
public class RecipeClient extends BaseClient{

    @Autowired
    private IRecipeHisService iRecipeHisService;

    public AdvanceInfoResTO getAdvanceInfo(AdvanceInfoReqTO advanceInfoReqTO) {
        logger.info("RecipeClient AdvanceInfoReqTO advanceInfoReqTO:{}", JSON.toJSONString(advanceInfoReqTO));
        HisResponseTO<AdvanceInfoResTO> advanceInfo = iRecipeHisService.getAdvanceInfo(advanceInfoReqTO);
        try {
            return getResponse(advanceInfo);
        } catch (Exception e) {
            logger.error("RecipeClient getAdvanceInfo hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
