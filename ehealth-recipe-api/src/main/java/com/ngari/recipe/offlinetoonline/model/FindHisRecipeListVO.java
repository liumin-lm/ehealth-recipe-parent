package com.ngari.recipe.offlinetoonline.model;

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

    /**
     * 机构
     */
    @NotNull
    private Integer organId;

    /**
     * mpiId
     */
    @NotNull
    private String mpiId;

    /**
     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
     */
    private Integer timeQuantum;

    /**
     * 卡号
     */
    private String cardId;

    /**
     * onready（待处理）ongoing（进行中）isover（已完成）
     */
    @NotNull
    private String status;

    @Value("${start:0}")
    private Integer start;

    @Value("${limit:1000}")
    private Integer limit;

}


