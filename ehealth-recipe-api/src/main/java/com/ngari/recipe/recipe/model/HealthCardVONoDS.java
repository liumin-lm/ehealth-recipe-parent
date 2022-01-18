package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zgy
 * @date 2021/12/29 16:27
 */
@Schema
@Data
public class HealthCardVONoDS implements Serializable {

    private static final long serialVersionUID = 5585993275553236361L;
    @ItemProperty(alias = "证卡内码")
    private Integer healthCardId;

    @ItemProperty(alias = "主索引")
    private String mpiId;

    @ItemProperty(alias = "证卡号码（转大写）")
    private String cardId;

    @ItemProperty(alias = "证卡号码（转大写）")
    private String cardIdDS;

    @ItemProperty(alias = "证卡类型")
    @Dictionary(id = "eh.mpi.dictionary.CardType")
    private String cardType;

    @ItemProperty(alias = "发卡机构")
    private Integer cardOrgan;

    @ItemProperty(alias = "卡状态")
    private Integer cardStatus;

    @ItemProperty(alias = "有效期")
    private Date validDate;

    @ItemProperty(alias = "用户输入的医保卡")
    private String initialCardID;

    @ItemProperty(alias = "是否是默认卡")
    private Boolean defaultCard;

    /**
     * @date： 2017/12/7
     * @description： 个人新增:local 还是 远程拉取:remote
     */
    @ItemProperty(alias = "卡片来源")
    private String cardSource;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "卡名称")
    private String healthCardName;

    @ItemProperty(alias = "最近使用时间")
    private Date lastUseTime;

    @ItemProperty(alias = "使用标识 0：未使用过 1：使用过")
    private Boolean useFlag;

    private String cardBalance;

    private Boolean showFlag;

    @ItemProperty(alias = "患者医院patId")
    private String patId;

    public String getCardIdDS() {
        return this.cardId;
    }


}
