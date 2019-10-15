package recipe.constant;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
* @Description: TabStatusEnum 类（或接口）是 首页切换页面tab展示的处方或者订单对应的状态
* @Author: JRK
* @Date: 2019/8/21
*/
public enum TabStatusEnum {
    /**
     * 进行中订单的状态（待支付，待审核|后置，待取药(库存足够/库存不足)，待配送，配送中，准备中）
     */
    Ongoing_Order_StatusList("ongoing", "order", new ArrayList<>(Arrays.asList(1, 9, 2, 12, 10, 3, 4, 11))),
    /**
     * 进行中处方的状态（待处理，待审核|前置）
     */
    Ongoing_Recipe_StatusList("ongoing", "recipe" , new ArrayList<>(Arrays.asList(2, 8))),
    /**
     * 已结束订单的状态(已完成)
     */
    Isover_Order_StatusList("isover", "order", new ArrayList<>(Arrays.asList(5))),
    /**
     * 已结束处方的状态(未处理，失败，未支付，审核不通过, 已完成)
     */
    Isover_Recipe_StatusList("isover", "recipe", new ArrayList<>(Arrays.asList(14, 17, 13, 15, 12, 6, 8, 7)));

    private String tabStatus;

    private String statusType;

    private List<Integer> statusList;

    TabStatusEnum(String tabStatus, String statusType, List<Integer> statusList) {
        this.tabStatus = tabStatus;
        this.statusType = statusType;
        this.statusList = statusList;
    }

    /**
     * @method  fromTabStatusAndStatusType
     * @description 查找tab下某一场景的状态集合
     * @date: 2019/8/15
     * @author: JRK
     * @param tabStatus
     * @param statusType
     * @return recipe.constant.TabStatusEnum
     */
    public static TabStatusEnum fromTabStatusAndStatusType(String tabStatus, String statusType) {
        for (TabStatusEnum e : TabStatusEnum.values()) {
            if (tabStatus.equalsIgnoreCase(e.getTabStatus()) && statusType.equalsIgnoreCase(e.getStatusType())) {
                return e;
            }
        }
        return null;
    }

    public String getTabStatus() {
        return tabStatus;
    }

    public void setTabStatus(String tabStatus) {
        this.tabStatus = tabStatus;
    }

    public String getStatusType() {
        return statusType;
    }

    public void setStatusType(String statusType) {
        this.statusType = statusType;
    }

    public List<Integer> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<Integer> statusList) {
        this.statusList = statusList;
    }
}
