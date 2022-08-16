package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 第三方订单预算入参
 * @author： whf
 * @date： 2022-08-15 11:40
 */
@Data
public class ThirdOrderPreSettleReq implements Serializable {
    private static final long serialVersionUID = -3810798390458320912L;
    private String appkey;

    private String tid;

    private List<Integer> recipeIds;
}
