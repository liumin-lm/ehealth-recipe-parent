package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateDrugsEpCallable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 通用药企对接服务(国药协议)
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/10/19.
 */
public abstract class AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessDrugEnterpriseService.class);

    /**
     * 单个线程处理药企药品数量
     */
    protected static final int ONCETIME_DEAL_NUM = 100;

    public void updateAccessTokenById(Integer code, Integer depId) {
        Integer i = -2;
        if (i.equals(code)) {
            updateAccessToken(Arrays.asList(depId));
        }
    }

    /**
     * 获取药企AccessToken
     *
     * @param drugsEnterpriseIds
     * @return
     */
    public String updateAccessToken(List<Integer> drugsEnterpriseIds) {
        if (null != drugsEnterpriseIds && !drugsEnterpriseIds.isEmpty()) {
            List<UpdateDrugsEpCallable> callables = new ArrayList<>(0);
            for (int i = 0; i < drugsEnterpriseIds.size(); i++) {
                callables.add(new UpdateDrugsEpCallable(drugsEnterpriseIds.get(i)));
            }

            if (!callables.isEmpty()) {
                try {
                    RecipeBusiThreadPool.submitList(callables);
                } catch (InterruptedException e) {
                    LOGGER.error("updateAccessToken 线程池异常");
                }
            }
        }

        return null;
    }

    /**
     * 生成完整地址
     *
     * @param order 订单
     * @return
     */
    public String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            this.getAddressDic(address, order.getAddress1());
            this.getAddressDic(address, order.getAddress2());
            this.getAddressDic(address, order.getAddress3());
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }

    public void getAddressDic(StringBuilder address, String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                address.append(DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area));
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area);
            }
        }
    }

    /**
     * 格式化Double
     *
     * @param d
     * @return
     */
    protected String getFormatDouble(Double d) {
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    /**
     * 某一列表分成多段
     *
     * @param osize
     * @return
     */
    protected int splitGroupSize(int osize) {
        return (int) Math.ceil(osize / Double.parseDouble(String.valueOf(ONCETIME_DEAL_NUM)));
    }

    /**
     * 更新token
     *
     * @param drugsEnterprise
     */
    public abstract void tokenUpdateImpl(DrugsEnterprise drugsEnterprise);

    /**
     * 推送处方
     *
     * @param recipeIds  处方ID集合
     * @param enterprise
     * @return
     */
    public abstract DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise);

    /**
     * 库存检验
     *
     * @param recipeId        处方ID
     * @param drugsEnterprise 药企
     * @return
     */
    public abstract DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise);

    /**
     * 定时同步药企库存
     *
     * @param drugsEnterprise
     * @param drugIdList
     * @return
     */
    public abstract DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList);

    /**
     * 药师审核通过通知消息
     *
     * @param recipeId   处方ID
     * @param checkFlag  审核结果 1:审核通过 0:审核失败
     * @param enterprise
     * @return
     */
    public abstract DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise);

    /**
     * 查找供应商
     *
     * @param recipeIds
     * @param enterprise
     * @return
     */
    public abstract DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise);
    
    /**
     * @param rxId  处⽅Id
     * @param queryOrder  是否查询订单
     * @return 处方单
     */
    public DrugEnterpriseResult queryPrescription(String rxId, Boolean queryOrder) {
        return DrugEnterpriseResult.getSuccess();
    }


    /**
     * 推送药企处方状态，由于只是个别药企需要实现，故有默认实现
     * @param rxId
     * @return
     */
    public DrugEnterpriseResult updatePrescriptionStatus(String rxId, int status) {
        return DrugEnterpriseResult.getSuccess();
    }

    /**
     * 获取药企实现简称字段
     *
     * @return
     */
    public abstract String getDrugEnterpriseCallSys();
}
