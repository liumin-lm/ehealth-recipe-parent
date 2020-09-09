package recipe.service.manager;

import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.base.scratchable.service.IScratchableService;
import ctd.persistence.exception.DAOException;
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


    public Map<String, Map<String, Object>> queryRecipeLabelById(Integer recipeId, Integer organId) {
        if (null == recipeId || null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "parameter is null!");
        }
        Map<String, Object> recipeMap = RecipeServiceSub.getRecipeAndDetailByIdImpl(recipeId, false);
        if (CollectionUtils.isEmpty(recipeMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is null!");
        }
        Map<String, Object> labelMap = scratchableService.findRecipeDetailList(organId.toString());
        if (CollectionUtils.isEmpty(labelMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "labelMap is null!");
        }
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        labelMap.forEach((k, v) -> {
            List<ScratchableBean> value = (List<ScratchableBean>) v;
            if (CollectionUtils.isEmpty(value)) {
                return;
            }
            Map<String, Object> map = getValue(value, recipeMap);
            resultMap.put(k, map);
        });
        return resultMap;
    }

    private Map<String, Object> getValue(List<ScratchableBean> scratchableList, Map<String, Object> recipeMap) {
        Map<String, Object> boxLinkMap = new HashMap<>();
        scratchableList.forEach(a -> {
            if (StringUtils.isEmpty(a.getBoxLink())) {
                return;
            }
            Object obj = recipeMap.get(a.getBoxLink());
            if (null != obj) {
                boxLinkMap.put(a.getBoxTxt(), obj);
            } else {
                String[] boxLink = a.getBoxLink().split("\\.");
                if (2 == boxLink.length) {
                    obj = recipeMap.get(boxLink[0]);
                    String str = getFieldValueByName(boxLink[1], obj);
                    boxLinkMap.put(a.getBoxTxt(), str);
                } else {
                    logger.error("RecipeLabelManager getValue boxLink ={}", JSONUtils.toBytes(boxLink));
                }
            }
        });
        return boxLinkMap;
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
