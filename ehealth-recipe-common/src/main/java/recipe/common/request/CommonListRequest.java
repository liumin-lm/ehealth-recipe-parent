package recipe.common.request;

import java.io.Serializable;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/3/8
 */
public class CommonListRequest extends CommonRequest implements Serializable{

    private static final long serialVersionUID = -1775474631275097922L;

    protected int start;

    protected int limit;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
