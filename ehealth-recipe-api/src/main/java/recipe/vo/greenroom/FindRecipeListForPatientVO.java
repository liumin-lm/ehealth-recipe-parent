package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description
 * @Author yzl
 * @Date 2022-11-03
 */
@Data
public class FindRecipeListForPatientVO implements Serializable {
    private static final long serialVersionUID = 6293951072316234495L;

    @ItemProperty(alias = "onready待处理, ongoing进行中, isover已结束")
    private String tabStatus;

    private String mpiId;

    private Integer start;

    private Integer limit;

    private Date startTime;

    private Date endTime;
}
