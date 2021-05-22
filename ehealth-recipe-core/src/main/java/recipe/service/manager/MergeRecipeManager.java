package recipe.service.manager;

import com.google.common.collect.Maps;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2021\5\17 0017 19:52
 */
@Service
public class MergeRecipeManager {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IConfigurationCenterUtilsService configService;

    public Map<String, Object> getMergeRecipeSetting(){
        Map<String, Object> result = Maps.newHashMap();
        //默认
        Boolean mergeRecipeFlag = true;
        String mergeRecipeWayAfter = "e.registerId";
        try {
            //获取是否合并处方的配置--区域公众号如果有一个没开就默认全部关闭
            ICurrentUserInfoService currentUserInfoService = AppDomainContext.getBean("eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
            List<Integer> organIds = currentUserInfoService.getCurrentOrganIds();
            LOGGER.info("MergeRecipeManager organIds={}", JSONUtils.toString(organIds));
            if (CollectionUtils.isNotEmpty(organIds)) {
                for (Integer organId : organIds) {
                    //获取区域公众号
                    mergeRecipeFlag = (Boolean) configService.getConfiguration(organId, "mergeRecipeFlag");
                    if (mergeRecipeFlag == null || !mergeRecipeFlag) {
                        mergeRecipeFlag = false;
                        break;
                    }
                }
            }
            //再根据区域公众号里是否都支持同一种合并方式
            if (mergeRecipeFlag) {
                //获取合并处方分组方式
                //e.registerId支持同一个挂号序号下的处方合并支付
                //e.registerId,e.chronicDiseaseName支持同一个挂号序号且同一个病种的处方合并支付
                String mergeRecipeWay = (String) configService.getConfiguration(organIds.get(0), "mergeRecipeWay");
                //默认挂号序号分组
                if (StringUtils.isEmpty(mergeRecipeWay)) {
                    mergeRecipeWay = "e.registerId";
                }
                //如果只有一个就取第一个
                if (organIds.size() == 1) {
                    mergeRecipeWayAfter = mergeRecipeWay;
                }
                //从第二个开始进行比较
                for (Integer organId : organIds) {
                    mergeRecipeWayAfter = (String) configService.getConfiguration(organId, "mergeRecipeWay");
                    if (!mergeRecipeWay.equals(mergeRecipeWayAfter)) {
                        mergeRecipeFlag = false;
                        LOGGER.info("MergeRecipeManager 区域公众号存在机构配置不一致:organId={},mergeRecipeWay={}", organId, mergeRecipeWay);
                        break;
                    }
                }
                result.put("mergeRecipeFlag", mergeRecipeFlag);
                result.put("mergeRecipeWayAfter", mergeRecipeWayAfter);
                LOGGER.info("MergeRecipeManager mergeRecipeFlag={},mergeRecipeWay={}", mergeRecipeFlag, mergeRecipeWay);
            }
        } catch (Exception e) {
            LOGGER.error("MergeRecipeManager error configService", e);
        }
        return result;
    }
}
