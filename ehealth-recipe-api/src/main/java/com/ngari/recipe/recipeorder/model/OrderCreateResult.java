package com.ngari.recipe.recipeorder.model;

import com.ngari.recipe.common.RecipeResultBean;
import ctd.schema.annotation.Schema;

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
}
