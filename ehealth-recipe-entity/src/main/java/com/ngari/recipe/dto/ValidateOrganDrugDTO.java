package com.ngari.recipe.dto;

import lombok.*;

import java.io.Serializable;

/**
 * 校验机构药品对象
 *
 * @author fuzi
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateOrganDrugDTO implements Serializable {
    private static final long serialVersionUID = -5308929765476028814L;
    /**
     * 机构药品编号
     */
    private String organDrugCode;
    /**
     * 返回药品状态 T:正常，F：已失效
     */
    private Boolean validateStatus;
    /**
     * 药品序号
     */
    private Integer drugId;
}
