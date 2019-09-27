package recipe.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 处方页面按钮展示状态状态常量
 *
 * @author: JRK
 * date:2019/09/02
 */
public enum RecipePageButtonStatusEnum {
    /**
     * 审核模式待处理
     */
    To_Be_Pend("1", RecipeStatusConstant.CHECK_PASS,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
                    ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 0),
    /**
     * 审核模式待支付
     */
    To_Be_Paid("2", OrderStatusConstant.READY_PAY,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
            ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 1),
    /**
     * 审核模式查看物流
     */
    To_Send("2", OrderStatusConstant.SENDING,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
                    ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 2),
    /**
     * date 20190926
     * 添加前置的选择购药按钮模式
     * 前置审核模式待处理
     */
    Pre_To_Be_Pend("1", RecipeStatusConstant.READY_CHECK_YS,
            Arrays.asList(ReviewTypeConstant.Preposition_Check), 0),
    /**
     * 不展示
     */
    No_Show("-1", -1,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
            ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 3);

    private String recodeType;

    private Integer recodeCode;

    private List<Integer> reviewType;

    private Integer pageButtonStatus;

    RecipePageButtonStatusEnum(String recodeType, Integer recodeCode, List<Integer> reviewType, Integer pageButtonStatus) {
        this.recodeType = recodeType;
        this.recodeCode = recodeCode;
        this.pageButtonStatus = pageButtonStatus;
        this.reviewType = reviewType;
    }

    public static RecipePageButtonStatusEnum fromRecodeTypeAndRecodeCodeAndReviewType(String recodeType, Integer recodeCode, Integer reviewType) {
        for (RecipePageButtonStatusEnum e : RecipePageButtonStatusEnum.values()) {
            if (e.getRecodeType().equals(recodeType)  && e.getRecodeCode() == recodeCode && e.getReviewType().contains(reviewType)) {
                return e;
            }
        }
        return No_Show;
    }

    public List<Integer> getReviewType() {
        return reviewType;
    }

    public void setReviewType(List<Integer> reviewType) {
        this.reviewType = reviewType;
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
