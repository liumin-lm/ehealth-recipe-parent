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
    private static final long serialVersionUID = -1550787664443643742L;

    @ItemProperty(alias = "开方机构")
    private Integer organId;

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

    /**
     * 出生日期
     */
    @Temporal(TemporalType.DATE)
    private Date birthday;

    /**
     * 病人性别
     */
    private String sexCode;
    private String sexName;

    @Temporal(TemporalType.DATE)
    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "Asia/Shanghai"
    )
    public Date getBirthday() {
        return this.birthday;
    }

}
