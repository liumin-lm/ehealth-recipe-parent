package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\3\10 0010 19:52
 */
@Entity
@Schema
@Table(name = "cdr_comparedrug")
@Access(AccessType.PROPERTY)
@Deprecated
public class CompareDrug implements Serializable {

    private static final long serialVersionUID = 6383675726039631997L;

    @ItemProperty(alias = "原药品编号")
    private Integer originalDrugId;

    /**
     * 新字段 使用 OrganDrugList。regulationDrugCode
     */
    @ItemProperty(alias = "对照药品编号")
    private Integer targetDrugId;

    @Id
    @Column(name = "originalDrugId")
    public Integer getOriginalDrugId() {
        return originalDrugId;
    }

    public void setOriginalDrugId(Integer originalDrugId) {
        this.originalDrugId = originalDrugId;
    }

    @Column(name = "targetDrugId")
    public Integer getTargetDrugId() {
        return targetDrugId;
    }

    public void setTargetDrugId(Integer targetDrugId) {
        this.targetDrugId = targetDrugId;
    }
}
