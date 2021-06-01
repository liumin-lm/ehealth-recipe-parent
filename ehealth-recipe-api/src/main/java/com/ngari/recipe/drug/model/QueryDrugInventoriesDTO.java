package com.ngari.recipe.drug.model;

import lombok.Data;

import java.util.List;

/**
 * created by shiyuping on 2020/12/21
 * @author shiyuping
 */
@Data
public class QueryDrugInventoriesDTO {
    private Integer organId;
    private List<Integer> drugIds;
    private Integer pharmacyId;

}
