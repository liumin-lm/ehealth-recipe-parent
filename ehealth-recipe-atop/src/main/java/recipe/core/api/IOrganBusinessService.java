package recipe.core.api;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;

import java.util.List;

/**
 * 机构相关服务
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IOrganBusinessService {

    /**
     * 获取公众号下机构列表
     * @return 机构列表
     */
    List<Integer> getOrganForWeb();

    /**
     * 获取机构购药方式配置
     *
     * @param organId organId
     * @return 购药方式列表
     */
    List<GiveModeButtonBean> getOrganGiveModeConfig(Integer organId);

    /**
     * 获取机构库存
     *
     * @param recipe     处方
     * @param detailList 处方药品
     * @return
     */
    EnterpriseStock organStock(Recipe recipe, List<Recipedetail> detailList);

    /**
     * 校验  推送的购药方式配置 是否满足机构配置项
     *
     * @param orderId 订单id
     * @return
     */
    boolean giveModeValidate(Integer orderId);

    /**
     * 校验  推送的购药方式配置 是否满足机构配置项
     *
     * @param organId     机构id
     * @param giveModeKey 购药方式
     * @return
     */
    boolean giveModeValidate(Integer organId, String giveModeKey);
}
