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
     * date 20190930
     * 添加锁定处方
     */
    Lock_Recipe("1", RecipeStatusConstant.USING,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
                    ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 3),
    /**
     * 不展示
     */
    No_Show("-1", -1,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
                ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 3),

    /**
     * 完成展示用药按钮(处方已完成)
     */
    Finish_Recipe("1", RecipeStatusConstant.FINISH,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
                ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 4),

    /**
     * 完成展示用药按钮（订单已完成）
     */
    Finish_Order("2", OrderStatusConstant.FINISH,
            Arrays.asList(ReviewTypeConstant.Not_Need_Check,
                    ReviewTypeConstant.Postposition_Check, ReviewTypeConstant.Preposition_Check), 4);

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

    /**
     * @method  fromRecodeTypeAndRecodeCodeAndReviewType
     * @description 根据处方信息获得展示的按钮模式
     * @date: 2019/11/15
     * @author: JRK
     * @param recodeType 处方信息来源（1：处方，2：订单）
     * @param recodeCode 处方当前状态
     * @param reviewType 处方审核的模式
     * @return recipe.constant.RecipePageButtonStatusEnum 页面展示按钮的形式
     */
    public static RecipePageButtonStatusEnum fromRecodeTypeAndRecodeCodeAndReviewType(String recodeType, Integer recodeCode, Integer reviewType) {
        for (RecipePageButtonStatusEnum e : RecipePageButtonStatusEnum.values()) {
            if (e.getRecodeType().equals(recodeType)  && e.getRecodeCode() == recodeCode && e.getReviewType().contains(reviewType)) {
                return e;
            }
        }
        return No_Show;
    }

    /**
     * @method  fromRecodeTypeAndRecodeCodeAndReviewTypeByConfigure
     * @description 根据处方信息获得展示的按钮模式同时依赖于完成配置
     * @date: 2019/11/15
     * @author: JRK
     * @param recodeType 处方信息来源（1：处方，2：订单）
     * @param recodeCode 处方当前状态
     * @param reviewType 处方审核的模式
       * @param recodeCode 处方当前状态
     * @param reviewType 处方审核的模式
     * @return recipe.constant.RecipePageButtonStatusEnum 页面展示按钮的形式
     */
    public static RecipePageButtonStatusEnum fromRecodeTypeAndRecodeCodeAndReviewTypeByConfigure(String recodeType, Integer recodeCode, Integer reviewType, Boolean showUseDrugConfig, Boolean noHaveBuyDrugConfig, Boolean haveSendInfo) {
        for (RecipePageButtonStatusEnum e : RecipePageButtonStatusEnum.values()) {
            if (e.getRecodeType().equals(recodeType)  && e.getRecodeCode() == recodeCode && e.getReviewType().contains(reviewType)) {
                //根据模块的配置项具体的按钮展示
                //完成展示用药提醒按钮，只有当配置了用药提醒才展示
                if(4 == e.getPageButtonStatus() && !showUseDrugConfig){
                    return No_Show;
                }

                //展示购药方式按钮，当没有配置购药方式时不展示按钮
                if(0 == e.getPageButtonStatus() && noHaveBuyDrugConfig){
                    return No_Show;
                }

                //展示查看物流按钮，当配送信息不全时不展示按钮
                if(2 == e.getPageButtonStatus() && !haveSendInfo){
                    return No_Show;
                }
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
