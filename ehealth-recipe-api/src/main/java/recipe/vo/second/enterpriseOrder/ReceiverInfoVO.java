package recipe.vo.second.enterpriseOrder;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ReceiverInfoVO implements Serializable {
    private static final long serialVersionUID = -3554919043308312895L;
    @ItemProperty(alias = "收货人")
    private String receiver;

    @ItemProperty(alias = "收货人手机号")
    private String recMobile;

    @ItemProperty(alias = "地址（省）")
    private String province;

    private String provinceCode;

    @ItemProperty(alias = "地址（市）")
    private String city;

    private String cityCode;

    @ItemProperty(alias = "地址（区县）")
    private String district;

    @ItemProperty(alias = "区县编码")
    private String districtCode;

    @ItemProperty(alias = "街道编码")
    private String streetCode;

    @ItemProperty(alias = "街道")
    private String street;

    @ItemProperty(alias = "详细地址")
    private String address;

    @ItemProperty(alias = "社区编码")
    private String communityCode;

    @ItemProperty(alias = "社区名称")
    private String communityName;

    @ItemProperty(alias = "邮政编码")
    private String zipCode;

    @ItemProperty(alias = "期望配送日期")
    private String expectSendDate;

    @ItemProperty(alias = "期望配送时间")
    private String expectSendTime;
}
