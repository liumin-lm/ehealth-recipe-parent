package recipe.bussutil;


import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import ctd.net.rpc.async.exception.AsyncTaskException;

import javax.swing.*;
import java.awt.*;
import java.io.*;


/**
 * created by liumin on 2020/10/28
 */
public class CreateRecipePdfUtilByLowagie {

    public static void main(String[] args) {
        addWaterPrintForRecipePdf(new byte[1],"sss");
    }
    static byte[] addWaterPrintForRecipePdf(byte[] data, String waterText) {
        try {
            //读取已有的pdf，在已有的pdf上添加图片或者是添加水印的
            //String waterText="重庆医科大学附属医院";
            //int degree=10;
            //PdfReader pdfReader = new PdfReader("/Volumes/d/data/chufangwmold.pdf");
            PdfReader pdfReader = new PdfReader(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper pdfStamper = new PdfStamper(pdfReader, baos);
            BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H",BaseFont.NOT_EMBEDDED);
            Rectangle pageRect=null;
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.2f);
            gs.setStrokeOpacity(0.4f);
            int total = pdfReader.getNumberOfPages() + 1;//// 原pdf文件的总页数

            JLabel label = new JLabel();
            FontMetrics metrics;
            int textH = 0;
            int textW = 0;
            //label.setText("一二三四五六七八九");//固定label宽高
            //logger.info(waterText.length());
            if(waterText.length()<9){
                label.setText("一二三四五六七八九");
            }else{
                label.setText("一二三四五六七八九四五");
                //label.setText(waterText);//固定label宽高
            }


            metrics = label.getFontMetrics(label.getFont());//// 文字水印 起始位置
            textH = metrics.getHeight();
            textW = metrics.stringWidth(label.getText());
            int interval = -textH/4;
            PdfContentByte under2;
            for (int i = 1; i < total; i++) {//循环页
                pageRect = pdfReader.getPageSizeWithRotation(i);
                under2 = pdfStamper.getOverContent(i);// 水印在之前文本上
                //under = pdfStamper.getUnderContent(i);
                under2.saveState();
                under2.setGState(gs);
                under2.beginText();
                under2.setFontAndSize(base, 10);// 文字水印 字体及字号
                under2.setColorFill(Color.GRAY);
                // 水印文字成任意度角倾斜
                //你可以随心所欲的改你自己想要的角度
                for (int height = interval + textH; height < pageRect.getHeight();
                     height = height + textH*10) {
                    for (int width = interval + textW; width < pageRect.getWidth() + textW;
                         width = width + textW*2) {
                        under2.showTextAligned(Element.ALIGN_LEFT
                                , waterText, width - textW,//右移+50
                                height - textH , -30);//上下移+直角三角形的高
                    }
                }
                // 添加水印文字
                under2.endText();
            }
            //一定不要忘记关闭流
            pdfReader.close();
            pdfStamper.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new AsyncTaskException(AsyncTaskException.EXECUTOR_NOT_FOUND, e.getMessage());
        }
    }

}
