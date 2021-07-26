package recipe.dto;

import eh.cdr.api.vo.response.EmrConfigRes;

/**
 * 电子病例明细对象
 *
 * @author fuzi
 */
public class EmrDetailDTO extends EmrConfigRes {
    public EmrDetailDTO(String key, String name, String type, String value, Boolean required) {
        super.setKey(key);
        super.setName(name);
        super.setRequired(required);
        super.setType(type);
        super.setValue(value);
    }
}


