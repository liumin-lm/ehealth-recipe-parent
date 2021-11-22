package com.ngari.recipe.dto;

import lombok.Data;

import java.util.List;

/**
 * 库存
 */
@Data
public class DrugStockAmountDTO {
    /**
     * t :有库存 F：无库存
     */
    private boolean result;
    private List<String> notDrugNames;
    private List<DrugInfoDTO> drugInfoList;
}
