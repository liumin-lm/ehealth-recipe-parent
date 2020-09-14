package recipe.service.manager;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
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

import java.util.*;

/**
 * 处方签
 *
 * @author fuzi
 */
@Service
public class RecipeLabelManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 机构配置展示特殊字段
     */
    private final static List<String> CONFIG_STRING = Arrays.asList("recipeDetailRemark");

    @Autowired
    private IScratchableService scratchableService;

    @Autowired
    private IConfigurationCenterUtilsService configService;

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
        //处理特殊字段拼接
        setRecipeMap(recipeMap);

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
            try {
                List<RecipeLabelVO> list = getValue(value, recipeMap, organId);
                resultMap.put(k, list);
            } catch (Exception e) {
                logger.error("RecipeLabelManager queryRecipeLabelById error ", e);
            }
        });
        logger.info("RecipeLabelManager queryRecipeLabelById resultMap={}", JSONUtils.toString(resultMap));
        return resultMap;
    }

    /**
     * 根据运营平台配置 组织字段值对象
     * todo 暂时需求如此 仅支持固定格式解析 （patient.patientName 这种一层对象解析）
     *
     * @param scratchableList
     * @param recipeMap
     * @return
     */
    private List<RecipeLabelVO> getValue(List<Scratchable> scratchableList, Map<String, Object> recipeMap, Integer organId) {
        logger.info("RecipeLabelManager getValue scratchableList ={} recipeMap={}", JSONUtils.toString(scratchableList), JSONUtils.toString(recipeMap));
        List<RecipeLabelVO> recipeLabelList = new LinkedList<>();
        scratchableList.forEach(a -> {
            if (StringUtils.isEmpty(a.getBoxLink())) {
                return;
            }
            String boxLink = a.getBoxLink().trim();
            /**根据模版匹配 value*/
            Object value = recipeMap.get(boxLink);
            if (null == value && CONFIG_STRING.contains(boxLink)) {
                value = configService.getConfiguration(organId, boxLink);
            }

            if (null == value) {
                //对象获取字段
                String[] boxLinks = boxLink.split(ByteUtils.DOT);
                Object key = recipeMap.get(boxLinks[0]);
                if (2 == boxLinks.length && null != key) {
                    value = MapValueUtil.getFieldValueByName(boxLinks[1], key);
                } else {
                    logger.warn("RecipeLabelManager getValue boxLinks ={}", JSONUtils.toString(boxLinks));
                }
            }

            //组织返回对象
            RecipeLabelVO recipeLabel = new RecipeLabelVO();
            recipeLabel.setName(a.getBoxTxt());
            recipeLabel.setEnglishName(boxLink);
            recipeLabel.setValue(value);
            recipeLabelList.add(recipeLabel);
        });
        return recipeLabelList;
    }

    /**
     * 处理特殊模版匹配规则
     *
     * @param recipeMap
     */
    private void setRecipeMap(Map<String, Object> recipeMap) {
        String doctorSignImg = null == recipeMap.get("doctorSignImg") ? "" : recipeMap.get("doctorSignImg").toString();
        String doctorSignImgToken = null == recipeMap.get("doctorSignImgToken") ? "" : recipeMap.get("doctorSignImgToken").toString();
        recipeMap.put("doctorSignImg,doctorSignImgToken", doctorSignImg + ByteUtils.COMMA + doctorSignImgToken);
        String checkerSignImg = null == recipeMap.get("checkerSignImg") ? "" : recipeMap.get("checkerSignImg").toString();
        String checkerSignImgToken = null == recipeMap.get("checkerSignImgToken") ? "" : recipeMap.get("checkerSignImgToken").toString();
        recipeMap.put("checkerSignImg,checkerSignImgToken", checkerSignImg + ByteUtils.COMMA + checkerSignImgToken);

    }
}
