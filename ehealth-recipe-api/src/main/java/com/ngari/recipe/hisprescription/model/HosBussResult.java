package com.ngari.recipe.hisprescription.model;

import com.ngari.recipe.common.RecipeCommonResTO;
import ctd.util.JSONUtils;

import java.io.Serializable;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/4/11
 */
public class HosBussResult extends RecipeCommonResTO implements Serializable {

    private static final long serialVersionUID = 5559506569191005535L;

    private HospitalRecipeDTO prescription;

    public HosBussResult() {
    }


    public HospitalRecipeDTO getPrescription() {
        return prescription;
    }

    public void setPrescription(HospitalRecipeDTO prescription) {
        this.prescription = prescription;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
