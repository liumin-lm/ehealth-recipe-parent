package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class RecipeSkipDTO implements Serializable {

    private static final long serialVersionUID = 6670154283822148552L;
    //是否展示
    private boolean showFlag;
    //是否可点击
    private boolean clickFlag;
    //跳转地址
    private String skipUrl;
}
