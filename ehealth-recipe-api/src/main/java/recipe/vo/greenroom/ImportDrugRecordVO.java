package recipe.vo.greenroom;


import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 导入药品记录
 *
 * @author yinfeng 2020-05-21
 */
@Data
public class ImportDrugRecordVO implements java.io.Serializable {
    public static final long serialVersionUID = -3983203173007645688L;

    @ItemProperty(alias = "药品记录ID")
    private Integer recordId;

    @ItemProperty(alias = "导入文件名称")
    private String fileName;

    @ItemProperty(alias = "新增药品数")
    private Integer addNum;

    @ItemProperty(alias = "更新药品数")
    private Integer updateNum;

    @ItemProperty(alias = "失败药品数")
    private Integer failNum;

    @ItemProperty(alias = "导入人员")
    private String importOperator;

    @ItemProperty(alias = "错误提示")
    private String errMsg;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "导入状态 1、导入成功——指写入平台成功 2、正在导入——还没存在oss里，或者已存在oss里，未但导入至平台 3、导入失败，点击查看原因——导入失败（不管哪里失败都展示失败）=第三种，点击查看原因，出现报错内容弹窗，如右图所示")
    private Integer status;

    @FileToken
    private String fileId;

    private List<ImportDrugRecordMsgVO> importDrugRecordMsg;
}