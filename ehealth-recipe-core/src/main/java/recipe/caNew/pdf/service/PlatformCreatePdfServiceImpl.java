package recipe.caNew.pdf.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.dto.RecipeLabelVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.dictionary.DictionaryController;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.SignImgNode;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.IConfigurationClient;
import recipe.client.OperationClient;
import recipe.constant.OperationConstant;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.RecipeManager;
import recipe.manager.RedisManager;
import recipe.manager.SignManager;
import recipe.util.DictionaryUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 平台创建pdf实现类
 * 根据运营平台配置模版方式生成的 业务处理代码类
 *
 * @author fuzi
 */
@Service
public class PlatformCreatePdfServiceImpl implements CreatePdfService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IConfigurationClient configurationClient;
    @Resource
    private IESignBaseService esignService;
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private SignManager signManager;


    @Override
    public byte[] queryPdfOssId(Recipe recipe) throws Exception {
        //生成pdf
        SignRecipePdfVO signRecipePdfVO = queryPdfBytePdf(recipe);
        SignRecipePdfVO pdfEsign = new SignRecipePdfVO();
        pdfEsign.setData(signRecipePdfVO.getData());
        pdfEsign.setFileName("recipe_" + recipe.getRecipeId() + ".pdf");
        pdfEsign.setDoctorId(recipe.getDoctor());
        pdfEsign.setPosX(80f);
        pdfEsign.setPosY(57f);
        pdfEsign.setWidth(150f);
        byte[] data = esignService.signForRecipe2(pdfEsign);
        logger.info("PlatformCreatePdfServiceImpl queryPdfOssId data:{}", data.length);
        return data;
    }


    @Override
    public CaSealRequestTO queryPdfByte(Recipe recipe) throws Exception {
        SignRecipePdfVO signRecipePdfVO = queryPdfBytePdf(recipe);
        return CreatePdfFactory.caSealRequestTO(55, 76, recipe.getRecipeId().toString(), signRecipePdfVO.getDataStr());
    }

    @Override
    public String updateDoctorNamePdf(Recipe recipe, SignImgNode signImgNode) throws Exception {
        logger.info("PlatformCreatePdfServiceImpl updateDoctorNamePdf recipe:{}", JSON.toJSONString(recipe));
        SignRecipePdfVO signRecipePdfVO = queryPdfBytePdf(recipe);
        signImgNode.setSignFileData(signRecipePdfVO.getData());
        signImgNode.setX(55f);
        signImgNode.setY(76f);
        return CreateRecipePdfUtil.generateSignImgNode(signImgNode);
    }

    @Override
    public CaSealRequestTO queryCheckPdfByte(Recipe recipe) {
        logger.info("PlatformCreatePdfServiceImpl queryCheckPdfByte recipe:{}", JSON.toJSONString(recipe));
        return CreatePdfFactory.caSealRequestTO(190, 76, "check" + recipe.getRecipeId(), CreateRecipePdfUtil.signFileBase64(recipe.getSignFile()));
    }


    @Override
    public String updateCheckNamePdf(Recipe recipe, String signImageId) throws Exception {
        String recipeId = recipe.getRecipeId().toString();
        logger.info("PlatformCreatePdfServiceImpl updateCheckNamePdf recipeId:{}", recipeId);
        //更新pdf文件
        if (StringUtils.isNotEmpty(signImageId)) {
            SignImgNode signImgNode = new SignImgNode(recipeId, recipeId, signImageId, recipe.getSignFile(),
                    null, 40f, 20f, 190f, 76f, false);
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
    public String updateCodePdf(Recipe recipe) throws Exception {
        Integer recipeId = recipe.getRecipeId();
        logger.info("PlatformCreatePdfServiceImpl updateCodePdf  recipeId={}", recipeId);
        String barcode = "";
        List<Scratchable> scratchableList = operationClient.scratchableList(recipe.getClinicOrgan(), "moduleFive");
        if (!CollectionUtils.isEmpty(scratchableList)) {
            for (Scratchable scratchable : scratchableList) {
                if (!"条形码".equals(scratchable.getBoxTxt())) {
                    continue;
                }
                if ("recipe.patientID".equals(scratchable.getBoxLink())) {
                    barcode = recipe.getPatientID();
                    break;
                }
                if ("recipe.recipeCode".equals(scratchable.getBoxLink())) {
                    barcode = recipe.getRecipeCode();
                    break;
                }
            }
        }
        CoOrdinateVO ordinate = new CoOrdinateVO();
        ordinate.setValue(barcode);
        ordinate.setX(10);
        ordinate.setY(560);
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
        return CreateRecipePdfUtil.generateRecipeCodeAndPatientIdForRecipePdf(recipe.getSignFile(), coOrdinateList, ordinate);
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
        return new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString(), null,
                null, null, 50f, 20f, 210f, 99f, true);
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
     * 获取pdf Byte字节 给前端SDK
     *
     * @param recipe 处方信息
     * @return
     */
    private SignRecipePdfVO queryPdfBytePdf(Recipe recipe) throws Exception {
        logger.info("PlatformCreatePdfServiceImpl queryPdfBytePdf recipe:{}", JSON.toJSONString(recipe));
        //获取pdf值对象
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDTO(recipe.getRecipeId());
        ApothecaryDTO apothecaryDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipe.getGiveUser(), recipe.getRecipeId());
        recipePdfDTO.setApothecary(apothecaryDTO);
        Map<String, List<RecipeLabelVO>> result = operationClient.queryRecipeLabel(recipePdfDTO);
        List<RecipeLabelVO> list = result.get("moduleThree");
        //组装生成pdf的参数
        Map<String, Object> map = new HashMap<>();
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            //中药pdf参数
            map.put("templateType", "tcm");
            createChineMedicinePDF(list, recipePdfDTO.getRecipeExtend(), recipePdfDTO.getRecipe());
            //添加斜线位置 1,中间  2 下面
            String invalidInfoObject = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "diagonalLineLayer", "1");
            map.put("diagonalLineLayer", Integer.valueOf(invalidInfoObject));
        } else {
            map.put("templateType", "wm");
            createMedicinePDF(list, recipePdfDTO.getRecipe());
        }
        map.put("rp", configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "rptorx", null));
        map.put("paramMap", result);
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
    private void createMedicinePDF(List<RecipeLabelVO> list, Recipe recipe) {
        RecipeLabelVO recipeLabelVO = list.stream().filter(a -> OperationConstant.OP_RECIPE_DETAIL.equals(a.getEnglishName())).findAny().orElse(null);
        if (null == recipeLabelVO) {
            return;
        }
        List<Recipedetail> recipeDetailList = (List<Recipedetail>) recipeLabelVO.getValue();
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            return;
        }
        for (int i = 0; i < recipeDetailList.size(); i++) {
            Recipedetail d = recipeDetailList.get(i);
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
            String useDose = null == d.getUseDose() ? "" : d.getUseDose() + d.getUseDoseUnit();
            String uDose = "Sig: 每次" + useDose;

            //用药频次
            String dRateName = d.getUsingRateTextFromHis() != null ? d.getUsingRateTextFromHis() : DictionaryUtil.getDictionary("eh.cdr.dictionary.UsingRate", d.getUsingRate());
            //用法
            String dWay = d.getUsePathwaysTextFromHis() != null ? d.getUsePathwaysTextFromHis() : DictionaryUtil.getDictionary("eh.cdr.dictionary.UsePathways", d.getUsePathways());
            stringBuilder.append(uDose).append("    ").append(dRateName).append("    ").append(dWay).append("    ").append(getUseDays(d.getUseDaysB(), d.getUseDays()));

            if (!StringUtils.isEmpty(d.getMemo())) {
                stringBuilder.append(" \n ").append("嘱托:").append(d.getMemo());
            }
            list.add(new RecipeLabelVO("medicine", "drugInfo" + i, stringBuilder.toString()));
        }
    }

    /**
     * 中药pdf 摸版参数
     *
     * @param list
     * @param extend
     * @param recipe
     */
    private void createChineMedicinePDF(List<RecipeLabelVO> list, RecipeExtend extend, Recipe recipe) {
        RecipeLabelVO recipeLabelVO = list.stream().filter(a -> OperationConstant.OP_RECIPE_DETAIL.equals(a.getEnglishName())).findAny().orElse(null);
        if (null == recipeLabelVO) {
            return;
        }
        List<Recipedetail> recipeDetailList = (List<Recipedetail>) recipeLabelVO.getValue();
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            return;
        }
        String drugShowName;
        for (int i = 0; i < recipeDetailList.size(); i++) {
            Recipedetail detail = recipeDetailList.get(i);
            String dTotal;
            if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                dTotal = detail.getUseDoseStr() + detail.getUseDoseUnit();
            } else {
                dTotal = detail.getUseDose() + detail.getUseDoseUnit();
            }
            if (!StringUtils.isEmpty(detail.getMemo()) && !"无特殊煎法".equals(detail.getMemo())) {
                dTotal = dTotal + "(" + detail.getMemo() + ")";
            }
            drugShowName = detail.getDrugName() + " " + dTotal;
            list.add(new RecipeLabelVO("chineMedicine", "drugInfo" + i, drugShowName));
        }
        Recipedetail detail = recipeDetailList.get(0);
        list.add(new RecipeLabelVO("天数", "tcmUseDay", getUseDays(detail.getUseDaysB(), detail.getUseDays())));
        try {
            list.add(new RecipeLabelVO("用药途径", "tcmUsePathways", DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detail.getUsePathways())));
            list.add(new RecipeLabelVO("用药频次", "tcmUsingRate", DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detail.getUsingRate())));
        } catch (Exception e) {
            logger.error("用药途径 用药频率有误");
        }
        list.add(new RecipeLabelVO("贴数", "copyNum", recipe.getCopyNum() + "贴"));
        if (null != extend) {
            list.add(new RecipeLabelVO("煎法", "tcmDecoction", extend.getDecoctionText() == null ? "" : extend.getDecoctionText()));
            list.add(new RecipeLabelVO("每付取汁", "tcmJuice", extend.getJuice() + extend.getJuiceUnit()));
            list.add(new RecipeLabelVO("次量", "tcmMinor", extend.getMinor() + extend.getMinorUnit()));
            list.add(new RecipeLabelVO("制法", "tcmMakeMethod", extend.getMakeMethodText() == null ? "" : extend.getMakeMethodText()));
        }
        list.add(new RecipeLabelVO("嘱托", "tcmRecipeMemo", recipe.getRecipeMemo() == null ? "" : recipe.getRecipeMemo()));
    }


    /**
     * 获取天数 与 单位字符串展示
     *
     * @param useDaysB
     * @param useDays
     * @return
     */
    private String getUseDays(String useDaysB, Integer useDays) {
        if (StringUtils.isNotEmpty(useDaysB) && !"0".equals(useDaysB)) {
            return useDaysB + "天";
        }
        if (!ValidateUtil.integerIsEmpty(useDays)) {
            return useDays + "天";
        }
        return "";
    }
}
