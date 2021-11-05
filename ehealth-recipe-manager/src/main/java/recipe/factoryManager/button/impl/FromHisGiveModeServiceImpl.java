package recipe.factoryManager.button.impl;

import com.google.common.base.Splitter;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.constant.HisDeliveryConstant;
import recipe.factoryManager.button.GiveModeManager;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Service
public class FromHisGiveModeServiceImpl extends GiveModeManager {
    @Override
    public void setSpecialItem(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        List<String> deliveryCodeList = Splitter.on("|").splitToList(recipeExtend.getDeliveryCode()).stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList());

        /**
         * 药房有库存时显示企业配送，药柜有库存时显示药柜取药；药柜和云药房都有库存时药柜取药和企业配送按钮；
         */
        if (CollectionUtils.isNotEmpty(deliveryCodeList)) {
            int size = deliveryCodeList.size();
            boolean boo = deliveryCodeList.contains(HisDeliveryConstant.YG_HIS_DELIVERY_CODE);
            if (!boo) {
                //药企配送按钮
                saveGiveModeData(giveModeButtonBeans, "showSendToEnterprises");
            } else if (size == 1 && boo) {
                //药柜取药按钮
                saveGiveModeData(giveModeButtonBeans, "supportTFDS");
            } else if (size > 1 && boo) {
                //"药企配送" && "药柜取药"
                addGiveModeData(giveModeButtonBeans, Arrays.asList("showSendToEnterprises", "supportTFDS"));
            } else {
                //都不支持
                saveGiveModeData(giveModeButtonBeans, "");
            }
        }
    }


    /**
     * 多个按钮显示
     *
     * @param giveModeButtonBeans
     * @param addGiveModeList
     */
    private void addGiveModeData(List<GiveModeButtonDTO> giveModeButtonBeans, List<String> addGiveModeList) {
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonDTO giveModeShowButtonVO = (GiveModeButtonDTO) iterator.next();
            if (!addGiveModeList.contains(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }
}
