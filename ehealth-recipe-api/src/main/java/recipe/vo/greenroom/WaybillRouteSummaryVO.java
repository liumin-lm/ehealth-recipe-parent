package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author zgy
 * @Date 2023-01-04
 * 运单路由
 */
@Data
public class WaybillRouteSummaryVO implements Serializable {

    private static final long serialVersionUID = -4911107275215726685L;

    @ItemProperty(alias="路由节点产生的时间")
    private Date acceptTime;

    @ItemProperty(alias="路由节点具体描述")
    private String acceptRemark;

    @ItemProperty(alias="路由节点具体描述")
    private String czCode;
}
