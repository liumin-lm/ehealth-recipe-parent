package recipe.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* @Description: UpdateSendMsgStatusEnum 配送信息同步his状态
* @Author: JRK
* @Date: 20200312
*/
public enum UpdateSendMsgStatusEnum {

    HIS_SEND("医院取药", "0", new ArrayList<>(Arrays.asList(RecipeBussConstant.PAYMODE_TO_HOS))),

    LOGISTIC_SEND("物流配送", "1", new ArrayList<>(Arrays.asList(RecipeBussConstant.PAYMODE_ONLINE, RecipeBussConstant.PAYMODE_COD))),

    PHARAMCY_SEND("药店取药", "2", new ArrayList<>(Arrays.asList(RecipeBussConstant.PAYMODE_TFDS))),

    OTHER("其他", "3", new ArrayList<>(Arrays.asList(RecipeBussConstant.PAYMODE_DOWNLOAD_RECIPE, RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE))),

    ALL("都支持", "4", new ArrayList<>(Arrays.asList(RecipeBussConstant.PAYMODE_COMPLEX)));

    private String sendMemo;

    private String sendType;

    private List<Integer> giveType;

    UpdateSendMsgStatusEnum(String sendMemo, String sendType, List<Integer> giveType) {
        this.sendType = sendType;
        this.sendMemo = sendMemo;
        this.giveType = giveType;
    }

    public static UpdateSendMsgStatusEnum fromGiveType(Integer giveType) {
        for (UpdateSendMsgStatusEnum e : UpdateSendMsgStatusEnum.values()) {
            if (e.getGiveType().contains(giveType)) {
                return e;
            }
        }
        return UpdateSendMsgStatusEnum.LOGISTIC_SEND;
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

    public List<Integer> getGiveType() {
        return giveType;
    }

    public void setGiveType(List<Integer> giveType) {
        this.giveType = giveType;
    }
}