package recipe.enumerate.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author maoze
 * @description
 * @date 2021年12月21日 16:16
 */
@Getter
@AllArgsConstructor
public enum DocIndexShowEnum {

    NO_AUDIT(0,"不用审核"),
    SHOW(0,"显示"),
    HIDE(1,"隐藏"),
    NORMAL(1,"正常"),
    REVOKE(0,"撤销");

    private Integer code;
    private String name;

}
