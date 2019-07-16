package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
* @Description: GroupSumResult 类（或接口）是 分组计算药品结果的对象
* @Author: JRK
* @Date: 2019/7/16
*/
@Schema
public class GroupSumResult implements Serializable {
    private static final long serialVersionUID = 3647144402254286016L;
    /**
     * 库存合格数量
     */
    private int complacentNum;
    /**
     * 总金额
     */
    private double feeSum;

    public int getComplacentNum() {
        return complacentNum;
    }

    public void setComplacentNum(int complacentNum) {
        this.complacentNum = complacentNum;
    }

    public double getFeeSum() {
        return feeSum;
    }

    public void setFeeSum(double feeSum) {
        this.feeSum = feeSum;
    }
}