package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/6/11
 */
public class UseDoseAndUnitRelationBean implements Serializable {
    private static final long serialVersionUID = 2259539784428084158L;
    @ItemProperty(alias = "医生端填充的每次剂量")
    private Double useDose;
    @ItemProperty(alias = "医生端填充剂量单位")
    private String useDoseUnit;
    @ItemProperty(alias = "医生端药品单位剂量")
    private Double realUseDose;

    public UseDoseAndUnitRelationBean() {}

    public UseDoseAndUnitRelationBean(Double useDose, String useDoseUnit,Double realUseDose) {
        this.useDose = useDose;
        this.useDoseUnit = useDoseUnit;
        this.realUseDose = realUseDose;
    }

    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public Double getRealUseDose() {
        return realUseDose;
    }

    public void setRealUseDose(Double realUseDose) {
        this.realUseDose = realUseDose;
    }
}
