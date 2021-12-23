package com.ngari.recipe.recipeorder.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @company: ngarihealth
 * @author: liujin
 * @date:20111223
 */
@Schema
@Data
public class RecipeOrderBeanNoDS extends RecipeOrderBean implements Serializable {

    private static final long serialVersionUID = -1365227235362189226L;

    @ItemProperty(alias = "药企电话")
    private String tel;
}
