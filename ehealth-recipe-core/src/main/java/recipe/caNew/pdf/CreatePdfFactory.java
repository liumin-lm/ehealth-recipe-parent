package recipe.caNew.pdf;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.dto.AttachSealPicDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.SignImgNode;
import recipe.caNew.pdf.service.CreatePdfService;
import recipe.client.IConfigurationClient;
import recipe.constant.ErrorCode;
import recipe.constant.OperationConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.manager.SignManager;
import recipe.service.RecipeLogService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static recipe.util.DictionaryUtil.getDictionary;

/**
 * pdf 构建工厂类
 *
 * @author fuzi
 */
@Service
public class CreatePdfFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource(name = "platformCreatePdfServiceImpl")
    private CreatePdfService platformCreatePdfServiceImpl;
    @Resource(name = "customCreatePdfServiceImpl")
    private CreatePdfService customCreatePdfServiceImpl;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO orderDAO;
    @Autowired
    private SignManager signManager;

    /**
     * 获取pdf oss id
     *
     * @param recipe
     * @return
     */
    public void queryPdfOssId(Recipe recipe) {
        logger.info("CreatePdfFactory queryPdfOssId recipe:{}", recipe.getRecipeId());
        CreatePdfService createPdfService = createPdfService(recipe);
        try {
            SignRecipePdfVO signRecipePdfVO = createPdfService.queryPdfOssId(recipe);
            if (null == signRecipePdfVO) {
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "获取pdf_oss_id格式生成失败");
                return;
            }
            String fileId = CreateRecipePdfUtil.signFileByte(signRecipePdfVO.getData(), "fileName");
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipe.getRecipeId());
            recipeUpdate.setSignFile(fileId);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        } catch (Exception e) {
            logger.error("CreatePdfFactory queryPdfOssId 使用平台医生部分pdf的,生成失败 recipe:{}", recipe.getRecipeId(), e);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "获取pdf_oss_id格式生成失败");
        }
    }

    /**
     * 获取pdf byte 格式
     *
     * @param recipeId
     * @return
     */
    public CaSealRequestTO queryPdfByte(Integer recipeId) {
        logger.info("CreatePdfFactory queryPdfByte recipe:{}", recipeId);
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        try {
            CaSealRequestTO caSealRequest = createPdfService.queryPdfByte(recipe);
            if (null == caSealRequest) {
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "获取pdf_byte格式生成失败");
            }
            return caSealRequest;
        } catch (Exception e) {
            logger.error("CreatePdfFactory updateDoctorNamePdf 使用平台医生部分pdf的,生成失败 recipe:{}", recipe.getRecipeId(), e);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "获取pdf_byte格式生成失败");
            return null;
        }
    }


    /**
     * 医生签名
     *
     * @param recipe
     */
    public void updateDoctorNamePdf(Recipe recipe) {
        logger.info("CreatePdfFactory updateDoctorNamePdf recipe:{}", recipe.getRecipeId());
        try {
            boolean usePlatform = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeUsePlatformCAPDF", true);
            if (!usePlatform) {
                return;
            }
            //设置签名图片
            AttachSealPicDTO sttachSealPicDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipe.getRecipeId());
            SignImgNode signImgNode = new SignImgNode();
            signImgNode.setRecipeId(recipe.getRecipeId().toString());
            signImgNode.setSignImgFileId(sttachSealPicDTO.getDoctorSignImg());
            signImgNode.setHeight(20f);
            signImgNode.setWidth(40f);
            signImgNode.setRepeatWrite(false);
            CreatePdfService createPdfService = createPdfService(recipe);
            String fileId = createPdfService.updateDoctorNamePdf(recipe, signImgNode);
            if (StringUtils.isEmpty(fileId)) {
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "平台医生部分pdf的生成失败");
                return;
            }
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipe.getRecipeId());
            recipeUpdate.setSignFile(fileId);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            logger.info("CreatePdfFactory updateDoctorNamePdf recipeUpdate={}", JSON.toJSONString(recipeUpdate));
        } catch (Exception e) {
            logger.error("CreatePdfFactory updateDoctorNamePdf 使用平台医生部分pdf的,生成失败 recipe:{}", recipe.getRecipeId(), e);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "平台医生部分pdf的生成失败");
        }
    }

    /**
     * 获取药师 pdf byte 格式
     *
     * @param recipeId 处方id
     * @return
     */
    public CaSealRequestTO queryCheckPdfByte(Integer recipeId) {
        logger.info("CreatePdfFactory queryCheckPdfByte recipeId:{}", recipeId);
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        CaSealRequestTO caSealRequestTO = createPdfService.queryCheckPdfByte(recipe);
        if (null == caSealRequestTO) {
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "获取药师pdf_byte格式生成失败");
        }
        logger.info("CreatePdfFactory queryCheckPdfByte caSealRequestTO:{}", JSON.toJSONString(caSealRequestTO));
        return caSealRequestTO;
    }



    /**
     * 药师签名
     *
     * @param recipeId
     */
    public void updateCheckNamePdf(Integer recipeId) {
        Recipe recipe = validate(recipeId);
        logger.info("CreatePdfFactory updateCheckNamePdf recipeId:{}", recipeId);
        boolean usePlatform = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeUsePlatformCAPDF", true);
        if (!usePlatform) {
            return;
        }
        //获取签名图片
        AttachSealPicDTO sttachSealPicDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipeId);
        String signImageId = sttachSealPicDTO.getCheckerSignImg();
        try {
            CreatePdfService createPdfService = createPdfService(recipe);
            String fileId = createPdfService.updateCheckNamePdf(recipe, signImageId);
            if (StringUtils.isEmpty(fileId)) {
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "药师签名部分生成失败");
                return;
            }
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipeId);
            recipeUpdate.setChemistSignFile(fileId);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            logger.info("CreatePdfFactory updateCheckNamePdf  recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
        } catch (Exception e) {
            logger.error("CreatePdfFactory updateCheckNamePdf  recipe: {}", recipe.getRecipeId());
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "平台药师部分pdf的生成失败");
        }
    }

    /**
     * 处方金额
     *
     * @param recipeId
     * @param recipeFee
     */
    public void updateTotalPdfExecute(Integer recipeId, BigDecimal recipeFee) {
        logger.info("CreatePdfFactory updateTotalPdfExecute recipeId:{},recipeFee:{}", recipeId, recipeFee);
        if (null == recipeFee) {
            logger.warn("CreatePdfFactory updateTotalPdfExecute recipeFee is null");
            return;
        }
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        RecipeBusiThreadPool.execute(() -> {
            CoOrdinateVO coords = createPdfService.updateTotalPdf(recipe, recipeFee);
            logger.info("CreatePdfFactory updateTotalPdfExecute  coords ={}", JSON.toJSONString(coords));
            if (null == coords) {
                return;
            }
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
                logger.error("CreatePdfFactory updateTotalPdfExecute  error recipeId={}", recipeId, e);
                return;
            }
            if (StringUtils.isNotEmpty(fileId)) {
                recipeUpdate.setRecipeId(recipeId);
                recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            }
        });
    }

    /**
     * pdf 处方号和患者病历号
     *
     * @param recipeId
     */
    public void updateCodePdfExecute(Integer recipeId) {
        logger.info("CreatePdfFactory updateCodePdfExecute recipeId:{}", recipeId);
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        RecipeBusiThreadPool.execute(() -> {
            try {
                String fileId = createPdfService.updateCodePdf(recipe);
                logger.info("CreatePdfFactory updateCodePdfExecute fileId ={}", fileId);
                if (StringUtils.isEmpty(fileId)) {
                    return;
                }
                Recipe recipeUpdate = new Recipe();
                recipeUpdate.setRecipeId(recipeId);
                recipeUpdate.setSignFile(fileId);
                recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            } catch (Exception e) {
                logger.error("CreatePdfFactory updateCodePdfExecute error！recipeId:{}", recipeId, e);
            }
        });
    }

    /**
     * 支付成功后修改pdf 添加收货人信息/煎法
     *
     * @param recipeId
     */
    public void updateAddressPdfExecute(Integer recipeId) {
        logger.info("CreatePdfFactory updateAddressPdfExecute recipeId:{}", recipeId);
        RecipeOrder order = orderDAO.getRelationOrderByRecipeId(recipeId);
        if (null == order) {
            logger.warn("CreatePdfFactory updateAddressPdfExecute   order is null  recipeId={}", recipeId);
            return;
        }
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        RecipeBusiThreadPool.execute(() -> {
            try {
                List<CoOrdinateVO> list = createPdfService.updateAddressPdf(recipe, order, getCompleteAddress(order));
                logger.info("CreatePdfFactory updateAddressPdfExecute list ={}", JSON.toJSONString(list));
                if (CollectionUtils.isEmpty(list)) {
                    return;
                }
                Recipe recipeUpdate = new Recipe();
                String fileId = null;
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    fileId = CreateRecipePdfUtil.generateOrdinateList(recipe.getSignFile(), list);
                    recipeUpdate.setChemistSignFile(fileId);
                } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                    fileId = CreateRecipePdfUtil.generateOrdinateList(recipe.getSignFile(), list);
                    recipeUpdate.setChemistSignFile(fileId);
                }
                if (StringUtils.isNotEmpty(fileId)) {
                    recipeUpdate.setRecipeId(recipe.getRecipeId());
                    recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
                }
            } catch (Exception e) {
                logger.error("CreatePdfFactory updateAddressPdfExecute  recipe: {}", recipe.getRecipeId(), e);
            }
        });
    }

    /**
     * pdf 核对发药
     *
     * @param recipe 处方
     * @return
     */
    public void updateGiveUser(Recipe recipe) {
        logger.info("CreatePdfFactory updateGiveUser recipe={}", JSON.toJSONString(recipe));
        //获取 核对发药药师签名id
        ApothecaryDTO apothecaryDTO = signManager.giveUser(recipe.getClinicOrgan(), recipe.getGiveUser(), recipe.getRecipeId());
        if (StringUtils.isEmpty(apothecaryDTO.getGiveUserSignImg())) {
            return;
        }
        //判断发药状态
        if (StringUtils.isEmpty(recipe.getOrderCode())) {
            return;
        }
        RecipeOrder recipeOrder = orderDAO.getByOrderCode(recipe.getOrderCode());
        if (null == recipeOrder || null == recipeOrder.getDispensingTime()) {
            return;
        }
        //获取pdf坐标
        CreatePdfService createPdfService = createPdfService(recipe);
        SignImgNode signImgNode = createPdfService.updateGiveUser(recipe);
        if (null == signImgNode) {
            return;
        }
        signImgNode.setSignImgFileId(apothecaryDTO.getGiveUserSignImg());
        String fileId = null;
        Recipe recipeUpdate = new Recipe();
        try {
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                signImgNode.setSignFileId(recipe.getChemistSignFile());
                fileId = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
                recipeUpdate.setChemistSignFile(fileId);
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                signImgNode.setSignFileId(recipe.getSignFile());
                fileId = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
                recipeUpdate.setSignFile(fileId);
            }
        } catch (Exception e) {
            logger.error("CreatePdfFactory updateGiveUser  recipe: {}", recipe.getRecipeId(), e);
            return;
        }
        logger.info("CreatePdfFactory updateGiveUser recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
        if (StringUtils.isNotEmpty(fileId)) {
            recipeUpdate.setRecipeId(recipe.getRecipeId());
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        }
    }


    /**
     * pdf 转 图片
     *
     * @param recipeId
     * @return
     */
    public void updatePdfToImg(Integer recipeId) {
        logger.info("CreatePdfFactory updatePdfToImg recipeId:{}", recipeId);
        if (ValidateUtil.validateObjects(recipeId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "参数错误");
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (ValidateUtil.validateObjects(recipe, recipe.getSignFile())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "参数错误");
        }
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            return;
        }
        RecipeBusiThreadPool.execute(() -> {
            try {
                String imageFile = CreateRecipePdfUtil.updatePdfToImg(recipe.getRecipeId(), recipe.getSignFile());
                if (StringUtils.isNotEmpty(imageFile)) {
                    Recipe recipeUpdate = new Recipe();
                    recipeUpdate.setRecipeId(recipeId);
                    recipeUpdate.setSignImg(imageFile);
                    recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
                }
            } catch (Exception e) {
                logger.error("CreatePdfFactory updatePdfToImg error", e);
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "pdf转图片生成失败");
            }
        });
    }

    /**
     * pdf 机构印章
     *
     * @param recipeId
     */
    public void updatesealPdfExecute(Integer recipeId) {
        logger.info("CreatePdfFactory updatesealPdfExecute recipeId:{}", recipeId);
        RecipeBusiThreadPool.execute(() -> updatesealPdf(recipeId));
    }


    /**
     * 组织pdf Byte字节 给前端SDK 出餐
     *
     * @param leftX        行坐标
     * @param pdfName      文件名
     * @param pdfBase64Str 文件
     * @return
     */
    public static CaSealRequestTO caSealRequestTO(int leftX, int leftY, String pdfName, String pdfBase64Str) {
        CaSealRequestTO caSealRequest = new CaSealRequestTO();
        caSealRequest.setPdfBase64Str(pdfBase64Str);
        //这个赋值后端没在用 可能前端在使用,所以沿用老代码写法
        caSealRequest.setLeftX(leftX);
        caSealRequest.setLeftY(leftY);
        caSealRequest.setPdfName("recipe" + pdfName + ".pdf");

        caSealRequest.setSealHeight(40);
        caSealRequest.setSealWidth(40);
        caSealRequest.setPage(1);
        caSealRequest.setPdfMd5("");
        caSealRequest.setMode(1);
        return caSealRequest;
    }

    private void updatesealPdf(Integer recipeId) {
        logger.info("GenerateSignetRecipePdfRunable start recipeId={}", recipeId);
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe || StringUtils.isEmpty(recipe.getChemistSignFile())) {
            logger.info("GenerateSignetRecipePdfRunable recipe is null");
            return;
        }
        //获取配置--机构印章
        String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), "recipeUsePlatformCAPDF", "");
        if (StringUtils.isEmpty(organSealId)) {
            logger.info("GenerateSignetRecipePdfRunable organSeal is null");
            return;
        }
        try {
            SignImgNode signImgNode = new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString(),
                    organSealId, recipe.getChemistSignFile(), null, 90F, 90F, 160f, 490f, false);
            String fileId = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            if (StringUtils.isEmpty(fileId)) {
                return;
            }

            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipeId);
            recipeUpdate.setChemistSignFile(fileId);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            logger.info("GenerateSignetRecipePdfRunable end recipeUpdate={}", JSON.toJSONString(recipeUpdate));
        } catch (Exception e) {
            logger.error("GenerateSignetRecipePdfRunable error recipeId={}, e={}", recipeId, e);
        }
    }

    private Recipe validate(Integer recipeId) {
        if (ValidateUtil.validateObjects(recipeId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "参数错误");
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (ValidateUtil.validateObjects(recipe, recipe.getRecipeId(), recipe.getClinicOrgan())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "参数错误");
        }
        return recipe;
    }

    /**
     * 判断使用那个实现类
     *
     * @param recipe
     * @return
     */
    private CreatePdfService createPdfService(Recipe recipe) {
        String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), OperationConstant.OP_CONFIG_PDF, "");
        CreatePdfService createPdfService;
        if (StringUtils.isNotEmpty(organSealId)) {
            createPdfService = customCreatePdfServiceImpl;
        } else {
            createPdfService = platformCreatePdfServiceImpl;
        }
        return createPdfService;
    }


    private String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            address.append(getDictionary("eh.base.dictionary.AddrArea", order.getAddress1()));
            address.append(getDictionary("eh.base.dictionary.AddrArea", order.getAddress2()));
            address.append(getDictionary("eh.base.dictionary.AddrArea", order.getAddress3()));
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }

}
