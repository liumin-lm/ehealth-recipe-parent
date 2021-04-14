package recipe.service.afterpay;

import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import recipe.thread.CardDataUploadRunable;
import recipe.thread.RecipeBusiThreadPool;

import java.util.List;

/**
 * 健康卡上传业务
 * @author yinsheng
 * @date 2021\4\13 0013 09:28
 */
@Component("healthCardService")
public class HealthCardService implements IAfterPayBussService{

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCardService.class);

    /**
     * 上传健康卡信息
     * @param recipes  处方信息
     */
    public void uploadHealthCard(List<Recipe> recipes) {
        LOGGER.info("HealthCardService uploadHealthCard:{}.", JSONUtils.toString(recipes));
        //健康卡数据上传
        RecipeBusiThreadPool.execute(new CardDataUploadRunable(recipes.get(0).getClinicOrgan(), recipes.get(0).getMpiid(), "030102"));
    }
}
