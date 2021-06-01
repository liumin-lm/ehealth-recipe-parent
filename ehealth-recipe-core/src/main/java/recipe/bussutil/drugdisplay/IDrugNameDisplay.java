package recipe.bussutil.drugdisplay;

import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2021/3/10
 */
public interface IDrugNameDisplay {

    List<String> sortConfigName(Map<String, Integer> keyMap);
}
