package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 电子病历入参
 *
 * @author fuzi
 */
@Getter
@Setter
public class CaseHistoryVO implements Serializable {
    private static final long serialVersionUID = -2947993962106367511L;
    /**
     * 复诊id
     */
    private Integer clinicId;
    /**
     * 处方id
     */
    private Integer recipeId;
    /**
     * 电子病历id
     */
    private Integer docIndexId;
    /**
     * 操作类型 1：查看，2：copy
     */
    private Integer actionType;
}
