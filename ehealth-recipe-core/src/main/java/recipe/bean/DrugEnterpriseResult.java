package recipe.bean;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import recipe.drugsenterprise.AccessDrugEnterpriseService;

/**
 * 药企对接返回结果
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/3/7.
 */
public class DrugEnterpriseResult extends RecipeResultBean {

    /**
     * 选择的药企具体实现
     */
    private AccessDrugEnterpriseService accessDrugEnterpriseService;

    private DrugsEnterprise drugsEnterprise;

    /**
     * 药企端返回订单号
     */
    private String depSn;

    public DrugEnterpriseResult(Integer code) {
        setCode(code);
    }

    public static DrugEnterpriseResult getSuccess() {
        return new DrugEnterpriseResult(SUCCESS);
    }

    public static DrugEnterpriseResult getFail() {
        return new DrugEnterpriseResult(FAIL);
    }

    public AccessDrugEnterpriseService getAccessDrugEnterpriseService() {
        return accessDrugEnterpriseService;
    }

    public void setAccessDrugEnterpriseService(AccessDrugEnterpriseService accessDrugEnterpriseService) {
        this.accessDrugEnterpriseService = accessDrugEnterpriseService;
    }

    public DrugsEnterprise getDrugsEnterprise() {
        return drugsEnterprise;
    }

    public void setDrugsEnterprise(DrugsEnterprise drugsEnterprise) {
        this.drugsEnterprise = drugsEnterprise;
    }

    public String getDepSn() {
        return depSn;
    }

    public void setDepSn(String depSn) {
        this.depSn = depSn;
    }

    @Override
    public String toString() {
        return "code=" + getCode() + "***msg=" + getMsg() + "***accessDrugEnterpriseService=" +
                ((null != this.accessDrugEnterpriseService) ? this.accessDrugEnterpriseService.getClass().getSimpleName() : "");
    }
}
