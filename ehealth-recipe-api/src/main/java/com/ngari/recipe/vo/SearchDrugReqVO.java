package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 药品查询入参
 * @author： whf
 * @date： 2021-08-23 18:07
 */
@Getter
@Setter
public class SearchDrugReqVO implements Serializable {
    /**
     * 搜索关键字
     */
    private String saleName;
    private String drugName;
    /**
     * 机构id
     */
    private Integer organId;
    /**
     * 就诊序号(对应来源的业务id)
     */
    private Integer clinicId;
    /**
     * 药房
     */
    private Integer pharmacyId;
    /**
     * 药品类型
     */
    private Integer drugType;
    /**
     * 药品适用业务 历史数据默认 1    1-药品处方 2-诊疗处方  保存方式类似  1,2
     */
    private String applyBusiness;

    /**
     * 处方剂型类型 1 饮片方 2 颗粒方
     */
    private Integer recipeDrugForm;

    /**
     * 起始条数
     */
    private int start;
    /**
     * 条数
     */
    private int limit;
}
