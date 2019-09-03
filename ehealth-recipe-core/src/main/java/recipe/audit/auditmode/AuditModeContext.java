package recipe.audit.auditmode;

import com.google.common.collect.Maps;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * created by shiyuping on 2019/8/15
 */
@Service
public class AuditModeContext {
    private static Map<Integer,String> map = Maps.newHashMap();
    private IAuditMode auditMode;

    /*public IAuditMode getAuditMode(Integer auditMode){
        AuditModeEnum[] list = AuditModeEnum.values();
        String serviceName = null;
        for (AuditModeEnum e : list) {
            if (e.getAuditMode().equals(auditMode)) {
                serviceName = e.getServiceName();
                break;
            }
        }
        IAuditMode iAuditMode = null;
        if (StringUtils.isNotEmpty(serviceName)) {
            iAuditMode = AppContextHolder.getBean(serviceName, IAuditMode.class);
        }
        return iAuditMode;
    }*/

    public IAuditMode getAuditModes(Integer auditMode){
        String className = map.get(auditMode);
        Object o;
        try {
            o = Class.forName(className).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
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
