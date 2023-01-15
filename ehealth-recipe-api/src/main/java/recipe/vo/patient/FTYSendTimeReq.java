package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @description： 腹透液配送时间入参
 * @author： whf
 * @date： 2022-07-15 16:53
 */
@Getter
@Setter
public class FTYSendTimeReq implements Serializable {

    private static final long serialVersionUID = 1375372375803514638L;

    @ItemProperty(alias = "省")
    private String province;

    @ItemProperty(alias = "市")
    private String city;

    @ItemProperty(alias = "区")
    private String district;

    @ItemProperty(alias = "开始时间")
    private Date startDate;

    @ItemProperty(alias = "结束时间")
    private Date endDate;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "处方id列表")
    private List<Integer> recipes;
}
