package recipe.mq;

import com.ngari.common.dto.TempMsgType;
import ctd.net.broadcast.MQHelper;
import ctd.net.broadcast.Observer;
import ctd.net.broadcast.Subscriber;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.ApplicationUtils;

import javax.annotation.PostConstruct;

@RpcBean
public class BusMsgConsumer {

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(BusMsgConsumer.class);

    /**
     * 订阅消息
     */
    @PostConstruct
    @RpcService
    public void busConsultMsgConsumer() {
        //该对象不可删除，此处需要初始化
        if (!OnsConfig.onsSwitch) {
            LOGGER.info("the onsSwitch is set off, consumer not subscribe.");
            return;
        }
        Subscriber subscriber = MQHelper.getMqSubscriber();
        subscriber.attach(OnsConfig.recipeTopic, new Observer<TempMsgType>() {
            @Override
            public void onMessage(TempMsgType tMsg) {
                LOGGER.info("recipeTopic msg[{}]", JSONUtils.toString(tMsg));
                if("synPatientStatusToRecipe".equals(tMsg.getMsgType())){
                    RemoteRecipeService remoteRecipeService = ApplicationUtils.getRecipeService(RemoteRecipeService.class);
                    remoteRecipeService.synPatientStatusToRecipe(tMsg.getMsgContent());
                }
            }
        });
    }
}
