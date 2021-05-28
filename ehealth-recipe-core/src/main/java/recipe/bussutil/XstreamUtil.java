package recipe.bussutil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liumin
 * @date 2010/5/20
 * Xml转换实体工具类
 */
@Slf4j
@UtilityClass
public class XstreamUtil {
    /**
     * 将bean转换为xml
     * @param obj 转换的bean
     * @return bean转换为xml
     */
    public String objectToXml(Object obj) {
        //解决下划线问题
        XStream xStream = new XStream(new XppDriver(new XmlFriendlyNameCoder("_-", "_")));
        //去掉class 属性
        xStream.aliasSystemAttribute(null, "class");
        //xstream使用注解转换
        xStream.processAnnotations(obj.getClass());
        xStream.ignoreUnknownElements();
        return xStream.toXML(obj);
    }

    /**
     * 将xml转换为bean
     * @param <T> 泛型
     * @param xml 要转换为bean的xml
     * @param cls bean对应的Class
     * @return xml转换为bean
     */
    public <T> T xmlToObject(String xml, Class<T> cls){
        XStream xstream = new XStream(new DomDriver());
        //xstream使用注解转换
        xstream.processAnnotations(cls);
        //忽略多余的xml节点
        xstream.ignoreUnknownElements();
        try {
            return (T) xstream.fromXML(xml);
        } catch (Exception e) {
            log.error("xml parse error:{}",e);
            return null;
        }
    }

    public static void main(String[] args) {

    }
}
