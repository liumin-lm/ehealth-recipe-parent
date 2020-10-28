package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author Created by liuxiaofeng on 2020/10/26.
 * 处方订单电子票据表实体
 */
@Schema
@Entity
@Table(name = "cdr_recipeorder_bill")
@Access(AccessType.PROPERTY)
public class RecipeOrderBill implements Serializable{
    private static final long serialVersionUID = -273859595973892969L;

    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "处方订单号")
    private String recipeOrderCode;

    @ItemProperty(alias = "电子票据代码")
    private String billBathCode;

    @ItemProperty(alias = "电子票据号码")
    private String billNumber;

    @ItemProperty(alias = "电子票据二维码")
    private String billQrCode;

    @ItemProperty(alias = "电子票据h5地址")
    private String billPictureUrl;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "修改时间")
    private Date updateTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false, length = 11)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "recipe_order_code")
    public String getRecipeOrderCode() {
        return recipeOrderCode;
    }

    public void setRecipeOrderCode(String recipeOrderCode) {
        this.recipeOrderCode = recipeOrderCode;
    }

    @Column(name = "bill_bath_code")
    public String getBillBathCode() {
        return billBathCode;
    }

    public void setBillBathCode(String billBathCode) {
        this.billBathCode = billBathCode;
    }

    @Column(name = "bill_number")
    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    @Column(name = "bill_qr_code")
    public String getBillQrCode() {
        return billQrCode;
    }

    public void setBillQrCode(String billQrCode) {
        this.billQrCode = billQrCode;
    }

    @Column(name = "bill_picture_url")
    public String getBillPictureUrl() {
        return billPictureUrl;
    }

    public void setBillPictureUrl(String billPictureUrl) {
        this.billPictureUrl = billPictureUrl;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "update_time")
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
