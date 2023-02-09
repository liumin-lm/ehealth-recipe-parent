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
     * 根据数据源 找出 符合目标值的 数组排列组合集合，若无符合数据源集合。则递减数据源 从新找出
     *
     * @param source 排列组合数据源
     * @param target 对比获取目标值
     * @return 目标值，数据源集合
     */
    public static List<List<Integer>> permutationDrugsTargetDecline(List<PermutationDTO> source, List<Integer> target) {
        for (int i = target.size(); i > 0; i--) {
            List<Integer> targetDecline = target.stream().limit(i).collect(Collectors.toList());
            List<List<Integer>> drugIdsList = permutationDrugs(source, targetDecline);
            if (CollectionUtils.isNotEmpty(drugIdsList)) {
                return drugIdsList;
            }
        }
        return Collections.emptyList();
    }


    /**
     * 根据数据源 找出 符合目标值的 数组排列组合集合
     *
     * @param source 排列组合数据源
     * @param target 对比获取目标值
     * @return 目标值，数据源集合
     */
    public static List<List<Integer>> permutationDrugs(List<PermutationDTO> source, List<Integer> target) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(target)) {
            return Collections.emptyList();
        }
        //生产穷举 排列组合
        List<List<PermutationDTO>> permutationList = Generator.subset(source).simple().stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList());

        //获取最小组合-按照条件从新分组
        List<List<Integer>> minSplitKeyList = new ArrayList<>();
        List<List<Integer>> minKeyList = permutation(permutationList, target, null);
        minKeyList.forEach(a -> minSplitKeyList.addAll(Lists.partition(a, 5)));

        //获取符合条件的最小组合
        List<List<Integer>> sizeKeyList = permutation(permutationList, target, 5);

        //计算最小组合 与 条件组合 中最优组合-返回最优组合id分组
        if (minSplitKeyList.size() < sizeKeyList.size()) {
            return minSplitKeyList;
        } else {
            return sizeKeyList;
        }
    }

    /**
     * 筛选符合目标的排列组合结果
     *
     * @param permutationList 穷举的排列组合
     * @param target          目标集合
     * @param isSize          筛选条件
     * @return 符合目标的 排列组合结果
     */
    private static List<List<Integer>> permutation(List<List<PermutationDTO>> permutationList, List<Integer> target, Integer isSize) {
        for (List<PermutationDTO> permutation : permutationList) {
            //条件过滤
            if (null != isSize) {
                boolean size = permutation.stream().anyMatch(a -> a.getValue().size() > isSize);
                if (size) {
                    continue;
                }
            }
            //筛选符合目标的排列组合结果
            Set<Integer> valueSet = new HashSet<>();
            List<List<Integer>> idsList = permutation.stream().map(a -> {
                valueSet.addAll(a.getValue());
                return a.getValue();
            }).collect(Collectors.toList());
            List<Integer> value = valueSet.stream().sorted().collect(Collectors.toList());
            if (value.containsAll(target)) {
                return idsList;
            }
        }
        return Collections.emptyList();
    }


    public static void main(String[] args) {
        List<PermutationDTO> list = new ArrayList<>();
        PermutationDTO b = new PermutationDTO("A", Arrays.asList(1, 2, 3));
        list.add(b);
        PermutationDTO b1 = new PermutationDTO("B", Arrays.asList(4));
        list.add(b1);
        PermutationDTO b2 = new PermutationDTO("C", Arrays.asList(1, 2, 5, 6, 8, 10));
        list.add(b2);
        PermutationDTO b3 = new PermutationDTO("D", Arrays.asList(5));
        list.add(b3);
        PermutationDTO b4 = new PermutationDTO("E", Arrays.asList(3, 4, 7, 9, 11));
        list.add(b4);
        PermutationDTO b5 = new PermutationDTO("F", Arrays.asList(1, 2, 3, 4));
        list.add(b5);
        PermutationDTO b6 = new PermutationDTO("G", Arrays.asList(6, 7, 8, 9, 10));
        list.add(b6);
        PermutationDTO b7 = new PermutationDTO("H", Arrays.asList(11));
        list.add(b7);
        PermutationDTO b8 = new PermutationDTO("I", Arrays.asList(5));
        list.add(b8);

        PermutationDTO b9 = new PermutationDTO("I", Arrays.asList(4));
        list.add(b9);
        List<Integer> target = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).sorted().collect(Collectors.toList());
        List<List<Integer>> drugIdsList = ListValueUtil.permutationDrugsTargetDecline(list, target);
        System.out.println(drugIdsList);
    }


}
