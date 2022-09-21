package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class RecipeRefundInfoReqVO extends PageVO implements Serializable {
    private static final long serialVersionUID = 7465817826997583598L;

    @ItemProperty(alias = "医生id")
    private Integer doctorId;

    @ItemProperty(alias = "开始时间")
    private Date startTime;

    @ItemProperty(alias = "结束时间")
    private Date endTime;
}
