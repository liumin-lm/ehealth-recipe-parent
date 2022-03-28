package com.ngari.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * his收费项
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChargeItemDTO implements Serializable {
    private static final long serialVersionUID = -2594112940686929142L;

    private Integer expressFeePayType;
    private Double expressFee;
}
