package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class FastRecipeAndDetailResVO implements Serializable {
    private static final long serialVersionUID = 3181309499804281168L;

    private String introduce;
    private String title;
    private String backgroundImg;
    private FastRecipeResVO fastRecipeResVO;
    private List<FastRecipeDetailVO> fastRecipeDetailList;
}
