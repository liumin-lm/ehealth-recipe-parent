package recipe.audit.auditmode;

import com.google.common.collect.Maps;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.audit.IAuditMode;

import java.util.Map;
import java.util.Set;

/**
 * created by shiyuping on 2019/8/15
 */
@Service
public class AuditModeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditModeContext.class);
    private static Map<Integer,String> map = Maps.newHashMap();


    public IAuditMode getAuditModes(Integer auditMode){
        String className = map.get(auditMode);
        LOGGER.info("getAuditModes auditMode[{}] className[{}]",auditMode,className);
        Object o;
        try {
            o = Class.forName(className).newInstance();
        } catch (Exception e) {
            LOGGER.error("getAuditModes error,use auditPostMode",e);
            return new AuditPostMode();
        }
        return (IAuditMode)o;
    }

    static{
        Reflections reflections = new Reflections("recipe.audit.auditmode");
        Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(AuditMode.class);
        for (Class<?> auditModeClass :classSet){
            AuditMode annotation = auditModeClass.getAnnotation(AuditMode.class);
            map.put(annotation.value(),auditModeClass.getName());
        }
    }
}
