package recipe.medicationguide.service;

import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import recipe.medicationguide.bean.PatientInfoDTO;

import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2019/10/25
 */
public interface IMedicationGuideService {
    /**
     * 获取用药指导跳转url
     * @param patient 患者信息
     * @param recipeBean 处方信息
     * @param recipeDetails 药品信息集
     * @param reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
     * @return
     */
    Map<String,Object> getHtml5LinkInfo(PatientInfoDTO patient, RecipeBean recipeBean, List<RecipeDetailBean> recipeDetails, Integer reqType);
}
