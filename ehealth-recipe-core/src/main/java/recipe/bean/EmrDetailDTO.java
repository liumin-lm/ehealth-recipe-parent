package recipe.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * 电子病例明细对象
 *
 * @author fuzi
 */
@Setter
@Getter
public class EmrDetailDTO {

    public EmrDetailDTO(String key, String name, String type, String value, Boolean required) {
        this.key = key;
        this.name = name;
        this.type = type;
        this.value = value;
        this.required = required;
    }

    /**
     * 配置代码
     */
    private String key;
    /**
     * 配置名称
     */
    private String name;
    /**
     * 配置类型
     */
    private String type;
    /**
     * 配置相关值
     */
    private String value;
    /**
     * 是否必填
     */
    private Boolean required;
}


