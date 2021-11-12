package recipe.caNew.pdf.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.dto.RecipeLabelDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.dictionary.DictionaryController;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.SignImgNode;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.RedisManager;
import recipe.manager.SignManager;
import recipe.util.ByteUtils;
import recipe.util.DictionaryUtil;
import recipe.util.RecipeUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static recipe.constant.OperationConstant.OP_RECIPE_EXTEND;
import static recipe.constant.OperationConstant.OP_RECIPE_EXTEND_SUPERVISE;
import static recipe.util.ByteUtils.DOT_EN;

/**
 * 平台创建pdf实现类
 * 根据运营平台配置模版方式生成的 业务处理代码类
 *
 * @author fuzi
 */
@Service
public class PlatformCreatePdfServiceImpl extends BaseCreatePdf implements CreatePdfService {

    @Resource
    private IESignBaseService esignService;
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private SignManager signManager;

    @Override
    public byte[] queryPdfByte(Recipe recipe) throws Exception {
        return signRecipePdfVO(recipe).getData();
    }


    @Override
    public byte[] queryPdfOssId(Recipe recipe) throws Exception {
        byte[] pdfByte = signRecipePdfVO(recipe).getData();
        SignRecipePdfVO pdfEsign = new SignRecipePdfVO();
        pdfEsign.setData(pdfByte);
        pdfEsign.setFileName("recipe_" + recipe.getRecipeId() + ".pdf");
        pdfEsign.setDoctorId(recipe.getDoctor());
        pdfEsign.setPosX(80f);
        pdfEsign.setPosY(57f);
        pdfEsign.setWidth(150f);
        pdfEsign.setQrCodeSign(true);
        byte[] data = esignService.signForRecipe2(pdfEsign);
        logger.info("PlatformCreatePdfServiceImpl queryPdfOssId data:{}", data.length);
        return data;
    }


    @Override
    public CaSealRequestTO queryPdfBase64(byte[] data, Integer recipeId) throws Exception {
        String pdfBase64Str = new String(Base64.encode(data));
        return caSealRequestTO(55, 76, recipeId.toString(), pdfBase64Str);
    }


    @Override
    public String updateDoctorNamePdf(byte[] data, Integer recipeId, SignImgNode signImgNode) throws Exception {
        logger.info("PlatformCreatePdfServiceImpl updateDoctorNamePdf signImgNode:{}", JSON.toJSONString(signImgNode));
        signImgNode.setSignFileData(data);
        signImgNode.setX(55f);
        signImgNode.setY(76f);
        return CreateRecipePdfUtil.generateSignImgNode(signImgNode);
    }

    @Override
    public CaSealRequestTO queryCheckPdfByte(Recipe recipe) {
        logger.info("PlatformCreatePdfServiceImpl queryCheckPdfByte recipe:{}", JSON.toJSONString(recipe));
        return caSealRequestTO(190, 76, "check" + recipe.getRecipeId(), CreateRecipePdfUtil.signFileBase64(recipe.getSignFile()));
    }


