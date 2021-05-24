package recipe.factory.status.offlineToOnlineFactory;

import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import recipe.bean.RecipeGiveModeButtonRes;

import java.util.List;

/**
 * @Author liumin
 * @Date 2021/5/18 上午11:42
 * @Description 线下转线上接口类
 */
public interface IOfflineToOnlineService {


    /**
     * 获取实现类 类型
     *
     * @return
     */
    Integer getPayMode();

    /**
     * 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
     *
     * @param request
     * @return
     */
    List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request);
}
