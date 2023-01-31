package recipe.vo.doctor;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 医生默认数据
 * @author fuzi
 */
@Getter
@Setter
public class DoctorDefaultVO implements Serializable {
    private static final long serialVersionUID = -9102550628733728512L;
    private Integer id;

    private Integer organId;

    private Integer doctorId;
    /**
     * 类别 ：0:默认，1:药房，2药企
     */
    private Integer category;
    /**
     * 0 默认
     * 类型：
     * category =1 时type： 1西药 2中成药 3中药 4膏方
     * category =2 时type： 1机构 2药企
     */
    private Integer type;
    /**
     * 关联数据主键，如：药房id，药企id
     */
    private Integer idKey;

    /**
     * 查询筛选条件用 复诊id 非必传
     */
    private Integer clinicId;
    /**
     * 查询筛选条件用 行政科室id 非必传
     */
    private Integer departId;
}
