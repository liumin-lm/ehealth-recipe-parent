package recipe.hisservice.syncdata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeLogService;
import recipe.util.DateConversion;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2019/2/18
 * @description： 与监管平台数据同步执行服务
 * @version： 1.0
 */
@RpcBean("syncExecutorService")
public class SyncExecutorService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncExecutorService.class);

    private static final String REGULATION_GD = "gdsjgpt";

    /**
     * 上传批量处方，暂时用不到
     */
    @RpcService
    public void uploadRecipeIndicators() {
        
    }

    /**
     * 上传单个处方
     *
     * @param recipe
     */
    public void uploadRecipeIndicators(Recipe recipe) {
        CommonSyncSupervisionForIHosService iHosService =
                ApplicationUtils.getRecipeService(CommonSyncSupervisionForIHosService.class);
        CommonResponse response = null;
        try {
            //RPC调用上传
            response = iHosService.uploadRecipeIndicators(Arrays.asList(recipe));
            if (CommonConstant.SUCCESS.equals(response.getCode())){
                LOGGER.info("uploadRecipeIndicators rpc execute success. recipeId={}", recipe.getRecipeId());
            } else{
                LOGGER.warn("uploadRecipeIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeIndicators rpc exception recipe={}", JSONUtils.toString(recipe), e);
        }

        //上传openApi的
        CommonSyncSupervisionService service = ApplicationUtils.getRecipeService(CommonSyncSupervisionService.class);
        try {
            response = null;
            response = service.uploadRecipeIndicators(Arrays.asList(recipe));
            if (CommonConstant.SUCCESS.equals(response.getCode())) {
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                //更新字段
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("syncFlag", 1));
                //记录日志
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                        recipe.getStatus(), "监管平台上传成功");
                LOGGER.info("uploadRecipeIndicators openapi execute success. recipeId={}", recipe.getRecipeId());
            }else {
                //记录日志
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                        recipe.getStatus(), "监管平台上传失败,"+response.getMsg());
                LOGGER.warn("uploadRecipeIndicators openapi execute error. recipe={}", JSONUtils.toString(recipe));
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeIndicators openapi exception recipe={}", JSONUtils.toString(recipe), e);
        }


    }

    /**
     * 上传核销信息
     * @param recipeId
     */
    public void uploadRecipeVerificationIndicators(int recipeId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if(null == recipe){
            LOGGER.warn("uploadRecipeVerificationIndicators recipe is null. recipeId={}", recipeId);
            return;
        }

        CommonSyncSupervisionForIHosService iHosService =
                ApplicationUtils.getRecipeService(CommonSyncSupervisionForIHosService.class);
        CommonResponse response = null;
        try {
            //RPC调用上传
            response = iHosService.uploadRecipeVerificationIndicators(Arrays.asList(recipe));
            if (CommonConstant.SUCCESS.equals(response.getCode())){
                LOGGER.info("uploadRecipeVerificationIndicators rpc execute success. recipeId={}", recipe.getRecipeId());
            } else{
                LOGGER.warn("uploadRecipeVerificationIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeVerificationIndicators rpc exception recipe={}", JSONUtils.toString(recipe), e);
        }

        //上传openApi的
        CommonSyncSupervisionService service = ApplicationUtils.getRecipeService(CommonSyncSupervisionService.class);
        try {
            response = null;
            response = service.uploadRecipeVerificationIndicators(Arrays.asList(recipe));
            if (CommonConstant.SUCCESS.equals(response.getCode())){
                //记录日志
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                        recipe.getStatus(), "监管平台上传核销信息成功");
                LOGGER.info("uploadRecipeVerificationIndicators openapi execute success. recipeId={}", recipe.getRecipeId());
            } else{
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                        recipe.getStatus(), "监管平台上传核销信息失败,"+response.getMsg());
                LOGGER.warn("uploadRecipeVerificationIndicators openapi execute error. recipe={}", JSONUtils.toString(recipe));
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeVerificationIndicators openapi exception recipe={}", JSONUtils.toString(recipe), e);
        }

    }

    /**
     * 广东省定时上传接口（批量上传）每天0点上传
     *
     */
    @RpcService
    public void uploadRecipeIndicatorsTimeTask() {
        LOGGER.info("uploadRecipeIndicatorsTimeTask start");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //获取当前时间
        Calendar now = Calendar.getInstance();
        Date time = now.getTime();
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(time, DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(1), DateConversion.DEFAULT_DATE_TIME);

        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        //获取所有监管平台机构列表
        List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
        if (CollectionUtils.isNotEmpty(list)){
            List<Integer> organs = Lists.newArrayList();
            for (ServiceConfigResponseTO serviceConfigResponseTO : list){
                if (REGULATION_GD.equals(serviceConfigResponseTO.getRegulationAppDomainId())){
                    organs.add(serviceConfigResponseTO.getOrganid());
                }
            }
            List<Recipe> recipeList = recipeDAO.findRecipeListForDate(organs, startDt, endDt);
            LOGGER.info("uploadRecipeIndicatorsTimeTask size="+recipeList.size());
            HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
            CommonResponse response = null;
            try {
                for (Recipe recipe : recipeList){
                    if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus()
                        && recipe.getSyncFlag() == 1){
                        continue;
                    }
                    response = service.uploadRecipeIndicators(Arrays.asList(recipe));
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
                }
            } catch (Exception e) {
                LOGGER.warn("uploadRecipeIndicators exception ", e);
            }
        }


    }

}
