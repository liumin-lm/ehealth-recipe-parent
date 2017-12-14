package recipe.standalone;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by sean on 15/6/20.
 */
public class App {
    public static void main(String[] args){
        @SuppressWarnings("resource")
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("conf/spring.xml");

    }
}
