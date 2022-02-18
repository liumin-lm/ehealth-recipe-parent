package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 订单费用的设置条件
 */
@Getter
@Setter
public class OrderFeeSetCondition  implements Serializable {
    private static final long serialVersionUID = 4580683076093298642L;

    //是否为配送类型
    private Boolean payModeSupportFlag;
}
