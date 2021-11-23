package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 可开方药品出参
 * @author： whf
 * @date： 2021-11-23 16:11
 */
@Getter
@Setter
public class PatientOpenDrugWithEsDTO extends PatientDrugWithEsDTO implements Serializable {

    @ItemProperty(alias = "0 库存不足 1库存充足")
    private Integer stock;

    @ItemProperty(alias = "1西药 2中成药 3中药 4膏方")
    private Integer drugType;

}
