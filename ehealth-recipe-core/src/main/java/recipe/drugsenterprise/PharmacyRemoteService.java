package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeBussConstant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/20
 * @description： TODO
 * @version： 1.0
 */
public class PharmacyRemoteService extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PharmacyRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PharmacyRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PharmacyRemoteService scanStock not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        LOGGER.info("PharmacyRemoteService syncEnterpriseDrug not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService pushCheckResult not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService findSupportDep not implement.");
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        List<DepDetailBean> list = new ArrayList<>(5);
        DepDetailBean detailBean;
        for (int i = 0; i < 5; i++) {
            detailBean = new DepDetailBean();
            detailBean.setDepId(enterprise.getId());
            detailBean.setDepName("测试大药房" + i);
            detailBean.setPharmacyCode("cedyf" + i);
            detailBean.setRecipeFee(new BigDecimal((int) (Math.random() * 100)));
            detailBean.setAddress("东大街江南大道滨盛路1189潮人汇9楼 ");
            detailBean.setPayModeList(Arrays.asList(RecipeBussConstant.PAYMODE_TFDS));

            list.add(detailBean);
        }
        result.setObject(list);
        return result;
    }

    public static void main(String[] args) {
        System.out.println((int) (Math.random() * 100));
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_PHARMACY;
    }
}
