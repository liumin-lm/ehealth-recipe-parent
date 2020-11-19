package recipe.constant;

import java.math.BigDecimal;

/**
* @Description: RecipePayTipEnum 类（或接口）是 处方支付提示枚举
* @Author: JRK
* @Date: 2019/9/17
*/
public enum RecipePayTipEnum {
    /**
     * 配送到家货到付款，不需要审方/审方金额为0
     */
    Send_To_Home_Below_No_CheckFee("ngarihealth", 1, 2, true, "", "（费用货到支付)"),
    /**
     * 配送到家货到付款，审核费用不为0
     */
    Send_To_Home_Below_Need_CheckFee("ngarihealth", 1, 2, false, "", "(需先支付药师审核费：<em>¥1.00</em>，其余费用货到支付)"),
    /**
     * 到院取药，不需要审方/审方金额为0
     * 20201117: 医院取药 药品费用支付提示改为 按机构配置项getHisDrugPayTip 进行提示
     */
    To_Hos_No_CheckFee("ngarihealth", 2, 3, true, "提交订单后，药品费用需到医院进行支付。", "（费用请到药店支付）"),
    /**
     * 到院取药，审核费用不为0
     * 20201117: 医院取药 药品费用支付提示改为 按机构配置项getHisDrugPayTip 进行提示
     */
    To_Hos_Need_CheckFee("ngarihealth", 2, 3, false, "提交订单后，药品费用需到医院进行支付。", "（需先支付药师审核费：<em>¥</em>，其余到医院支付）"),
    /**
     * 药店取药，不需要审方/审方金额为0
     */
    TFDS_No_CheckFee("ngarihealth", 3, 4, true, "提交订单后，药品费用需到医院进行支付。", "（费用请到药店支付）"),
    /**
     * 药店取药，审核费用不为0
     */
    TFDS_Need_CheckFee("ngarihealth", 3, 4, false, "提交订单后，药品费用需到医院进行支付。", "（需先支付药师审核费：<em>¥</em>，其余到医院支付）"),
    /**
     * 下载处方，不需要审方/审方金额为0
     */
    Download_No_CheckFee("ngarihealth", 5, 6, true, "", "提交订单后可下载处方笺"),
    /**
     * 下载处方，审核费用不为0
     */
    Download_Need_CheckFee("ngarihealth", 5, 6, false, "", "(支付药师审核费后可下载处方笺）"),
    /**
     * 默认值
     */
    Default("", 0, 0, false, null, null);
    /**
     * 处方模式
     */
    private String recipeMode;
    /**
     * 购药方式
     */
    private Integer giveMode;
    /**
     * 支付方式
     */
    private Integer payMode;
    /**
     * 是否需要审核费用
     */
    private Boolean notNeedCheckFee;
    /**
     * 支付提示
     */
    private String payTip;

    /**
     * 支付注意事项
    */
    private String payNote;

    RecipePayTipEnum(String recipeMode, Integer giveMode, Integer payMode, Boolean notNeedCheckFee, String payNote, String payTip) {
        this.recipeMode = recipeMode;
        this.payNote = payNote;
        this.giveMode = giveMode;
        this.payMode = payMode;
        this.notNeedCheckFee = notNeedCheckFee;
        this.payTip = payTip;
    }

    /**
     * 到院取药支付提示专用。
     * 根据医院到院取药需付款文案配置 + 审方费用生成支付提示
     * @param gettingDrugPayTip 医院到院取药需付款文案配置
     * @param checkFee 审核费用，不需要审方时可为null
     * @return 支付提示
     */
    public String getToHosPayTip(String gettingDrugPayTip, BigDecimal checkFee) {
        boolean notNeedCheckFee = this.notNeedCheckFee ? this.notNeedCheckFee :
                (checkFee == null && checkFee.compareTo(BigDecimal.ZERO) <= 0);
        if (notNeedCheckFee) {
            if (gettingDrugPayTip == null || gettingDrugPayTip.trim().length() == 0)  {
                return "";
            }
            return String.format("(%s)", gettingDrugPayTip.trim());
        } else {
            String checkFeeTip = String.format("需先支付药师审核费：<em>¥%s</em>", checkFee.toPlainString());
            if (gettingDrugPayTip != null && gettingDrugPayTip.trim().length() > 0)  {
                return String.format("(%s，%s)", checkFeeTip, gettingDrugPayTip);
            }
            return String.format("(%s)", checkFeeTip);
        }
    }


    public static RecipePayTipEnum fromRecipeModeAndGiveModeAndPayModeAndNotNeedCheckFee(String recipeMode, Integer giveMode, Integer payMode, Boolean notNeedCheckFee){
        for(RecipePayTipEnum ep : RecipePayTipEnum.values()){
            if(ep.getRecipeMode().equalsIgnoreCase(recipeMode) && ep.getGiveMode().equals(giveMode)
                    && ep.getPayMode().equals(payMode) && ep.getNotNeedCheckFee().equals(notNeedCheckFee)){
                return ep;
            }
        }
        return Default;
    }

    public String getRecipeMode() {
        return recipeMode;
    }

    public void setRecipeMode(String recipeMode) {
        this.recipeMode = recipeMode;
    }

    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public void setPayMode(Integer payMode) {
        this.payMode = payMode;
    }

    public Boolean getNotNeedCheckFee() {
        return notNeedCheckFee;
    }

    public void setNotNeedCheckFee(Boolean notNeedCheckFee) {
        this.notNeedCheckFee = notNeedCheckFee;
    }

    public String getPayTip() {
        return payTip;
    }

    public void setPayTip(String payTip) {
        this.payTip = payTip;
    }

    public String getPayNote() {
        return payNote;
    }

    public void setPayNote(String payNote) {
        this.payNote = payNote;
    }
}