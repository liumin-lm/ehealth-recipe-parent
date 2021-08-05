package recipe.bussutil;

import com.alibaba.fastjson.JSON;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.upload.service.IFileUploadService;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.FileService;
import ctd.net.rpc.async.exception.AsyncTaskException;
import ctd.util.JSONUtils;
import lombok.Cleanup;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import recipe.ApplicationUtils;
import recipe.third.IFileDownloadService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * created by shiyuping on 2019/10/18
 */
public class CreateRecipePdfUtil {
    private static final Logger logger = LoggerFactory.getLogger(CreateRecipePdfUtil.class);
    private static final IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
    private static final IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);


    /**
     * 通用 写入特殊文本 节点信息
     *
     * @param pdfId     原文件id
     * @param decoction 特殊节点写入
     */
    public static String generateCoOrdinatePdf(String pdfId, CoOrdinateVO decoction) throws Exception {
        return generateOrdinateList(pdfId, Collections.singletonList(decoction));
    }

    /**
     * 通用 写入特殊文本 多节点信息
     *
     * @param pdfId          原文件id
     * @param coOrdinateList 多节点信息
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    public static String generateOrdinateList(String pdfId, List<CoOrdinateVO> coOrdinateList) throws Exception {
        logger.info("CreateRecipePdfUtil generateOrdinateList pdfId={}, coOrdinateList={} ", pdfId, coOrdinateList);
        if (StringUtils.isEmpty(pdfId) || CollectionUtils.isEmpty(coOrdinateList)) {
            return null;
        }
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        File file = new File(fileMetaRecord.getFileName());
        @Cleanup OutputStream output = new FileOutputStream(file);
        @Cleanup InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        generateOrdinateList(coOrdinateList, stamper);
        stamper.close();
        reader.close();
        //上传pdf文件
        byte[] bytes = File2byte(file);
        String fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
        //删除本地文件
        file.delete();
        return fileId;
    }


    /**
     * 通用 根据 x，y坐标写入 图片放置
     *
     * @param signImgNode
     * @return
     */
    public static String generateSignImgNode(SignImgNode signImgNode) throws Exception {
        if (StringUtils.isAnyEmpty(signImgNode.getRecipeId(), signImgNode.getSignImgFileId())) {
            return null;
        }
        //获取图片
        byte[] doctorSignImageByte = fileDownloadService.downloadAsByte(signImgNode.getSignImgFileId());
        File giveUserImage = new File("recipe_" + signImgNode.getRecipeId() + ".png");
        getFileByBytes(doctorSignImageByte, giveUserImage);
        URL url = giveUserImage.toURI().toURL();
        //获取pdf
        byte[] signFileByte;
        if (StringUtils.isNotEmpty(signImgNode.getSignFileId())) {
            signFileByte = fileDownloadService.downloadAsByte(signImgNode.getSignFileId());
        } else {
            signFileByte = signImgNode.getSignFileData();
        }
        File signFilePdf = new File("recipe_" + signImgNode.getRecipeId() + ".pdf");
        @Cleanup InputStream input = new ByteArrayInputStream(signFileByte);
        @Cleanup OutputStream output = new FileOutputStream(signFilePdf);
        addImgByRecipePdf(input, output, url, signImgNode.getWidth(), signImgNode.getHeight(), signImgNode.getX(),
                signImgNode.getY(), signImgNode.getRepeatWrite());
        //上传pdf文件
        byte[] bytes = File2byte(signFilePdf);
        String fileId = fileUploadService.uploadFileWithoutUrt(bytes, signFilePdf.getName());
        //删除本地文件
        signFilePdf.delete();
        giveUserImage.delete();
        return fileId;
    }


    /**
     * 通用 写入条形码 与特殊文本多节点信息
     *
     * @param pdfId          原文件id
     * @param coOrdinateList 写入文本节点信息
     * @param barcode        条形码
     * @return 新文件id
     * @throws Exception
     */
    public static String generateOrdinateListAndBarcode(String pdfId, List<CoOrdinateVO> coOrdinateList, CoOrdinateVO barcode) throws Exception {
        logger.info("generateRecipeCodeAndPatientIdRecipePdf pdfId={}, coOrdinateList={} ", pdfId, coOrdinateList);
        if (null == barcode || StringUtils.isEmpty(barcode.getValue())) {
            return generateOrdinateList(pdfId, coOrdinateList);
        }
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        File file = new File(fileMetaRecord.getFileName());
        @Cleanup OutputStream output = new FileOutputStream(file);
        @Cleanup InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        File barCodeFile = BarCodeUtil.generateFile(barcode.getValue(), "barcode.png");
        //获取图片url
        URL url = barCodeFile.toURI().toURL();
        //添加图片
        PdfContentByte page = stamper.getOverContent(1);
        //将图片贴入pdf
        Image image = Image.getInstance(url);
        image.setAbsolutePosition(barcode.getX(), barcode.getY());
        image.scaleToFit(110, 20);
        page.addImage(image);
        //处方pdf添加处方号和患者病历号
        generateOrdinateList(coOrdinateList, stamper);
        barCodeFile.delete();
        stamper.close();
        reader.close();
        //上传pdf文件
        byte[] bytes = File2byte(file);
        String fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
        //删除本地文件
        file.delete();
        return fileId;
    }

    /**
     * 取药标签
     *
     * @param pdfId
     * @return
     * @throws Exception
     */
    public static String transPdfIdForRecipePdf(String pdfId) throws Exception {
        @Cleanup InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null) {
            File file = new File(fileMetaRecord.getFileName());
            @Cleanup OutputStream output = new FileOutputStream(file);
            //获取图片url
            URL url = CreateRecipePdfUtil.class.getClassLoader().getResource("drug.png");
            //添加图片
            addImgByRecipePdf(input, output, url, null, null, 250, 500, false);
            //上传pdf文件
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
        }
        return fileId;
    }

    /**
     * 上传 pdf签名图片
     *
     * @param recipeId 处方id
     * @param pdfId
     * @return
     * @throws Exception
     */
    public static String updatePdfToImg(Integer recipeId, String pdfId) throws Exception {
        //获取pdf
        byte[] doctorSignImageByte = fileDownloadService.downloadAsByte(pdfId);
        File giveUserImage = new File("recipe_" + recipeId + ".pdf");
        getFileByBytes(doctorSignImageByte, giveUserImage);
        //pdf转图片
        PDDocument pdDocument = PDDocument.load(doctorSignImageByte);
        PDFRenderer renderer = new PDFRenderer(pdDocument);
        BufferedImage image = renderer.renderImageWithDPI(0, 150);
        //获取图片文件id
        File imageFile = new File("recipe_" + recipeId + ".jpeg");
        ImageIO.write(image, "jpeg", imageFile);
        String fileId = uploadImage(imageFile, imageFile.getName());
        imageFile.delete();
        giveUserImage.delete();
        return fileId;
    }


    /**
     * 所有ca模式医生签名完成后添加水印
     *
     * @param pdfId
     * @param waterPrintText
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    public static String generateWaterPrintRecipePdf(String pdfId, String waterPrintText) throws IOException, DocumentException {
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        if (fileMetaRecord != null) {
            //因为导入包不同，放在此类调用一直报错，所以addWaterPrintForRecipePdf放在新建工具类
            byte[] bytes = addWaterPrintForRecipePdf(fileDownloadService.downloadAsByte(pdfId), waterPrintText);
            return signFileByte(bytes, fileMetaRecord.getFileName());
        }
        return null;
    }


    /**
     * 下载oss服务器上的签名文件
     *
     * @param signFile ossId
     * @return
     */
    public static String signFileBase64(String signFile) {
        byte[] signFileByte = signFileByte(signFile);
        if (null == signFileByte) {
            return "";
        }
        return new String(Base64.encode(signFileByte));
    }

    public static byte[] signFileByte(String signFile) {
        return fileDownloadService.downloadAsByte(signFile);
    }

    /**
     * 上传文件到oss服务器
     *
     * @param bytes    文件
     * @param fileName 文件名
     * @return
     */
    public static String signFileByte(byte[] bytes, String fileName) {
        String fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileName);
        if (null == fileId) {
            return "";
        }
        return fileId;
    }


    /**
     * 读取pdf模板 拷贝模版流 生成新pdf
     *
     * @param recipeId
     * @param bos
     * @return
     * @throws Exception
     */
    public static byte[] generateTemplatePdf(Integer recipeId, ByteArrayOutputStream bos) throws Exception {
        //拷贝模版生成新pdf
        File file = new File("recipe_" + recipeId + ".pdf");
        @Cleanup OutputStream output = new FileOutputStream(file);
        Document doc = new Document();
        PdfSmartCopy copy = new PdfSmartCopy(doc, output);
        doc.open();
        PdfImportedPage importPage = copy.getImportedPage(new PdfReader(bos.toByteArray()), 1);
        copy.addPage(importPage);
        doc.close();
        byte[] bytes = File2byte(file);
        file.delete();
        return bytes;
    }


    /**
     * 上传图片文件到oss服务器
     *
     * @param file     图片文件
     * @param fileName 文件名
     * @return
     */
    private static String uploadImage(File file, String fileName) throws Exception {
        FileMetaRecord meta = new FileMetaRecord();
        meta.setManageUnit("eh");
        meta.setLastModify(new Date());
        meta.setUploadTime(new Date());
        meta.setMode(0);
        meta.setCatalog("other-doc");
        meta.setContentType("image/jpeg");
        meta.setFileName(fileName);
        meta.setFileSize(file.length());
        logger.info("uploadPicture.meta=[{}]", JSONUtils.toString(meta));
        FileService.instance().upload(meta, file);
        return meta.getFileId();
    }


    /**
     * 修改处方单号和患者病历号
     *
     * @param stamper
     * @param coOrdinateList
     * @throws IOException
     * @throws DocumentException
     */
    private static void generateOrdinateList(List<CoOrdinateVO> coOrdinateList, PdfStamper stamper) throws Exception {
        if (!CollectionUtils.isEmpty(coOrdinateList)) {
            for (CoOrdinateVO cCoOrdinateVO : coOrdinateList) {
                addTextForPdf(stamper, cCoOrdinateVO);
            }
        }
    }


    private static byte[] File2byte(File tradeFile) {
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
        } catch (Exception e) {
            logger.error("File2byte e", e);
        }
        return buffer;
    }


    /**
     * 生成本地文件
     *
     * @param bytes
     * @param file
     */
    private static void getFileByBytes(byte[] bytes, File file) {
        try {
            //输出流
            @Cleanup FileOutputStream fos = new FileOutputStream(file);
            //缓冲流
            @Cleanup BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } catch (Exception e) {
            logger.info("getFileByBytes error", e);
        }
    }

    /**
     * 水印
     *
     * @param data
     * @param waterText
     * @return
     */
    private static byte[] addWaterPrintForRecipePdf(byte[] data, String waterText) {
        try {
            //读取已有的pdf，在已有的pdf上添加图片或者是添加水印的
            com.lowagie.text.pdf.PdfReader pdfReader = new com.lowagie.text.pdf.PdfReader(data);
            @Cleanup ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfStamper pdfStamper = new com.lowagie.text.pdf.PdfStamper(pdfReader, baos);
            com.lowagie.text.pdf.BaseFont base = com.lowagie.text.pdf.BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
            com.lowagie.text.pdf.PdfGState gs = new com.lowagie.text.pdf.PdfGState();
            gs.setFillOpacity(0.2f);
            gs.setStrokeOpacity(0.4f);
            //原pdf文件的总页数
            int total = pdfReader.getNumberOfPages() + 1;
            JLabel label = new JLabel();
            //固定label宽高
            label.setText(waterText);
            //文字水印 起始位置
            FontMetrics metrics = label.getFontMetrics(label.getFont());
            int textH = metrics.getHeight();
            int textW = metrics.stringWidth(label.getText());
            int interval = -textH / 3;
            //循环页
            for (int i = 1; i < total; i++) {
                com.lowagie.text.Rectangle pageRect = pdfReader.getPageSizeWithRotation(i);
                // 水印在之前文本上
                com.lowagie.text.pdf.PdfContentByte under2 = pdfStamper.getOverContent(i);
                under2.saveState();
                under2.setGState(gs);
                under2.beginText();
                // 文字水印 字体及字号
                under2.setFontAndSize(base, 10);
                under2.setColorFill(Color.GRAY);
                // 水印文字成任意度角倾斜
                //你可以随心所欲的改你自己想要的角度//上下移+直角三角形的高
                for (int height = interval + textH; height < pageRect.getHeight(); height = height + textH * 10) {
                    for (int width = interval + textW; width < pageRect.getWidth() + textW; width = width + textW * 2) {
                        under2.showTextAligned(com.lowagie.text.Element.ALIGN_LEFT, waterText, width - textW, height - textH, -30);
                    }
                }
                // 添加水印文字
                under2.endText();
            }
            pdfReader.close();
            pdfStamper.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new AsyncTaskException(AsyncTaskException.EXECUTOR_NOT_FOUND, e.getMessage());
        }
    }


    /**
     * 根据 x，y坐标放置 图片
     *
     * @param input     输入流
     * @param output    输出流
     * @param url       图片地址
     * @param newWidth  显示宽度
     * @param newHeight 显示高度
     * @param xPoint    定位坐标x
     * @param yPoint    定位坐标y
     * @throws Exception
     */
    private static void addImgByRecipePdf(InputStream input, OutputStream output, URL url
            , Float newWidth, Float newHeight, float xPoint, float yPoint, Boolean repeatWrite) throws Exception {
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);
        if (repeatWrite) {
            //添加空白覆盖
            page.saveState();
            page.setColorFill(BaseColor.WHITE);
            page.rectangle(xPoint, yPoint, newWidth, newHeight);
            page.fill();
            page.restoreState();
        }

        //将图片贴入pdf
        Image image = Image.getInstance(url);
        if (null != newWidth) {
            //显示的大小
            image.scaleAbsolute(newWidth, newHeight);
        }
        //设置图片在页面中的坐标
        image.setAbsolutePosition(xPoint, yPoint);
        page.addImage(image);
        stamper.close();
        reader.close();
    }

    /**
     * 根据 x，y坐标写入text文本内容
     *
     * @param stamper
     * @param decoction 坐标
     * @throws Exception
     */
    private static void addTextForPdf(PdfStamper stamper, CoOrdinateVO decoction) throws Exception {
        logger.info("addTextForRecipePdf text:{}", JSON.toJSONString(decoction));
        PdfContentByte page = stamper.getOverContent(1);
        BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        if (null != decoction.getRepeatWrite() && decoction.getRepeatWrite()) {
            //添加空白覆盖
            page.saveState();
            page.setColorFill(BaseColor.WHITE);
            page.rectangle(decoction.getX(), decoction.getY(), 120, 14);
            page.fill();
            page.restoreState();
        }
        //添加文本块
        page.beginText();
        page.setColorFill(BaseColor.BLACK);
        page.setFontAndSize(bf, 10);
        page.setTextMatrix(decoction.getX(), decoction.getY());
        page.showText(decoction.getValue());
        page.endText();
    }


}
