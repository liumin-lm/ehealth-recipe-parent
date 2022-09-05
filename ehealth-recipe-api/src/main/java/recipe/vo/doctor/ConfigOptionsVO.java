package recipe.vo.doctor;

import lombok.Getter;
import lombok.Setter;

/**
 * @author fuzi
 * 配置项逻辑结果类
 */
@Setter
@Getter
public class ConfigOptionsVO {
    /**
     * 配置项
     */
    private String key;
    /**
     * 配置项内容
     */
    private String value;
    /**
     * 配置项返回处理类型：0：正常，1提示，2：阻断
     */
    private int type;

}
