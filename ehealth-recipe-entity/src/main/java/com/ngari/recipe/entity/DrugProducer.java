package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by  on 2016/7/4 0004.
 * 药品产地
 * @author zhongzx
 */

@Entity
@Schema
@Table(name = "base_drugproducer")
@Access(AccessType.PROPERTY)
public class DrugProducer implements java.io.Serializable{

    private static final long serialVersionUID = 1487645370491284920L;

    @ItemProperty(alias = "序号")
    private Integer id;

    @ItemProperty(alias = "药品产地名称")
    private String name;

    @ItemProperty(alias = "药品产地在相应机构的代码")
    private String code;

    @ItemProperty(alias = "机构代码")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer organ;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "Id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "Name", length = 50)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "Code", length = 20)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Column(name = "Organ", length = 11)
    public Integer getOrgan() {
        return organ;
    }

    public void setOrgan(Integer organ) {
        this.organ = organ;
    }
}
