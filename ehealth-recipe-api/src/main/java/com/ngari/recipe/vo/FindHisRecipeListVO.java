package com.ngari.recipe.vo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取列表参数
 */
@Data
public class FindHisRecipeListVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

     //机构
    @NotNull
    private Integer organId;

     //mpiId
    @NotNull
    private String mpiId;

    //
    private Integer timeQuantum;

    //
    private String cardId;

    //查询类型（1线上、2线下）
    @NotNull
    private String status;

    @Value("${start:0}")
    private Integer start;

    @Value("${limit:1000}")
    private Integer limit;

}


