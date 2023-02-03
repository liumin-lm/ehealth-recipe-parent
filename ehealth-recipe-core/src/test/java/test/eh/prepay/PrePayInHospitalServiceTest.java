package test.eh.prepay;

import com.ngari.recipe.recipe.model.DispendingPharmacyReportReqTo;
import ctd.util.JSONUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
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
    }

    public static void main(String[] args) {

        List<B> list = new ArrayList<>();
        B b = new B(1, Arrays.asList("A", "B", "C"));
        list.add(b);
        B b1 = new B(2, Arrays.asList("D"));
        list.add(b1);
        B b2 = new B(3, Arrays.asList("A", "B", "E"));
        list.add(b2);
        B b3 = new B(4, Arrays.asList("E"));
        list.add(b3);
        B b4 = new B(5, Arrays.asList("C", "D"));
        list.add(b4);

        List<String> abcde = Stream.of("A", "B", "C", "E", "D").sorted().collect(Collectors.toList());
        List<List<B>> list1 = Generator.subset(list).simple().stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList());
        for (List<B> l : list1) {
            Set<String> keySet = new HashSet<>();
            l.forEach(a -> keySet.addAll(a.key));
            List<String> key = keySet.stream().sorted().collect(Collectors.toList());
            if (abcde.equals(key)) {
                System.out.println(l);
                break;
            }
        }
    }
}
