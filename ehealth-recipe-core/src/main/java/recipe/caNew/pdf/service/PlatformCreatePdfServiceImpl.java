package recipe.caNew.pdf.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.ESignDTO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.ca.PdfSignResultDTO;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.AttachSealPicDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.openapi.request.province.SignImgNode;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.comment.DictionaryUtil;
import recipe.constant.CacheConstant;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.service.RecipeLogService;
import recipe.service.client.IConfigurationClient;
import recipe.service.manager.RecipeLabelManager;
import recipe.service.manager.SignManager;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static recipe.service.RecipeServiceSub.getRecipeAndDetailByIdImpl;

/**
 * 平台创建pdf实现类
 * 根据运营平台配置模版方式生成的 业务处理代码类
 *
 * @author fuzi
 */
@Service
public class PlatformCreatePdfServiceImpl implements CreatePdfService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IConfigurationClient configurationClient;
    @Resource
    private IESignBaseService esignService;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private RecipeLabelManager recipeLabelManager;
    @Autowired
    private SignManager signManager;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO orderDAO;

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Override
    public PdfSignResultDTO queryPdfOssId(Recipe recipe) {
        int recipeId = recipe.getRecipeId();
        Map<String, Object> recipeMap = getRecipeAndDetailByIdImpl(recipeId, false);
        Map<String, List<RecipeLabelVO>> result = recipeLabelManager.queryRecipeLabelById(recipe.getClinicOrgan(), recipeMap);
        try {
            PatientDTO patientDTO = (PatientDTO) recipeMap.get("patient");
            //组装生成pdf的参数
            ESignDTO eSignDTO = new ESignDTO();
            eSignDTO.setLoginId(patientDTO.getLoginId());
            eSignDTO.setDoctorName(recipe.getDoctorName());
            eSignDTO.setDoctorId(recipe.getDoctor());
            eSignDTO.setOrgan(recipe.getClinicOrgan());
            eSignDTO.setFileName("recipe_" + recipeId + ".pdf");
            eSignDTO.setParamMap(Collections.unmodifiableMap(result));
            eSignDTO.setRp(configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "rptorx", null));
            if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                //中药pdf参数
                eSignDTO.setTemplateType("tcm");
                createChineMedicinePDF(result, recipeMap, (RecipeBean) recipeMap.get("recipe"));
            } else {
                eSignDTO.setTemplateType("wm");
                eSignDTO.setImgFileId(String.valueOf(recipeId));
                createMedicinePDF(result, (RecipeBean) recipeMap.get("recipe"));
            }
            Map<String, Object> backMap = esignService.signForRecipe2(eSignDTO);
            logger.info("RecipeLabelManager queryPdfRecipeLabelById backMap={},eSignDTO={}", JSONUtils.toString(backMap), JSONUtils.toString(eSignDTO));

            List<CoOrdinateVO> coOrdinateVO = MapValueUtil.getList(backMap, "coOrdinateList");
            coOrdinate(recipeId, coOrdinateVO);

            String imgFileId = MapValueUtil.getString(backMap, "imgFileId");
            Integer code = MapValueUtil.getInteger(backMap, "code");
            String recipeFileId = MapValueUtil.getString(backMap, "fileId");
            return new PdfSignResultDTO(imgFileId, code, recipeFileId);
        } catch (Exception e) {
            logger.error("queryPdfRecipeLabelById error ", e);
            //日志记录
            String memo = "签名上传文件失败！原因：" + e.getMessage();
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), memo);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "pdf error");
        }
    }


    @Override
    public CaSealRequestTO queryPdfByte(Recipe recipe) {
        SignRecipePdfVO signRecipePdfVO = queryPdfBytePdf(recipe);
        return caSealRequestTO(55, recipe.getRecipeId().toString(), signRecipePdfVO.getDataStr());
    }


    @Override
    public CaSealRequestTO queryCheckPdfByte(Recipe recipe) {
        return caSealRequestTO(190, "check" + recipe.getRecipeId(), CreateRecipePdfUtil.signFileByte(recipe.getSignFile()));
    }

    @Override
    public void updateTotalPdf(Integer recipeId, BigDecimal recipeFee) {
        logger.info("UpdateTotalRecipePdfRunable start. recipeId={},recipeFee={}", recipeId, recipeFee);
        if (null == recipeFee) {
            logger.warn("UpdateTotalRecipePdfRunable recipeFee is null  recipeFee={}", recipeFee);
            return;
        }
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if (null == recipe) {
            logger.warn("UpdateTotalRecipePdfRunable recipe is null  recipeId={}", recipeId);
            return;
        }
        List<Scratchable> scratchableList = recipeLabelManager.scratchableList(recipe.getClinicOrgan(), "moduleFour");
        if (CollectionUtils.isEmpty(scratchableList)) {
            return;
        }
        boolean actualPrice = scratchableList.stream().noneMatch(a -> "recipe.actualPrice".equals(a.getBoxLink()));
        if (actualPrice) {
            return;
        }
        CoOrdinateVO coords = new CoOrdinateVO();
        coords.setValue("药品金额 ：" + recipeFee + "元");
        coords.setX(285);
        coords.setY(80);
        coords.setRepeatWrite(true);
        Recipe recipeUpdate = new Recipe();
        String fileId = null;
        try {
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                fileId = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getChemistSignFile(), coords);
                recipeUpdate.setChemistSignFile(fileId);
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                fileId = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), coords);
                recipeUpdate.setSignFile(fileId);
            }
        } catch (Exception e) {
            logger.error("UpdateTotalRecipePdfRunable error recipeId={}", recipeId, e);
            return;
        }

        if (StringUtils.isEmpty(fileId)) {
            return;
        }
        recipeUpdate.setRecipeId(recipeId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("UpdateTotalRecipePdfRunable recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
    }


    @Override
    public void updateCheckNamePdf(Integer recipeId) {
        logger.info("recipe pharmacyToRecipePDF,recipeId={}", recipeId);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return;
        }
        boolean usePlatform = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeUsePlatformCAPDF", true);
        if (!usePlatform) {
            return;
        }
        String fileId = null;
        try {
            //设置签名图片
            AttachSealPicDTO sttachSealPicDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipeId);
            String signImageId = sttachSealPicDTO.getCheckerSignImg();
            if (StringUtils.isNotEmpty(signImageId)) {
                SignImgNode signImgNode = new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString()
                        , signImageId, recipe.getSignFile(), null, 40f, 20f, 190f, 76f, false);
                fileId = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            } else if (StringUtils.isNotEmpty(recipe.getCheckerText())) {
                CoOrdinateVO coords = new CoOrdinateVO();
                coords.setValue(recipe.getCheckerText());
                coords.setX(199);
                coords.setY(82);
                fileId = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), coords);
            }
        } catch (Exception e) {
            logger.warn("当前处方{}是使用平台药师部分pdf的,生成失败！", recipe.getRecipeId());
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "平台药师部分pdf的生成失败");
        }
        if (StringUtils.isEmpty(fileId)) {
            return;
        }
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipeId);
        recipeUpdate.setChemistSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("UpdateTotalRecipePdfRunable recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
    }


    @Override
    public void updateDoctorNamePdf(Recipe recipe) {
        logger.info("doctorToRecipePDF recipe:{}", JSON.toJSONString(recipe));
        boolean usePlatform = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeUsePlatformCAPDF", true);
        if (!usePlatform) {
            return;
        }
        Integer recipeId = recipe.getRecipeId();
        SignRecipePdfVO signRecipePdfVO = queryPdfBytePdf(recipe);
        String fileId = null;
        try {
            //设置签名图片
            AttachSealPicDTO sttachSealPicDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipeId);
            SignImgNode signImgNode = new SignImgNode(recipeId.toString(), recipeId.toString(), sttachSealPicDTO.getDoctorSignImg(),
                    null, signRecipePdfVO.getData(), 40f, 20f, 55f, 76f, false);
            fileId = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
        } catch (Exception e) {
            logger.warn("当前处方是使用平台医生部分pdf的,生成失败！{}", recipe.getRecipeId(), e);
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "平台医生部分pdf的生成失败");
        }
        if (StringUtils.isEmpty(fileId)) {
            return;
        }
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipeId);
        recipeUpdate.setSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("UpdateTotalRecipePdfRunable recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
    }

    @Override
    public void updateCodePdfExecute(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            return;
        }
        String barcode = "";
        List<Scratchable> scratchableList = recipeLabelManager.scratchableList(recipe.getClinicOrgan(), "moduleFive");
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

        List<CoOrdinateVO> coOrdinateList = new LinkedList<>();
        CoOrdinateVO patientId = getPdfCoordsHeight(recipeId, "recipe.patientID");
        if (null != patientId) {
            patientId.setValue(recipe.getPatientID());
            coOrdinateList.add(patientId);
        }
        CoOrdinateVO recipeCode = getPdfCoordsHeight(recipeId, "recipe.recipeCode");
        if (null != recipeCode) {
            recipeCode.setValue(recipe.getRecipeCode());
            coOrdinateList.add(recipeCode);
        }
        String fileId = null;
        try {
            fileId = CreateRecipePdfUtil.generateRecipeCodeAndPatientIdForRecipePdf(recipe.getSignFile(), coOrdinateList, barcode);
        } catch (Exception e) {
            logger.error("当前处方是使用平台医生部分pdf的,生成失败！{}", recipe.getRecipeId(), e);
        }
        if (StringUtils.isEmpty(fileId)) {
            return;
        }
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipeId);
        recipeUpdate.setSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("addRecipeCodeAndPatientForRecipePdf recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
    }

    @Override
    public void updateAddressPdfExecute(Integer recipeId) {
        logger.info("UpdateReceiverInfoRecipePdfRunable start. recipeId={}", recipeId);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if (null == recipe) {
            logger.warn("UpdateReceiverInfoRecipePdfRunable recipe is null  recipeId={}", recipeId);
            return;
        }
        RecipeOrder order = orderDAO.getRelationOrderByRecipeId(recipeId);
        if (null == order) {
            logger.warn("UpdateReceiverInfoRecipePdfRunable order is null  recipeId={}", recipeId);
            return;
        }
        String fileId = null;
        Recipe recipeUpdate = new Recipe();
        try {
            // 煎法
            CoOrdinateVO decoction = validateDecoction(recipe);
            CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
            logger.info("UpdateReceiverInfoRecipePdfRunable recipeid:{},order:{}", recipeId, JSONUtils.toString(order));
            //存在收货人信息
            if (StringUtils.isNotEmpty(order.getReceiver()) || StringUtils.isNotEmpty(order.getRecMobile())) {
                CoOrdinateVO coOrdinateVO = getPdfCoordsHeight(recipe.getRecipeId(), "receiverPlaceholder");
                if (null == coOrdinateVO) {
                    return;
                }
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    fileId = CreateRecipePdfUtil.generateReceiverInfoRecipePdf(recipe.getChemistSignFile(), order.getReceiver(), order.getRecMobile(), commonRemoteService.getCompleteAddress(order), coOrdinateVO.getY(), decoction);
                    recipeUpdate.setChemistSignFile(fileId);
                } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                    fileId = CreateRecipePdfUtil.generateReceiverInfoRecipePdf(recipe.getSignFile(), order.getReceiver(), order.getRecMobile(), commonRemoteService.getCompleteAddress(order), coOrdinateVO.getY(), decoction);
                    recipeUpdate.setSignFile(fileId);
                }
            } else if (null != decoction) {
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    fileId = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), decoction);
                    recipeUpdate.setChemistSignFile(fileId);
                } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                    fileId = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), decoction);
                    recipeUpdate.setSignFile(fileId);
                }
            }
        } catch (Exception e) {
            logger.error("UpdateReceiverInfoRecipePdfRunable error recipeId={}", recipeId, e);
            return;
        }
        recipeUpdate.setRecipeId(recipeId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("addRecipeCodeAndPatientForRecipePdf recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
    }

    @Override
    public Recipe updateGiveUser(Recipe recipe) {
        logger.info("RecipeLabelManager giveUserUpdate recipe={}", JSON.toJSONString(recipe));
        //获取 核对发药药师签名id
        ApothecaryVO apothecaryVO = signManager.giveUser(recipe.getClinicOrgan(), recipe.getGiveUser(), recipe.getRecipeId());
        //判断发药状态
        if (StringUtils.isEmpty(recipe.getOrderCode())) {
            return null;
        }
        RecipeOrder recipeOrder = orderDAO.getByOrderCode(recipe.getOrderCode());
        if (null == recipeOrder || null == recipeOrder.getDispensingTime()) {
            return null;
        }
        //判断配置是否有核对发药
        List<Scratchable> scratchableList = recipeLabelManager.scratchableList(recipe.getClinicOrgan(), "moduleFour");
        if (CollectionUtils.isEmpty(scratchableList)) {
            return null;
        }
        boolean isGiveUser = scratchableList.stream().noneMatch(a -> "recipe.giveUser".equals(a.getBoxLink()));
        if (isGiveUser) {
            return null;
        }
        //修改pdf文件
        SignImgNode signImgNode = new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString()
                , apothecaryVO.getGiveUserSignImg(), null, null, 50f, 20f, 210f, 99f, true);
        Recipe recipeUpdate = new Recipe();
        if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
            signImgNode.setSignFileId(recipe.getChemistSignFile());
            String newPfd = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            if (StringUtils.isEmpty(newPfd)) {
                return null;
            }
            recipeUpdate.setChemistSignFile(newPfd);
        } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
            signImgNode.setSignFileId(recipe.getSignFile());
            String newPfd = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            if (StringUtils.isEmpty(newPfd)) {
                return null;
            }
            recipeUpdate.setSignFile(newPfd);
        }
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        logger.info("RecipeLabelManager giveUserUpdate recipeUpdate={}", JSON.toJSONString(recipeUpdate));
        return recipeUpdate;
    }


    /**
     * 校验煎法
     *
     * @param recipe
     * @return
     */
    private CoOrdinateVO validateDecoction(Recipe recipe) {
        String decoctionDeploy = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "decoctionDeploy", null);
        if (null == decoctionDeploy) {
            return null;
        }

        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (null == recipeExtend || StringUtils.isEmpty(recipeExtend.getDecoctionText())) {
            return null;
        }
        //decoctionDeploy煎法
        CoOrdinateVO coOrdinateVO = getPdfCoordsHeight(recipe.getRecipeId(), "tcmDecoction");
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
    private SignRecipePdfVO queryPdfBytePdf(Recipe recipe) {
        int recipeId = recipe.getRecipeId();
        Map<String, Object> recipeMap = getRecipeAndDetailByIdImpl(recipeId, false);
        if (CollectionUtils.isEmpty(recipeMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is null!");
        }
        Map<String, List<RecipeLabelVO>> result = recipeLabelManager.queryRecipeLabelById(recipe.getClinicOrgan(), recipeMap);
        try {
            //组装生成pdf的参数
            Map<String, Object> map = new HashMap<>();
            if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                //中药pdf参数
                map.put("templateType", "tcm");
                createChineMedicinePDF(result, recipeMap, (RecipeBean) recipeMap.get("recipe"));
                //添加斜线位置 1,中间  2 下面
                String invalidInfoObject = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "diagonalLineLayer", "1");
                map.put("diagonalLineLayer", Integer.valueOf(invalidInfoObject));
            } else {
                map.put("templateType", "wm");
                createMedicinePDF(result, (RecipeBean) recipeMap.get("recipe"));
            }
            map.put("rp", configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "rptorx", null));
            map.put("paramMap", result);
            SignRecipePdfVO signRecipePdfVO = esignService.createSignRecipePDF(map);
            logger.info("RecipeLabelManager queryPdfRecipeLabelById map={},signRecipePdfVO={}", JSONUtils.toString(map), JSONUtils.toString(signRecipePdfVO));
            coOrdinate(recipeId, signRecipePdfVO.getCoOrdinateList());
            return signRecipePdfVO;
        } catch (Exception e) {
            logger.error("queryPdfRecipeLabelById error ", e);
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "获取pdf-byte-格式" + e.getMessage());
            throw new DAOException(ErrorCode.SERVICE_ERROR, "pdf error");
        }
    }

    /**
     * 特殊字段坐标记录
     *
     * @param recipeId
     * @param coOrdinateList
     */
    private void coOrdinate(Integer recipeId, List<CoOrdinateVO> coOrdinateList) {
        if (CollectionUtils.isEmpty(coOrdinateList) || null == recipeId) {
            logger.error("RecipeLabelManager coOrdinate error ");
            return;
        }
        redisClient.addList(CacheConstant.KEY_RECIPE_LABEL + recipeId.toString(), coOrdinateList, 3 * 24 * 60 * 60L);
    }


    /**
     * 获取pdf 特殊字段坐标
     *
     * @param recipeId   处方id
     * @param coordsName 特殊字段名称
     * @return
     */
    private CoOrdinateVO getPdfCoordsHeight(Integer recipeId, String coordsName) {
        if (ValidateUtil.integerIsEmpty(recipeId) || StringUtils.isEmpty(coordsName)) {
            return null;
        }
        List<CoOrdinateVO> coOrdinateList = redisClient.getList(CacheConstant.KEY_RECIPE_LABEL + recipeId.toString());
        logger.info("RecipeLabelManager getPdfReceiverHeight recipeId={}，coOrdinateList={}", recipeId, JSONUtils.toString(coOrdinateList));

        if (CollectionUtils.isEmpty(coOrdinateList)) {
            logger.error("RecipeLabelManager getPdfReceiverHeight recipeId为空 recipeId={}", recipeId);
            return null;
        }

        for (CoOrdinateVO coOrdinate : coOrdinateList) {
            if (coordsName.equals(coOrdinate.getName())) {
                coOrdinate.setY(499 - coOrdinate.getY());
                coOrdinate.setX(coOrdinate.getX() + 5);
                return coOrdinate;
            }
        }
        return null;
    }


    /**
     * 西药 pdf 摸版参数
     *
     * @param result
     * @param recipe
     */
    private void createMedicinePDF(Map<String, List<RecipeLabelVO>> result, RecipeBean recipe) {
        List<RecipeLabelVO> list = result.get("moduleThree");
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<RecipeDetailBean> recipeDetailList = (List<RecipeDetailBean>) list.get(0).getValue();
        for (int i = 0; i < recipeDetailList.size(); i++) {
            RecipeDetailBean d = recipeDetailList.get(i);
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
     * @param result
     * @param recipeMap
     * @param recipe
     */
    private void createChineMedicinePDF(Map<String, List<RecipeLabelVO>> result, Map<String, Object> recipeMap, RecipeBean recipe) {
        List<RecipeLabelVO> list = result.get("moduleThree");
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<RecipeDetailBean> recipeDetailList = (List<RecipeDetailBean>) list.get(0).getValue();
        String drugShowName;
        for (int i = 0; i < recipeDetailList.size(); i++) {
            RecipeDetailBean detail = recipeDetailList.get(i);
            String dTotal;
            if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                dTotal = detail.getUseDoseStr() + detail.getUseDoseUnit();
            } else {
                dTotal = detail.getUseDose() + detail.getUseDoseUnit();
            }
            if (!StringUtils.isEmpty(detail.getMemo())) {
                dTotal = dTotal + "(" + detail.getMemo() + ")";
            }
            drugShowName = detail.getDrugName() + " " + dTotal;
            list.add(new RecipeLabelVO("chineMedicine", "drugInfo" + i, drugShowName));
        }
        RecipeDetailBean detail = recipeDetailList.get(0);
        list.add(new RecipeLabelVO("天数", "tcmUseDay", getUseDays(detail.getUseDaysB(), detail.getUseDays())));
        try {
            list.add(new RecipeLabelVO("用药途径", "tcmUsePathways", DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detail.getUsePathways())));
            list.add(new RecipeLabelVO("用药频次", "tcmUsingRate", DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detail.getUsingRate())));
        } catch (Exception e) {
            logger.error("用药途径 用药频率有误");
        }
        list.add(new RecipeLabelVO("贴数", "copyNum", recipe.getCopyNum() + "贴"));
        RecipeExtend extend = (RecipeExtend) recipeMap.get("recipeExtend");
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

    /**
     * 组织pdf Byte字节 给前端SDK 出餐
     *
     * @param leftX        行坐标
     * @param pdfName      文件名
     * @param pdfBase64Str 文件
     * @return
     */
    private CaSealRequestTO caSealRequestTO(int leftX, String pdfName, String pdfBase64Str) {
        CaSealRequestTO caSealRequest = new CaSealRequestTO();
        caSealRequest.setPdfBase64Str(pdfBase64Str);
        //这个赋值后端没在用 可能前端在使用,所以沿用老代码写法
        caSealRequest.setLeftX(leftX);
        caSealRequest.setLeftY(76);
        caSealRequest.setPdfName("recipe" + pdfName + ".pdf");

        caSealRequest.setSealHeight(40);
        caSealRequest.setSealWidth(40);
        caSealRequest.setPage(1);
        caSealRequest.setPdfMd5("");
        caSealRequest.setMode(1);
        return caSealRequest;
    }

}
