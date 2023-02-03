package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description： 支付唤起小程序需要的值
 * @author： whf
 * @date： 2023-02-02 14:37
 */
@Data
public class HisSettleReqDTO implements Serializable {
    private static final long serialVersionUID = -1005734441026438477L;
    private String ybId;
    private String hisBusId;
}
