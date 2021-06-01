package recipe.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Created by liuxiaofeng on 2021/2/20 0020.
 */
public class RecipeInvalidDTO implements Serializable {
    private static final long serialVersionUID = -452309539833305947L;

    private String invalidType;
    private Date invalidDate;

    public String getInvalidType() {
        return invalidType;
    }

    public void setInvalidType(String invalidType) {
        this.invalidType = invalidType;
    }

    public Date getInvalidDate() {
        return invalidDate;
    }

    public void setInvalidDate(Date invalidDate) {
        this.invalidDate = invalidDate;
    }
}
