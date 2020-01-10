package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/1/3
 * 月经史结构体数据集
 * @author shiyuping
 */
public class MenstrualHistoryInfoDTO implements Serializable {
    private static final long serialVersionUID = -7829662624996923446L;
    private String menarcheAge;//初潮年龄--岁数
    private String menstrualDuration;//月经持续时间--天数
    private String menstrualCycle;//月经周期--天数
    private String lastMenstrualDate;//绝经日期或末次月经日期 yyyy-mm-dd

    public String getMenarcheAge() {
        return menarcheAge;
    }

    public void setMenarcheAge(String menarcheAge) {
        this.menarcheAge = menarcheAge;
    }

    public String getMenstrualDuration() {
        return menstrualDuration;
    }

    public void setMenstrualDuration(String menstrualDuration) {
        this.menstrualDuration = menstrualDuration;
    }

    public String getMenstrualCycle() {
        return menstrualCycle;
    }

    public void setMenstrualCycle(String menstrualCycle) {
        this.menstrualCycle = menstrualCycle;
    }

    public String getLastMenstrualDate() {
        return lastMenstrualDate;
    }

    public void setLastMenstrualDate(String lastMenstrualDate) {
        this.lastMenstrualDate = lastMenstrualDate;
    }
}
