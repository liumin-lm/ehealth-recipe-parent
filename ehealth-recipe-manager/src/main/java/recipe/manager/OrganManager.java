package recipe.manager;

import com.ngari.recipe.dto.ConfigOptionsDTO;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.CacheConstant;
import recipe.dao.RecipeParameterDao;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 机构相关
 *
 * @author fuzi
 */
@Service
public class OrganManager extends BaseManager {
    @Autowired
    private RedisClient redisClient;

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
        ArrayList<Integer> excludeRelationJgptIdList = JSONUtils.parse(val, ArrayList.class);
        if (excludeRelationJgptIdList.contains(relationJgptId)) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    /**
     * 根据配置项判断处方天数
     *
     * @param organId    机构id
     * @param detailList 处方明细
     * @return
     */
    public ConfigOptionsDTO recipeNumberDoctorConfirm(Integer organId, List<Recipedetail> detailList) {
        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        //用药天数-阻断
        Integer numberBlocking = configurationClient.getValueCatchReturnInteger(organId, "recipeNumberDoctorConfirmBlocking", null);
        ConfigOptionsDTO blocking = recipeNumberDoctorConfirm(numberBlocking, detailList, 2);
        if (null != blocking) {
            return blocking;
        }
        //用药天数-提示
        Integer numberCaution = configurationClient.getValueCatchReturnInteger(organId, "recipeNumberDoctorConfirmCaution", null);
        return recipeNumberDoctorConfirm(numberCaution, detailList, 1);
    }

    /**
     * 根据配置项判断处方金额
     *
     * @param organId    机构id
     * @param totalMoney 处方金额
     * @return
     */
    public ConfigOptionsDTO recipeMoneyDoctorConfirm(Integer organId, BigDecimal totalMoney) {
        //处方金额-阻断
        Integer moneyBlocking = configurationClient.getValueCatchReturnInteger(organId, "recipeMoneyDoctorConfirmBlocking", null);
        ConfigOptionsDTO blocking = recipeMoneyDoctorConfirm(moneyBlocking, totalMoney, 2);
        if (null != blocking) {
            return blocking;
        }
        //处方金额-提示
        Integer moneyCaution = configurationClient.getValueCatchReturnInteger(organId, "recipeMoneyDoctorConfirmCaution", null);
        return recipeMoneyDoctorConfirm(moneyCaution, totalMoney, 1);
    }

    private ConfigOptionsDTO recipeNumberDoctorConfirm(Integer number, List<Recipedetail> detailList, Integer type) {
        if (null == number) {
            return null;
        }
        boolean blocking = detailList.stream().allMatch(a -> a.getUseDays() <= number);
        if (blocking) {
            return null;
        }
        return new ConfigOptionsDTO("recipeNumberDoctorConfirm", number.toString(), type);
    }


    private ConfigOptionsDTO recipeMoneyDoctorConfirm(Integer money, BigDecimal totalMoney, Integer type) {
        if (null == money) {
            return null;
        }
        BigDecimal moneyBig = new BigDecimal(money);
        //totalMoney 小于等于 money
        if (0 >= totalMoney.compareTo(moneyBig)) {
            return null;
        }
        return new ConfigOptionsDTO("recipeMoneyDoctorConfirm", money.toString(), type);
    }
}