    @Override
    public String updateCheckNamePdf(Recipe recipe, String signImageId) throws Exception {
        String recipeId = recipe.getRecipeId().toString();
        logger.info("PlatformCreatePdfServiceImpl updateCheckNamePdf recipeId:{}", recipeId);
        //更新pdf文件
        if (StringUtils.isNotEmpty(signImageId)) {
            SignImgNode signImgNode = new SignImgNode(recipeId, signImageId, recipe.getSignFile(), null,
                    40f, 20f, 190f, 76f, false);
            return CreateRecipePdfUtil.generateSignImgNode(signImgNode);
        } else if (StringUtils.isNotEmpty(recipe.getCheckerText())) {
            CoOrdinateVO coords = new CoOrdinateVO();
            coords.setValue(recipe.getCheckerText());
            coords.setX(199);
            coords.setY(82);
            return CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), coords);
        }
        return null;
    }

    @Override
    public byte[] updateCheckNamePdfEsign(Integer recipeId, SignRecipePdfVO pdfEsign) throws Exception {
        pdfEsign.setPosX(240F);
        pdfEsign.setPosY(80F);
        byte[] data = esignService.signForRecipe2(pdfEsign);
        logger.info("CustomCreatePdfServiceImpl updateCheckNamePdfEsign data:{}", data.length);
        return data;
    }

    @Override
    public CoOrdinateVO updateTotalPdf(Recipe recipe, BigDecimal recipeFee) {
        logger.info("PlatformCreatePdfServiceImpl updateTotalPdf  recipeId={},recipeFee={}", recipe.getRecipeId(), recipeFee);
        List<Scratchable> scratchableList = operationClient.scratchableList(recipe.getClinicOrgan(), "moduleFour");
        logger.info("PlatformCreatePdfServiceImpl updateTotalPdf  scratchableList:{}", JSON.toJSONString(scratchableList));
        if (CollectionUtils.isEmpty(scratchableList)) {
            return null;
        }
        boolean actualPrice = scratchableList.stream().noneMatch(a -> "recipe.actualPrice".equals(a.getBoxLink()));
        if (actualPrice) {
            return null;
        }
        CoOrdinateVO coords = new CoOrdinateVO();
        coords.setValue("药品金额 ：" + recipeFee + "元");
        coords.setX(285);
        coords.setY(80);
        coords.setRepeatWrite(true);
        return coords;
    }

    @Override
    public CoOrdinateVO updateDispensingTimePdf(Recipe recipe, String dispensingTime) {
        CoOrdinateVO ordinateVO = redisManager.getPdfCoordsHeight(recipe.getRecipeId(), "recipeOrder.dispensingTime");
        if (null == ordinateVO) {
            return null;
        }
        CoOrdinateVO coords = new CoOrdinateVO();
        coords.setValue(dispensingTime);
        coords.setX(ordinateVO.getX());
        coords.setY(ordinateVO.getY());
        coords.setRepeatWrite(true);
        return coords;
    }


    @Override
    public String updateCodePdf(Recipe recipe) throws Exception {
        Integer recipeId = recipe.getRecipeId();
        logger.info("PlatformCreatePdfServiceImpl updateCodePdf  recipeId={}", recipeId);
        List<CoOrdinateVO> coOrdinateList = new LinkedList<>();
        CoOrdinateVO patientId = redisManager.getPdfCoordsHeight(recipeId, "recipe.patientID");
        if (null != patientId) {
            patientId.setValue(recipe.getPatientID());
            coOrdinateList.add(patientId);
        }
        CoOrdinateVO recipeCode = redisManager.getPdfCoordsHeight(recipeId, "recipe.recipeCode");
        if (null != recipeCode) {
            recipeCode.setValue(recipe.getRecipeCode());
            coOrdinateList.add(recipeCode);
        }
        //条形码
        CoOrdinateVO ordinate = barcodeVO(recipe);
        return CreateRecipePdfUtil.generateOrdinateListAndBarcode(recipe.getSignFile(), coOrdinateList, ordinate);
    }

    @Override
    public String updateSuperviseRecipeCodeExecute(Recipe recipe, String fileId, String superviseRecipeCode) throws Exception {
        List<CoOrdinateVO> coOrdinateList = new LinkedList<>();
        CoOrdinateVO supervise = redisManager.getPdfCoordsHeight(recipe.getRecipeId(), OP_RECIPE_EXTEND + DOT_EN + OP_RECIPE_EXTEND_SUPERVISE);
        if (null != supervise) {
            supervise.setValue(superviseRecipeCode);
            coOrdinateList.add(supervise);
        }
        //条形码
        CoOrdinateVO ordinate = barcodeVO(recipe);
        return CreateRecipePdfUtil.generateOrdinateListAndBarcode(fileId, coOrdinateList, ordinate);
    }


    @Override
    public List<CoOrdinateVO> updateAddressPdf(Recipe recipe, RecipeOrder order, String address) {
        Integer recipeId = recipe.getRecipeId();
        logger.info("PlatformCreatePdfServiceImpl updateAddressPdfExecute  recipeId={}", recipeId);
        List<CoOrdinateVO> list = new LinkedList<>();
        // 煎法
        CoOrdinateVO decoction = validateDecoction(recipe);
        if (null != decoction) {
            list.add(decoction);
        }
        CoOrdinateVO coOrdinateVO = redisManager.getPdfCoordsHeight(recipe.getRecipeId(), "receiverPlaceholder");
        if (null == coOrdinateVO) {
            return list;
        }
        if (StringUtils.isNotEmpty(order.getReceiver())) {
            CoOrdinateVO receiver = new CoOrdinateVO();
            receiver.setX(10);
            receiver.setY(coOrdinateVO.getY());
            receiver.setValue("收货人姓名：" + order.getReceiver());
            list.add(receiver);
        }
        if (StringUtils.isNotEmpty(order.getReceiver())) {
            CoOrdinateVO receiver = new CoOrdinateVO();
            receiver.setX(149);
            receiver.setY(coOrdinateVO.getY());
            receiver.setValue("收货人电话：" + order.getRecMobile());
            list.add(receiver);
        }
        if (StringUtils.isNotEmpty(address)) {
            CoOrdinateVO receiver = new CoOrdinateVO();
            receiver.setX(10);
            receiver.setY(coOrdinateVO.getY() - 12);
            receiver.setValue("收货人地址：" + address);
            list.add(receiver);
        }
        return list;
    }

    @Override
    public SignImgNode updateGiveUser(Recipe recipe) {
        logger.info("PlatformCreatePdfServiceImpl updateGiveUser recipe={}", JSON.toJSONString(recipe));
        //判断配置是否有核对发药
        List<Scratchable> scratchableList = operationClient.scratchableList(recipe.getClinicOrgan(), "moduleFour");
        if (CollectionUtils.isEmpty(scratchableList)) {
            return null;
        }
        boolean isGiveUser = scratchableList.stream().noneMatch(a -> "recipe.giveUser".equals(a.getBoxLink()));
        if (isGiveUser) {
            return null;
        }
        //修改pdf文件
        return new SignImgNode(recipe.getRecipeId().toString(), null, null, null,
                50f, 20f, 210f, 99f, true);
    }

    @Override
    public SignImgNode updateSealPdf(Integer recipeId, String organSealId, String fileId) {
        return new SignImgNode(recipeId.toString(), organSealId, fileId, null, 90F, 90F, 160f, 490f, false);
    }

    /**
     * 条形码
     *
     * @param recipe
     * @return
     */
    private CoOrdinateVO barcodeVO(Recipe recipe) {
        List<Scratchable> scratchableList = operationClient.scratchableList(recipe.getClinicOrgan(), "moduleFive");
        if (CollectionUtils.isEmpty(scratchableList)) {
            return null;
        }
        CoOrdinateVO ordinate = new CoOrdinateVO();
        String barcode = super.barcode(recipe);
        ordinate.setValue(barcode);
        ordinate.setX(10);
        ordinate.setY(560);
        return ordinate;
    }


    /**
     * 校验煎法
     *
     * @param recipe
     * @return
     */
    private CoOrdinateVO validateDecoction(Recipe recipe) {
        //患者端煎法生效
        String decoctionDeploy = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "decoctionDeploy", null);
        if (null == decoctionDeploy) {
            return null;
        }

        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (null == recipeExtend || StringUtils.isEmpty(recipeExtend.getDecoctionText())) {
            return null;
        }
        //decoctionDeploy煎法
        CoOrdinateVO coOrdinateVO = redisManager.getPdfCoordsHeight(recipe.getRecipeId(), "tcmDecoction");
        if (null == coOrdinateVO) {
            return null;
        }
        coOrdinateVO.setValue(recipeExtend.getDecoctionText());
        return coOrdinateVO;
    }

    /**
     * 获取pdf Byte字节
     *
     * @param recipe 处方信息
     * @return
     */
    private SignRecipePdfVO signRecipePdfVO(Recipe recipe) throws Exception {
        logger.info("PlatformCreatePdfServiceImpl queryPdfBytePdf recipe:{}", JSON.toJSONString(recipe));
        //获取pdf值对象
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDictionary(recipe.getRecipeId());
        ApothecaryDTO apothecaryDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipe.getGiveUser(), recipe.getRecipeId());
        recipePdfDTO.setApothecary(apothecaryDTO);
        Map<String, List<RecipeLabelDTO>> result = operationClient.queryRecipeLabel(recipePdfDTO);
        List<RecipeLabelDTO> list = result.get("moduleThree");
        //组装生成pdf的参数
        Map<String, Object> map = new HashMap<>();
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            //中药pdf参数
            map.put("templateType", "tcm");
            createChineMedicinePDF(list, recipePdfDTO.getRecipeDetails(), recipePdfDTO.getRecipeExtend(), recipePdfDTO.getRecipe());
            //添加斜线位置 1,中间  2 下面
            String invalidInfoObject = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "diagonalLineLayer", "1");
            map.put("diagonalLineLayer", Integer.valueOf(invalidInfoObject));
        } else {
            map.put("templateType", "wm");
            createMedicinePDF(list, recipePdfDTO.getRecipeDetails(), recipePdfDTO.getRecipe());
        }
        map.put("rp", configurationClient.getValueCatch(recipe.getClinicOrgan(), "rptorx", "Rp"));
        map.put("paramMap", result);
        map.put("recipeId", recipe.getRecipeId());
        SignRecipePdfVO signRecipePdfVO = esignService.createSignRecipePDF(map);
        logger.info("PlatformCreatePdfServiceImpl queryPdfRecipeLabelById map={},signRecipePdfVO={}", JSON.toJSONString(map), JSON.toJSONString(signRecipePdfVO));
        redisManager.coOrdinate(recipe.getRecipeId(), signRecipePdfVO.getCoOrdinateList());
        return signRecipePdfVO;
    }

    /**
     * 西药 pdf 摸版参数
     *
     * @param list
     * @param recipe
     */
    private void createMedicinePDF(List<RecipeLabelDTO> list, List<Recipedetail> recipeDetails, Recipe recipe) {
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return;
        }
        for (int i = 0; i < recipeDetails.size(); i++) {
            Recipedetail d = recipeDetails.get(i);
            //名称+规格+药品单位+开药总量+药品单位
            StringBuilder stringBuilder = new StringBuilder(i + 1);
            stringBuilder.append(i + 1).append("、");
            //药品显示名处理
            if (StringUtils.isNotEmpty(d.getDrugDisplaySplicedName())) {
                stringBuilder.append(d.getDrugDisplaySplicedName());
            } else {
                stringBuilder.append(d.getDrugName()).append(d.getDrugSpec()).append("/").append(d.getDrugUnit());
            }
            stringBuilder.append("   ").append("X").append(d.getUseTotalDose()).append(d.getDrugUnit());
            Boolean canShowDrugCost = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "canShowDrugCost", false);
            if (canShowDrugCost) {
                BigDecimal drugCost = d.getDrugCost().divide(BigDecimal.ONE, 2, RoundingMode.UP);
                stringBuilder.append("   ").append(drugCost).append("元");
            }
            stringBuilder.append(" \n ");
            //每次剂量+剂量单位
            String useDose = StringUtils.isNotEmpty(d.getUseDoseStr()) ? d.getUseDoseStr() : d.getUseDose() + d.getUseDoseUnit();
            String uDose = "Sig: 每次" + useDose;

            //用药频次
            String dRateName = d.getUsingRateTextFromHis() != null ? d.getUsingRateTextFromHis() : DictionaryUtil.getDictionary("eh.cdr.dictionary.UsingRate", d.getUsingRate());
            //用法
            String dWay = d.getUsePathwaysTextFromHis() != null ? d.getUsePathwaysTextFromHis() : DictionaryUtil.getDictionary("eh.cdr.dictionary.UsePathways", d.getUsePathways());
            stringBuilder.append(uDose).append("    ").append(dRateName).append("    ").append(dWay).append("    ").append(getUseDays(d.getUseDaysB(), d.getUseDays()));

            if (!StringUtils.isEmpty(d.getMemo())) {
                stringBuilder.append(" \n ").append("嘱托:").append(d.getMemo());
            }
            list.add(new RecipeLabelDTO("medicine", "drugInfo" + i, stringBuilder.toString()));
        }
    }

    /**
     * 中药pdf 摸版参数
     *
     * @param list
     * @param extend
     * @param recipe
     */
    private void createChineMedicinePDF(List<RecipeLabelDTO> list, List<Recipedetail> recipeDetails, RecipeExtend extend, Recipe recipe) {
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return;
        }
        for (int i = 0; i < recipeDetails.size(); i++) {
            String drugShowName = RecipeUtil.drugChineShowName(recipeDetails.get(i));
            list.add(new RecipeLabelDTO("chineMedicine", "drugInfo" + i, drugShowName));
        }
        Recipedetail detail = recipeDetails.get(0);
        list.add(new RecipeLabelDTO("天数", "tcmUseDay", getUseDays(detail.getUseDaysB(), detail.getUseDays())));
        try {
            list.add(new RecipeLabelDTO("用药途径", "tcmUsePathways", DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detail.getUsePathways())));
            list.add(new RecipeLabelDTO("用药频次", "tcmUsingRate", DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detail.getUsingRate())));
        } catch (Exception e) {
            logger.error("用药途径 用药频率有误");
        }
        if (null != extend) {

            list.add(new RecipeLabelDTO("煎法", "tcmDecoction", ByteUtils.objValueOfString(extend.getDecoctionText())));
            list.add(new RecipeLabelDTO("每付取汁", "tcmJuice", ByteUtils.objValueOfString(extend.getJuice()) + ByteUtils.objValueOfString(extend.getJuiceUnit())));
            list.add(new RecipeLabelDTO("次量", "tcmMinor", ByteUtils.objValueOfString(extend.getMinor()) + ByteUtils.objValueOfString(extend.getMinorUnit())));
            list.add(new RecipeLabelDTO("制法", "tcmMakeMethod", ByteUtils.objValueOfString(extend.getMakeMethodText())));
        }
        list.add(new RecipeLabelDTO("贴数", "copyNum", recipe.getCopyNum() + "贴"));
        list.add(new RecipeLabelDTO("嘱托", "tcmRecipeMemo", ByteUtils.objValueOfString(recipe.getRecipeMemo())));
    }
}
