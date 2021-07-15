package recipe.vo.second;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 二方 电子病历 对象 vo
 *
 * @author fuzi
 */
@Getter
@Setter
public class EmrConfigVO implements Serializable {
    private static final long serialVersionUID = -6604988044493266204L;
    private String name;
    private String type;
    private String value;
    private Boolean required;
    private String key;
    private String jsonValue;
}
