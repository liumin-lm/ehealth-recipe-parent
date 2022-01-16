package recipe.vo.second;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 签名信息用于前端展示
 *
 * @author fuzi
 */
@Getter
@Setter
public class ApothecaryVO implements Serializable {

    private static final long serialVersionUID = 2398885985048336367L;
    @ItemProperty(alias = "订单ID")
    private Integer recipeId;

    @ItemProperty(alias = "医生签名Id")
    private Integer doctorId;

    @ItemProperty(alias = "审核药师Id")
    private Integer checkerId;

    @ItemProperty(alias = "审核药师姓名")
    private String checkApothecaryName;

    @ItemProperty(alias = "审核药师身份证")
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String checkApothecaryIdCard;

    @ItemProperty(alias = "发药药师Id")
    private Integer giveUserId;
    @ItemProperty(alias = "发药药师姓名")
    private String giveUserName;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "发药药师身份证")
    private String giveUserIdCard;
    /**
     * 发药药师身份证 明文 运营平台使用
     */
    private String giveUserIdCardCleartext;
    private String giveUserSignImg;
    private String giveUserSignImgToken;
}
