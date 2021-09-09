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
public class SearchDrugReqVo implements Serializable {
    /**
     * 搜索关键字
     */
    private String saleName;
    /**
     * 机构id
     */
    private String organId;
    /**
     * 起始条数
     */
    private int start;
    /**
     * 条数
     */
    private int limit;
}
