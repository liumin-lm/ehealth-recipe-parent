package test.eh.prepay;

import com.google.common.collect.Lists;
import com.ngari.recipe.recipe.model.DispendingPharmacyReportReqTo;
import ctd.util.JSONUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.paukov.combinatorics3.Generator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/*@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")*/
public class PrePayInHospitalServiceTest {

    /*@Autowired
    RecipeDAO recipeDAO;*/

    @Test
    public void test() {
        /*String start = DateConversion.formatDateTimeWithSec(new Date());
        String end = DateConversion.formatDateTimeWithSec(new Date());
        start = "2020-01-01 00:00:00";
        end = "2020-10-31 23:59:59";
        List<RecipeDrugDetialReportDTO> recipeDrugDetialReport = recipeDAO.findRecipeDrugDetialReport(1, start, end,null, null, null, null, null, "13,14,15", null, null, null, null, 0, 10);
        */
        DispendingPharmacyReportReqTo dispendingPharmacyReportReqTo = new DispendingPharmacyReportReqTo();
        dispendingPharmacyReportReqTo.setOrganId(1);
        dispendingPharmacyReportReqTo.setStartDate(new Date());
        dispendingPharmacyReportReqTo.setEndDate(new Date());
        dispendingPharmacyReportReqTo.setStart(0);
        dispendingPharmacyReportReqTo.setLimit(10);
        dispendingPharmacyReportReqTo.setOrderStatus(1);
        System.out.println(JSONUtils.toString(dispendingPharmacyReportReqTo));
    }

    @AllArgsConstructor
    @Data
    static class B {
        private Integer id;
        private List<String> key;

        public B() {

        }
    }

    public static void main(String[] args) {

        List<B> list = new ArrayList<>();
        B b = new B(1, Arrays.asList("A", "B", "C"));
        list.add(b);
        B b1 = new B(2, Arrays.asList("D"));
        list.add(b1);
        B b2 = new B(3, Arrays.asList("A", "B", "E", "F", "H", "J"));
        list.add(b2);
        B b3 = new B(4, Arrays.asList("E"));
        list.add(b3);
        B b4 = new B(5, Arrays.asList("C", "D", "G", "I", "K"));
        list.add(b4);

        B b5 = new B(6, Arrays.asList("A", "B", "C", "D"));
        list.add(b5);
        B b6 = new B(7, Arrays.asList("F", "G", "H", "I", "J"));
        list.add(b6);
        B b7 = new B(8, Arrays.asList("K"));
        list.add(b7);
        B b8 = new B(9, Arrays.asList("E"));
        list.add(b8);

        List<String> abcde = Stream.of("A", "B", "C", "E", "D", "F", "G", "H", "I", "J", "K").sorted().collect(Collectors.toList());
        List<List<B>> list1 = Generator.subset(list).simple().stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList());

        List<B> minKeyList = new ArrayList<>();

        List<B> sizeKeyList = new ArrayList<>();
        System.out.println(list1.size());

        for (List<B> l : list1) {
            Set<String> keySet = new HashSet<>();
            l.forEach(a -> keySet.addAll(a.key));
            List<String> key = keySet.stream().sorted().collect(Collectors.toList());
            if (CollectionUtils.isEmpty(minKeyList) && abcde.equals(key)) {
                minKeyList = l;
            }
            boolean size = l.stream().anyMatch(a -> a.getKey().size() > 5);
            if (size) {
                continue;
            }
            if (CollectionUtils.isEmpty(sizeKeyList) && abcde.equals(key)) {
                sizeKeyList = l;
            }
        }

        System.out.println("minKeyList:" + minKeyList);
        System.out.println("sizeKeyList:" + sizeKeyList);

        List<B> minSplitKeyList = new ArrayList<>();
        minKeyList.forEach(a -> {
            List<List<String>> minKeyLists = Lists.partition(a.getKey(), 5);
            minKeyLists.forEach(c -> {
                B bb = new B();
                bb.setKey(c);
                bb.setId(a.id);
                minSplitKeyList.add(bb);
            });
        });
        System.out.println("minSplitKeyList:" + minSplitKeyList);

        if (minSplitKeyList.size() < sizeKeyList.size()) {
            System.out.println("List:" + minSplitKeyList);
        } else {
            System.out.println("List:" + sizeKeyList);
        }

    }


}
