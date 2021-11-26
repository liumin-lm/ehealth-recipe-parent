package recipe.util;

import org.apache.commons.collections.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * list 工具类 用于处理list相关公共操作
 *
 * @author fuzi
 */
public class ListValueUtil {


    /**
     * 获取 List<List<T>> 中的最小交集 list 如：
     * "1", "2", "3", "4"
     * "1", "2", "3"
     * "2"，"1"
     * 返回 ："1", "2"
     *
     * @param groupList 需要获取数据
     * @param <T>       任意想通类型
     * @return 最小交集 list
     */
    public static <T> List<T> minIntersection(List<List<T>> groupList) {
        List<List<T>> group = groupList.stream().filter(CollectionUtils::isNotEmpty).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(group)) {
            return null;
        }
        group = group.stream().sorted(Comparator.comparingInt(List::size)).collect(Collectors.toList());
        List<T> resultList = group.get(0);

        if (1 == group.size()) {
            return resultList;
        }

        if (2 == group.size()) {
            boolean enterprise = group.get(1).containsAll(group.get(0));
            return enterprise ? resultList : null;
        }

        for (List<T> list : group) {
            boolean enterprise = list.containsAll(resultList);
            if (!enterprise) {
                resultList = null;
                break;
            }
        }
        return resultList;
    }
}
