package recipe.vo.doctor;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 药品说明书
 * @author： whf
 * @date： 2021-08-26 9:40
 */
@Getter
@Setter
public class DrugBookVo implements Serializable {

    @ItemProperty(alias = "药品说明书序号")
    private Integer dispensatoryId;

    @ItemProperty(alias = "药品名称")
    private String name;

    @ItemProperty(alias = "生产厂家")
    private String manufacturers;

    @ItemProperty(alias = "通用名")
    private String drugName;

    @ItemProperty(alias = "英文名")
    private String englishName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "规格")
    private String specs;

    @ItemProperty(alias = "成分")
    private String composition;

    @ItemProperty(alias = "性状")
    private String property;

    @ItemProperty(alias = "适应症")
    private String indication;

    @ItemProperty(alias = "用法用量")
    private String dosage;

    @ItemProperty(alias = "不良反应")
    private String reactions;

    @ItemProperty(alias = "禁忌")
    private String contraindications;

    @ItemProperty(alias = "注意事项")
    private String attention;

    @ItemProperty(alias = "特殊人群用药")
    private String specialPopulations;

    @ItemProperty(alias = "药物相互作用")
    private String interaction;

    @ItemProperty(alias = "药理作用")
    private String pharmacological;

    @ItemProperty(alias = "贮藏")
    private String storage;

    @ItemProperty(alias = "有效期")
    private String validityDate;

    @ItemProperty(alias = "批准文号")
    private String approvalNumber;

    @ItemProperty(alias = "页面ID")
    private String pageId;

    @ItemProperty(alias = "图片地址")
    private String picUrl;

    @ItemProperty(alias = "信息来源")
    private Integer source;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后操作时间")
    private Date lastModifyTime;

    @ItemProperty(alias = "药品Id")
    private Integer drugId;
}
