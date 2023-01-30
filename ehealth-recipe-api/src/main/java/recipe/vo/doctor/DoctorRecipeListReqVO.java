package recipe.vo.doctor;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @description： 医生端查询处方列表入参
 * @author： whf
 * @date： 2022-12-13 9:57
 */
@Data
public class DoctorRecipeListReqVO implements Serializable {
    private static final long serialVersionUID = 5559373753185159589L;

    @ItemProperty(alias = "医生id")
    private Integer doctorId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "开始")
    private Integer start;

    @ItemProperty(alias = "条数")
    private Integer limit;

    @ItemProperty(alias = "0 全部 1 未签名 2处理中 3审核不通过 4已结束")
    private Integer tabStatus;

    @ItemProperty(alias = "处方来源类型 1线下转线上 + 线上处方 3诊疗处方 4常用方")
    private Integer recipeType;

    @ItemProperty(alias = "搜索关键词")
    private String keyWord;
}
