package recipe;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
public class Server {
    @SuppressWarnings({"unused"})
    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("spring.xml");
    }
}
