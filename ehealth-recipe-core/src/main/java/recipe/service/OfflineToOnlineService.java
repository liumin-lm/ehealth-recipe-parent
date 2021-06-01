package recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.dao.RecipeDAO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上底层类
 */
@Service
public class OfflineToOnlineService {
    private static final Logger logger = LoggerFactory.getLogger(OfflineToOnlineService.class);

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private HisRecipeService hisRecipeService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RecipeDAO recipeDAO;

    /**
     * 获取购药按钮
     *
     * @param recipeIds
     * @return
     */
    public List<RecipeGiveModeButtonRes> getRecipeGiveModeButtonRes(List<Integer> recipeIds) {
        logger.info("OfflineToOnlineService getRecipeGiveModeButtonRes request = {}",  JSONUtils.toString(recipeIds));
        List<RecipeGiveModeButtonRes> recipeGiveModeButtonRes = recipeService.getRecipeGiveModeButtonRes(recipeIds);
        if (CollectionUtils.isEmpty(recipeGiveModeButtonRes)) {
            throw new DAOException(609, "“抱歉，当前处方没有可支持的购药方式”");
        }
        logger.info("OfflineToOnlineService getRecipeGiveModeButtonRes response = {}",  JSONUtils.toString(recipeGiveModeButtonRes));
        return recipeGiveModeButtonRes;
    }

    /**
     * @param request
     * @return
     * @Description 批量同步线下处方数据
     * @Author liumin
     */
    public List<Integer> batchSyncRecipeFromHis(SettleForOfflineToOnlineVO request) {
        logger.info("OfflineToOnlineService batchSyncRecipeFromHis request = {}", JSONUtils.toString(request));
        List<Integer> recipeIds = new ArrayList<>();
        // 1、删数据
        hisRecipeService.deleteRecipeByRecipeCodes(request.getOrganId(),request.getRecipeCode());

        request.getRecipeCode().forEach(recipeCode -> {
            // 2、线下转线上
            Map<String, Object> map = hisRecipeService.getHisRecipeDetail(null, request.getMpiId(), recipeCode, request.getOrganId(), null, request.getCardId());
            RecipeBean recipeBean = objectMapper.convertValue(map.get("recipe"), RecipeBean.class);
            if (null != recipeBean) {
                recipeIds.add(recipeBean.getRecipeId());
            }
        });
        logger.info("batchSyncRecipeFromHis recipeIds:{}", JSONUtils.toString(recipeIds));
        //部分处方线下转线上成功
        if (recipeIds.size() != request.getRecipeCode().size()) {
            throw new DAOException(609, "抱歉，无法查找到对应的处方单数据");
        }
        //存在已失效处方
        List<Recipe> recipes=recipeDAO.findRecipeByRecipeIdAndClinicOrgan(Integer.parseInt(request.getOrganId()),recipeIds);
        if(CollectionUtils.isNotEmpty(recipes)&& recipes.size()>0){
            logger.info("batchSyncRecipeFromHis 存在已失效处方");
            throw new DAOException(600, "处方单过期已失效");
        }
        logger.info("OfflineToOnlineService batchSyncRecipeFromHis response = {}",  JSONUtils.toString(recipeIds));
        return recipeIds;
    }

}
