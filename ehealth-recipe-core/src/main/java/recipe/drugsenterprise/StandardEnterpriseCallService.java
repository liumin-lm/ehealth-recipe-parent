package recipe.drugsenterprise;

import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.drugsenterprise.bean.StandardResult;

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

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardEnterpriseCallService.class);

    @RpcService
    public StandardResult send(List<Map<String, Object>> list){
        LOGGER.info("send param : " + JSONUtils.toString(list));
        StandardResult result = new StandardResult();
        if(CollectionUtils.isEmpty(list)){
            result.setMsg("参数错误");
            return result;
        }



        return null;
    }

    @RpcService
    public StandardResult finish(List<Map<String, Object>> list){

        return null;
    }

}
