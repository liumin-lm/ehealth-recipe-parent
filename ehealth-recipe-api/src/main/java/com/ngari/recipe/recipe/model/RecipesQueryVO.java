package com.ngari.recipe.recipe.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 处方查询入参
 *
 * @author fuzi
 */
@Getter
@Setter
public class RecipesQueryVO implements Serializable {
    private static final long serialVersionUID = 1006821231283734183L;

    private List<Integer> organIds;
    private Integer organId;
    private Integer status;
    private Integer doctor;
    private String patientName;
    private Date bDate;
    private Date eDate;
    private Integer dateType;
    private Integer depart;
    private Integer giveMode;
    private Integer sendType;
    private Integer fromFlag;
    private Integer recipeId;
    private Integer enterpriseId;
    private Integer checkStatus;
    private Integer payFlag;
    private Integer orderType;
    private Integer recipeType;

    private int start;
    private int limit;
}