package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 机构药品目录同步字段
 *
 *
 */
@Data
public class OrganDrugListSyncFieldVo implements java.io.Serializable {
    private static final long serialVersionUID = -7090271704460035622L;

    @ItemProperty(alias = "id")
    private Integer id;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "<dic>\n" +
            "\t<item key=\"1\" text=\"新增药品\"/>\n" +
            "\t<item key=\"2\" text=\"更新药品\"/>\n" +
            "\t<item key=\"3\" text=\"删除药品\"/>\n" +
            "</dic>")
    private String type;

    @ItemProperty(alias = "字段名称")
    private String fieldName;

    @ItemProperty(alias = "字段编码")
    private String fieldCode;

    @ItemProperty(alias = "是否同步 同步勾选 0否 1是")
    private String isSync;

    @ItemProperty(alias = "是否允许编辑 0不允许 1允许")
    private String isAllowEdit;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "更新时间")
    private Date updateTime;






}