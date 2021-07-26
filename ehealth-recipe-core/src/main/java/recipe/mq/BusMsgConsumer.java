package recipe.mq;

import com.ngari.common.dto.TempMsgType;
import ctd.net.broadcast.MQHelper;
import ctd.net.broadcast.MQSubscriber;
import ctd.net.broadcast.Observer;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.msg.constant.MqConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeSystemConstant;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;

import javax.annotation.PostConstruct;

@RpcBean
public class BusMsgConsumer {

    /**
     * logger
     */
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

        MQSubscriber subscriber = MQHelper.getMqSubscriber();
        /**
         * basic相关消息
         */
        subscriber.attach(OnsConfig.basicInfoTopic, new Observer<TempMsgType>() {
            @Override
            public void onMessage(TempMsgType tMsg) {
                LOGGER.info("basicInfoTopic msg[{}]", JSONUtils.toString(tMsg));
                invalidPatient(tMsg);
            }
        });

        /**
         * 接收HIS消息处理
         */
        subscriber.attach(OnsConfig.hisCdrinfo, MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM,
                new RecipeStatusFromHisObserver());

        /**
         * 接收药品修改消息
         */
        subscriber.attach(OnsConfig.dbModifyTopic, "base_druglist||base_organdruglist",
                new DrugSyncObserver());

        /**
         * 接收电子病历删除发送
         */
        subscriber.attach(OnsConfig.emrRecipe, "emrDeleted_recipe", new MqEmrRecipeServer());

        /**
         * 接收处方失效延迟消息
         */
        subscriber.attach(OnsConfig.recipeDelayTopic, RecipeSystemConstant.RECIPE_INVALID_TOPIC_TAG, new RecipeInvalidMsgConsumer());

        /*
        subscriber.attach(OnsConfig.hisCdrinfo, "recipeMedicalInfoFromHis",
                new RecipeMedicalInfoFromHisObserver());*/

    }


    @RpcService
    public void invalidPatient(TempMsgType tMsg) {
        //"删除就诊人"
        if ("10".equals(tMsg.getMsgType())) {
            RemoteRecipeService remoteRecipeService = ApplicationUtils.getRecipeService(RemoteRecipeService.class);
            remoteRecipeService.synPatientStatusToRecipe(tMsg.getMsgContent());
        }
    }
}
