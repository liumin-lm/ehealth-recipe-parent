package test.eh.prepay;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.constant.RecipeSupportGiveModeEnum;
import com.ngari.recipe.recipe.model.DispendingPharmacyReportReqTo;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.dao.RecipeDAO;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;

import java.util.*;


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

    private RecipeDAO recipeDAO;

    /**
     * 根据处方的id获取多个处方支持的购药方式
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    public List<RecipeGiveModeButtonRes> getRecipeGiveModeButtonRes(List<Integer> recipeIds) {
        List<RecipeGiveModeButtonRes> list = new LinkedList<>();
        if (CollectionUtils.isEmpty(recipeIds)) {
            return list;
        }
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipes)) {
            return list;
        }
        Map<String, RecipeSupportGiveModeEnum> giveModeEnumMap = giveModeEnumMap();

        Map<Integer, List<Integer>> giveModeByRecipeIdMap = new HashMap<>();
        recipes.forEach(a -> {
            String giveModeStr = a.getRecipeSupportGiveMode();
            if (StringUtils.isEmpty(giveModeStr)) {
                return;
            }
            String[] giveModeArray = giveModeStr.split(",");
            getGiveModeByRecipeIdMap(giveModeArray, a.getRecipeId(), giveModeByRecipeIdMap, giveModeEnumMap);
        });

        // 从运营平台获取所有的购药方式
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        GiveModeShowButtonVO giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(recipes.get(0).getClinicOrgan());

        List<GiveModeButtonBean> giveModeButtons = giveModeShowButtonVO.getGiveModeButtons();
        giveModeButtons.forEach(a -> {
            RecipeSupportGiveModeEnum recipeSupportGiveModeEnum = giveModeEnumMap.get(a.getShowButtonKey());
            if (null == recipeSupportGiveModeEnum) {
                return;
            }
            RecipeGiveModeButtonRes recipeGiveModeButtonRes;
            if (RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE == recipeSupportGiveModeEnum) {
                recipeGiveModeButtonRes = recipeGiveModeButton(recipeSupportGiveModeEnum, giveModeByRecipeIdMap, a, 1);
            } else {
                recipeGiveModeButtonRes = recipeGiveModeButton(recipeSupportGiveModeEnum, giveModeByRecipeIdMap, a, recipeIds.size());
            }
            if (RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText().equals(a.getShowButtonKey())) {
                recipeGiveModeButtonRes.setButtonFlag(true);
            }
            list.add(recipeGiveModeButtonRes);
        });
        return list;
    }

    private void getGiveModeByRecipeIdMap(String[] giveModeArray, Integer recipeId, Map<Integer, List<Integer>> map, Map<String, RecipeSupportGiveModeEnum> giveModeEnumMap) {
        for (String giveMode : giveModeArray) {
            RecipeSupportGiveModeEnum recipeSupportGiveModeEnum = giveModeEnumMap.get(giveMode);
            if (null == recipeSupportGiveModeEnum) {
                continue;
            }
            int type = recipeSupportGiveModeEnum.getType();

            List<Integer> giveModeButton = map.get(type);
            if (CollectionUtils.isEmpty(giveModeButton)) {
                giveModeButton = new LinkedList<>();
                giveModeButton.add(recipeId);
                map.put(type, giveModeButton);
            } else {
                giveModeButton.add(recipeId);
            }
        }
    }

    private RecipeGiveModeButtonRes recipeGiveModeButton(RecipeSupportGiveModeEnum recipeSupportGiveModeEnum, Map<Integer, List<Integer>> map,
                                                         GiveModeButtonBean giveModeButtonBean, Integer size) {
        int type = recipeSupportGiveModeEnum.getType();
        List<Integer> recipeIdList = map.get(type);
        if (CollectionUtils.isEmpty(recipeIdList)) {
            recipeIdList = new LinkedList<>();
        }

        RecipeGiveModeButtonRes recipeGiveModeButtonRes = new RecipeGiveModeButtonRes(recipeSupportGiveModeEnum.getText(), recipeSupportGiveModeEnum.getName());
        recipeGiveModeButtonRes.setRecipeIds(recipeIdList);
        recipeGiveModeButtonRes.setJumpType(giveModeButtonBean.getButtonSkipType());
        if (size.equals(recipeIdList.size())) {
            recipeGiveModeButtonRes.setButtonFlag(true);
        } else {
            recipeGiveModeButtonRes.setButtonFlag(false);
        }
        return recipeGiveModeButtonRes;
    }


    public static Map<String, RecipeSupportGiveModeEnum> giveModeEnumMap() {
        Map<String, RecipeSupportGiveModeEnum> giveModeEnumMap = new HashMap<>();
        for (RecipeSupportGiveModeEnum e : RecipeSupportGiveModeEnum.values()) {
            giveModeEnumMap.put(e.getText(), e);
        }
        return giveModeEnumMap;
    }
}
