package com.ngari.recipe.recipereportform.model;

import lombok.Data;


/**
 * Created by Administrator on 2022-06-13.
 */
@Data
public class RecipeReportFormsResponse<T> {

    private Long total;

    private T data;
}
