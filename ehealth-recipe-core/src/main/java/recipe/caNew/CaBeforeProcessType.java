package recipe.caNew;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.service.manager.RecipeLabelManager;

import java.util.LinkedList;
import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

//JRK
//前置处方签名实现
public class CaBeforeProcessType extends AbstractCaProcessType {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaBeforeProcessType.class);
    //我们将开方的流程拆开：
    //前置CA操作：1.保存处方（公共操作）=》2.触发CA结果=》3.成功后将处方推送到his，推送相关操作

    @Override
    public void signCABeforeRecipeFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        LOGGER.info("Before---signCABeforeRecipeFunction 当前CA执行签名之前特应性行为，入参：recipeBean：{}，detailBeanList：{} ", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList));
        //前置签名，CA前操作，将处方设置成【医生签名中】
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        recipeDAO.updateRecipeInfoByRecipeId(recipeBean.getRecipeId(), RecipeStatusConstant.SIGN_ING_CODE_DOC, null);
    }

    @Override
    public void signCAAfterRecipeCallBackFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        LOGGER.info("Before---signCAAfterRecipeCallBackFunction 当前CA执行签名之后回调特应性行为，入参：recipeBean：{}，detailBeanList：{} ", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList));
        try {
            recipeHisResultBeforeCAFunction(recipeBean, detailBeanList);
        } catch (Exception e) {
            LOGGER.error("CaBeforeProcessType signCAAfterRecipeCallBackFunction recipeBean= {}", JSON.toJSONString(recipeBean), e);
        }
    }

    @Override
    public RecipeResultBean hisCallBackCARecipeFunction(Integer recipeId) {
        LOGGER.info("Before---当前CA执行his回调之后组装CA响应特应性行为，入参：recipeId：{}", recipeId);
        //这里前置，触发CA时机在推送之前,这里判定his之后的为签名成功
        RecipeResultBean recipeResultBean = new RecipeResultBean();
        recipeResultBean.setCode(RecipeResultBean.SUCCESS);
        try {
            addRecipeCodeAndPatientForRecipePdf(recipeId);
        } catch (Exception e) {
            LOGGER.error("addRecipeCodeAndPatientForRecipePdf error recipeId={}", recipeId, e);
        }
        LOGGER.info("Before---当前CA执行his回调之后组装CA响应特应性行为，出参：recipeId：{}，{}", recipeId, JSON.toJSONString(recipeResultBean));
        //将返回的CA结果给处方，设置处方流转
        return recipeResultBean;
    }


    /**
     * 新版本前置CA his回调之后给处方pdf添加处方号和患者病历号
     *
     * @param recipeId
     */
    private void addRecipeCodeAndPatientForRecipePdf(Integer recipeId) throws Exception {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            return;
        }
        String barcode = "";

        RecipeLabelManager recipeLabelManager = AppContextHolder.getBean("recipeLabelManager", RecipeLabelManager.class);
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
        CoOrdinateVO patientId = recipeLabelManager.getPdfCoordsHeight(recipeId, "recipe.patientID");
        if (null != patientId) {
            patientId.setValue(recipe.getPatientID());
            coOrdinateList.add(patientId);
        }
        CoOrdinateVO recipeCode = recipeLabelManager.getPdfCoordsHeight(recipeId, "recipe.recipeCode");
        if (null != recipeCode) {
            recipeCode.setValue(recipe.getRecipeCode());
            coOrdinateList.add(recipeCode);
        }
        String newPdf = CreateRecipePdfUtil.generateRecipeCodeAndPatientIdForRecipePdf(recipe.getSignFile(), coOrdinateList, barcode);
        if (StringUtils.isNotEmpty(newPdf)) {
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipeId);
            recipeUpdate.setSignFile(newPdf);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        }
        LOGGER.info("addRecipeCodeAndPatientForRecipePdf  recipeId={},newPdf={}", recipeId, newPdf);
    }
}