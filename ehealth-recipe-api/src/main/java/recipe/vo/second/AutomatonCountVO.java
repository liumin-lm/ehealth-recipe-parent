package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.util.List;

/**
 * 自助机大盘展示对象
 * @author
 */
@Data
@Schema
public class AutomatonCountVO extends PageVO {

    @ItemProperty(alias = "时间 ")
    private String time;
    @ItemProperty(alias = "次数")
    private Integer count;
    @ItemProperty(alias = "处方来源机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer organId;

    /**
     * 申请完成次数统计用
     */
    @ItemProperty(alias = "申请次数")
    private Integer applyAount;
    @ItemProperty(alias = "完成次数")
    private Integer finishAount;

}
