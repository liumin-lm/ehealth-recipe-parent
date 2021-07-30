package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2021\7\23 0023 16:25
 */
@Setter
@Getter
@NoArgsConstructor
public class OutRecipeDetailReqVO implements Serializable{
    private static final long serialVersionUID = 8234864864205437765L;

    /**
     * 机构ID
     */
    private Integer organId;

    /**
     * HIS处方单号
     */
    private String recipeCode;

    /**
     * 患者名称
     */
    private String patientName;

    /**
     * 挂号序号
     */
    private String registerID;

    /**
     * 病历号
     */
    private String patientId;
}
