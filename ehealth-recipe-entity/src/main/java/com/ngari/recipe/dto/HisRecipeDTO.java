package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 线下处方返回接收对象封装类
 * @author fuzi
 */
@Getter
@Setter
public class HisRecipeDTO {
    private HisRecipeInfoDTO  hisRecipeInfo;
    private List<HisRecipeDetailDTO> hisRecipeDetail;
    private HisRecipeExtDTO hisRecipeExtDTO;
}
