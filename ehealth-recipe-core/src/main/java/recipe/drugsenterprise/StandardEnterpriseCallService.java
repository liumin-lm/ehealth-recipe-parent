package recipe.drugsenterprise;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.bean.ThirdResultBean;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 药企标准服务
 * @version： 1.0
 */
@RpcBean("distributionService")
public class StandardEnterpriseCallService {

    @RpcService
    public List<ThirdResultBean> send(List<Map<String, Object>> list){

        return null;
    }

    @RpcService
    public List<ThirdResultBean> finish(List<Map<String, Object>> list){

        return null;
    }

}
