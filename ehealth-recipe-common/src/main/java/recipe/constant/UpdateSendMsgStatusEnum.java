package recipe.constant;

/**
* @Description: UpdateSendMsgStatusEnum 配送信息同步his状态
* @Author: JRK
* @Date: 20200312
*/
public enum UpdateSendMsgStatusEnum {

    HIS_SEND("医院取药", "0", RecipeBussConstant.GIVEMODE_TO_HOS),

    LOGISTIC_SEND("物流配送", "1", RecipeBussConstant.GIVEMODE_SEND_TO_HOME),

    PHARAMCY_SEND("药店取药", "2", RecipeBussConstant.GIVEMODE_TFDS),

    OTHER("其他", "3", -1),

    ALL("都支持", "4", RecipeBussConstant.GIVEMODE_FREEDOM);

    private String sendMemo;

    private String sendType;

    private Integer giveType;

    UpdateSendMsgStatusEnum(String sendMemo, String sendType, Integer giveType) {
        this.sendType = sendType;
        this.sendMemo = sendMemo;
        this.giveType = giveType;
    }

    public static UpdateSendMsgStatusEnum fromGiveType(Integer giveType) {
        for (UpdateSendMsgStatusEnum e : UpdateSendMsgStatusEnum.values()) {
            if (e.getGiveType() == giveType) {
                return e;
            }
        }
        return UpdateSendMsgStatusEnum.OTHER;
    }

    public String getSendMemo() {
        return sendMemo;
    }

    public void setSendMemo(String sendMemo) {
        this.sendMemo = sendMemo;
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType;
    }

    public Integer getGiveType() {
        return giveType;
    }

    public void setGiveType(Integer giveType) {
        this.giveType = giveType;
    }
}