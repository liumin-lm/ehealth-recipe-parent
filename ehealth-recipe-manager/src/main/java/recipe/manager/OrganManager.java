package recipe.manager;

import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.constant.CacheConstant;
import recipe.dao.RecipeParameterDao;
import recipe.util.RedisClient;

import java.util.ArrayList;

/**
 * @author fuzi
 */
@Service
public class OrganManager extends BaseManager {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private IConfigurationClient configurationClient;

    /**
     * 查询是否关联的监管平台(排除关联空的监管平台，以及上海监管平台)
     * 平台环境：【默认未关联，是null】【关联空，0】【关联上海监管平台 17，shsjgpt】
     * 浙江省互联网环境：无关联数据
     * 胸科独立部署：关联上海监管平台 26，shsjgpt
     * 宁夏独立环境：关联监管 30，但是地址没有
     * @param organId
     * @return
     */
    public Boolean isRelationJgpt(Integer organId) {
        Integer relationJgptId=configurationClient.getRelationJgptId(organId);
        logger.info("isRelationJgpt organId={},relationJgptId={}", organId,relationJgptId);
        //未关联监管平台，数据为null;
        if(relationJgptId==null){
            return Boolean.FALSE;
        }

        //先从缓存获取，缓存
        String key="exclude_relation_jgpt_id";
        String val = redisClient.hget(CacheConstant.RECIPE_CACHE_KEY, key);
        if (StringUtils.isEmpty(val)) {
            RecipeParameterDao parameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            val = parameterDao.getByName(key);
            if (StringUtils.isNotEmpty(val)) {
                redisClient.hsetEx(CacheConstant.RECIPE_CACHE_KEY, key, val, 7 * 24 * 3600L);
            } else {
                //返回默认值
                val = "[]";
            }
        }

        //排除空监管，上海监管
        ArrayList<Integer> excludeRelationJgptIdList= JSONUtils.parse(val,ArrayList.class);
        if(excludeRelationJgptIdList.contains(relationJgptId)){
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }




}
