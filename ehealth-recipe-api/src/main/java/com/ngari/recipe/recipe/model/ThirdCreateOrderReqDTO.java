package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @description： 第三方创建订单入参
 * @author： whf
 * @date： 2022-06-30 11:04
 */
@Data
public class ThirdCreateOrderReqDTO implements Serializable {
    private static final long serialVersionUID = 4158944567311750864L;

    private String appkey;

    private String tid;

    private List<Integer> recipeIds;

    private List<String> recipeCodes;

    private Integer giveMode;

    private String addressId;

    private String payway;

    private String payMode;

    private String decoctionFlag;

    private String decoctionId;

    private String gfFeeFlag;

    private String depId;

    private BigDecimal expressFee;

    private String gysCode;

    private String sendMethod;

    private BigDecimal calculateFee;

    private String pharmacyCode;

    private BigDecimal registerFee;

    private BigDecimal decoctionFee;

    private BigDecimal auditFee;

    private BigDecimal recipeFee;

    private BigDecimal totalFee;
}
