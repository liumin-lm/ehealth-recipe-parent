package recipe.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 公共分页对象
 *
 * @author fuzi
 */
@Getter
@Setter
public class PageVO {

    /**
     * 分页总条数
     */
    private Integer total;
    /**
     * 页数
     */
    private Integer start;
    /**
     * 分页条数
     */
    private Integer limit;
}
