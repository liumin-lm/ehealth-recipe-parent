package recipe.constant;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/25
 * @description： 处方消息枚举
 * @version： 1.0
 */
public enum RecipeMsgEnum {
    /**
     * 参考 RecipeStatusConstant
     */
    RECIPE_CHECK_NOT_PASS(-1, "RecipeCheckNotPass"),
    RECIPE_CHECK_PASS(2, "RecipeCheckPass"),
    RECIPE_HAVE_PAY(3, "RecipeHavePay"),
    RECIPE_IN_SEND(4, "RecipeInSend"),
    RECIPE_CHECK_PASS_YS(7, "RecipeCheckPassYs"),
    RECIPE_READY_CHECK_YS(8, "RecipeReadyCheckYs"),
    RECIPE_REVOKE(9, "RecipeRevoke"),
    RECIPE_HIS_FAIL(11, "RecipeHisFail"),
    RECIPE_NO_DRUG(12, "RecipeNoDrug"),
    RECIPE_NO_PAY(13, "RecipeNoPay"),
    RECIPE_NO_OPERATOR(14, "RecipeNoOperator"),
    RECIPE_REMIND_NO_OPERATOR(101, "RecipeRemindNoOper"),
    RECIPE_REMIND_NO_PAY(102, "RecipeRemindNoPay"),
    RECIPE_REACHPAY_FINISH(103, "RecipeReachPayFinish"),
    RECIPE_REACHHOS_PAYONLINE(104, "RecipeReachHosPayOnline"),
    RECIPE_GETGRUG_FINISH(105, "RecipeGetDrugFinish"),
    CHECK_NOT_PASS_YS_PAYONLINE(106, "NotPassYsPayOnline"),
    CHECK_NOT_PASS_YS_REACHPAY(107, "NotPassYsReachPay"),
    RECIPE_PATIENT_HIS_FAIL(108, "RecipePatientHisFail"),
    RECIPE_LOW_STOCKS(109, "RecipeLowStocks"),
    RECIPE_REMIND_NO_DRUG(110, "RecipeRemindNoDrug"),
    RECIPR_NOT_CONFIRM_RECEIPT(111, "RecipeNotConfirmReceipt"),
    /**
     * fromflag=2的处方药师审核不通过
     */
    RECIPE_YS_CHECKNOTPASS_4HIS(112, "RecipeYsCheckNotPass4His"),
    /**
     * fromflag=2的处方药师审核通过-配送到家
     */
    RECIPE_YS_CHECKPASS_4STH(113, "RecipeYsCheckPass4Sth"),
    /**
     * fromflag=2的处方药师审核通过-药店取药
     */
    RECIPE_YS_CHECKPASS_4TFDS(114, "RecipeYsCheckPass4Tfds"),
    /**
     * fromflag=2的处方取药完成
     */
    RECIPE_FINISH_4HIS(115, "RecipeFinish4His"),
    /**
     * fromflag=2的处方药师审核通过-患者自选
     */
    RECIPE_YS_CHECKPASS_4FREEDOM(116, "RecipeYsCheckPass4Freedom"),

    /**
     * fromflag=2的处方药师待审核(当前推送给身边医生)
     */
    RECIPE_YS_READYCHECK_4HIS(117, "RecipeReadyCheckYs4His"),
    /**
     * fromflag=2的处方超时未支付
     */
    RECIPE_CANCEL_4HIS(118,"RecipeCancel4His"),

    /**
     * 武昌新增，无库存情况
     */
    RECIPE_HOSSUPPORT_NOINVENTORY(119,"RecipeHosSupportNoInventory"),

    /**
     * 武昌新增，有库存情况
     */
    RECIPE_HOSSUPPORT_INVENTORY(120,"RecipeHosSupportInventory"),

    /**
     * Date:2019/09/09
     * 药店取药-无库存-准备药品
     */
    RECIPE_DRUG_NO_STOCK_READY(121, "RecipeDrugNoStockReady"),

    /**
     * Date:2019/09/09
     * 药店取药-无库存-到货
     */
    RECIPE_DRUG_NO_STOCK_ARRIVAL(122, "RecipeDrugNoStockArrival"),

    /**
     * Date:2019/09/09
     * 药店取药-有库存-可取药
     */
    RECIPE_DRUG_HAVE_STOCK(123, "RecipeDrugHaveStock"),

    /**
     * Date:2019/09/18
     * 药店取药-已完成
     */
    RECIPE_TAKE_MEDICINE_FINISH(124, "RecipeTakeMedicineFinish"),

    /**
     * 药师撤销处方
     */
    RECIPE_REVOKE_YS(125, "RecipeRevokeYs"),

    /**
     * 处方未支付运费提醒
     */
    RECIPE_EXPRESSFEE_REMIND_NOPAY(126, "RecipeExpressFeeRemindNoPay"),

    /**
     * 提交审核推送
     */
    RECIPE_REFUND_APPLY(130, "RecipRefundApply"),
    /**
     * 退费失败
     */
    RECIPE_REFUND_FAIL(131, "RecipeRefundFail"),
    /**
     * 退费成功
     */
    RECIPE_REFUND_SUCC(132, "RecipeRefundSucc"),
    /**
     * 医生审核不通过
     */
    RECIPE_REFUND_AUDIT_FAIL(133, "RecipeRefundAuditFail"),

    /**
     * 药师审核通过
     */
    RECIPE_APPROVED(134, "RecipeApproved"),
    /**
     * 第三方审核不通过
     */
    HIS_OR_PHARMACEUTICAL_REFUND_FAIL(135, "HisOrPharmaceuticalRefundFail"),
    /**
     * 第三方审核通过
     */
    HIS_OR_PHARMACEUTICAL_REFUND_SUCCESS(136, "HisOrPharmaceuticalRefundSuccess"),

    /**
     * 处方开方成功
     */
    PRESCRIBE_SUCCESS(137, "PrescribeSuccess"),
    /**
     * 推送快递信息
     */
    EXPRESSINFO_REMIND(138, "ExpressInfoRemind"),

    /**
     * 到院取药
     */
    RECIPE_HOS_TAKE_MEDICINE(139,"RecipeHosTakeMedicine"),

    /**
     * 默认消息
     */
    DEFAULT(999, "");

    private String msgType;

    private int status;

    private RecipeMsgEnum(int status, String msgType){
        this.status = status;
        this.msgType = msgType;
    }

    public int getStatus() {
        return status;
    }

    public String getMsgType() {
        return msgType;
    }


}
