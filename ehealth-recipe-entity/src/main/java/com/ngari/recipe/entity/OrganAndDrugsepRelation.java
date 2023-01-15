package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 组织与药企间关系
 *
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */

@Entity
@Schema
@Table(name = "cdr_organ_drugsep_relation")
@Access(AccessType.PROPERTY)
@DynamicInsert
public class OrganAndDrugsepRelation implements java.io.Serializable {

    private static final long serialVersionUID = 2439194296959727414L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "组织序号")
    private Integer organId;

    @ItemProperty(alias = "药企序号")
    private Integer drugsEnterpriseId;

    @ItemProperty(alias = "药企支持的购药方式")
    private String drugsEnterpriseSupportGiveMode;

    @ItemProperty(alias = "药企支持的可流转处方类型")
    private String enterpriseRecipeTypes;

    @ItemProperty(alias = "药企支持的可流转中药煎法 全部：-1")
    private String enterpriseDecoctionIds;

    @ItemProperty(alias = "优先级")
    private Integer priorityLevel;

    @ItemProperty(alias = "药品剂型 药企配置后，对应处方单及药品属性，将不支持配送")
    private String enterpriseDrugForm;

    // todo 这个字段没有用!
    @ItemProperty(alias = "支持煎法的类型  药企配置后，将不支持配送 0 非代煎 1 代煎 ")
    private Integer supportDecoctionType;

    @ItemProperty(alias = "支持煎法的类型  药企配置后，将不支持配送 0 非代煎 1 代煎 ")
    private String supportDecoctionState;

    @ItemProperty(alias = "支持自取煎法的类型  药企配置后，将不支持自取 0 非代煎 1 代煎 ")
    private String supportSelfDecoctionState;

    @ItemProperty(alias = "不可流转医保类型 0不支持自费处方 1不支持医保处方")
    private String cannotMedicalFlag;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "DrugsEnterpriseId")
    public Integer getDrugsEnterpriseId() {
        return drugsEnterpriseId;
    }

    public void setDrugsEnterpriseId(Integer drugsEnterpriseId) {
        this.drugsEnterpriseId = drugsEnterpriseId;
    }

    @Column(name = "drug_enterprise_support_give_mode")
    public String getDrugsEnterpriseSupportGiveMode() {
        return drugsEnterpriseSupportGiveMode;
    }

    public void setDrugsEnterpriseSupportGiveMode(String drugsEnterpriseSupportGiveMode) {
        this.drugsEnterpriseSupportGiveMode = drugsEnterpriseSupportGiveMode;
    }

    @Column(name = "enterprise_recipe_types")
    public String getEnterpriseRecipeTypes() {
        return enterpriseRecipeTypes;
    }

    public void setEnterpriseRecipeTypes(String enterpriseRecipeTypes) {
        this.enterpriseRecipeTypes = enterpriseRecipeTypes;
    }

    @Column(name = "enterprise_decoction_Ids")
    public String getEnterpriseDecoctionIds() {
        return enterpriseDecoctionIds;
    }

    public void setEnterpriseDecoctionIds(String enterpriseDecoctionIds) {
        this.enterpriseDecoctionIds = enterpriseDecoctionIds;
    }

    @Column(name = "priority_level")
    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    @Column(name = "enterprise_drug_form")
    public String getEnterpriseDrugForm() {
        return enterpriseDrugForm;
    }

    public void setEnterpriseDrugForm(String enterpriseDrugForm) {
        this.enterpriseDrugForm = enterpriseDrugForm;
    }

    @Column(name = "support_decoction_type")
    public Integer getSupportDecoctionType() {
        return supportDecoctionType;
    }

    public void setSupportDecoctionType(Integer supportDecoctionType) {
        this.supportDecoctionType = supportDecoctionType;
    }

    @Column(name = "support_decoction_state")
    public String getSupportDecoctionState() {
        return supportDecoctionState;
    }

    public void setSupportDecoctionState(String supportDecoctionState) {
        this.supportDecoctionState = supportDecoctionState;
    }

    @Column(name = "support_self_decoction_state")
    public String getSupportSelfDecoctionState() {
        return supportSelfDecoctionState;
    }

    public void setSupportSelfDecoctionState(String supportSelfDecoctionState) {
        this.supportSelfDecoctionState = supportSelfDecoctionState;
    }

    @Column(name = "cannot_medical_flag")
    public String getCannotMedicalFlag() {
        return cannotMedicalFlag;
    }

    public void setCannotMedicalFlag(String cannotMedicalFlag) {
        this.cannotMedicalFlag = cannotMedicalFlag;
    }
}
