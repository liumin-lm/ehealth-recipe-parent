package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.constant.CacheConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeServiceSub;
import recipe.util.RedisClient;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * created by shiyuping on 2019/6/13
 * @author shiyuping
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

    /**
     * 福建省监管平台(通过前置机来平台查询处方)
     */
    private static final String REGULATION_FJ = "fjsjgpt";

    private Integer recipeId;

    /**
     * 2-处方审核后推送 1-开处方或者取消处方推送
     */
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
        Map<Integer,String> regulationOrgan = new HashMap<>(list.size());
        for (ServiceConfigResponseTO serviceConfigResponseTO : list){
            regulationOrgan.put(serviceConfigResponseTO.getOrganid(),serviceConfigResponseTO.getRegulationAppDomainId());
        }
        logger.info("uploadRecipeIndicators regulationOrgan:"+JSONUtils.toString(list));
        try {
            //各个状态都推送给前置机 由前置机判断什么状态的处方推哪个监管平台
            if (CollectionUtils.isNotEmpty(list)){
                String domainId = regulationOrgan.get(recipe.getClinicOrgan());
                boolean flag = true;
                if (StringUtils.isNotEmpty(domainId) && domainId.startsWith(REGULATION_JS)){
                    //江苏省推送处方规则：（1）如果没有审核直接推送处方数据、（2）status=2表示审核了，则推送处方审核后的数据，（3）审核数据推送成功后再推送处方流转数据
                    /*if (status == 2) {
                        response = service.uploadRecipeAuditIndicators(Arrays.asList(recipe));
                        if (CommonConstant.SUCCESS.equals(response.getCode())) {
                            //if (RecipeStatusConstant.CHECK_PASS_YS==recipe.getStatus()){
                            response = service.uploadRecipeCirculationIndicators(Arrays.asList(recipe));
                        } else {
                            logger.warn("uploadRecipeAuditIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
                        }
                    } */
                    //处方开立，处方审核处方流转都用同一个接口，由前置机转换数据(可根据处方状态判断)
                    response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                }else {
                    //浙江省推送处方规则：（1）将status=2 处方审核后的数据推送给监管平台，不会推送审核中、流传的数据
                    //审核后推送
                    //互联网网模式下--审核通过后是待处理状态
                    if (status == 2 && canUploadByReviewType(recipe)) {
                        response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                        flag = false;
                    }
                }
                //从缓存中取机构列表上传--可配置
                RedisClient redisClient = RedisClient.instance();
                Set<String> organIdList = redisClient.sMembers(CacheConstant.UPLOAD_OPEN_RECIPE_LIST);
                if (organIdList != null && organIdList.contains(recipe.getClinicOrgan().toString())&&flag){
                    response = service.uploadRecipeIndicators(Arrays.asList(recipe));
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

    private boolean canUploadByReviewType(Recipe recipe) {
        //后置-审核通过7  or  前置/不需要审核-待处理2
        switch (recipe.getStatus()){
            case RecipeStatusConstant.CHECK_PASS_YS:
                if (ReviewTypeConstant.Postposition_Check.equals(recipe.getReviewType())){
                    return true;
                }
            case RecipeStatusConstant.CHECK_PASS:
                if (!ReviewTypeConstant.Postposition_Check.equals(recipe.getReviewType())){
                    return true;
                }
            default:
                return false;
        }
    }
}
