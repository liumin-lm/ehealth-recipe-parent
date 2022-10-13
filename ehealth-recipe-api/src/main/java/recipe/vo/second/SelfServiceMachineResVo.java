package recipe.vo.second;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 自助机查询请求出参
 * @author： whf
 * @date： 2022-10-11 14:26
 */
@Setter
@Getter
public class SelfServiceMachineResVo implements Serializable {
    private static final long serialVersionUID = -2051951000779514276L;

    private Integer recipeId;
    private String patientName;
    private String doctorDepart;
    private String diseaseName;
    private String signTime;
    private String doctorName;
    private Integer subState;
    private String subStateText;
    private Integer processState;
    //药品信息
    private List<DrugInfoResVo> rp;
    private String memo;
}
