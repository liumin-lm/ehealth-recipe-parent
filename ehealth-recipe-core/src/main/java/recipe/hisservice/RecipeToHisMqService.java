package recipe.hisservice;

import com.ngari.his.recipe.mode.NoticeHisRecipeInfoReq;
import ctd.net.broadcast.MQHelper;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.msg.constant.MqConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.OnsConfig;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/30
 * @description： 与HIS交互MQ实现
 * @version： 1.0
 */
@RpcBean("recipeToHisMqService")
public class RecipeToHisMqService {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeToHisMqService.class);

    /**
     * 发送HIS处方状态
     *
     * @param notice
     */
    @RpcService
    public void recipeStatusToHis(NoticeHisRecipeInfoReq notice) {
        log(OnsConfig.hisCdrinfo, MqConstant.HIS_CDRINFO_TAG_TO_HIS, notice);
        MQHelper.getMqPublisher().publish(OnsConfig.hisCdrinfo, notice, MqConstant.HIS_CDRINFO_TAG_TO_HIS,
                notice.getOrganizeCode() + "-" + notice.getRecipeID());
    }

    public void log(String topic, String tag, Object obj) {
        LOGGER.info("topic={}, tag={}, msg={}", topic, tag, JSONUtils.toString(obj));
    }
}
