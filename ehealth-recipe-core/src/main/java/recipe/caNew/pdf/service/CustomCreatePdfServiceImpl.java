package recipe.caNew.pdf.service;

import com.alibaba.fastjson.JSON;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import lombok.Cleanup;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bean.RecipeInfoDTO;
import recipe.bussutil.*;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.comment.DictionaryUtil;
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
import java.util.stream.Collectors;

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
            "recipe.actualPrice", "recipe.patientID", "recipe.recipeCode", "address", "tcmDecoction", "recipe.giveUser");
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
            //模版pdfId
            String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), "xxxxxxpdf", "");
            @Cleanup InputStream input = new ByteArrayInputStream(CreateRecipePdfUtil.signFileByte(organSealId));
            PdfReader reader = new PdfReader(input);
            PdfStamper stamper = new PdfStamper(reader, output);
            AcroFields form = stamper.getAcroFields();
            form.addSubstitutionFont(BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED));
            //记录坐标的的字段对象
            redisManager.coOrdinate(recipe.getRecipeId(), ordinateList(form));
            //模版填充 数据方法
            List<WordToPdfBean> generatePdfList = templatePdfList(recipe, form);
            //如果为false，生成的PDF文件可以编辑，如果为true，生成的PDF文件不可以编辑
            stamper.setFormFlattening(true);
            stamper.close();
            //拷贝模版流 生成新pdf
            byte[] data = CreateRecipePdfUtil.generateTemplatePdf(recipe.getRecipeId(), output);
            //删除生成的图片
            generatePdfList.forEach(a -> {
                if (null != a.getUri()) {
                    new File(a.getUri()).delete();
                }
            });
            return data;
        } catch (Exception e) {
            logger.error("CustomCreatePdfServiceImpl generateTemplatePdf error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
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
        RecipeInfoDTO recipePdfDTO = (RecipeInfoDTO) recipeManager.getRecipeDTO(recipe.getRecipeId());
        PatientDTO patientBean = patientClient.getPatientDTO(recipe.getMpiid());
        recipePdfDTO.setPatientBean(patientBean);
        //获取模版填充字段
        List<WordToPdfBean> generatePdfList = generatePdfList(map.keySet(), recipePdfDTO);
        //替换的模版字段
        for (WordToPdfBean wordToPdf : generatePdfList) {
            try {
                String key = wordToPdf.getKey();
                if (null == wordToPdf.getUri()) {
                    //文字类的内容处理
                    form.setField(key, wordToPdf.getValue());
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
     * 获取模版填充字段,根据模版表单字段解析 反射获取值
     *
     * @param keySet       模版表单字段
     * @param recipePdfDTO pdf值对象
     * @return 模版填充对象
     */
    private List<WordToPdfBean> generatePdfList(Set<String> keySet, RecipeInfoDTO recipePdfDTO) {
        List<WordToPdfBean> generatePdfList = new LinkedList<>();
        Map<String, Object> recipeDetailMap;
        if (RecipeUtil.isTcmType(recipePdfDTO.getRecipe().getRecipeType())) {
            //中药
            recipeDetailMap = createChineMedicinePDF(recipePdfDTO);
        } else {
            //西药
            recipeDetailMap = createMedicinePDF(recipePdfDTO);
        }
        for (String key : keySet) {
            String[] keySplit = key.split(ByteUtils.DOT);
            //特殊节点处理
            if (3 == keySplit.length) {
                String identifyName = keySplit[0];
                String objectName = keySplit[1];
                String fieldName = keySplit[2];
                //条形码
                if ("barCode".equals(identifyName)) {
                    WordToPdfBean wordToPdf = invokeFieldName(key, objectName, fieldName, recipePdfDTO, null);
                    URI uri = BarCodeUtil.generateFileUrl(wordToPdf.getValue(), "barcode.png");
                    wordToPdf.setUri(uri);
                    generatePdfList.add(wordToPdf);
                }
                //二维码
                if ("qrCode".equals(identifyName)) {
                    generatePdfList.add(invokeFieldName(key, objectName, fieldName, recipePdfDTO, null));
                }
                continue;
            }
            //对象字段处理
            if (2 == keySplit.length) {
                String objectName = keySplit[0];
                String fieldName = keySplit[1];
                generatePdfList.add(invokeFieldName(key, objectName, fieldName, recipePdfDTO, recipeDetailMap));
            }
        }
        return generatePdfList;
    }

    /**
     * 反射获取所需字段值
     *
     * @param key          表单名
     * @param objectName   对象名
     * @param fieldName    字段名
     * @param recipePdfDTO pdf值对象
     * @return 对应的value
     */
    private WordToPdfBean invokeFieldName(String key, String objectName, String fieldName, RecipeInfoDTO recipePdfDTO, Map<String, Object> recipeDetailMap) {
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
            String value = ByteUtils.objValueOfString(recipeDetailMap.get(key));
            return new WordToPdfBean(key, value, null);
        }
        return null;
    }

    /**
     * 中药
     *
     * @param recipeInfoDTO 处方明细
     * @return
     */
    private Map<String, Object> createChineMedicinePDF(RecipeInfoDTO recipeInfoDTO) {
        List<Recipedetail> recipeDetails = recipeInfoDTO.getRecipeDetails();
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return null;
        }
        List<RecipeLabelVO> list = new LinkedList<>();
        for (int i = 0; i < recipeDetails.size(); i++) {
            Recipedetail detail = recipeDetails.get(i);
            list.add(new RecipeLabelVO("药品名称", "recipeDetail.drugName_" + i, detail.getDrugName() + "(" + detail.getMemo() + ")"));
        }
        Recipedetail recipedetail = recipeDetails.get(0);
        list.add(new RecipeLabelVO("天数", "recipeDetail.useDays", recipedetail.getUseDays()));
        list.add(new RecipeLabelVO("用药途径", "recipeDetail.usePathways", DictionaryUtil.getDictionary("eh.cdr.dictionary.UsePathways", recipedetail.getUsePathways())));
        list.add(new RecipeLabelVO("用药频次", "recipeDetail.usingRate", DictionaryUtil.getDictionary("eh.cdr.dictionary.UsingRate", recipedetail.getUsingRate())));
        Recipe recipe = recipeInfoDTO.getRecipe();
        list.add(new RecipeLabelVO("贴数", "recipe.copyNum", recipe.getCopyNum()));
        list.add(new RecipeLabelVO("嘱托", "recipe.recipeMemo", recipe.getRecipeMemo()));
        RecipeExtend recipeExtend = recipeInfoDTO.getRecipeExtend();
        list.add(new RecipeLabelVO("煎法", "recipeExtend.decoctionText", recipeExtend.getDecoctionText()));
        list.add(new RecipeLabelVO("制法", "recipeExtend.makeMethodText", recipeExtend.getMakeMethodText()));
        list.add(new RecipeLabelVO("每付取汁", "recipeExtend.juice", recipeExtend.getJuice()));
        list.add(new RecipeLabelVO("每次用汁", "recipeExtend.minor", recipeExtend.getMinor()));
        logger.info("CreateRecipePdfUtil createChineMedicinePDF list :{} ", JSON.toJSONString(list));
        return list.stream().collect(Collectors.toMap(RecipeLabelVO::getEnglishName, RecipeLabelVO::getValue));
    }


    /**
     * 西药
     *
     * @param recipeInfoDTO 处方明细
     * @return
     */
    private Map<String, Object> createMedicinePDF(RecipeInfoDTO recipeInfoDTO) {
        List<Recipedetail> recipeDetails = recipeInfoDTO.getRecipeDetails();
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return null;
        }
        List<RecipeLabelVO> list = new LinkedList<>();
        for (int i = 0; i < recipeDetails.size(); i++) {
            Recipedetail detail = recipeDetails.get(i);
            list.add(new RecipeLabelVO("药品名称", "recipeDetail.drugName_" + i, detail.getDrugName()));
            list.add(new RecipeLabelVO("包装规格", "recipeDetail.drugSpec_" + i, detail.getDrugSpec()));
            list.add(new RecipeLabelVO("发药数量", "recipeDetail.useTotalDose_" + i, detail.getUseTotalDose()));
            list.add(new RecipeLabelVO("每次用量", "recipeDetail.useDose_" + i, detail.getUseDose()));
            list.add(new RecipeLabelVO("用药频次", "recipeDetail.usingRate_" + i, DictionaryUtil.getDictionary("eh.cdr.dictionary.UsingRate", detail.getUsingRate())));
            list.add(new RecipeLabelVO("用药途径", "recipeDetail.usePathways_" + i, DictionaryUtil.getDictionary("eh.cdr.dictionary.UsePathways", detail.getUsePathways())));
            list.add(new RecipeLabelVO("用药天数", "recipeDetail.useDays_" + i, detail.getUseDays()));
            list.add(new RecipeLabelVO("用药天数", "recipeDetail.memo_" + i, detail.getMemo()));
        }
        logger.info("CreateRecipePdfUtil createMedicinePDF list :{} ", JSON.toJSONString(list));
        return list.stream().collect(Collectors.toMap(RecipeLabelVO::getEnglishName, RecipeLabelVO::getValue));
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

}
