package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/9/7
 */
@Data
public class CanOpenRecipeReqDTO implements Serializable {
    private static final long serialVersionUID = -6349422432515748840L;

    /**
     * 机构id
     */
    private Integer organId;
    /**
     * 业务id
     */
    private Integer clinicID;
    /**
     * 业务类型
     */
    private Integer bussSource;
}
