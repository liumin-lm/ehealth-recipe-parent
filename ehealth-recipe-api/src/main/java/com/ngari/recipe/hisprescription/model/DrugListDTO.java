package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author zgy
 * @Date 2023-02-08
 */
@Data
public class DrugListDTO implements Serializable {

    private static final long serialVersionUID = 1276719186594967440L;

    @ItemProperty(alias = "机构Id")
    private List<Integer> organIds;

    @ItemProperty(alias = "机构名称")
    private List<String> organName;

    @ItemProperty(alias = "起始位置")
    private Integer start;

    @ItemProperty(alias = "分页条数")
    private Integer limit;
}
