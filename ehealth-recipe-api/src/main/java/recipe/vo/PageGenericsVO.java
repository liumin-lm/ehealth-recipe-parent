package recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 公共分页对象
 *
 * @author fuzi
 */
@Getter
@Setter
public class PageGenericsVO<T> extends PageVO  {
    /**
     *出入参数
     */
    private T data;

    /**
     *出入参数
     */
    private List<T> dataList;
}
