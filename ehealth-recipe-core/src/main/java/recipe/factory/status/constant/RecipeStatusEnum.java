package recipe.factory.status.constant;

/**
 * 处方状态枚举
 *
 * @author fuzi
 */

public enum RecipeStatusEnum {
    RECIPE_STATUS_CHECK_PASS(2, "待处理", " 审核通过(医院平台)"),
    RECIPE_STATUS_HAVE_PAY(3, "已支付，待取药", "已支付 (HIS回传状态)"),
    RECIPE_STATUS_IN_SEND(4, "配送中", ""),
    RECIPE_STATUS_WAIT_SEND(5, "待配送", ""),
    RECIPE_STATUS_FINISH(6, "已完成", ""),
    RECIPE_STATUS_CHECK_PASS_YS(7, "审核通过", ""),
    RECIPE_STATUS_READY_CHECK_YS(8, "待审核", "待药师审核"),
    RECIPE_STATUS_REVOKE(9, "已撤销", "此处为已撤销的取消状态"),
    RECIPE_STATUS_DELETE(10, "已删除", " 已删除(医生端历史处方不可见)"),
    RECIPE_STATUS_HIS_FAIL(11, "写入his失败", "已取消：HIS写入失败"),
    RECIPE_STATUS_NO_PAY(13, "未支付", "已取消:超过3天未支付"),
    RECIPE_STATUS_NO_OPERATOR(14, "未处理", "已取消:超过3天未操作"),
    RECIPE_STATUS_CHECK_NOT_PASS_YS(15, "审核不通过", "审核未通过(药师平台人工审核)"),
    RECIPE_STATUS_RECIPE_FAIL(17, "失败", "已取消：取药失败"),
    RECIPE_STATUS_RECIPE_DOWNLOADED(18, "待取药", "已下载：处方已下载"),
    RECIPE_STATUS_USING(22, "处理中", "天猫使用中"),
    RECIPE_STATUS_SIGN_ERROR_CODE_PHA(27, "待审核", "签名失败-药师"),
    RECIPE_STATUS_SIGN_ING_CODE_PHA(31, "待审核", " 签名中-药师"),
    RECIPE_STATUS_SIGN_NO_CODE_PHA(32, "待审核", " 未签名-药师"),

    ;


    private Integer type;
    private String name;
    private String desc;

    RecipeStatusEnum(Integer type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }


    public String getName() {
        return name;
    }

    public static String getRecipeStatus(Integer type) {
        for (RecipeStatusEnum e : RecipeStatusEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }
}
