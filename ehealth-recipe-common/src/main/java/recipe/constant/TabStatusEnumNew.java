package recipe.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: TabStatusEnum 类（或接口）是 首页切换页面tab展示的处方或者订单对应的状态--new
 * 为了兼容TabStatusEnum还留着
 */
public enum TabStatusEnumNew {
    /**
     * 待处理处方的状态（待处理，待审核|前置）
     * 未生成订单前的处方状态
     */
    Onready_Recipe_StatusList("onready", "recipe", Arrays.asList(2, 8, 27, 31, 32)),

    Onready_Order_StatusList("onready", "order", new ArrayList<>()),

    /**
     * 进行中订单的状态（待支付，待审核|后置，待取药(库存足够/库存不足)，待配送，配送中，准备中）
     */
    Ongoing_Order_StatusList("ongoing", "order", Arrays.asList(1, 9, 2, 12, 10, 3, 4, 11, 13)),
    /**
     * 进行中处方的状态（待处理，待审核|前置）
     * date 20200424
     *
     *
     * 添加药师，签名失败的状态，页面显示状态待审核
     */
    Ongoing_Recipe_StatusList("ongoing", "recipe", Arrays.asList( 3, 4, 5, 40,8,7)),
    /**
     * 已结束订单的状态(已完成)
     * date 2019/10/16
     * 修改已完成，可能没有订单，所以直接取处方已完成的状态
     */
    Isover_Order_StatusList("isover", "order", Arrays.asList(5, 14, 15)),
    /**
     * 已结束处方的状态(未处理，失败，未支付，审核不通过, 已完成, 已撤销，已删除，推his失败)
     * date 20191023
     * 结束添加互联网的天猫结束状态:天猫已过期,天猫已退回
     * date 2020/1/6
     * 不展示已删除的处方
     * 已下载(待取药)的处方归并到已完成里
     */
    Isover_Recipe_StatusList("isover", "recipe", Arrays.asList(14, 17, 13, 15, 12, 6, 9, 11, 20, 23, 18, 41, 42, 43));

    private String tabStatus;

    private String statusType;

    private List<Integer> statusList;

    TabStatusEnumNew(String tabStatus, String statusType, List<Integer> statusList) {
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
    public static TabStatusEnumNew fromTabStatusAndStatusType(String tabStatus, String statusType) {
        for (TabStatusEnumNew e : TabStatusEnumNew.values()) {
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
