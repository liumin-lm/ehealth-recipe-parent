package recipe.factory.status.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 订单状态枚举
 *
 * @author fuzi
 */
public enum RecipeOrderStatusEnum {
    NONE(-9, "未知", ""),
    ORDER_STATUS_READY_PAY(1, "待支付", ""),
    ORDER_STATUS_READY_GET_DRUG(2, "待取药", ""),
    ORDER_STATUS_AWAIT_SHIPPING(3, "待配送", ""),
    ORDER_STATUS_PROCEED_SHIPPING(4, "配送中", ""),
    ORDER_STATUS_DONE(5, "已完成", ""),
    ORDER_STATUS_CANCEL_NOT_PASS(6, "审核不通过", "已取消"),
    ORDER_STATUS_CANCEL_MANUAL(7, "已取消", "手动取消"),
    ORDER_STATUS_CANCEL_AUTO(8, "已取消", "处方单自动取消或其他原因导致的订单取消"),
    ORDER_STATUS_READY_CHECK(9, "待审核", ""),
    ORDER_STATUS_NO_DRUG(10, "无库存", "药店取药（无库存可取药）"),
    ORDER_STATUS_READY_DRUG(11, "准备中", "药店取药（无库存准备中"),
    ORDER_STATUS_HAS_DRUG(12, "有库存", "药店取药（有库存可取药）"),
    ORDER_STATUS_DONE_DISPENSING(13, "已发药", ""),
    ORDER_STATUS_DECLINE(14, "已拒发", ""),
    ORDER_STATUS_DRUG_WITHDRAWAL(15, "已退药", "");
    private Integer type;
    private String name;
    private String desc;

    RecipeOrderStatusEnum(Integer type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    /**
     * 待取药 list
     */
    public static final List<Integer> READY_GET_DRUG = Arrays.asList(ORDER_STATUS_NO_DRUG.getType()
            , ORDER_STATUS_READY_GET_DRUG.getType()
            , ORDER_STATUS_HAS_DRUG.getType());

    public static final List<Integer> DOCTOR_SHOW_ORDER_STATUS = Arrays.asList(ORDER_STATUS_READY_GET_DRUG.type
            , ORDER_STATUS_AWAIT_SHIPPING.type, ORDER_STATUS_PROCEED_SHIPPING.type
            , ORDER_STATUS_NO_DRUG.type, ORDER_STATUS_READY_DRUG.type, ORDER_STATUS_HAS_DRUG.type);

    public Integer getType() {
        return type;
    }


    public String getName() {
        return name;
    }

    /**
     * 根据类型获取名称
     *
     * @param type
     * @return
     */
    public static String getOrderStatus(Integer type) {
        for (RecipeOrderStatusEnum e : RecipeOrderStatusEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return NONE.getName();
    }

    /**
     * 根据类型 获取枚举类型
     *
     * @param type
     * @return
     */
    public static RecipeOrderStatusEnum getRecipeOrderStatusEnum(Integer type) {
        for (RecipeOrderStatusEnum e : RecipeOrderStatusEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return NONE;
    }
}
