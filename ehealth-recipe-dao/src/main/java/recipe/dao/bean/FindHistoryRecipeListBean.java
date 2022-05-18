package recipe.dao.bean;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 患者端线上 处方列表查询 bean
 * @author： whf
 * @date： 2021-06-03 15:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindHistoryRecipeListBean implements Serializable {

    /**
     *
     */
    private Integer consultId;

    /**
     *机构
     */
    private Integer organId;

    /**
     *医生工号
     */
    private Integer doctorId;

    /**
     *mpiId
     */
    private String mpiId;

    /**
     * 开始时间
     */
    private String startDate;

    /**
     * 结束时间
     */
    private String endDate;

    /**
     *医生工号
     */
    private Integer start;

    /**
     *mpiId
     */
    private Integer limit;

}
