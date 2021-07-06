package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.constant.CacheConstant;
import recipe.util.RedisClient;
import recipe.util.ValidateUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fuzi
 */
@Service
public class RedisManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisClient redisClient;

    /**
     * 特殊字段坐标记录
     *
     * @param recipeId
     * @param coOrdinateList
     */
    public void coOrdinate(Integer recipeId, List<CoOrdinateVO> coOrdinateList) {
        if (CollectionUtils.isEmpty(coOrdinateList) || null == recipeId) {
            logger.warn("RedisManager coOrdinate error ");
            return;
        }
        redisClient.addList(CacheConstant.KEY_RECIPE_LABEL + recipeId.toString(), coOrdinateList, 3 * 24 * 60 * 60L);
    }


    /**
     * 获取pdf 特殊字段坐标
     *
     * @param recipeId   处方id
     * @param coordsName 特殊字段名称
     * @return 坐标对象
     */
    public CoOrdinateVO getPdfCoordsHeight(Integer recipeId, String coordsName) {
        CoOrdinateVO coOrdinate = getPdfCoords(recipeId, coordsName);
        if (null != coOrdinate) {
            coOrdinate.setY(499 - coOrdinate.getY());
            coOrdinate.setX(coOrdinate.getX() + 5);
            return coOrdinate;
        }
        return null;
    }

    /**
     * 获取坐标
     *
     * @param recipeId 处方id
     * @param namePdf  pdf 坐标名称字段
     * @return 坐标对象
     */
    public CoOrdinateVO getPdfCoords(Integer recipeId, String namePdf) {
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            return null;
        }
        List<CoOrdinateVO> coOrdinateList = redisClient.getList(CacheConstant.KEY_RECIPE_LABEL + recipeId.toString());
        logger.info("RedisManager getPdfCoords recipeId={}，namePdf = {} ,coOrdinateList={}", recipeId, namePdf, JSON.toJSONString(coOrdinateList));

        if (CollectionUtils.isEmpty(coOrdinateList)) {
            logger.warn("RedisManager getPdfCoords recipeId为空 recipeId={}", recipeId);
            return null;
        }
        Map<String, CoOrdinateVO> map = coOrdinateList.stream().collect(Collectors.toMap(CoOrdinateVO::getName, a -> a, (k1, k2) -> k1));
        return map.get(namePdf);
    }

}
