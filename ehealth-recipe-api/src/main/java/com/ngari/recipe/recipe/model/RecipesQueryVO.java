package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
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
    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private Integer processState;
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
    private String mpiid;
    private Integer refundNodeStatus;
    private Integer bussSource;
    private Integer recipeBusinessType;
    @ItemProperty(alias = "快捷购药处方标识： 0其他, 1快捷处方, 2医嘱申请复诊 3一键续方复诊")
    @Dictionary(id = "eh.cdr.dictionary.FastRecipeFlag")
    private Integer fastRecipeFlag;

    @ItemProperty(alias = "审方方式：0不需要审核 1自动审方 2药师审方")
    private Integer autoCheckFlag;

    @ItemProperty(alias = "HIS处方编码")
    private String recipeCode;

    private int start;
    private int limit;
}