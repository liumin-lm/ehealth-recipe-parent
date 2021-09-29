package com.ngari.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description： 处方金额出参(邵逸夫模式专用)
 * @author： whf
 * @date： 2021-09-22 11:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeFeeDTO implements Serializable {
    /**
     * 费用类型
     * 药品费用 : drugFee
     * 药师服务费 : serviceFee
     * 运费 : freightFee
     */
    private String feeType;

    /**
     * 支付状态 1 已支付
     */
    private Integer payFlag;
}
