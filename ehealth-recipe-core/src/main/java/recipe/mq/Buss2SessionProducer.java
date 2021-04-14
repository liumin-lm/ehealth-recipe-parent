package recipe.mq;

import com.ngari.common.dto.Buss2SessionMsg;
import com.ngari.recipe.entity.Recipe;
import ctd.net.broadcast.MQHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yinsheng
 * @date 2021\4\12 0012 15:09
 */
public class Buss2SessionProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Buss2SessionProducer.class);

    public static void sendMsgToMq(Recipe recipe, String contentType, Integer sessionType, String sessionId, String assessHisId) {
        LOGGER.info("Buss2SessionProducer sendMsgToMq recipeID:{},contentType:{},sessionId:{}.", recipe.getRecipeId(), contentType, sessionId);
        Buss2SessionMsg msg = new Buss2SessionMsg();
        if (StringUtils.isNotEmpty(assessHisId)){
            msg.setMsgKey(assessHisId);
        }
        msg.setBusId(String.valueOf(recipe.getRecipeId()));
        msg.setContentId(String.valueOf(recipe.getClinicId()));
        msg.setContentType(contentType);
        msg.setDoctorId(recipe.getDoctor());
        msg.setStatus(0);
        msg.setMpiId(recipe.getMpiid());
        msg.setSessionType(sessionType);
        msg.setSessionId(sessionId);
        if (null == msg.getSessionType()) {
            LOGGER.info("Buss2SessionProducer sendMsgToMq sessionType null");
            return;
        }
        try {
            MQHelper.getMqPublisher().publish(OnsConfig.sessionTopic, msg, "tag_revisit");
            LOGGER.info("Buss2SessionProducer sendMsgToMq send to MQ, sessionType:{}, key:{}", msg.getSessionType(), msg.getMsgKey());
        } catch (Exception e) {
            LOGGER.error("Buss2SessionProducer sendMsgToMq can't send to MQ, sessionType:{}, key:{}", msg.getSessionType(), msg.getMsgKey(), e);
        }
    }
}
