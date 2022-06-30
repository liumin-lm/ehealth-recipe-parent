package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;
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

    private Double expressFee;

    private String gysCode;

    private String sendMethod;

    private Double calculateFee;

    private String pharmacyCode;

    private Double registerFee;

    private Double decoctionFee;

    private Double auditFee;

    private Double recipeFee;

    private Double totalFee;
}
