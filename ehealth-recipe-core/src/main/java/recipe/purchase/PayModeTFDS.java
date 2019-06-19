package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeServiceSub;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 药店取药购药方式
 * @version： 1.0
 */
public class PayModeTFDS implements IPurchaseService{
    private static final Logger LOGGER = LoggerFactory.getLogger(PayModeTFDS.class);

    @Override
    public RecipeResultBean findSupportDepList(Recipe recipe, Map ext) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        Integer recipeId = recipe.getRecipeId();
        //获取购药方式查询列表
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(getPayMode());
        if (CollectionUtils.isEmpty(payModeSupport)) {
            LOGGER.warn("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, getPayMode());
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("配送模式配置有误");
            return resultBean;
        }
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), payModeSupport);
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            //该机构没有对应可药店取药的药企
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("没有对应可药店取药的药企");
            return resultBean;
        }
        LOGGER.info("findSupportDepList recipeId={}, 匹配到支持药店取药药企数量[{}]", recipeId, drugsEnterprises.size());
        List<Integer> recipeIds = new ArrayList<>();
        recipeIds.add(recipeId);
        boolean succFlag;
        List<DepDetailBean> depDetailList = new ArrayList<>();
        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterprises.size());
        for (DrugsEnterprise dep : drugsEnterprises) {
            //通过查询该药企对应药店库存
            succFlag = remoteDrugService.scanStock(recipeId, dep);
            if (succFlag) {
                subDepList.add(dep);
            } else {
                LOGGER.warn("findSupportDepList 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]",
                        recipeId, dep.getId(), dep.getName());
            }
            if (CollectionUtils.isEmpty(subDepList)) {
                LOGGER.warn("findSupportDepList 该处方没有提供取药的药店. recipeId=[{}]", recipeId);
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("没有药企可以配送");
                return resultBean;
            }
            //需要从接口获取药店列表
            DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(recipeIds, ext, dep);
            if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
                Object result = drugEnterpriseResult.getObject();
                if (result != null && result instanceof List) {
                    List<DepDetailBean> ysqList = (List) result;
                    for (DepDetailBean depDetailBean : ysqList) {
                        depDetailBean.setDepId(dep.getId());
                        depDetailBean.setBelongDepName(dep.getName());
                        depDetailBean.setPayModeText("药店支付");
                    }
                    depDetailList.addAll(ysqList);
                    //对药店列表进行排序
                    String sort = (String)ext.get("sort");
                    Collections.sort(depDetailList, new DepDetailBeanComparator(sort));
                }
                //设置样式
                resultBean.setStyle(drugEnterpriseResult.getStyle());
            }
        }
        LOGGER.info("findSupportDepList recipeId={}, 获取到药店数量[{}]", recipeId, depDetailList.size());
        depListBean.setList(depDetailList);
        resultBean.setObject(depListBean);
        return resultBean;
    }

    @Override
    public RecipeResultBean order(Recipe dbRecipe) {

        return null;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TFDS;
    }

    @Override
    public String getServiceName() {
        return "payModeTFDSService";
    }

    class DepDetailBeanComparator implements Comparator<DepDetailBean> {
        String sort;
        DepDetailBeanComparator(String sort){
            this.sort = sort;
        }
        @Override
        public int compare(DepDetailBean depDetailBeanOne, DepDetailBean depDetailBeanTwo) {
            int cp = 0;
            if (StringUtils.isNotEmpty(sort) && sort.equals("1")) {
                //价格排序
                BigDecimal price = depDetailBeanOne.getRecipeFee().subtract(depDetailBeanTwo.getRecipeFee());
                int compare = price.compareTo(BigDecimal.ZERO);
                if (compare != 0) {
                    cp = (compare > 0) ? 2 : -1;
                }
            } else {
                //距离排序
                Double distance = depDetailBeanOne.getDistance() - depDetailBeanTwo.getDistance();
                if (distance != 0.0) {
                    cp = (distance > 0.0) ? 2 : -1;
                }
            }
            return cp;
        }
    }
}
