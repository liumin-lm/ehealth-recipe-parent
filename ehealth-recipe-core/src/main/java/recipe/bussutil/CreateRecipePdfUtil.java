package recipe.bussutil;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.upload.service.IFileUploadService;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.exception.FileRegistryException;
import ctd.mvc.upload.exception.FileRepositoryException;
import ctd.persistence.DAOFactory;
import lombok.Cleanup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.third.IFileDownloadService;
import sun.misc.BASE64Decoder;

import javax.persistence.criteria.CriteriaBuilder;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.Date;
import java.util.List;

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

    public static String generateTotalRecipePdf(String pdfId, String total, Integer type, Integer recipeId) throws IOException, DocumentException {
        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null) {
            File file = new File(fileMetaRecord.getFileName());
            OutputStream output = new FileOutputStream(file);
            //添加价格
            addTextForRecipePdf(input, output, total, type,recipeId);
            //上传pdf文件
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes, fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
        }
        return fileId;
    }


    /**
     * pdf写入 药品价格
     *
     * @param input
     * @param output
     * @param total
     * @throws IOException
     * @throws DocumentException
     */
    private static void addTextForRecipePdf(InputStream input, OutputStream output, String total, Integer type, Integer recipeId ) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);
        //将文字贴入pdf
        BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        //page.beginText();
        //page.setColorFill(BaseColor.BLACK);

