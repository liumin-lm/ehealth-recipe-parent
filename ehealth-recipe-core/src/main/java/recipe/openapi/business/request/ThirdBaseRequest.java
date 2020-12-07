package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\28 0028 11:08
 */
@Data
public class ThirdBaseRequest implements Serializable{

    private static final long serialVersionUID = 6775365035285709683L;

    private String appkey;

    private String tid;
}
