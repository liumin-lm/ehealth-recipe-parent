package recipe.bussutil;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.ngari.upload.service.IFileUploadService;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.exception.FileRegistryException;
import ctd.mvc.upload.exception.FileRepositoryException;
import lombok.Cleanup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.third.IFileDownloadService;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.net.URL;
import java.util.Date;

/**
 * created by shiyuping on 2019/10/18
 */
public class CreateRecipePdfUtil {
    private static final Logger logger = LoggerFactory.getLogger(CreateRecipePdfUtil.class);

    public static String transPdfIdForRecipePdf(String pdfId) throws IOException, DocumentException, FileRegistryException, FileRepositoryException {

        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null) {
            File file = new File(fileMetaRecord.getFileName());
            OutputStream output = new FileOutputStream(file);
            //获取图片url
            URL url = CreateRecipePdfUtil.class.getClassLoader().getResource("drug.png");
            //添加图片
            addImgForRecipePdf(input, output, url);
            //上传pdf文件
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
        }
        return fileId;
    }
    /**
     * 处方签pdf添加收货人信息
     *
     * @param pdfId
     * @param receiver
     * @param recMobile
     * @param completeAddress
     * @param height
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    public static String generateReceiverInfoRecipePdf(String pdfId, String receiver, String recMobile, String completeAddress, Integer height) throws IOException, DocumentException {
        logger.info("generateReceiverInfoRecipePdf pdfId={}, receiver={} ,recMobile={} ,completeAddress={}", pdfId, receiver, recMobile, completeAddress);
        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null) {
            File file = new File(fileMetaRecord.getFileName());
            OutputStream output = new FileOutputStream(file);
            //添加接收人信息
            addReceiverInfoRecipePdf(input, output, receiver, recMobile, completeAddress, height);
            //上传pdf文件
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
        }
        return fileId;
    }

    /**
     * 处方签pdf添加收货人信息
     *
     * @param input
     * @param output
     * @param receiver
     * @param recMobile
     * @param completeAddress
     * @param height
     * @throws IOException
     * @throws DocumentException
     */
    private static void addReceiverInfoRecipePdf(InputStream input, OutputStream output, String receiver, String recMobile, String completeAddress, Integer height) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);
        //将文字贴入pdf
        BaseFont bf = BaseFont.createFont(ClassLoader.getSystemResource("recipe/font/simhei.ttf").toString(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
        page.beginText();
        page.setColorFill(BaseColor.BLACK);
        page.setFontAndSize(bf, 10);
        page.setTextMatrix(10, height);
        page.showText("收货人姓名：" + receiver);
        page.setTextMatrix(149, height);
        page.showText("收货人电话：" + recMobile);
        page.setTextMatrix(10, height - 12);
        page.showText("收货人地址：" + completeAddress);
        page.endText();
        stamper.close();
        reader.close();
        input.close();
        output.close();
    }


    private static void addImgForRecipePdf(InputStream input, OutputStream output, URL url) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);

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
        try {
            FileInputStream fis = new FileInputStream(tradeFile);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }catch (FileNotFoundException e){
            logger.error("File2byte e", e);
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return buffer;
    }


    //带坐标和比例的的图片放置
    private static void addBarCodeImgForRecipePdfByCoordinates(InputStream input, OutputStream output, URL url,
                                                               float newWidth, float newHeight, float xPoint, float yPoint) throws Exception{
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);
        //将图片贴入pdf
        Image image = Image.getInstance(url);
        //显示的大小为原尺寸的20%
        image.scaleAbsolute(newWidth, newHeight);
        //设置图片在页面中的坐标
        //image.setAbsolutePosition(285, 781);
        //date 20200909 修改位置居左
        image.setAbsolutePosition(xPoint, yPoint);
        page.addImage(image);
        stamper.close();
        reader.close();
        input.close();
    }

    /**
     * 在pdf加盖印章
     *
     * @param pdfId       药师签名文件id
     * @param organSealId 机构配置印章id
     * @param type        处方类型
     * @return
     * @throws Exception
     */
    public static String generateSignetRecipePdf(String pdfId, String organSealId, Integer type) throws Exception {
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);

        //获取印章图片
        @Cleanup InputStream organSealInput = new ByteArrayInputStream(fileDownloadService.downloadAsByte(organSealId));
        FileMetaRecord organSealRecord = fileDownloadService.downloadAsRecord(organSealId);

        if (null == organSealRecord) {
            return null;
        }
        //获取图片url
        File organSealFile = new File(organSealRecord.getFileName());
        @Cleanup OutputStream organSealOutput = new FileOutputStream(organSealFile);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = organSealInput.read(buffer)) != -1) {
            organSealOutput.write(buffer, 0, bytesRead);
        }

        URL url = organSealFile.toURI().toURL();
        String fileId = null;

        //获取 处方pdf 加印章
        @Cleanup InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        logger.info("generateSignetRecipePdf pdfId={}, organSealId={}", pdfId, organSealId);
        if (null != fileMetaRecord) {
            File file = new File(fileMetaRecord.getFileName());
            //添加图片
            @Cleanup OutputStream output = new FileOutputStream(file);
            addSignetImgForRecipePdf(output, input, url, type);
            //上传pdf文件
            IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
        }
        organSealFile.delete();
        return fileId;
    }

    /**
     * 在pdf追加图片
     *
     * @param output 处方pdf 文件
     * @param url  印章图片
     * @param type 处方类型
     * @throws Exception
     */
    private static void addSignetImgForRecipePdf(OutputStream output, InputStream input, URL url, Integer type) throws Exception {
        logger.info("addSignetImgForRecipePdf url={}, type={}", url, type);
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);

        PdfContentByte page = stamper.getOverContent(1);
        //将图片贴入pdf
        Image image = Image.getInstance(url);
        //直接设定显示尺寸
        image.scaleAbsolute(90, 90);
        image.setAbsolutePosition(250, 740);
        page.addImage(image);

        stamper.close();
        reader.close();
    }

    /**
     * 在处方pdf上手动挂上药师姓名
     *
     * @param pdfId   pdfId
     * @param checker 药师id姓名
     */
    public static String generateDocSignImageInRecipePdf(String pdfId, String checker) throws IOException, DocumentException {
        logger.info("generateDocSignImageInRecipePdf pdfId={}, checker={}", pdfId, checker);
        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        if (fileMetaRecord != null) {
            File file = new File(fileMetaRecord.getFileName());
            @Cleanup OutputStream output = new FileOutputStream(file);
            @Cleanup InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
            PdfReader reader = new PdfReader(input);
            PdfStamper stamper = new PdfStamper(reader, output);
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
            PdfContentByte page = stamper.getOverContent(1);
            page.beginText();
            page.setColorFill(BaseColor.BLACK);
            page.setFontAndSize(bf, 10);
            page.setTextMatrix(199f, 82f);
            page.showText(checker);
            page.endText();
            stamper.close();
            reader.close();
            //上传pdf文件
            byte[] bytes = File2byte(file);
            String fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
            return fileId;
        }
        return null;
    }


    /**
     * 在处方pdf上手动挂上医生药师图片
     *
     * @param pdfBase64String   pdf
     * @param doctorSignImageId 医生的图片id
     */
    public static String generateDocSignImageInRecipePdf(Integer recipeId, Integer doctorId, Boolean isDoctor, Boolean isTcm,
                                                         String pdfBase64String, String doctorSignImageId) throws Exception {
        logger.info("generateDocSignImageInRecipePdf recipeId={}, doctorId={}", recipeId, doctorId);
        float xPoint;
        float yPoint;
        if (isDoctor) {
            xPoint = 55f;
            yPoint = 76f;
        } else {
            xPoint = 190f;
            yPoint = 76f;
        }

        String fileId = null;
        OutputStream output = null;
        InputStream input = null;
        try {
            //首先将产生的base64位的处方pdf生成读出流
            BASE64Decoder d = new BASE64Decoder();
            byte[] data = new byte[0];
            try {
                data = d.decodeBuffer(pdfBase64String);
            } catch (IOException e) {
                e.printStackTrace();
            }
            input = new ByteArrayInputStream(data);

            IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
            IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
            byte[] doctorSignImageByte = fileDownloadService.downloadAsByte(doctorSignImageId);
            File docSignImage = new File(doctorId + new Date().toString() + ".png");
            getFileByBytes(doctorSignImageByte, docSignImage);
            fileId = null;
            if (doctorSignImageByte != null){
                File file = new File(recipeId + new Date().toString() + ".pdf");
                output = new FileOutputStream(file);
                //获取图片url
                URL url = docSignImage.toURI().toURL();
                //添加图片
                addBarCodeImgForRecipePdfByCoordinates(input,output,url, 50f, 20f, xPoint, yPoint);
                //上传pdf文件
                byte[] bytes = File2byte(file);
                fileId = fileUploadService.uploadFileWithoutUrt(bytes, file.getName());
                //删除本地文件
                file.delete();
            }
            docSignImage.delete();
        } catch (Exception e) {
            logger.warn("当前处方{}pdf添加用户图片异常{}", e);
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileId;

    }


    public static void getFileByBytes(byte[] bytes, File file) {
        BufferedOutputStream bos = null;

        FileOutputStream fos = null;
        try {
            //输出流
            fos = new FileOutputStream(file);
            //缓冲流
            bos = new BufferedOutputStream(fos);
            //将字节数组写出
            bos.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 所有ca模式医生签名完成后添加水印
     * @param pdfId
     * @param waterPrintText
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    public static String generateWaterPrintRecipePdf(String pdfId, String waterPrintText) throws IOException, DocumentException {
        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null) {
            //因为导入包不同，放在此类调用一直报错，所以addWaterPrintForRecipePdf放在新建工具类
            byte[] bytes = CreateRecipePdfUtilByLowagie.addWaterPrintForRecipePdf(fileDownloadService.downloadAsByte(pdfId), waterPrintText);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
        }
        logger.info("generateWaterPrintRecipePdf newFileId:{}",fileId);
        return fileId;
    }
}
