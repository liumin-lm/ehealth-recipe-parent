package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/1/11 10:55
 */
@Data
public class WriteDrugRecipeBean implements Serializable {

    private static final long serialVersionUID = 8891378081107381886L;

    private Integer appointDepartInDepartId;
}
