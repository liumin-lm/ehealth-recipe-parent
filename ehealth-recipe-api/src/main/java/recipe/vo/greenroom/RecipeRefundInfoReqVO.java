package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class RecipeRefundInfoReqVO implements Serializable {
    private static final long serialVersionUID = 7465817826997583598L;

    @ItemProperty(alias = "医生id")
    private Integer doctorId;

    @ItemProperty(alias = "开始时间")
    private Date startTime;

    @ItemProperty(alias = "结束时间")
    private Date endTime;
}
