package recipe.service.manager;

import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import ctd.persistence.exception.DAOException;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.constant.ErrorCode;
import recipe.service.RecipeServiceSub;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
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
     * 运营平台机构配置，处方签配置， 特殊字段替换展示
     */
    private final static List<String> CONFIG_STRING = Arrays.asList("recipeDetailRemark");

    @Autowired
    private IScratchableService scratchableService;

    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Resource
    private IESignBaseService esignService;

    public Map<String, Object> queryPdfRecipeLabelById(Map<String, List<RecipeLabelVO>> result, Map<String, Object> recipeMap) {
        //组装生成pdf的参数
        String fileName = "recipe_" + recipeId + ".pdf";
        Map<String, Object> paramMap;
        recipe.setSignDate(DateTime.now().toDate());
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            //中药pdf参数
            paramMap = RecipeServiceSub.createParamMapForChineseMedicine(recipe, map, fileName);
        } else {
            paramMap = RecipeServiceSub.createParamMap(recipe, map, fileName);
            paramMap.put("recipeImgId", recipeId);
        }
        //上传阿里云
        String memo = "";
        Object footerRemark = configService.getConfiguration(recipe.getClinicOrgan(), "recipeDetailRemark");
        if (null != footerRemark) {
            paramMap.put("footerRemark", footerRemark.toString());
        }
        Map<String, Object> backMap = esignService.signForRecipe(true, recipe.getDoctor(), paramMap);
        return backMap;
    }

    /**
     * 获取处方签 配置 给前端展示。
     * 1获取处方信息，2获取运营平台配置，3替换运营平台配置字段值，4返回对象给前端展示
     *
     * @param recipeMap 处方
     * @param organId   机构id
     * @return
     */
    public Map<String, List<RecipeLabelVO>> queryRecipeLabelById(Integer organId, Map<String, Object> recipeMap) {
        logger.info("RecipeLabelManager queryRecipeLabelById ,organId={}", organId);
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构id为空");
        }
        //处理特殊字段拼接
        setRecipeMap(recipeMap);

        Map<String, Object> labelMap = scratchableService.findRecipeListDetail(organId.toString());
        if (CollectionUtils.isEmpty(labelMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "运营平台配置为空");
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
            if (CONFIG_STRING.contains(boxLink)) {
                value = configService.getConfiguration(organId, boxLink);
                if (null == value) {
                    return;
                }
            }

            if (null == value) {
                //对象获取字段
                String[] boxLinks = boxLink.split(ByteUtils.DOT);
                Object key = recipeMap.get(boxLinks[0]);
                if (2 == boxLinks.length && null != key) {
                    if (key instanceof List && !CollectionUtils.isEmpty((List) key)) {
                        key = ((List) key).get(0);
                    }
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
        if (CollectionUtils.isEmpty(recipeMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeMap is null!");
        }
        String doctorSignImg = null == recipeMap.get("doctorSignImg") ? "" : recipeMap.get("doctorSignImg").toString();
        String doctorSignImgToken = null == recipeMap.get("doctorSignImgToken") ? "" : recipeMap.get("doctorSignImgToken").toString();
        if (!StringUtils.isAnyEmpty(doctorSignImg, doctorSignImgToken)) {
            recipeMap.put("doctorSignImg,doctorSignImgToken", doctorSignImg + ByteUtils.COMMA + doctorSignImgToken);
        }
        String checkerSignImg = null == recipeMap.get("checkerSignImg") ? "" : recipeMap.get("checkerSignImg").toString();
        String checkerSignImgToken = null == recipeMap.get("checkerSignImgToken") ? "" : recipeMap.get("checkerSignImgToken").toString();
        if (!StringUtils.isAnyEmpty(checkerSignImg, checkerSignImgToken)) {
            recipeMap.put("checkerSignImg,checkerSignImgToken", checkerSignImg + ByteUtils.COMMA + checkerSignImgToken);
        }
    }

}
