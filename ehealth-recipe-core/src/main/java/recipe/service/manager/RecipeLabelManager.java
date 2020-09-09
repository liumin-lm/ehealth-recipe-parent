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

import java.lang.reflect.Method;
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


    public Map<String, List<RecipeLabelVO>> queryRecipeLabelById(Integer recipeId, Integer organId) {
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
        return resultMap;
    }

    private List<RecipeLabelVO> getValue(List<Scratchable> scratchableList, Map<String, Object> recipeMap) {
        List<RecipeLabelVO> recipeLabelList = new LinkedList<>();
        scratchableList.forEach(a -> {
            if (StringUtils.isEmpty(a.getBoxLink())) {
                return;
            }
            RecipeLabelVO recipeLabel = new RecipeLabelVO();
            recipeLabel.setName(a.getBoxTxt());
            recipeLabel.setEnglishName(a.getBoxLink());
            Object obj = recipeMap.get(a.getBoxLink());
            if (null != obj) {
                recipeLabel.setValue(obj);
            } else {
                String[] boxLink = a.getBoxLink().split("\\.");
                if (2 == boxLink.length) {
                    obj = recipeMap.get(boxLink[0]);
                    String str = getFieldValueByName(boxLink[1], obj);
                    recipeLabel.setValue(str);
                } else {
                    logger.error("RecipeLabelManager getValue boxLink ={}", JSONUtils.toBytes(boxLink));
                }
            }
            recipeLabelList.add(recipeLabel);
        });
        return recipeLabelList;
    }

    public String getFieldValueByName(String fieldName, Object o) {
        if (StringUtils.isEmpty(fieldName) || null == o) {
            return null;
        }
        try {
            String getter = "get" + captureName(fieldName);
            Method method = o.getClass().getMethod(getter);
            Object value = method.invoke(o);
            if (null == value) {
                return "";
            }
            return value.toString();
        } catch (Exception e) {
            logger.error("getFieldValueByName error fieldName ={}", fieldName, e);
            return null;
        }
    }

    public static String captureName(String name) {
        char[] cs = name.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
    }
}
