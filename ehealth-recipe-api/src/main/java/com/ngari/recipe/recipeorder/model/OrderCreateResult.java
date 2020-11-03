package com.ngari.recipe.recipeorder.model;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.schema.annotation.Schema;

import java.util.List;

/**
 * 订单创建返回对象结构
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/2/24.
 */
@Schema
public class OrderCreateResult extends RecipeResultBean {

    /**
     * 优惠券类型
     */
    private Integer couponType;

    private String orderCode;

    /**
     * 合并处方用
     */
    private List<RecipeBean> recipes;

    public OrderCreateResult(Integer code) {
        super(code);
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getCouponType() {
        return couponType;
    }

    public void setCouponType(Integer couponType) {
        this.couponType = couponType;
    }

    public List<RecipeBean> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<RecipeBean> recipes) {
        this.recipes = recipes;
    }
}
