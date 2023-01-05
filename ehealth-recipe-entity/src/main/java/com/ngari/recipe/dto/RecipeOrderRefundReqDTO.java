package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 运营平台处方订单退费
 * @author yinsheng
 * @date 2022/04/07 15:41
 */
@Getter
@Setter
public class RecipeOrderRefundReqDTO implements Serializable {
    private static final long serialVersionUID = -3009122469403595160L;
    private List<Integer> organIds;
    private Integer busType;
    private Integer organId;
    private Integer orderStatus;
    private Integer payFlag;
    private Integer depId;
    private Integer refundStatus;
    private String giveModeKey;
    private String orderCode;
    private String patientName;
    private Date beginTime;
    private Date endTime;
    private Integer invoiceStatus;
    private Integer fastRecipeFlag;
    private Integer start;
    private Integer limit;
    private Date payTimeStart;
    private Date payTimeEnd;
    private Integer dateType;
}
