package recipe.bussutil;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import org.apache.commons.lang.StringUtils;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.tools.UnitConv;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

/**
 * created by shiyuping on 2020/6/3
 * 条形码生成工具
 */
public class BarCodeUtil {
    /**
     * 生成文件
     *
     * @param msg
     * @param path
     * @return
     */
    public static File generateFile(String msg, String path) throws Exception{
        File file = new File(path);
        generate(msg, new FileOutputStream(file));
        return file;
    }

    /**
     * 生成字节
     *
     * @param msg
     * @return
     */
    public static byte[] generate(String msg) throws Exception{
        ByteArrayOutputStream ous = new ByteArrayOutputStream();
        generate(msg, ous);
        return ous.toByteArray();
    }

    /**
     * 生成到流
     *
     * @param msg
     * @param ous
     */
    public static void generate(String msg, OutputStream ous) throws Exception {
        if (StringUtils.isEmpty(msg) || ous == null) {
            return;
        }

        Code128Bean bean = new Code128Bean();

        // 精细度
        final int dpi = 150;
        // module宽度// 设置条码每一条的宽度 UnitConv 是barcode4j 提供的单位转换的实体类，用于毫米mm,像素px,英寸in,点pt之间的转换
        final double moduleWidth = UnitConv.in2mm(3.0f / dpi);

        // 配置对象
        bean.setModuleWidth(moduleWidth);
        // 设置两侧是否加空白
        bean.doQuietZone(false);
        //bean.setWideFactor(3);

        // 设置文本位置（包括是否显示）
        bean.setMsgPosition(HumanReadablePlacement.HRP_NONE);

        String format = "image/png";
        // 输出到流
        BitmapCanvasProvider canvas = new BitmapCanvasProvider(ous, format, dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);

        // 生成条形码
        bean.generateBarcode(canvas, msg);
        // 结束绘制
        canvas.finish();
        ous.close();
    }

    public static void main(String[] args) throws Exception{
        String msg = "9631457";
        String path = "barcode1.png";
        File file1 = generateFile(msg, path);
        File file = new File("D:/pdf/chufang10.pdf");
        OutputStream output = new FileOutputStream(file);
        //获取图片url
        URL url = file1.toURI().toURL();
        //添加图片
        PdfReader reader = new PdfReader(new FileInputStream(new File("D:/pdf/chufangwmold.pdf")));
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);
        //将图片贴入pdf
        Image image = Image.getInstance(url);
        //直接设定显示尺寸
        //image.scaleAbsolute();
        //显示的大小为原尺寸的50%
        image.scalePercent(50);
        //参数r为弧度，如果旋转角度为30度，则参数r= Math.PI/6。
        //image.setRotation((float) (Math.PI/6));
        //设置图片在页面中的坐标 条形码
        image.setAbsolutePosition(20, 781);
        page.addImage(image);

        //盖章
        image.scaleAbsolute(90, 90);
        image.setAbsolutePosition(250, 740);
        //image.setAbsolutePosition(140, 750);
        page.addImage(image);

        //医生签名
        image.scaleAbsolute(30, 30);
        image.setAbsolutePosition(105, 75);
        page.addImage(image);


        //药师签名
        image.scaleAbsolute(30, 30);
        image.setAbsolutePosition(350, 100);
        page.addImage(image);

//        //医生签名2
//        image.scaleAbsolute(50, 20);
//        image.setAbsolutePosition(95, 100);
//        page.addImage(image);
//        //药师签名2
//        image.scaleAbsolute(50, 20);
//        image.setAbsolutePosition(280, 100);
//        page.addImage(image);

        //医生签名3
        image.scaleAbsolute(50, 20);
        image.setAbsolutePosition(290, 80);
        page.addImage(image);
        //药师签名3
        image.scaleAbsolute(50, 20);
        image.setAbsolutePosition(470, 80);
        page.addImage(image);

        //将文字贴入pdf
        BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        page.beginText();
        page.setColorFill(BaseColor.BLACK);
        page.setFontAndSize(bf, 10);
        page.setTextMatrix(410, 135); //设置文字在页面中的坐标
        String s = "药品价格 ： " + " 34";
        page.showText(s);
        page.endText();

        stamper.close();
        reader.close();
    }
}
