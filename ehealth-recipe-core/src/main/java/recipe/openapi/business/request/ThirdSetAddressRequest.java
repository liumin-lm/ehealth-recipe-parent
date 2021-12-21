package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 15:55
 * 入参用 无需脱敏
 */
@Data
public class ThirdSetAddressRequest implements Serializable {
    private static final long serialVersionUID = 7656655233610326896L;

    private String appkey;

    private String tid;

    private String receiver;

    private String recMobile;

    private String recTel;

    private String address1;

    private String address2;

    private String address3;

    private String address4;

    private String streetAddress;

    private String zipCode;

    private Date createDt;

    private Date lastModify;
}
