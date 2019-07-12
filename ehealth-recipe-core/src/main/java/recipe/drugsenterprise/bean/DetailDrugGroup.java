package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
@Schema
public class DetailDrugGroup implements Serializable{
    private static final long serialVersionUID = -2402232342922946328L;

    private Integer drugId;

    private Integer recipeDetailId;

    private Double sumUsage;

    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public Integer getRecipeDetailId() {
        return recipeDetailId;
    }

    public void setRecipeDetailId(Integer recipeDetailId) {
        this.recipeDetailId = recipeDetailId;
    }

    public Double getSumUsage() {
        return sumUsage;
    }

    public void setSumUsage(Double sumUsage) {
        this.sumUsage = sumUsage;
    }
}