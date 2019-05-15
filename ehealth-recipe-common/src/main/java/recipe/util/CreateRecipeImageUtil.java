package recipe.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * created by shiyuping on 2019/3/1
 */
public class CreateRecipeImageUtil {

    /*public static byte[] createImg(Map<String, Object> paramMap) throws Exception{
        *//*DesignReport   designReport = DIYRecipeImgCreator.createWmTpl();
        JasperReport jasperReport = DIYReportCreator.getJasperReport(designReport);
        JasperPrint print = DIYReportCreator.createJasperPrint(jasperReport, paramMap);
        JRGraphics2DExporter exporter = new JRGraphics2DExporter();//创建graphics输出器
        //创建一个影像对象
        BufferedImage bufferedImage = new BufferedImage(print.getPageWidth() * 4, print.getPageHeight() * 4, BufferedImage.TYPE_INT_RGB);
        //取graphics
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        //设置相应参数信息
        exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, g);
        exporter.setParameter(JRGraphics2DExporterParameter.ZOOM_RATIO, Float.valueOf(4));
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.exportReport();
        g.dispose();//释放资源信息
        //这里的bufferedImage就是最终的影像图像信息,可以通过这个对象导入到cm中了.
       *//**//* ImageIO.write(bufferedImage, "JPEG", new File("E:/pdf/syp3.jpg"));*//**//*
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage,"JPEG",stream);
        byte[] bytes = stream.toByteArray();
        return bytes;*//*

    }*/

