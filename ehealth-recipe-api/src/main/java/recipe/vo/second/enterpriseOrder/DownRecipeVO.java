package recipe.vo.second.enterpriseOrder;

import com.fasterxml.jackson.annotation.JsonFormat;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.base.BaseRecipeDetailVO;
import recipe.vo.base.BaseRecipeVO;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description： 第三方下载处方信息
 * @author： yins
 * @date： 2021-12-08 15:50
 */
@Getter
@Setter
public class DownRecipeVO extends BaseRecipeVO implements Serializable {
    private static final long serialVersionUID = -551657299563879216L;
    @ItemProperty(alias = "开方机构")
    private Integer organId;

    @ItemProperty(alias = "组织机构代码")
    private String organizeCode;

    @ItemProperty(alias = "签名文件的URL")
    private String signFileUrl;

    @ItemProperty(alias = "审方机构名称")
    private String checkOrganName;

    @ItemProperty(alias = "审核药师姓名")
    private String checkerName;

    @ItemProperty(alias = "处方详情")
    private List<BaseRecipeDetailVO> recipeDetailList;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;

    @ItemProperty(alias = "处方签名图片")
    private String recipeSignImgUrl;

    @ItemProperty(alias = "出生日期")
    @Temporal(TemporalType.DATE)
    private Date birthday;

    @ItemProperty(alias = "病人性别")
    //后续病人性别如果要对外不使用这个字段
    private String sexCode;

    //后续病人性别如果要对外使用这个字段
    private String gender;

    @ItemProperty(alias = "病人性别")
    private String sexName;

    @ItemProperty(alias = "处方费用")
    private BigDecimal recipeFee;

    @Temporal(TemporalType.DATE)
    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "Asia/Shanghai"
    )
    public Date getBirthday() {
        return this.birthday;
    }

    @ItemProperty(alias = "处方签（Base64图片编码）")
    private String recipeSignImg;


    @ItemProperty(alias = "开方时间")
    @Temporal(TemporalType.DATE)
    private Date createDate;

    @ItemProperty(alias = "审核日期")
    @Temporal(TemporalType.DATE)
    private Date checkDate;

    @ItemProperty(alias = "失效时间")
    @Temporal(TemporalType.DATE)
    private Date invalidTime;

    @Temporal(TemporalType.DATE)
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Shanghai"
    )
    public Date getCreateDate() {
        return createDate;
    }

    @Temporal(TemporalType.DATE)
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Shanghai"
    )
    public Date getCheckDate() {
        return checkDate;
    }

    @Temporal(TemporalType.DATE)
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Shanghai"
    )
    public Date getInvalidTime() {
        return invalidTime;
    }

    @ItemProperty(alias = "处方剂型 1 饮片方 2 颗粒方")
    private Integer recipeDrugForm;

}
