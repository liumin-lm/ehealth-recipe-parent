package com.ngari.recipe.offlinetoonline.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @Author liumin
 * @Date 2021/5/18 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
public class SettleForOfflineToOnlineVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    /**
     * 机构
     */
    @NotNull(message = "organId 不能为空")
    private String organId;

    /**
     * mpiId
     */
    @NotNull
    private String mpiId;

    /**
     * 处方号
     */
    @NotNull
    private List<String> recipeCode;

    /**
     * 卡号
     */
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String cardId;

    /**
     * 业务类型来源 1：处方  2：缴费
     */
    @NotNull
    private String busType;

    /**
     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
     * 23 代表3天
     */
    private Integer timeQuantum;

    public Integer getTimeQuantum() {
        if ("2".equals(this.busType)) {
            timeQuantum = 23;
        } else {
            timeQuantum = 6;
        }
        return timeQuantum;
    }


}