    private static byte[] createImageFile(String fileLocation, BufferedImage image) throws Exception {
            /*File file = new File(fileLocation);*/
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image,"JPEG",stream);
            byte[] bytes = stream.toByteArray();
            return bytes;
    }

    public static byte[] generationRecipeImg(String path, Map<String, Object> paramMap) throws Exception {
        int imageWidth = 793;// 图片的宽度

        int imageHeight = 1000;// 图片的高度

        BufferedImage image = new BufferedImage(imageWidth, imageHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, imageWidth, imageHeight);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("黑体", Font.BOLD, 20));
        graphics.drawString(trans(paramMap,"organName"),170,50);
        graphics.drawString(trans(paramMap,"title"),200,70);
        graphics.setFont(new Font("黑体", Font.BOLD, 25));
        graphics.drawString(trans(paramMap,"label"),600,70);
        graphics.setFont(new Font("黑体", Font.PLAIN, 14));
        graphics.drawLine(570,40,720,40);
        graphics.drawLine(570,80,720,80);
        graphics.drawLine(570,40,570,80);
        graphics.drawLine(720,40,720,80);
        graphics.drawString("病历号:",40,120);
        graphics.drawString(trans(paramMap,"patientId"),90,120);
        graphics.drawString("处方号:",350,120);
        graphics.drawString(trans(paramMap,"recipeCode"),400,120);
        graphics.drawLine(20,130,773,130);
        //第二部分
        graphics.drawString("姓名:",40,155);
        graphics.drawString(trans(paramMap,"pName"),80,155);
        graphics.drawString("性别:",240,155);
        graphics.drawString(trans(paramMap,"pGender"),280,155);
        graphics.drawString("年龄:",340,155);
        graphics.drawString(trans(paramMap,"pAge"),380,155);
        graphics.drawString("费别:",490,155);
        graphics.drawString(trans(paramMap,"pType"),540,155);

        graphics.drawString("科别:",40,180);
        graphics.drawString(trans(paramMap,"departInfo"),80,180);
        graphics.drawString("开方日期:",340,180);
        graphics.drawString(trans(paramMap,"cDate"),420,180);
        graphics.drawString("电话:",550,180);
        graphics.drawString(trans(paramMap,"mobile"),590,180);

        graphics.drawString("诊断:",40,205);
        graphics.drawString(trans(paramMap,"disease"),80,205);

        graphics.drawString("备注:",40,245);
        graphics.drawString(trans(paramMap,"diseaseMemo"),80,245);
        graphics.drawLine(20,270,773,270);
        //第三部分
        graphics.setFont(new Font("黑体", Font.BOLD, 20));
        graphics.drawString("Rp:",40,300);
        graphics.setFont(new Font("黑体", Font.PLAIN, 14));
        for (int i=0;i<5;i++){
            if (paramMap.get("drugInfo"+i)==null){
                continue;
            }
            graphics.drawString(trans(paramMap,"drugInfo"+i), 70, 340+80*i);
            graphics.drawString(trans(paramMap,"dTotal"+i), 600, 340+80*i);
            graphics.drawString(trans(paramMap,"useInfo"+i), 70, 360+80*i);
            graphics.drawString(trans(paramMap,"dMemo"+i), 70, 380+80*i);
        }
        //第四部分
        graphics.drawString("开方医生:",450,900);
        graphics.drawString(trans(paramMap,"doctor"),530,900);
        graphics.drawString("药师:",610,900);

        return createImageFile(path, image);

    }

    public static String trans(Map<String, Object> paramMap,String key){
        Object o = paramMap.get(key);
        if (o==null){
            return "";
        }else {
            return (String)o;
        }


    }

    public static byte[] graphicsGeneration(String path, Map<String, Object> paramMap) throws Exception {

        int imageWidth = 1000;// 图片的宽度

        int imageHeight = 1414;// 图片的高度

        BufferedImage image = new BufferedImage(imageWidth, imageHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, imageWidth, imageHeight);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("宋体", Font.BOLD, 30));
        graphics.drawString("阿里健康网络医院处方笺",350,75);
        graphics.setFont(new Font("宋体", Font.PLAIN, 20));
        //第一排
        graphics.drawString("费别:", 20, 170);
        graphics.drawString("□公费", 100, 150);
        graphics.drawString("□自费", 165, 150);
        graphics.drawString("□医保", 100, 170);
        graphics.drawString("□其他", 165, 170);
        //打钩、默认自费
        graphics.drawLine(169,142,174,148);
        graphics.drawLine(174,148,179,138);
        graphics.drawString("医疗证/医保卡号:", 300, 170);
        graphics.drawString("处方编号: " +paramMap.get("recipeCode"), 700, 170);
        graphics.drawLine(10,175,990,175);
        //第二排
        graphics.drawString("姓名:", 20, 220);
        graphics.drawString((String)paramMap.get("pName"), 100, 220);
        graphics.drawLine(100,223,400,223);
        graphics.drawString("性别:", 500, 220);
        graphics.drawString("□男", 566, 220);
        //打钩
        if ("男".equals(paramMap.get("pGender"))){
            graphics.drawLine(570,212,575,218);
            graphics.drawLine(575,218,580,208);
        }else if ("女".equals(paramMap.get("pGender"))){
            graphics.drawLine(570,212,575,218);
            graphics.drawLine(575,218,580,208);
        }
        graphics.drawLine(570,212,575,218);
        graphics.drawLine(575,218,580,208);
        graphics.drawString("□女", 630, 220);
        graphics.drawString("年龄:", 800, 220);
        graphics.drawString( paramMap.get("pAge")+"岁", 850, 220);
        graphics.drawLine(850,223,990,223);
        //第三排
        graphics.drawString("病历号:", 20, 270);
        graphics.drawString((String)paramMap.get("patientId"), 100, 270);
        graphics.drawLine(100,273,400,273);
        graphics.drawString("科别:", 500, 270);
        graphics.drawString((String)paramMap.get("departInfo"), 550, 270);
        graphics.drawLine(550,273,990,273);
        //第四排
        graphics.drawString("诊断:", 20, 320);
        graphics.drawString((String)paramMap.get("disease"), 100, 320);
        graphics.drawLine(100,323,400,323);
        graphics.drawString("开具日期:", 500, 320);
        graphics.drawString((String)paramMap.get("cDate"), 600, 320);
        graphics.drawLine(600,323,990,323);
        /*graphics.drawString("2019", 600, 320);
        graphics.drawLine(600,323,650,323);
        graphics.drawString("年", 650, 320);
        graphics.drawString("01", 675, 320);
        graphics.drawLine(675,323,700,323);
        graphics.drawString("月", 700, 320);
        graphics.drawString("12", 720, 320);
        graphics.drawLine(720,323,750,323);
        graphics.drawString("日", 750, 320);*/
        //第四排
        graphics.drawString("电话:", 20, 370);
        graphics.drawString((String)paramMap.get("mobile"),100,370);
        graphics.drawLine(100,373,400,373);
        graphics.drawLine(10,420,990,420);
        //第五排
        graphics.setFont(new Font("宋体", Font.BOLD, 30));
        graphics.drawString("Rp:", 20, 460);
        graphics.setFont(new Font("宋体", Font.PLAIN, 20));
        for (int i=0;i<5;i++){
            if (paramMap.get("drugInfo"+i)==null){
                continue;
            }
            graphics.drawString((String) paramMap.get("drugInfo"+i), 70, 510+120*i);
            graphics.drawString((String) paramMap.get("dTotal"+i), 800, 510+120*i);
            graphics.drawString((String) paramMap.get("useInfo"+i), 70, 540+120*i);
            graphics.drawString((String) paramMap.get("dMemo"+i), 70, 570+120*i);
        }


        //最后尾部
        graphics.drawLine(10,1200,990,1200);
        graphics.drawString("医 师:", 20, 1250);
        graphics.drawString((String) paramMap.get("doctor"), 100, 1250);
        graphics.drawLine(100,1250,280,1250);
        graphics.drawString("药品金额:", 300, 1250);
        graphics.drawLine(400,1250,600,1250);
        graphics.drawString("审核药师:", 20, 1300);
        graphics.drawLine(120,1300,280,1300);
        graphics.drawString("调配药师/士:", 300, 1300);
        graphics.drawLine(420,1300,600,1300);
        graphics.drawString("核对、发药药师:", 650, 1300);
        graphics.drawLine(800,1300,990,1300);
        graphics.drawLine(10,1350,990,1350);

        return createImageFile(path, image);
    }
}
