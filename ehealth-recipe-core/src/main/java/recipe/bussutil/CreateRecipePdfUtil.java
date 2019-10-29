package recipe.bussutil;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.ngari.upload.service.IFileUploadService;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.exception.FileRegistryException;
import ctd.mvc.upload.exception.FileRepositoryException;
import recipe.ApplicationUtils;
import recipe.third.IFileDownloadService;

import java.io.*;
import java.net.URL;
import java.util.Date;

/**
 * created by shiyuping on 2019/10/18
 */
public class CreateRecipePdfUtil {

    public static String transPdfIdForRecipePdf(String pdfId) throws IOException, DocumentException, FileRegistryException, FileRepositoryException {

        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null){
            File file = new File(fileMetaRecord.getFileName());
            OutputStream output = new FileOutputStream(file);
            //获取图片url
            URL url = CreateRecipePdfUtil.class.getClassLoader().getResource("drug.png");
            //添加图片
            addImgForRecipePdf(input,output,url);
            //上传pdf文件
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes,fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
        }
        return fileId;

    }

    private static void addImgForRecipePdf(InputStream input, OutputStream output, URL url) throws IOException, DocumentException {
        /*BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H",BaseFont.NOT_EMBEDDED);*/
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);

        //将文字贴入pdf
        /*page.beginText();
        page.setFontAndSize(baseFont,12);
        Color coler = new Color(255, 0, 0);
        page.setColorFill(coler);
        page.setTextMatrix(100,500); //设置文字在页面中的坐标
        page.showText("添加文字信息");
        page.endText();*/

        //将图片贴入pdf
        Image image = Image.getInstance(url);
        //直接设定显示尺寸
        //image.scaleAbsolute();
        //显示的大小为原尺寸的50%
        //image.scalePercent(50);
        //参数r为弧度，如果旋转角度为30度，则参数r= Math.PI/6。
        //image.setRotation((float) (Math.PI/6));
        //设置图片在页面中的坐标
        image.setAbsolutePosition(250,500);
        page.addImage(image);

        stamper.close();
        reader.close();
        input.close();
    }

    private static byte[] File2byte(File tradeFile){
        byte[] buffer = null;
        try
        {
            FileInputStream fis = new FileInputStream(tradeFile);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1)
            {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return buffer;
    }
}
