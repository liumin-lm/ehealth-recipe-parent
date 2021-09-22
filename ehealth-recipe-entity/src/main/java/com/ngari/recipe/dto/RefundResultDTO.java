package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 退费返回信息
 *
 * @author yinsheng
 */
@Setter
@Getter
public class RefundResultDTO implements Serializable {

    /**
     * 退费状态 0 成功 -1 失败
     */
    private Integer status;
    /**
     * 退费ID
     */
    private String refundId;
    /**
     * 退费金额
     */
    private String refundAmount;
}
