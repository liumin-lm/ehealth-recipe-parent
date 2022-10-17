package recipe.vo.second;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 自助机药品出参
 * @author： whf
 * @date： 2022-10-11 14:33
 */
@Setter
@Getter
public class DrugInfoResVo implements Serializable {
    private static final long serialVersionUID = -8963830698660292080L;
    private String drugName;
    private String drugTotal;
    private String useWay;
}
