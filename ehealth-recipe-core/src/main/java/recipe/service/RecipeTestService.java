package recipe.service;

import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.OrganService;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.entity.Recipe;
import ctd.account.session.ClientSession;
import ctd.net.broadcast.MQHelper;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.msg.constant.MqConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;
import recipe.mq.OnsConfig;
import recipe.util.RecipeMsgUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author yu_yun
 * @date 2016/7/13
 * 用于测试处方流程
 */
@RpcBean(value = "recipeTestService", mvc_authentication = false)
public class RecipeTestService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestService.class);

    @RpcService
    public String testanyway() {
        ClientSession clientSession = ClientSession.getCurrent();
        return JSONUtils.toString(clientSession);
    }

    @RpcService
    public int checkPassFail(Integer recipeId, Integer errorCode, String msg) {
        HisCallBackService.checkPassFail(recipeId, errorCode, msg);
        return 0;
    }

    /**
     * 测试用-将处方单改成已完成状态
     */
    @RpcService
    public int changeRecipeToFinish(String recipeCode, int organId) {
        HisCallBackService.finishRecipesFromHis(Arrays.asList(recipeCode), organId);
        return 0;
    }

    @RpcService
    public int changeRecipeToPay(String recipeCode, int organId) {
        HisCallBackService.havePayRecipesFromHis(Arrays.asList(recipeCode), organId);
        return 0;
    }

    @RpcService
    public int changeRecipeToHisFail(Integer recipeId) {
        HisCallBackService.havePayFail(recipeId);
        return 0;
    }

    @RpcService
    public void testSendMsg(String bussType, Integer bussId, Integer organId) {
        SmsInfoBean info = new SmsInfoBean();
        // 业务表主键
        info.setBusId(bussId);
        // 业务类型
        info.setBusType(bussType);
        info.setSmsType(bussType);
        info.setStatus(0);
        // 短信服务对应的机构， 0代表通用机构
        info.setOrganId(organId);
        info.setExtendValue("康复药店");
        info.setExtendWithoutPersist(JSONUtils.toString(Arrays.asList("2c9081814d720593014d758dd0880020")));
        ISmsPushService smsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);
        smsPushService.pushMsg(info);
    }

    @RpcService
    public void testSendMsg4new(Integer bussId, String bussType) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(bussId);
        RecipeMsgService.sendRecipeMsg(RecipeMsgUtils.valueOf(bussType), recipe);
    }


    @RpcService
    public void testSendMsgForRecipe(Integer recipeId, int afterStatus) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeMsgService.batchSendMsg(recipe, afterStatus);
    }

    @RpcService
    public void testSendMqMsg(Integer recipeId, String status) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        NoticeNgariRecipeInfoReq notice = new NoticeNgariRecipeInfoReq();
        Recipe recipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        notice.setOrganId(recipe.getClinicOrgan());
        notice.setRecipeID(recipe.getRecipeCode());
        notice.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
        notice.setRecipeStatus(status);
        MQHelper.getMqPublisher().publish(OnsConfig.hisCdrinfo, notice, MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM);
    }


    @RpcService(timeout = 1000)
    public Map<String, Object> analysisDrugList(List<Integer> drugIdList, int organId, boolean useFile) {
        DrugsEnterpriseTestService testService = new DrugsEnterpriseTestService();
        try {
            return testService.analysisDrugList(drugIdList, organId, useFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @RpcService
    public List<SearchDrugDetailDTO> findDrugListsByNameOrCodePageStaitc(
            Integer organId, int drugType, String drugName, int start) {
        DrugListExtService drugListExtService = ApplicationUtils.getRecipeService(DrugListExtService.class, "drugList");

        return drugListExtService.findDrugListsByNameOrCodePageStaitc(organId, drugType, drugName, start);
    }

    @RpcService
    public void updateCheckerName(){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        List<Recipe> recipes = recipeDAO.findAllRecipeListForChecker();
        for (Recipe recipe : recipes) {
            DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
            if (doctorDTO != null) {
                recipe.setCheckerName(doctorDTO.getName());
                recipeDAO.update(recipe);
            }
        }
    }
}
