package recipe.constant;

/**
 * 分页信息常量
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2016/4/26.
 */
public class PageConstant
{

    /**
     * 每页显示数量
     */
    private static int PAGE_LIMIT = 6;

    public static int getPageLimit(Integer limit){
        if(null != limit && limit > 0){
            return limit;
        }

        return PAGE_LIMIT;
    }
}
