package recipe.enumerate.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author maoze
 * @description
 * @date 2022年02月15日 11:59
 */
@Getter
@AllArgsConstructor
public enum RecipeAuditStateEnum {

    DEFAULT(0,"默认"),
    PENDING_REVIEW(1,"待审核"),
    REVIEWING(2,"审核中"),
    FAIL(3,"审核未通过"),
    FAIL_DOC_CONFIRMING(4,"未通过，医生确认中"),
    PASS(5,"审核通过"),
    DOC_FORCED_PASS(6,"医生强制通过"),
    NO_REVIEW(7,"无需审核"),;

    private Integer type;

    private String msg;

}
