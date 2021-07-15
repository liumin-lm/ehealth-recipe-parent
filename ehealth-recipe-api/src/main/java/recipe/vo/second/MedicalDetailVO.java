package recipe.vo.second;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 二方 电子病历 对象 vo
 *
 * @author fuzi
 */
@Getter
@Setter
public class MedicalDetailVO implements Serializable {

    private static final long serialVersionUID = -1136736898140046189L;
    private Integer medicalDetailId;
    private Integer docIndexId;
    private String detail;
    private Date createTime;
    private Date lastModify;
    private List<EmrConfigVO> detailList;
}
