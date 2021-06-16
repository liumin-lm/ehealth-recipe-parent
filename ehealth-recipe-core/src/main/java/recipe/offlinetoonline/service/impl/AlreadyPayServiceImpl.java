package recipe.offlinetoonline.service.impl;

import recipe.offlinetoonline.vo.SettleForOfflineToOnlineVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.offlinetoonline.constant.OfflineToOnlineEnum;
import recipe.offlinetoonline.service.IOfflineToOnlineService;
import recipe.service.OfflineToOnlineService;

import java.util.List;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上已缴费处方实现类
 */
@Service
public class AlreadyPayServiceImpl implements IOfflineToOnlineService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("offlineToOnlineService")
    private OfflineToOnlineService offlineToOnlineService;

    @Override
    public Integer getPayMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType();
    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        return null;
    }

}
