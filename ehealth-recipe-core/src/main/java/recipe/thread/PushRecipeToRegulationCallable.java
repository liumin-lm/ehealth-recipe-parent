package recipe.thread;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
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
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeLogService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private List<Integer> recipeIds;

    /**
     * 2-处方审核后推送 1-开处方或者取消处方推送
     */
    private Integer status;

    public PushRecipeToRegulationCallable(List<Integer> recipeIds,Integer status) {
        this.recipeIds = recipeIds;
        this.status = status;
    }

    @Override
    public String call() throws Exception {
        if (CollectionUtils.isEmpty(recipeIds)){
            return null;
        }
        logger.info("uploadRecipeIndicators start recipeIds={},status={}", JSON.toJSONString(recipeIds), status);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        //获取所有监管平台机构列表
        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        for (Integer recipeId : recipeIds) {
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            CommonResponse response = null;

            List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
            Map<Integer,String> regulationOrgan = new HashMap<>(list.size());
            for (ServiceConfigResponseTO serviceConfigResponseTO : list){
                regulationOrgan.put(serviceConfigResponseTO.getOrganid(),serviceConfigResponseTO.getRegulationAppDomainId());
            }
            logger.info("uploadRecipeIndicators regulationOrgan:{}", JSONUtils.toString(regulationOrgan));
            Boolean flag = false;
            //默认1-开处方就上传审方后再上传  2-审方后再上传
            Integer uploadRegulationWay = 1;
            try {
                //各个状态都推送给前置机 由前置机判断什么状态的处方推哪个监管平台
                String domainId = regulationOrgan.get(recipe.getClinicOrgan());
                if (CollectionUtils.isNotEmpty(list) && StringUtils.isNotEmpty(domainId)){
                    try {
                        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                        uploadRegulationWay = (Integer) configurationService.getConfiguration(recipe.getClinicOrgan(), "uploadRegulationRecipeWay");
                    }catch (Exception e){
                        logger.error("获取运营平台处方上传监管平台方式",e);
                    }
                    if (domainId.startsWith(REGULATION_ZJ)){
                        //浙江省推送处方规则：（1）将status=2 处方审核后的数据推送给监管平台，不会推送审核中、流传的数据
                        //审核后推送
                        //互联网网模式下--审核通过后是待处理状态
                        if (status == 2 && canUploadByReviewType(recipe)) {
                            response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                            flag = true;
                        }
                    }else{
                        //江苏省处方开立，处方审核处方流转都用同一个接口，由前置机转换数据(可根据处方状态判断)
                        //除浙江省之外的都直接推
                        if (uploadRegulationWay == 2 && status == 1){
                            //配置了审方后上传 status=1时不上传
                            return null;
                        }
                        response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                    }
                }else {
                    //互联网模式
                    if (status == 2 && canUploadByReviewType(recipe)) {
                        response = service.uploadRecipeIndicators(Arrays.asList(recipe));
                        flag = true;
                    }
                }
            } catch (Exception e) {
                logger.error("uploadRecipeIndicators exception recipe={}", JSONUtils.toString(recipe), e);
            }
            logger.info("uploadRecipeIndicators res={}",response);
            if (response != null){
                if (CommonConstant.SUCCESS.equals(response.getCode())) {
                    //更新字段
                    recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("syncFlag", 1));
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "监管平台上传成功");
                }else{
                    //记录日志-暂时只处理浙江省的
                    //由于有些监管平台不是这里主动推送的，也会导致上传失败，不需要在运营平台展示
                    if (flag){
                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "监管平台上传失败," + response.getMsg());
                    }

                }
            }
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
