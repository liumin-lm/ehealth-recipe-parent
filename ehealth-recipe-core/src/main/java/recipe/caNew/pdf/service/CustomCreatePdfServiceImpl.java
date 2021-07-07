package recipe.caNew.pdf.service;

import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.exception.DAOException;
import lombok.Cleanup;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bean.RecipePdfDTO;
import recipe.bussutil.BarCodeUtil;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.SignImgNode;
import recipe.bussutil.WordToPdfBean;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.constant.ErrorCode;
import recipe.service.client.IConfigurationClient;
import recipe.service.client.PatientClient;
import recipe.service.manager.RecipeManager;
import recipe.service.manager.RedisManager;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

/**
 * 自定义创建pdf
 * 根据自定义工具画模版 方式生成的业务处理代码类
 *
 * @author fuzi
 */
@Service
public class CustomCreatePdfServiceImpl implements CreatePdfService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 需要记录坐标的的字段
     */
    private final List<String> ADDITIONAL_FIELDS = Arrays.asList("doctorSignImg,doctorSignImgToken", "recipe.check",
            "recipe.actualPrice", "recipe.patientID", "recipe.recipeCode", "address", "tcmDecoction", "recipe.giveUser",
            "barCode.recipeExtend.cardNo", "qrCode.recipeExtend.cardNo");
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private PatientClient patientClient;


    @Override
    public SignRecipePdfVO queryPdfOssId(Recipe recipe) {
        byte[] data = generateTemplatePdf(recipe);
        CoOrdinateVO ordinateVO = redisManager.getPdfCoords(recipe.getRecipeId(), "doctorSignImg,doctorSignImgToken");
        // ordinateVO.getX(), ordinateVO.getY(),
        // SignRecipePdfVO signRecipePdfVO = signRecipePDFV2(signRecipePdfVO.getData(), recipe.getDoctor(), "recipe_" + recipe.getRecipeId() + ".pdf",x,y);
        return null;
    }

    @Override
    public CaSealRequestTO queryPdfByte(Recipe recipe) {
        logger.info("CustomCreatePdfServiceImpl queryPdfByte recipe = {}", recipe.getRecipeId());
        byte[] data = generateTemplatePdf(recipe);
        String pdfBase64Str = new String(Base64.encode(data));
        CoOrdinateVO ordinateVO = redisManager.getPdfCoords(recipe.getRecipeId(), "doctorSignImg,doctorSignImgToken");
        return CreatePdfFactory.caSealRequestTO(ordinateVO.getX(), ordinateVO.getY(), recipe.getRecipeId().toString(), pdfBase64Str);

    }

    @Override
    public String updateDoctorNamePdf(Recipe recipe, SignImgNode signImgNode) {
        byte[] data = generateTemplatePdf(recipe);
        CoOrdinateVO ordinateVO = redisManager.getPdfCoords(recipe.getRecipeId(), "doctorSignImg,doctorSignImgToken");
        signImgNode.setSignFileData(data);
        signImgNode.setX(ordinateVO.getX().floatValue());
        signImgNode.setY(ordinateVO.getY().floatValue());
        return CreateRecipePdfUtil.generateSignImgNode(signImgNode);
    }

    @Override
    public CaSealRequestTO queryCheckPdfByte(Recipe recipe) {
        CoOrdinateVO ordinateVO = redisManager.getPdfCoords(recipe.getRecipeId(), "recipe.check");
        return CreatePdfFactory.caSealRequestTO(ordinateVO.getX(), ordinateVO.getY(), "check" + recipe.getRecipeId(), CreateRecipePdfUtil.signFileBase64(recipe.getSignFile()));
    }

    @Override
    public String updateCheckNamePdf(Recipe recipe) {
        return null;
    }

    @Override
    public void updateTotalPdf(Integer recipeId, BigDecimal recipeFee) {
    }

    @Override
    public String updateCodePdf(Recipe recipe) {
        return null;
    }

    @Override
    public void updateAddressPdf(Recipe recipe) {

    }

    @Override
    public Recipe updateGiveUser(Recipe recipe) {
        return null;
    }

    /**
     * 按照模版生成 pdf
     *
     * @param recipe 处方
     * @return pdf byte
     */
    private byte[] generateTemplatePdf(Recipe recipe) {
        try {
            //替换模版pdf的字段
            @Cleanup ByteArrayOutputStream output = new ByteArrayOutputStream();
            List<WordToPdfBean> wordToPdf = generateTemplatePdf(recipe, output);
            byte[] data = CreateRecipePdfUtil.generateTemplatePdf(recipe.getRecipeId(), output);
            if (null == data) {
                return null;
            }
            wordToPdf.forEach(a -> {
                if (null != a.getUri()) {
                    File file = new File(a.getUri());
                    file.delete();
                }
            });
            return data;
        } catch (Exception e) {
            logger.error("CustomCreatePdfServiceImpl generateTemplatePdf error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 读取模版
     *
     * @param recipe 处方
     * @param output 输出流
     * @return 需要记录坐标的的字段对象
     * @throws Exception
     */
    private List<WordToPdfBean> generateTemplatePdf(Recipe recipe, ByteArrayOutputStream output) throws Exception {
        //模版pdfId
        String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), "xxxxxxpdf", "");
        @Cleanup InputStream input = new ByteArrayInputStream(CreateRecipePdfUtil.signFileByte(organSealId));
        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, output);
        AcroFields form = stamper.getAcroFields();
        form.addSubstitutionFont(BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED));
        //记录坐标的的字段对象
        List<CoOrdinateVO> ordinateList = ordinateList(form);
        redisManager.coOrdinate(recipe.getRecipeId(), ordinateList);
        //模版填充 数据方法
        List<WordToPdfBean> generatePdfList = templatePdfList(recipe, form);
        //如果为false，生成的PDF文件可以编辑，如果为true，生成的PDF文件不可以编辑
        stamper.setFormFlattening(true);
        stamper.close();
        return generatePdfList;
    }

    /**
     * 记录坐标的的字段对象
     *
     * @param form 模版表单
     * @return 需要记录坐标的的字段对象
     */
    private List<CoOrdinateVO> ordinateList(AcroFields form) {
        List<CoOrdinateVO> ordinateList = new LinkedList<>();
        ADDITIONAL_FIELDS.forEach(a -> {
            //定位某个表单字段坐标
            List<AcroFields.FieldPosition> pos = form.getFieldPositions(a);
            if (CollectionUtils.isEmpty(pos)) {
                return;
            }
            Rectangle pRectangle = pos.get(0).position;
            CoOrdinateVO coOrdinateVO = new CoOrdinateVO();
            coOrdinateVO.setX((int) pRectangle.getLeft());
            coOrdinateVO.setY((int) pRectangle.getBottom());
            coOrdinateVO.setName(a);
            ordinateList.add(coOrdinateVO);
        });
        return ordinateList;
    }

    /**
     * 需要替换的模版字段对象
     *
     * @param recipe
     * @param form
     * @return
     */
    private List<WordToPdfBean> templatePdfList(Recipe recipe, AcroFields form) {
        Map<String, AcroFields.Item> map = form.getFields();
        if (map.isEmpty()) {
            logger.warn("CustomCreatePdfServiceImpl generatePdfList map null");
            return null;
        }
        //获取pdf值对象
        RecipePdfDTO recipePdfDTO = (RecipePdfDTO) recipeManager.getRecipeDTO(recipe.getRecipeId());
        PatientDTO patientBean = patientClient.getPatientDTO(recipe.getMpiid());
        recipePdfDTO.setPatientBean(patientBean);
        //获取模版填充字段
        List<WordToPdfBean> generatePdfList = generatePdfList(map.keySet(), recipePdfDTO);
        //替换的模版字段
        for (WordToPdfBean wordToPdf : generatePdfList) {
            try {
                String key = wordToPdf.getKey();
                String value = wordToPdf.getValue();
                if (null == wordToPdf.getUri()) {
                    //文字类的内容处理
                    form.setField(key, value);
                } else {
                    //将图片写入指定的field
                    Image image = Image.getInstance(wordToPdf.getUri().toURL());
                    PushbuttonField pb = form.getNewPushbuttonFromField(key);
                    pb.setImage(image);
                    form.replacePushbuttonField(key, pb.getField());
                }
            } catch (Exception e) {
                logger.error("CreateRecipePdfUtil templatePdfList error ", e);
            }
        }
        return generatePdfList;
    }

    /**
     * 获取模版填充字段
     *
     * @param keySet       模版表单字段
     * @param recipePdfDTO pdf值对象
     * @return 模版填充对象
     */
    private List<WordToPdfBean> generatePdfList(Set<String> keySet, RecipePdfDTO recipePdfDTO) {
        List<WordToPdfBean> generatePdfList = new LinkedList<>();
        for (String key : keySet) {
            //单一字段处理
            String[] keySplit = key.split(ByteUtils.DOT);
            //条形码 ，二维码 等特殊节点处理
            if (3 == keySplit.length) {
                String identifyName = keySplit[0];
                String objectName = keySplit[1];
                String fieldName = keySplit[2];
                if ("barCode".equals(identifyName)) {
                    WordToPdfBean wordToPdf = getFieldValueByName(key, objectName, fieldName, recipePdfDTO);
                    URI uri = BarCodeUtil.generateFileUrl(wordToPdf.getValue(), "barcode.png");
                    wordToPdf.setUri(uri);
                    generatePdfList.add(wordToPdf);
                }
                if ("qrCode".equals(identifyName)) {
                    generatePdfList.add(getFieldValueByName(key, objectName, fieldName, recipePdfDTO));
                }
                continue;
            }
            //对象字段处理
            if (2 == keySplit.length) {
                String objectName = keySplit[0];
                String fieldName = keySplit[1];
                generatePdfList.add(getFieldValueByName(key, objectName, fieldName, recipePdfDTO));
            }
        }
        return generatePdfList;
    }

    /**
     * 获取pdf值对象 所对应的value
     *
     * @param key          表单名
     * @param objectName   对象名
     * @param fieldName    字段名
     * @param recipePdfDTO pdf值对象
     * @return 对应的value
     */
    private WordToPdfBean getFieldValueByName(String key, String objectName, String fieldName, RecipePdfDTO recipePdfDTO) {
        if ("recipe".equals(objectName)) {
            String value = MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getRecipe());
            return new WordToPdfBean(key, value, null);
        }
        if ("patient".equals(objectName)) {
            String value = MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getPatientBean());
            return new WordToPdfBean(key, value, null);
        }
        if ("recipeExtend".equals(objectName)) {
            String value = MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getRecipeExtend());
            return new WordToPdfBean(key, value, null);
        }
        if ("recipeDetail".equals(objectName)) {
            String value = MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getRecipeDetails());
            return new WordToPdfBean(key, value, null);
        }
        return null;
    }


}
