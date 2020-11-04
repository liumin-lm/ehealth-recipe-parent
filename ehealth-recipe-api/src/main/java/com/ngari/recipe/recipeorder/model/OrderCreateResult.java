package com.ngari.recipe.recipeorder.model;

import com.ngari.recipe.common.RecipeResultBean;
import ctd.schema.annotation.Schema;

import java.util.List;
import java.util.Map;

/**
 * 订单创建返回对象结构
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/2/24.
 */
@Schema
public class OrderCreateResult extends RecipeResultBean {

    private static final long serialVersionUID = 6692993559284916445L;
    /**
     * 优惠券类型
     */
    private Integer couponType;

    private String orderCode;

    /**
     * 合并处方用
     */
    private List<Map<String, Object>> recipes;

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

    public List<Map<String, Object>> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Map<String, Object>> recipes) {
        this.recipes = recipes;
    }
}
