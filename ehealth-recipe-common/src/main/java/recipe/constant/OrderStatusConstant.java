package recipe.constant;

/**
 * 处方订单状态常量
 * todo 废弃常量 改用枚举 RecipeOrderStatusEnum
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2016/4/27.
 */
@Deprecated
public class OrderStatusConstant {

    /**
     * 未知
     */
    public static final Integer UNKNOW = -9;

    /**
     * 待支付
     */
    public static final Integer READY_PAY = 1;

    /**
     * 待取药
     */
    public static final Integer READY_GET_DRUG = 2;

    /**
     * 待审核
     */
    public static final Integer READY_CHECK = 9;

    /**
     * 待配送
     */
    public static final Integer READY_SEND = 3;

    /**
     *配送中
     */
    public static final Integer SENDING = 4;

    /**
     *已完成
     */
    public static final Integer FINISH = 5;

    /**
     *审核未通过
     */
    public static final Integer CANCEL_NOT_PASS = 6;

    /**
     *手动取消
     */
    public static final Integer CANCEL_MANUAL = 7;

    /**
     *处方单自动取消或其他原因导致的订单取消
     */
    public static final Integer CANCEL_AUTO = 8;

    /**
     * 药店取药（无库存可取药）
     */
    public static final Integer NO_DRUG = 10;

    /**
     * 药店取药（无库存准备中）
     */
    public static final Integer READY_DRUG = 11;

    /**
     * 药店取药（有库存可取药）
     */
    public static final Integer HAS_DRUG = 12;

    /**
     * 取药失败
     */
    public static final Integer FAIL = 17;

    /**
     * 待取药
     */
    public static final int READY_GET_DRUG_INT = 2;

    /**
     * 准备中
     */
    public static final int READY_DRUG_INT = 11;

    /**
     * 待配送
     */
    public static final int READY_SEND_INT = 3;

    /**
     * 配送中
     */
    public static final int SENDING_INT = 4;

    /**
     * 药店取药（无库存可取药）
     */
    public static final int NO_DRUG_INT = 10;

    /**
     * 药店取药（有库存可取药）
     */
    public static final int HAS_DRUG_INT = 12;

}
