package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询his药品信息入参
 */
@Data
public class HisDrugInfoReqVO implements Serializable {
    private static final long serialVersionUID = 7681975140492748962L;

    @ItemProperty(alias = "机构ID")
    private Integer organId;
    @ItemProperty(alias = "搜索关键词")
    private String searchKeyWord;
    @ItemProperty(alias = "搜索范围 0门诊 1住院 2 全部")
    private Integer searchRang;
    @ItemProperty(alias = "搜索方式")
    private Integer searchWay;
    @ItemProperty(alias = "页码")
    private Integer start;
    @ItemProperty(alias = "每页条数")
    private Integer limit;
}
