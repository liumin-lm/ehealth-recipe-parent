package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Schema
@Table(name = "recipe_parameter")
@Access(AccessType.PROPERTY)
public class RecipeParameter implements Serializable {

    @ItemProperty(alias = "参数名称")
    private String paramName;

    @ItemProperty(alias = "参数值")
    private String paramValue;

    @ItemProperty(alias = "参数备注")
    private String paramAlias;

    @Id
    @Column(name = "paramName")
    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    @Column(name = "paramValue")
    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    @Column(name = "paramAlias")
    public String getParamAlias() {
        return paramAlias;
    }

    public void setParamAlias(String paramAlias) {
        this.paramAlias = paramAlias;
    }
}
