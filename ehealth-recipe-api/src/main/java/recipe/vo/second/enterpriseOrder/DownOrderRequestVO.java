package recipe.vo.second.enterpriseOrder;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 第三方下载处方订单信息入参
 * @author： yinsheng
 * @date： 2022-03-24 09:32
 */
@Getter
@Setter
public class DownOrderRequestVO implements Serializable {
    private static final long serialVersionUID = 653498815969534577L;

    @ItemProperty(alias = "平台分配药企唯一编码")
    private String appKey;
    @ItemProperty(alias = "患者身份证号")
    private String idCard;
    @ItemProperty(alias = "机构ID")
    private Integer organId;
    @ItemProperty(alias = "HIS处方号")
    private String recipeCode;
    @ItemProperty(alias = "查询开始时间")
    private String beginTime;
    @ItemProperty(alias = "查询结束时间")
    private String endTime;

}
