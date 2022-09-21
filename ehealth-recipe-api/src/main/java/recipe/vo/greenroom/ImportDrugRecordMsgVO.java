package recipe.vo.greenroom;


import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 导入药品记录提示
 *
 * @author liumin 20220616
 */
@Data
public class ImportDrugRecordMsgVO implements java.io.Serializable {
    public static final long serialVersionUID = -3983203173007645688L;

    @ItemProperty(alias = "ID")
    private Integer id;

    @ItemProperty(alias = "药品记录ID")
    private Integer importDrugRecordId;

    @ItemProperty(alias = "错误提示")
    private String errMsg;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModifyDate;


}