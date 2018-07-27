package recipe.mq;

import com.ngari.common.dto.TempMsgType;
import ctd.net.broadcast.MQHelper;
import ctd.net.broadcast.Observer;
import ctd.net.broadcast.Subscriber;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.MsgTypeEnum;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;

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
    public void busRecipeMsgConsumer() {
        //该对象不可删除，此处需要初始化
        OnsConfig onsConfig = (OnsConfig) AppContextHolder.getBean("onsConfig");
        if (!OnsConfig.onsSwitch) {
            LOGGER.info("the onsSwitch is set off, consumer not subscribe.");
            return;
        }
        LOGGER.info("busRecipeMsgConsumer start");
        Subscriber subscriber = MQHelper.getMqSubscriber();
        subscriber.attach(OnsConfig.patientTopic, new Observer<TempMsgType>() {
            @Override
            public void onMessage(TempMsgType tMsg) {
                LOGGER.info("patientTopic msg[{}]", JSONUtils.toString(tMsg));
                invalidPatient(tMsg);
            }
        });
    }

    @RpcService
    public void invalidPatient(TempMsgType tMsg){
        if(MsgTypeEnum.DELETE_PATIENT.equals(tMsg.getMsgType())){
            RemoteRecipeService remoteRecipeService = ApplicationUtils.getRecipeService(RemoteRecipeService.class);
            remoteRecipeService.synPatientStatusToRecipe(tMsg.getMsgContent());
        }
    }
}
