package com.ngari.recipe.recipeorder.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @company: ngarihealth
 * @author: liumin
 * @date:20111223
 */
@Schema
@Data
public class ObtainConfirmOrderObjectResNoDS extends RecipeOrderBean implements Serializable {

    private static final long serialVersionUID = -1365227235362189226L;

    @ItemProperty(alias = "完整地址")
    private String completeAddress;
}
