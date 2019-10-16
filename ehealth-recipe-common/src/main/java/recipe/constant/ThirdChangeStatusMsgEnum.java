package recipe.constant;

import java.util.List;

/**
* @Description: ThirdChangeStatusMsgEnum 类（或接口）是 第三方修改状态的发送消息枚举
* @Author: JRK
* @Date: 2019/9/12
*/
public enum ThirdChangeStatusMsgEnum {
    /**
     * 修改订单状态：无库存可取药
     */
    Get_Drug_No_Stock(1, OrderStatusConstant.NO_DRUG, RecipeStatusConstant.RECIPE_DRUG_NO_STOCK_ARRIVAL);

    //0:修改的处方状态，1：修改的订单状态
    private int status;

    private Integer changeStatus;

    private Integer msgStatus;

    ThirdChangeStatusMsgEnum(int status, Integer changeStatus, Integer msgStatus) {
        this.status = status;
        this.changeStatus = changeStatus;
        this.msgStatus = msgStatus;
    }

    /**
     * @method  fromTabStatusAndStatusType
     * @description 查找tab下某一场景的状态集合
     * @date: 2019/8/15
     * @author: JRK
     * @param status 修改类型
     * @param changeStatus 设置的状态
     * @return recipe.constant.ThirdChangeStatusMsgEnum 消息枚举
     */
    public static ThirdChangeStatusMsgEnum fromStatusAndChangeStatus(int status, Integer changeStatus) {
        for (ThirdChangeStatusMsgEnum e : ThirdChangeStatusMsgEnum.values()) {
            if (status == e.getStatus() && e.getChangeStatus().equals(changeStatus)) {
                return e;
            }
        }
        return null;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Integer getChangeStatus() {
        return changeStatus;
    }

    public void setChangeStatus(Integer changeStatus) {
        this.changeStatus = changeStatus;
    }

    public Integer getMsgStatus() {
        return msgStatus;
    }

    public void setMsgStatus(Integer msgStatus) {
        this.msgStatus = msgStatus;
    }
}