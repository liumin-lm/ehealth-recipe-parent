package recipe.client;

import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import ctd.util.JSONUtils;
import org.springframework.stereotype.Service;


/**
 * @description： 药品库存查询类
 * @author： whf
 * @date： 2021-07-19 9:25
 */
@Service
public class DrugStockClient extends BaseClient  {

    /**
     * 调用前置机接口查询his库存
     * @param request
     * @return
     */
    public DrugInfoResponseTO scanDrugStock(DrugInfoRequestTO request) {
        logger.info("scanDrugStock request={}", JSONUtils.toString(request));
        DrugInfoResponseTO response=null;
        try {
            response = recipeHisService.scanDrugStock(request);
            logger.info("scanDrugStock response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            logger.error("scanDrugStock error ", e);
            //抛异常流程不应该继续下去
            response = new DrugInfoResponseTO();
            response.setMsgCode(1);
        }
        return response;
    }
}
