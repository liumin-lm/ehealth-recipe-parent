package recipe.mq;

import com.alibaba.fastjson.JSON;
import com.ngari.common.dto.Buss2SessionMsg;
import com.ngari.recipe.entity.Recipe;
import ctd.net.broadcast.MQHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.OnsConfig;

/**
 * @author yinsheng
 * @date 2021\4\12 0012 15:09
 */
public class Buss2SessionProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Buss2SessionProducer.class);

    public static void sendMsgToMq(Recipe recipe, String contentType, String sessionId) {
        LOGGER.info("Buss2SessionProducer sendMsgToMq recipeID:{},contentType:{},sessionId:{}.", recipe.getRecipeId(), contentType, sessionId);
        Buss2SessionMsg msg = new Buss2SessionMsg();
        msg.setBusId(String.valueOf(recipe.getClinicId()));
        msg.setContentId(String.valueOf(recipe.getRecipeId()));
        msg.setContentType(contentType);
        msg.setDoctorId(recipe.getDoctor());
        msg.setStatus(0);
        msg.setMpiId(recipe.getMpiid());
        msg.setSessionType(4);
        msg.setSessionId(sessionId);
        LOGGER.info("Buss2SessionProducer sendMsgToMq send to MQ, msg = {}", JSON.toJSONString(msg));
        try {
            MQHelper.getMqPublisher().publish(OnsConfig.sessionTopic, msg, "tag_revisit");
        } catch (Exception e) {
            LOGGER.error("Buss2SessionProducer sendMsgToMq error, contentType:{}, recipeID:{}", msg.getContentType(), recipe.getRecipeId(), e);
        }
    }
}
