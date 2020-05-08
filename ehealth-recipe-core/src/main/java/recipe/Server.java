package recipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("spring.xml");
        logger.info("Server start");
    }

    private static String getString(String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(args.length).append("\r\n");
        for (String arg : args) {
            sb.append("$").append(arg.length()).append("\r\n");
            sb.append(arg).append("\r\n");
        }
        return sb.toString();
    }

    public static void initFile2() {
        String file = "D:/360Downloads/rmo.txt";
        BufferedWriter w = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("D:/360Downloads/rmo_origin.txt"), "UTF-8"));
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
            String line = null;

            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                sb.setLength(0);
                sb.append(getString(s[0], s[1], s[2]));
                w.append(sb.toString());

                System.out.println("文件内容: " + s.length);
                System.out.println(sb.toString());
            }
//            for (int i=1;i<10000;i++) {
//                sb.setLength(0);
//                sb.append(getString("pfadd","test",i+""));
//                w.append(sb.toString());
//            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                w.flush();
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
