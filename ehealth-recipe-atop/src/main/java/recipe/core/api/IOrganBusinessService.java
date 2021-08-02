package recipe.core.api;

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
}
