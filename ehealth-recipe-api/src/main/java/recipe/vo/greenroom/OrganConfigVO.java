package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;
import java.util.List;

/**
 * @description： 机构配置vo
 * @author： lium
 * @date： 2022-05-09 11:10
 */
@Data
public class OrganConfigVO  implements Serializable {

    @ItemProperty(alias = "id")
    private Integer id;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "药品是否支持接口同步 默认为0（0：开关关闭；1：开关打开）")
    private Boolean enableDrugSync;

    @ItemProperty(
            alias = "药品同步  接口对接模式 1 自主查询  2 主动推送   默认 1"
    )
    private Integer dockingMode;

    @ItemProperty(alias = "新增药品目录同步是否需要人工审核开关，默认为0（0：系统审核；1：人工审核）")
    private Boolean enableDrugSyncArtificial;

    @ItemProperty(
            alias = "药品同步 定时更新时间"
    )
    private Time regularTime;

    @ItemProperty(alias = "药品数据来源（0:非互联网药品 1：互联网药品 100：全部）")
    private String drugDataSource;

    @ItemProperty(alias = "药品同步是否勾选新增")
    private Boolean enableDrugAdd;

    @ItemProperty(alias = "药品同步是否勾选更新")
    private Boolean enableDrugUpdate;

    @ItemProperty(
            alias = "药品同步是否勾选删除（禁用）"
    )
    private Boolean enableDrugDelete;


    @ItemProperty(alias = "新增 药品同步 数据范围   1药品类型 2  药品剂型  默认1")
    private Integer addDrugDataRange;

    @ItemProperty(alias = "修改 药品同步 数据范围   1药品类型 2  药品剂型  默认1")
    private Integer updateDrugDataRange;

    @ItemProperty(alias = "删除 药品同步 数据范围   1药品类型 2  药品剂型  默认1")
    private Integer delDrugDataRange;

    @ItemProperty(alias = "新增 同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String addSyncDrugType;

    @ItemProperty(alias = "修改 同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String updateSyncDrugType;

    @ItemProperty(alias = "删除 同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String delSyncDrugType;

    @ItemProperty(alias = "新增药品同步剂型list")
    private String addDrugFromList;
    @ItemProperty(alias = "修改药品同步剂型list")
    private String updateDrugFromList;
    @ItemProperty(alias = "删除药品同步剂型list")
    private String delDrugFromList;

    //作废
    @ItemProperty(
            alias = "药品同步 数据范围   1所有药品（无限制条件） 2  药品剂型  默认 2"
    )
    private Integer drugDataRange;

    //作废
    @ItemProperty(alias = "手动同步 剂型list暂存")
    private String drugFromList;


    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "更新时间")
    private Date updateTime;

    private List<OrganDrugListSyncFieldVo> organDrugListSyncFieldList;

    @ItemProperty(alias = "同步到哪个机构去，选择需要同步目录的机构（将当前目录同步到选中机构中）")
    private String toOrganIds;

}
