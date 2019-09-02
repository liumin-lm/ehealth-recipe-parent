package recipe.constant;

/**
 * 处方页面按钮展示状态状态常量
 *
 * @author: JRK
 * date:2019/09/02
 */
public enum RecipePageButtonStatusEnum {
    /**
     * 待处理
     */
    To_Be_Pend("1", RecipeStatusConstant.CHECK_PASS, 0),
    /**
     * 待支付
     */
    To_Be_Paid("2", OrderStatusConstant.READY_PAY, 1),
    /**
     * 查看物流
     */
    To_Send("2", OrderStatusConstant.SENDING, 2);

    private String recodeType;

    private Integer recodeCode;

    private Integer pageButtonStatus;

    RecipePageButtonStatusEnum(String recodeType, Integer recodeCode, Integer pageButtonStatus) {
        this.recodeType = recodeType;
        this.recodeCode = recodeCode;
        this.pageButtonStatus = pageButtonStatus;
    }

    public static RecipePageButtonStatusEnum fromRecodeTypeAndRecodeCode(String recodeType, Integer recodeCode) {
        for (RecipePageButtonStatusEnum e : RecipePageButtonStatusEnum.values()) {
            if (e.getRecodeType().equals(recodeType)  && e.getRecodeCode() == recodeCode) {
                return e;
            }
        }
        return To_Be_Pend;
    }

    public String getRecodeType() {
        return recodeType;
    }

    public void setRecodeType(String recodeType) {
        this.recodeType = recodeType;
    }

    public Integer getRecodeCode() {
        return recodeCode;
    }

    public void setRecodeCode(Integer recodeCode) {
        this.recodeCode = recodeCode;
    }

    public Integer getPageButtonStatus() {
        return pageButtonStatus;
    }

    public void setPageButtonStatus(Integer pageButtonStatus) {
        this.pageButtonStatus = pageButtonStatus;
    }
}
