package recipe.vo.second;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 自助机请求入参
 * @author： whf
 * @date： 2022-10-11 14:21
 */
@Setter
@Getter
public class SelfServiceMachineReqVO implements Serializable {

    private static final long serialVersionUID = 7053603256343517819L;

    private String mpiId;
    private Integer organId;
    private List<Integer> statusList;
    private Integer start;
    private Integer limit;
}
