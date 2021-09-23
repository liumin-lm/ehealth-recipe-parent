package recipe.givemode.business;

import com.google.common.base.Splitter;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import recipe.constant.HisDeliveryConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hss
 */
@Component("fromHisDeliveryCodeService")
public class FromHisDeliveryCodeService extends AbstractGiveModeService implements IGiveModeBase {
    @Override
    public void setSpecialItem(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        List<String> deliveryCodeList = Splitter.on("|").splitToList(recipeExtend.getDeliveryCode()).stream()
                .filter(deliveryCode -> StringUtils.isNotEmpty(deliveryCode)).collect(Collectors.toList());

        /**
         * 药房有库存时显示企业配送，药柜有库存时显示药柜取药；药柜和云药房都有库存时药柜取药和企业配送按钮；
         */
        if(CollectionUtils.isNotEmpty(deliveryCodeList)){
            int size = deliveryCodeList.size();
            boolean boo = deliveryCodeList.contains(HisDeliveryConstant.YG_HIS_DELIVERY_CODE);
            if(!boo){
                //药企配送按钮
                saveGiveModeData(giveModeButtonBeans, "showSendToEnterprises");
            }else if(size == 1 && boo){
                //药柜取药按钮
                saveGiveModeData(giveModeButtonBeans, "supportTFDS");
            }else if(size > 1 && boo){
                //"药企配送" && "药柜取药"
                addGiveModeData(giveModeButtonBeans, Arrays.asList("showSendToEnterprises","supportTFDS"));
            }else{
                //都不支持
                saveGiveModeData(giveModeButtonBeans, "");
            }
        }
    }
}
