package recipe.bean;

import recipe.common.response.CommonResponse;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2019/3/5
 * @description： 购药返回对象
 * @version： 1.0
 */
public class PurchaseResponse extends CommonResponse {

    /**
     * 需要鉴权
     */
    public static final String AUTHORIZATION = "002";

    /**
     * 展示订单详情
     */
    public static final String ORDER_DETAIL = "003";

    /**
     * 下单
     */
    public static final String ORDER = "004";

    /**
     * 到院取药成功
     */
    public static final String TO_HOS_SUCCESS = "005";

    private String authUrl;

    private String orderUrl;

    private List<DeptOrderDTO> orderList;

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getOrderUrl() {
        return orderUrl;
    }

    public void setOrderUrl(String orderUrl) {
        this.orderUrl = orderUrl;
    }

    public List<DeptOrderDTO> getOrderList() {
        return orderList;
    }

    public void setOrderList(List<DeptOrderDTO> orderList) {
        this.orderList = orderList;
    }
}
