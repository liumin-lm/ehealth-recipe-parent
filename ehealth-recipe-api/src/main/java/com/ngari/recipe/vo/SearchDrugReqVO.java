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
    /**
     * 机构id
     */
    private String organId;
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
     * 起始条数
     */
    private int start;
    /**
     * 条数
     */
    private int limit;
}
