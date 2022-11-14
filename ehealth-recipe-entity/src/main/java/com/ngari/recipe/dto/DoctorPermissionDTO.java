package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author fuzi
 * 医生权限实体
 */
@Getter
@Setter
public class DoctorPermissionDTO {
    /**
     * 机构id
     */
    private Integer organId;
    /**
     * 医生id
     */
    private Integer doctorId;
    /**
     * 挂号科室id
     */
    private Integer appointId;
    /**
     * 行政科室id
     */
    private  Integer departId;
    /**
     * 开方权
     */
    private Boolean prescription;
    /**
     * 西药开方权
     */
    private Boolean xiYaoRecipeRight;
    /**
     * 中成药开方权
     */
    private Boolean zhongChengRecipeRight;
    /**
     * 中药开方权
     */
    private Boolean zhongRecipeRight;
    /**
     * 膏方开方权
     */
    private Boolean gaoFangRecipeRight;
    /**
     * 靶向药开方权
     */
    private Boolean targetedDrugTypeRecipeRight;
    /**
     * 能否开医保处方
     */
    private Boolean medicalFlag;
    /**
     * 处方权限结果
     */
    private Boolean result;
    /**
     * 提示内容
     */
    private String tips;
    /**
     * 开处方页顶部文案配置
     */
    private String unSendTitle;
}
