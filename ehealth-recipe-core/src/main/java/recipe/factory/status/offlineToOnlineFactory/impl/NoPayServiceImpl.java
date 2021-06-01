package recipe.factory.status.offlineToOnlineFactory.impl;

import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.factory.status.constant.OfflineToOnlineEnum;
import recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService;
import recipe.service.OfflineToOnlineService;

import java.util.List;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上待缴费处方实现类
 */
@Service
public class NoPayServiceImpl implements IOfflineToOnlineService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    OfflineToOnlineService offlineToOnlineService;

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        logger.info("NoPayServiceImpl settleForOfflineToOnline request = {}",  JSONUtils.toString(request));
        // 1、线下转线上
        List<Integer> recipeIds = offlineToOnlineService.batchSyncRecipeFromHis(request);
        // 2、获取够药按钮
        List<RecipeGiveModeButtonRes> recipeGiveModeButtonResList = offlineToOnlineService.getRecipeGiveModeButtonRes(recipeIds);
        logger.info("NoPayServiceImpl settleForOfflineToOnline response:{}", JSONUtils.toString(recipeGiveModeButtonResList));
        return recipeGiveModeButtonResList;
    }

    @Override
    public Integer getPayMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType();
    }


}
