package com.ngari.recipe.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 模板处方请求入参
 *
 * @author yinsheng
 * @date 2022\04\02 0021 09:24
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FormWorkRecipeReqVO implements Serializable {
    private static final long serialVersionUID = -1254576728303206506L;

    private Integer organId;
    private Integer start;
    private Integer limit;
}
