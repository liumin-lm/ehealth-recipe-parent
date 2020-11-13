package recipe.bean;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Created by liuxiaofeng on 2020/11/6.
 *         处方分账明细
 */
public class RecipeSplitDTO implements Serializable{
    private static final long serialVersionUID = 8025593830697527785L;

    @ItemProperty(alias = "参与方编码")
    private String accountNo;

    @ItemProperty(alias = "参与方类型")
    private Integer accountType;

    @ItemProperty(alias = "参与方名称")
    private String accountName;

    @ItemProperty(alias = "分账金额")
    private BigDecimal amount;

    @ItemProperty(alias = "二级明细")
    private RecipeSplitDTO splitDetail;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public RecipeSplitDTO getSplitDetail() {
        return splitDetail;
    }

    public void setSplitDetail(RecipeSplitDTO splitDetail) {
        this.splitDetail = splitDetail;
    }
}
