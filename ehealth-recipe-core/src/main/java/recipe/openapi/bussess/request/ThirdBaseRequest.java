package recipe.openapi.bussess.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 15:14
 */
@Data
public class ThirdBaseRequest implements Serializable{
    private static final long serialVersionUID = -2682231262232427106L;

    private String appkey;

    private String tid;
}
