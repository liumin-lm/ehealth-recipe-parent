package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/4/29.
 */
@Schema
@Entity
@Table(name = "cdr_recipe_log")
@Access(AccessType.PROPERTY)
public class RecipeLog implements java.io.Serializable{

    private static final long serialVersionUID = 1707921836539282782L;

    @ItemProperty(alias = "id")
    private int id;

    @ItemProperty(alias="处方序号")
    private Integer recipeId;

    @ItemProperty(alias="修改时间")
    private Date modifyDate;

    @ItemProperty(alias = "前处方状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer beforeStatus;

    @ItemProperty(alias = "后处方状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer afterStatus;

    @ItemProperty(alias = "备注信息")
    private String memo;

    @ItemProperty(alias = "预留")
    private String expand;

    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private Integer processState;
    @ItemProperty(alias = "处方子状态")
    private Integer subState;

    @Column(name = "process_state")
    public Integer getProcessState() {
        return processState;
    }

    public void setProcessState(Integer processState) {
        this.processState = processState;
    }

    @Column(name = "sub_state")
    public Integer getSubState() {
        return subState;
    }

    public void setSubState(Integer subState) {
        this.subState = subState;
    }


    public RecipeLog() {
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "Id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "RecipeID", nullable = false)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "ModifyDate", nullable = false)
    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    @Column(name = "BeforeStatus", nullable = false)
    public Integer getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(Integer beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    @Column(name = "AfterStatus", nullable = false)
    public Integer getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(Integer afterStatus) {
        this.afterStatus = afterStatus;
    }

    @Column(name = "Memo", nullable = false)
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "Expand", nullable = false)
    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }
}
