package com.ngari.recipe.entity.comment;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Schema
@Entity
@Table(name = "cdr_recipe_comment")
public class RecipeComment {

    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "处方编号")
    private Integer recipeId;

    @ItemProperty(alias = "点评结果")
    private String commentResult;

    @ItemProperty(alias = "点评备注")
    private String commentRemark;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastmodify;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column
    public String getCommentResult() {
        return commentResult;
    }

    public void setCommentResult(String commentResult) {
        this.commentResult = commentResult;
    }

    @Column
    public String getCommentRemark() {
        return commentRemark;
    }

    public void setCommentRemark(String commentRemark) {
        this.commentRemark = commentRemark;
    }

    @Column
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column
    public Date getLastmodify() {
        return lastmodify;
    }

    public void setLastmodify(Date lastmodify) {
        this.lastmodify = lastmodify;
    }
}
