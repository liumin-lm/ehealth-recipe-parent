package recipe.core.api;

import com.ngari.recipe.recipe.model.GiveModeButtonBean;

import java.util.List;
import java.util.Set;

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
     * @param organId organId
     * @return 购药方式列表
     */
    List<GiveModeButtonBean> getOrganGiveModeConfig(Integer organId);
}
