package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.RecipeThirdUrlReqTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.EnterpriseClient;
import recipe.client.PatientClient;
import recipe.client.RevisitClient;
import recipe.dao.DrugsEnterpriseDAO;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 订单
 *
 * @author yinsheng
 * @date 2021\6\30 0030 15:22
 */
@Service
public class OrderManager extends BaseManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private EnterpriseClient enterpriseClient;
    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RevisitClient revisitClient;

    /**
     * 通过订单号获取该订单下关联的所有处方
     *
     * @param orderCode 订单号
     * @return 处方集合
     */
    public List<Recipe> getRecipesByOrderCode(String orderCode) {
        logger.info("RecipeOrderManager getRecipesByOrderCode orderCode:{}", orderCode);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        logger.info("RecipeOrderManager getRecipesByOrderCode recipes:{}", JSON.toJSONString(recipes));
        return recipes;
    }

    /**
     * todo 迁移代码 需要优化 （尹盛）
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     * 或者处方审核成功后推送处方卡片消息时点击跳转(互联网)
     *
     * @param recipeId
     * @return
     */
    public SkipThirdDTO getThirdUrl(Integer recipeId, Integer giveMode) {
        SkipThirdDTO skipThirdDTO = new SkipThirdDTO();
        if (null == recipeId) {
            return skipThirdDTO;
        }
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe) {
            return skipThirdDTO;
        }
        if (recipe.getClinicOrgan() == 1005683) {
            return getUrl(recipe, giveMode);
        }
        if (null == recipe.getEnterpriseId()) {
            return skipThirdDTO;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
        if (drugsEnterprise != null && "bqEnterprise".equals(drugsEnterprise.getAccount())) {
            return getUrl(recipe, giveMode);
        }
        return skipThirdDTO;
    }

    private SkipThirdDTO getUrl(Recipe recipe, Integer giveMode) {
        RecipeThirdUrlReqTO req = new RecipeThirdUrlReqTO();
        req.setOrganId(recipe.getClinicOrgan());
        req.setRecipeCode(String.valueOf(recipe.getRecipeId()));
        req.setSkipMode(giveMode);
        req.setOrgCode(patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
        req.setUser(patientBaseInfo(recipe.getRequestMpiId()));
        PatientBaseInfo patientBaseInfo = patientBaseInfo(recipe.getMpiid());
        patientBaseInfo.setPatientID(recipe.getPatientID());
        patientBaseInfo.setMpi(recipe.getRequestMpiId());
        patientBaseInfo.setTid(enterpriseClient.getSimpleWxAccount().getTid());
        req.setPatient(patientBaseInfo);
        try {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if (revisitExDTO != null) {
                req.setPatientChannelId(revisitExDTO.getProjectChannel());
            }
        } catch (Exception e) {
            logger.error("queryPatientChannelId error:", e);
        }
        return enterpriseClient.skipThird(req);
    }


    /**
     * 通过订单 生成完整地址
     *
     * @param order 订单
     * @return
     */
    public String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            super.getAddressDic(address, order.getAddress1());
            super.getAddressDic(address, order.getAddress2());
            super.getAddressDic(address, order.getAddress3());
            super.getAddressDic(address, order.getStreetAddress());
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }


    /**
     * 获取订单列表
     *
     * @param orderCodes
     * @return
     */
    public List<RecipeOrder> getRecipeOrderList(Set<String> orderCodes) {
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            return recipeOrderDAO.findByOrderCode(orderCodes);
        }
        return new ArrayList<>();
    }

    public RecipeOrder getRecipeOrderById(Integer organId) {
        return recipeOrderDAO.getByOrderId(organId);
    }

    /**
     * 处理患者信息
     *
     * @param mpiId
     * @return
     */
    private PatientBaseInfo patientBaseInfo(String mpiId) {
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        if (StringUtils.isEmpty(mpiId)) {
            return patientBaseInfo;
        }
        PatientDTO patient = patientClient.getPatientBeanByMpiId(mpiId);
        if (patient != null) {
            patientBaseInfo.setPatientName(patient.getPatientName());
            patientBaseInfo.setCertificate(patient.getCertificate());
            patientBaseInfo.setCertificateType(patient.getCertificateType());
            patientBaseInfo.setMobile(patient.getMobile());
        }
        return patientBaseInfo;
    }
}
