package recipe.standalone;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author yuyun
 */
public class App {
    public static void main(String[] args){
        @SuppressWarnings("resource")
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("conf/spring.xml");

    }
}
