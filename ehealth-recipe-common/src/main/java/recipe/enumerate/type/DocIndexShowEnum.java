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

    SHOW(0,"显示"),
    HIDE(1,"隐藏");

    private Integer code;
    private String name;

}
