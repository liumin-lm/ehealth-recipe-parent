package recipe.util;

import com.google.common.collect.Lists;
import com.ngari.recipe.dto.PermutationDTO;
import org.apache.commons.collections.CollectionUtils;
import org.paukov.combinatorics3.Generator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * 根据数据源 找出 符合目标值的 数组排列组合集合
     *
     * @param source 排列组合数据源
     * @param target 对比获取目标值
     * @return 目标值，数据源集合
     */
    public static List<List<Integer>> permutationDrugs1(List<PermutationDTO> source, List<Integer> target) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(target)) {
            return Collections.emptyList();
        }
        //生产穷举 排列组合
        List<List<PermutationDTO>> permutationList = Generator.subset(source).simple().stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList());
        //遍历每种排列组合获取结果
        List<List<List<Integer>>> result = new ArrayList<>();
        permutationList.forEach(a -> {
            //每种排列组合数据源的value列表
            List<List<Integer>> valueList = a.stream().map(PermutationDTO::getValue).sorted(Comparator.comparing(List::size)).collect(Collectors.toList());
            //获取每种排列组合的-目标值拆分集合
            List<List<Integer>> list = targetSplit(target, valueList);
            if (CollectionUtils.isNotEmpty(list)) {
                result.add(list);
            }
        });
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        //排序拿到最小的目标值拆分集合
        return result.stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList()).get(0);
    }

    /**
     * @param target    对比获取目标值
     * @param valueList 每种排列组合数据源的value列表
     * @return 目标值拆分集合
     */
    private static List<List<Integer>> targetSplit(List<Integer> target, List<List<Integer>> valueList) {
        if (CollectionUtils.isEmpty(valueList)) {
            return Collections.emptyList();
        }
        List<List<Integer>> targetGroup = new ArrayList<>();
        for (List<Integer> value : valueList) {
            //value与目标值取交集，交集为目标值拆分集合
            List<Integer> targetList = value.stream().filter(target::contains).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(targetList)) {
                continue;
            }
            //拆分每个目标值组成的集合不能超过5
            if (targetList.size() > 5) {
                targetGroup.addAll(Lists.partition(targetList, 5));
            } else {
                targetGroup.add(targetList);
            }
            //value与目标值取差集 ，把差集赋值给本次遍历的目标值
            target = target.stream().filter(b -> !value.contains(b)).collect(Collectors.toList());
        }
        //差集为空则所有目标数据都已匹配
        if (CollectionUtils.isEmpty(target)) {
            return targetGroup;
        }
        return Collections.emptyList();
    }

    public static void main(String[] args) {
        List<PermutationDTO> list = new ArrayList<>();
        PermutationDTO b = new PermutationDTO("A", Arrays.asList(1, 2, 3, 4, 5, 6));
        list.add(b);
        PermutationDTO b1 = new PermutationDTO("B", Arrays.asList(7, 8, 9, 10));
        list.add(b1);
        PermutationDTO b2 = new PermutationDTO("C", Arrays.asList(1, 2, 3, 4));
        list.add(b2);
        PermutationDTO b3 = new PermutationDTO("D", Arrays.asList(5, 6, 7, 8));
        list.add(b3);
        PermutationDTO b4 = new PermutationDTO("E", Arrays.asList(9));
        list.add(b4);
        PermutationDTO b5 = new PermutationDTO("F", Arrays.asList(10));
        list.add(b5);
        PermutationDTO b7 = new PermutationDTO("H", Arrays.asList(7, 8, 9, 10, 6));
        list.add(b7);
        List<Integer> target = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).sorted().collect(Collectors.toList());
        List<List<Integer>> drugIdsList = ListValueUtil.permutationDrugs1(list, target);
        System.out.println(drugIdsList);

    }


}
