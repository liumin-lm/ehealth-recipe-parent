package recipe.service;


import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.service.common.RecipeCacheService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuzq on 2019/2/18.
 * 医生端开处方弹框提示风险规避文案与标题
 */
@RpcBean(value = "recipeTipConfigService", mvc_authentication = false)
public class RecipeTipConfigService {
    private final static Logger logger = LoggerFactory.getLogger(RecipeTipConfigService.class);
    /**
     *
     * @param tip  base_param表文案的key
     * @param title base_param表文案标题的key
     * @return map(文案,标题)
     */
    @RpcService
    public Map<String,Object> getDoctorRiskTip(String tip, String title){
        RecipeCacheService recipeCacheService= ApplicationUtils.getRecipeService(RecipeCacheService.class);
        String doctorRiskTip=recipeCacheService.getParam(tip);
        String doctorRiskTipTitle=recipeCacheService.getParam(title);
        Map<String,Object> map=new HashMap<>(2);
        map.put("doctorRiskTip",doctorRiskTip);
        map.put("doctorRiskTipTitle",doctorRiskTipTitle);
        return map;
    }
}
