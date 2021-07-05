package recipe.caNew.pdf;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.SignRecipePdfVO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.openapi.request.province.SignImgNode;
import recipe.caNew.pdf.service.CreatePdfService;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeDAO;
import recipe.service.client.IConfigurationClient;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;

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


    /**
     * 获取pdf oss id
     *
     * @param recipe
     * @return
     */
    public void queryPdfOssId(Recipe recipe) {
        if (ValidateUtil.validateObjects(recipe, recipe.getRecipeId(), recipe.getClinicOrgan())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "参数错误");
        }
        CreatePdfService createPdfService = createPdfService(recipe);
        SignRecipePdfVO signRecipePdfVO = createPdfService.queryPdfOssId(recipe);

        String fileId = CreateRecipePdfUtil.signFileByte(signRecipePdfVO.getData(), "fileName");
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        recipeUpdate.setSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
    }

    /**
     * 获取pdf byte 格式
     *
     * @param recipe
     * @return
     */
    public CaSealRequestTO queryPdfByte(Integer recipeId) {
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        return createPdfService.queryPdfByte(recipe);
    }


    /**
     * 获取药师 pdf byte 格式
     *
     * @param recipe
     * @return
     */
    public CaSealRequestTO queryCheckPdfByte(Integer recipeId) {
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        return createPdfService.queryCheckPdfByte(recipe);
    }



    /**
     * 药师签名
     *
     * @param recipeId
     */
    public void updateCheckNamePdf(Integer recipeId) {
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        String fileId = createPdfService.updateCheckNamePdf(recipe);

        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipeId);
        recipeUpdate.setChemistSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("CreatePdfFactory updateCheckNamePdf  recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
    }

    /**
     * 医生签名
     *
     * @param recipe
     */
    public void updateDoctorNamePdf(Recipe recipe) {
        CreatePdfService createPdfService = createPdfService(recipe);
        String fileId = createPdfService.updateDoctorNamePdf(recipe);

        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        recipeUpdate.setSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("CreatePdfFactory updateDoctorNamePdf recipeUpdate={}", JSON.toJSONString(recipeUpdate));
    }

    /**
     * 处方金额
     *
     * @param recipeId
     * @param recipeFee
     */
    public void updateTotalPdfExecute(Integer recipeId, BigDecimal recipeFee) {
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        RecipeBusiThreadPool.execute(() -> createPdfService.updateTotalPdf(recipeId, recipeFee));
    }

    /**
     * pdf 处方号和患者病历号
     *
     * @param recipeId
     */
    public void updateCodePdfExecute(Integer recipeId) {
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        RecipeBusiThreadPool.execute(() -> {
                    String fileId = createPdfService.updateCodePdf(recipe);
                    Recipe recipeUpdate = new Recipe();
                    recipeUpdate.setRecipeId(recipeId);
                    recipeUpdate.setSignFile(fileId);
                    recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
                    logger.info("CreatePdfFactory updateCodePdfExecute recipeUpdate ={}", JSON.toJSONString(recipeUpdate));
                }
        );
    }

    /**
     * 支付成功后修改pdf 添加收货人信息/煎法
     *
     * @param recipeId
     */
    public void updateAddressPdfExecute(Integer recipeId) {
        Recipe recipe = validate(recipeId);
        CreatePdfService createPdfService = createPdfService(recipe);
        RecipeBusiThreadPool.execute(() -> createPdfService.updateAddressPdf(recipe));
    }

    /**
     * pdf 核对发药
     *
     * @param recipe 处方
     * @return
     */
    public void updateGiveUser(Recipe recipe) {
        CreatePdfService createPdfService = createPdfService(recipe);
        Recipe recipeUpdate = createPdfService.updateGiveUser(recipe);
        //更新处方字段
        if (null != recipeUpdate) {
            recipe.setSignImg(recipeUpdate.getSignImg());
            recipe.setChemistSignFile(recipeUpdate.getChemistSignFile());
        }
        recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
        logger.info("CreatePdfFactory updateGiveUser recipe ={}", JSON.toJSONString(recipe));
    }


    /**
     * pdf 转 图片
     *
     * @param recipeId
     * @return
     */
    public void updatePdfToImg(Integer recipeId) {
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
            }
        });
    }

    /**
     * pdf 机构印章
     *
     * @param recipeId
     */
    public void updatesealPdfExecute(Integer recipeId) {
        RecipeBusiThreadPool.execute(() -> updatesealPdf(recipeId));
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
        String fileId = null;
        try {
            SignImgNode signImgNode = new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString(),
                    organSealId, recipe.getChemistSignFile(), null, 90F, 90F, 160f, 490f, false);
            fileId = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
        } catch (Exception e) {
            logger.error("GenerateSignetRecipePdfRunable error recipeId={}, e={}", recipeId, e);
        }
        if (StringUtils.isEmpty(fileId)) {
            return;
        }
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipeId);
        recipeUpdate.setChemistSignFile(fileId);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("GenerateSignetRecipePdfRunable end recipeUpdate={}", JSON.toJSONString(recipeUpdate));
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
        String organSealId = configurationClient.getValueCatch(recipe.getClinicOrgan(), "xxxxxxpdf", "");
        CreatePdfService createPdfService;
        if (StringUtils.isNotEmpty(organSealId)) {
            createPdfService = customCreatePdfServiceImpl;
        } else {
            createPdfService = platformCreatePdfServiceImpl;
        }
        return createPdfService;
    }
}
