package recipe.bean;

import java.math.BigDecimal;
import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/7/3.
 */
public class DepDetailBean {

    private Integer depId;

    private String depName;

    private List<Integer> payModeList;

    /**
     *  给药方式文案显示
     */
    private String giveModeText;

    /**
     * 处方费
     */
    private BigDecimal recipeFee;

    /**
     * 配送费
     */
    private BigDecimal expressFee;

    private String unSendTitle;

    /**
     * 以下为钥世圈字段，跳转链接时需要带上
     */
    private String gysCode;

    /**
     * sendMethod    0：送货上门   1：到店取药  2：两者皆可
     */
    private String sendMethod;

    /**
     * payMethod     0：线下支付   1：在线支付  2：两者皆可
     */
    private String payMethod;

    public Integer getDepId() {
        return depId;
    }

    public void setDepId(Integer depId) {
        this.depId = depId;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }

    public List<Integer> getPayModeList() {
        return payModeList;
    }

    public void setPayModeList(List<Integer> payModeList) {
        this.payModeList = payModeList;
    }

    public String getGiveModeText() {
        return giveModeText;
    }

    public void setGiveModeText(String giveModeText) {
        this.giveModeText = giveModeText;
    }

    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    public String getUnSendTitle() {
        return unSendTitle;
    }

    public void setUnSendTitle(String unSendTitle) {
        this.unSendTitle = unSendTitle;
    }

    public String getGysCode() {
        return gysCode;
    }

    public void setGysCode(String gysCode) {
        this.gysCode = gysCode;
    }

    public String getSendMethod() {
        return sendMethod;
    }

    public void setSendMethod(String sendMethod) {
        this.sendMethod = sendMethod;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }
}