//        Case(配送方式)
//        配送到家(药店配送)：读取pdf单个药品合计金额跟recipeDetail表drugCost比较，不等则替换，或不比较直接替换。
//        药店取药：读取pdf单个药品合计金额跟SgList.aleDruprice*药品数量比较，不等则替换，或不比较直接替换。

        try {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.get(recipeId);
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            Integer sendType = null;//配送主体类型 1医院配送 2 药企配送
            Integer giveMode = null;
            Integer settlementMode = null;
            List<Recipedetail> recipeDetails = null;
            if (recipe != null && StringUtil.isNotBlank(recipe.getOrderCode())) {
                IConfigurationCenterUtilsService configService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                Object canShowDrugCost = configService.getConfiguration(recipe.getClinicOrgan(), "canShowDrugCost");
                //配置单个药品金额总额在pdf显示
                if ((boolean) canShowDrugCost) {
                    giveMode = recipe.getGiveMode();
                    recipeDetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                    RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                    if (recipeOrder != null && null != recipeOrder.getSendType()) {
                        sendType = recipeOrder.getSendType();
                    }
                    Integer enterpriseId = recipeOrder.getEnterpriseId();
                    if(null != enterpriseId){
                        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(enterpriseId);
                        if(null != enterprise){
                            settlementMode = enterprise.getSettlementMode();
                        }
                    }
                    //【recipeDetail表的salePrice和actualSalePrice比较】，订单表的处方总费用和处方表的处方总费用比较？？，不相同则替换
                    //药企配送或药店取药
                    if ((new Integer("1").equals(giveMode) && new Integer("0").equals(settlementMode)) //药企配送
                            || new Integer("3").equals(giveMode)) { //药店取药
                        //更新单个药品金额总额
                        if (CollectionUtils.isNotEmpty(recipeDetails)) {
                            //中药
                            if (RecipeBussConstant.RECIPETYPE_TCM.equals(type)) {
                                //int i = 0;
                                String[] drugInfoArgs = new String[recipeDetails.size()];
//                                for (Recipedetail recipeDetail : recipeDetails) {
//                                    //recipeDetail.getDrugName();
//                                    recipeDetail.getActualSalePrice();
////                                    String dName = (i + 1) + "、" + recipeDetail.getDrugName();
////                                    //规格+药品单位
////                                    String dSpec = recipeDetail.getDrugSpec() + "/" + recipeDetail.getDrugUnit();
////                                    drugInfoArgs[i] = dName + dSpec;
//                                    //i++;
//                                }

                                int drugOneLine = 0;
                                int drugGroup = 4;
                                int startDrugLineNum = 1;

                                for (int i = 1; i <= recipeDetails.size(); i++) {
                                    drugOneLine++;
                                    page.saveState();
                                    page.setColorFill(BaseColor.WHITE);
                                    page.rectangle(138 + (i - 1) % drugGroup * 138, 518 - 20 * (startDrugLineNum - 1), 30, 20);
                                    page.fill();
                                    page.restoreState();

                                    if (0 == i % drugGroup) {
                                        startDrugLineNum++;
                                        drugOneLine = 0;
                                    }
                                }
                                page.beginText();
                                page.setColorFill(BaseColor.BLACK);
                                page.setFontAndSize(bf, 10);
                                startDrugLineNum = 1;
                                for (int i = 1; i <= recipeDetails.size(); i++) {
                                    drugOneLine++;
                                    page.setTextMatrix(138 + (i - 1) % drugGroup * 138, 522 - 20 * (startDrugLineNum - 1));
                                    page.showText(recipeDetails.get(i).getActualSalePrice().multiply(new BigDecimal(recipeDetails.get(i).getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP)+"");
                                    if (0 == i % drugGroup) {
                                        startDrugLineNum++;
                                        drugOneLine = 0;
                                    }
                                }
                                page.endText();
                                addTotalFee(page, type, bf, total);
                            } else {
                                //西药
                                for(int i=1;i<=recipeDetails.size();i++){
                                    page.saveState();
                                    page.setColorFill(BaseColor.WHITE);
                                    page.rectangle(420, 210 + 50 * (i - 1)+10, 100, 16);
                                    page.fill();
                                    page.restoreState();
                                }

                                page.beginText();
                                page.setColorFill(BaseColor.BLACK);
                                page.setFontAndSize(bf, 8);
                                page.setTextMatrix(400, 30);//TODO liu 中药位置待测试
                                for(int i=1;i<=recipeDetails.size();i++){
                                    page.setTextMatrix(420, 210 + 50 * (i - 1)+10);//TODO liu 中药位置待测试
                                    page.showText(recipeDetails.get(i).getActualSalePrice().multiply(new BigDecimal(recipeDetails.get(i).getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP)+"");
                                }
                                page.endText();
                                addTotalFee(page, type, bf, total);
                            }

                        }
                    }


                }


            }
        }catch (Exception e){
            logger.info("addTextForRecipePdf :{}",e);
        }finally {

        }

        //没有给预研修改原有pdf上某个位置数据的时间，暂时先重新生成好了

        stamper.close();
        reader.close();
        input.close();
        output.close();
    }

    public static void addTotalFee(PdfContentByte page, Integer type, BaseFont bf, String total){
        //添加覆盖
        page.saveState();
        page.setColorFill(BaseColor.WHITE);
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(type)) {
            //设中药文字在页面中的坐标 date20200910
            page.rectangle(410, 135, 60, 10);
        } else {
            //设置西药文字在页面中的坐标
            page.rectangle(420, 30, 60, 10);
        }
        page.fill();
        page.restoreState();

        //添加文本块
        page.beginText();
        page.setColorFill(BaseColor.BLACK);
        page.setFontAndSize(bf, 10);
        page.showText("药品金额 ：" + total);
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(type)) {
            //设中药文字在页面中的坐标 date20200910
            page.setTextMatrix(410, 135);
        } else {
            //设置西药文字在页面中的坐标
            page.setTextMatrix(420, 30);
        }
        page.endText();
    }

    public static void main(String[] args) throws IOException, DocumentException {
        PdfReader reader = null;
            reader = new PdfReader((new FileInputStream(new File("/Volumes/d/data/response.pdf"))));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        File file = new File("/Volumes/d/data/1.pdf");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        PdfStamper stamper = new PdfStamper(reader, fileOutputStream);

        PdfContentByte page = stamper.getOverContent(1);
        BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        page.beginText();
        page.setColorFill(BaseColor.BLACK);
        page.setFontAndSize(bf, 10);
        page.setTextMatrix(410, 135);//TODO liu 中药位置待测试
        page.showText("药品金额2222 ：" + 123);

        page.endText();


//        if (baos.toByteArray().length > 0) {
//            fileOutputStream.write(baos.toByteArray(), 0, baos.toByteArray().length);
//            fileOutputStream.flush();
//        }
        fileOutputStream.close();
        //page.moveText();
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

    /**
     * 为处方pdf文件生成条形码
     * @param pdfId
     * @param code
     */
    public static String generateBarCodeInRecipePdf(String pdfId,String code) throws Exception{
        IFileUploadService fileUploadService = ApplicationUtils.getBaseService(IFileUploadService.class);
        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
        InputStream input = new ByteArrayInputStream(fileDownloadService.downloadAsByte(pdfId));
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(pdfId);
        String fileId = null;
        if (fileMetaRecord != null){
            File file = new File(fileMetaRecord.getFileName());
            OutputStream output = new FileOutputStream(file);
            File barCodeFile = BarCodeUtil.generateFile(code, "barcode.png");
            //获取图片url
            URL url = barCodeFile.toURI().toURL();
            //添加图片
            addBarCodeImgForRecipePdf(input,output,url);
            //上传pdf文件
            byte[] bytes = File2byte(file);
            fileId = fileUploadService.uploadFileWithoutUrt(bytes,fileMetaRecord.getFileName());
            //删除本地文件
            file.delete();
            barCodeFile.delete();
        }
        return fileId;

    }

    private static void addBarCodeImgForRecipePdf(InputStream input, OutputStream output, URL url) throws Exception{
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        PdfContentByte page = stamper.getOverContent(1);
        //将图片贴入pdf
        Image image = Image.getInstance(url);
        //显示的大小为原尺寸的20%
        image.scalePercent(50);
        //设置图片在页面中的坐标
        //image.setAbsolutePosition(285, 781);
        //date 20200909 修改位置居左
        image.setAbsolutePosition(20, 781);
        page.addImage(image);
        stamper.close();
        reader.close();
        input.close();
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
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(type)) {
            //中药
            //修改了印章的坐标点 date 20200909 居中
            image.setAbsolutePosition(250, 740);
        } else {
            //西药
            //修改了印章的坐标点 date 20200909 居中
            image.setAbsolutePosition(250, 740);
        }
        page.addImage(image);

        stamper.close();
        reader.close();
    }

    /**
     * 在处方pdf上手动挂上医生药师图片
     * @param pdfBase64String pdf
     * @param doctorSignImageId 医生的图片id
     */
    public static String generateDocSignImageInRecipePdf(Integer recipeId, Integer doctorId, Boolean isDoctor, Boolean isTcm,
                                                         String pdfBase64String, String doctorSignImageId) throws Exception{
        float xPoint = 0f;
        float yPoint = 0f;
        if(isDoctor){
            if(isTcm){
                //医生-中药
                xPoint = 95f;
                yPoint = 100f;
            }else{
                //医生-西药
                xPoint = 290f;
                yPoint = 80f;
            }
        }else{
            if(isTcm){
                //药师-中药
                xPoint = 280f;
                yPoint = 100f;
            }else{
                //药师-西药
                xPoint = 470f;
                yPoint = 80f;
            }
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

}
