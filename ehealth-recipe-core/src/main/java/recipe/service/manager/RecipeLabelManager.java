package recipe.service.manager;

import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import ctd.persistence.exception.DAOException;
import eh.entity.base.Scratchable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.constant.ErrorCode;
import recipe.service.RecipeServiceSub;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 处方签
 *
 * @author fuzi
 */
@Service
public class RecipeLabelManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IScratchableService scratchableService;

    /**
     * 获取处方签 配置 给前端展示。
     * 1获取处方信息，2获取运营平台配置，3替换运营平台配置字段值，4返回对象给前端展示
     *
     * @param recipeId 处方id
     * @param organId  机构id
     * @return
     */
    public Map<String, List<RecipeLabelVO>> queryRecipeLabelById(Integer recipeId, Integer organId) {
        logger.info("RecipeLabelManager queryRecipeLabelById recipeId={},organId={}", recipeId, organId);
        if (null == recipeId || null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "parameter is null!");
        }
        Map<String, Object> recipeMap = RecipeServiceSub.getRecipeAndDetailByIdImpl(recipeId, false);
        if (CollectionUtils.isEmpty(recipeMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is null!");
        }
        Map<String, Object> labelMap = scratchableService.findRecipeListDetail(organId.toString());
        if (CollectionUtils.isEmpty(labelMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "labelMap is null!");
        }
        Map<String, List<RecipeLabelVO>> resultMap = new HashMap<>();
        labelMap.forEach((k, v) -> {
            List<Scratchable> value = (List<Scratchable>) v;
            if (CollectionUtils.isEmpty(value)) {
                return;
            }
            List<RecipeLabelVO> list = getValue(value, recipeMap);
            resultMap.put(k, list);
        });
        logger.info("RecipeLabelManager queryRecipeLabelById resultMap={}", JSONUtils.toBytes(resultMap));
        return resultMap;
    }

    /**
     * 根据运营平台配置 组织字段值对象
     * todo 暂时支持固定格式解析 （目前需求如此，后续如需求变更） 可优化为全格式解析
     *
     * @param scratchableList
     * @param recipeMap
     * @return
     */
    private List<RecipeLabelVO> getValue(List<Scratchable> scratchableList, Map<String, Object> recipeMap) {
        logger.info("RecipeLabelManager getValue scratchableList ={} recipeMap={}", JSONUtils.toBytes(scratchableList), JSONUtils.toBytes(recipeMap));
        List<RecipeLabelVO> recipeLabelList = new LinkedList<>();
        scratchableList.forEach(a -> {
            if (StringUtils.isEmpty(a.getBoxLink())) {
                return;
            }
            RecipeLabelVO recipeLabel = new RecipeLabelVO();
            recipeLabel.setName(a.getBoxTxt());
            recipeLabel.setEnglishName(a.getBoxLink());

            Object obj = recipeMap.get(a.getBoxLink());
            String[] boxLinks = a.getBoxLink().split(ByteUtils.COMMA);

            if (null != obj) {
                //一层结构对象
                recipeLabel.setValue(obj);
            } else if (boxLinks.length > 1) {
                //一层结构 逗号 分隔对象
                StringBuilder value = new StringBuilder();
                for (String boxLink : boxLinks) {
                    obj = recipeMap.get(boxLink);
                    if (null == obj) {
                        logger.error("RecipeLabelManager getValue boxLink ={}", JSONUtils.toBytes(boxLink));
                        continue;
                    }
                    value.append(obj).append(",");
                }
                if (!StringUtils.isEmpty(value)) {
                    recipeLabel.setValue(value.toString());
                }
            } else {
                //两层对象获取字段
                boxLinks = a.getBoxLink().split(ByteUtils.DOT);
                if (2 == boxLinks.length) {
                    obj = recipeMap.get(boxLinks[0]);
                    String str = MapValueUtil.getFieldValueByName(boxLinks[1], obj);
                    recipeLabel.setValue(str);
                } else {
                    logger.error("RecipeLabelManager getValue boxLinks ={}", JSONUtils.toBytes(boxLinks));
                }
            }
            recipeLabelList.add(recipeLabel);
        });
        return recipeLabelList;
    }
}
