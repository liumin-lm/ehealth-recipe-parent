package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeLogService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * created by shiyuping on 2019/6/13
 */
public class PushRecipeToRegulationCallable implements Callable<String> {

    private Logger logger = LoggerFactory.getLogger(PushRecipeToRegulationCallable.class);
    /**
     * 江苏省监管平台
     */
    private static final String REGULATION_JS = "jssjgpt";
    /**
     * 浙江省监管平台
     */
    private static final String REGULATION_ZJ = "zjsjgpt";

    private Integer recipeId;

    private Integer status;

    public PushRecipeToRegulationCallable(Integer recipeId,Integer status) {
        this.recipeId = recipeId;
        this.status = status;
    }

    @Override
    public String call() throws Exception {
        if (recipeId==null){
            return null;
        }
        logger.info("uploadRecipeIndicators start");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        CommonResponse response = null;
        //获取所有监管平台机构列表
        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
        try {
            for (ServiceConfigResponseTO serviceConfigResponseTO : list){
                if (REGULATION_JS.equals(serviceConfigResponseTO.getRegulationAppDomainId())
                        && (serviceConfigResponseTO.getOrganid().equals(recipe.getClinicOrgan()))){
                    //江苏省推送处方规则：（1）如果没有审核直接推送处方数据、（2）status=2表示审核了，则推送处方审核后的数据，（3）审核数据推送成功后再推送处方流转数据
                    if (status == 2){
                        response = service.uploadRecipeAuditIndicators(Arrays.asList(recipe));
                        if (CommonConstant.SUCCESS.equals(response.getCode())){
                            /*if (RecipeStatusConstant.CHECK_PASS_YS==recipe.getStatus()){*/
                                response = service.uploadRecipeCirculationIndicators(Arrays.asList(recipe));
                        } else{
                            logger.warn("uploadRecipeAuditIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
                        }
                    } else {
                        response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                    }
                } else if (REGULATION_ZJ.equals(serviceConfigResponseTO.getRegulationAppDomainId())
                        && (serviceConfigResponseTO.getOrganid().equals(recipe.getClinicOrgan()))) {
                    //浙江省推送处方规则：（1）将status=2 处方审核后的数据推送给监管平台，不会推送审核中、流传的数据
                    if (status == 2 && RecipeStatusConstant.CHECK_PASS_YS==recipe.getStatus()) {
                        response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                    }
                }

            }
        } catch (Exception e) {
            logger.warn("uploadRecipeIndicators exception recipe={}", JSONUtils.toString(recipe), e);
        }
        logger.info("uploadRecipeIndicators res={}",response);
        if (CommonConstant.SUCCESS.equals(response.getCode())) {
            //更新字段
            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("syncFlag", 1));
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                    recipe.getStatus(), "监管平台上传成功");
        }else{
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                    recipe.getStatus(), "监管平台上传失败,"+response.getMsg());
        }
        return null;
    }
}
