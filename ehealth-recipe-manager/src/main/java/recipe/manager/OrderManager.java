package recipe.manager;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.currentuserinfo.model.SimpleThirdBean;
import com.ngari.base.currentuserinfo.model.SimpleWxAccountBean;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.RecipeThirdUrlReqTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.SkipThirdBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.PatientClient;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;

import javax.annotation.Resource;

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
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private ICurrentUserInfoService userInfoService;
    @Autowired
    private IRecipeEnterpriseService hisService;
    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    /**
     * todo 迁移代码 需要优化 （尹盛）
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     * 或者处方审核成功后推送处方卡片消息时点击跳转(互联网)
     *
     * @param recipeId
     * @return
     */
    public SkipThirdBean getThirdUrl(Integer recipeId, Integer giveMode) {
        SkipThirdBean skipThirdBean = new SkipThirdBean();
        if (null == recipeId) {
            return new SkipThirdBean();
        }
        Recipe recipe = recipeDAO.get(recipeId);
        if (recipe.getClinicOrgan() == 1005683) {
            return getUrl(recipe, giveMode);
        }
        if (recipe.getEnterpriseId() != null) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
            if (drugsEnterprise != null && "bqEnterprise".equals(drugsEnterprise.getAccount())) {
                return getUrl(recipe, giveMode);
            }
            RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
            if (null == order) {
                return skipThirdBean;
            }
        }
        return skipThirdBean;
    }

    private SkipThirdBean getUrl(Recipe recipe, Integer giveMode) {
        SkipThirdBean skipThirdBean = new SkipThirdBean();
        String thirdUrl;
        if (null != recipe) {
            PatientDTO patient = patientClient.getPatientBeanByMpiId(recipe.getMpiid());
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            if (patient != null) {
                patientBaseInfo.setPatientName(patient.getPatientName());
                patientBaseInfo.setCertificateType(patient.getCertificateType());
                patientBaseInfo.setCertificate(patient.getCertificate());
                patientBaseInfo.setMobile(patient.getMobile());
                patientBaseInfo.setPatientID(recipe.getPatientID());
                patientBaseInfo.setMpi(recipe.getRequestMpiId());
                // 黄河医院获取药企患者id
                try {
                    SimpleWxAccountBean account = userInfoService.getSimpleWxAccount();
                    logger.info("querySimpleWxAccountBean account={}", JSONObject.toJSONString(account));
                    if (null != account) {
                        if (account instanceof SimpleThirdBean) {
                            SimpleThirdBean stb = (SimpleThirdBean) account;
                            patientBaseInfo.setTid(stb.getTid());
                        }
                    }
                } catch (Exception e) {
                    logger.error("黄河医院获取药企用户tid异常", e);
                }
            }
            PatientBaseInfo userInfo = new PatientBaseInfo();
            if (StringUtils.isNotEmpty(recipe.getRequestMpiId())) {
                PatientDTO user = patientClient.getPatientBeanByMpiId(recipe.getRequestMpiId());
                if (user != null) {
                    userInfo.setPatientName(user.getPatientName());
                    userInfo.setCertificate(user.getCertificate());
                    userInfo.setCertificateType(user.getCertificateType());
                    userInfo.setMobile(user.getMobile());
                }
            }
            RecipeThirdUrlReqTO req = new RecipeThirdUrlReqTO();
            req.setOrganId(recipe.getClinicOrgan());
            req.setPatient(patientBaseInfo);
            req.setUser(userInfo);
            req.setRecipeCode(String.valueOf(recipe.getRecipeId()));
            HisResponseTO<String> response;
            // 从复诊获取患者渠道id
            String patientChannelId = "";
            try {
                if (recipe.getClinicId() != null) {
                    IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                    logger.info("queryPatientChannelId req={}", recipe.getClinicId());
                    RevisitExDTO revisitExDTO = exService.getByConsultId(recipe.getClinicId());
                    if (revisitExDTO != null) {
                        logger.info("queryPatientChannelId res={}", JSONObject.toJSONString(revisitExDTO));
                        patientChannelId = revisitExDTO.getProjectChannel();
                        req.setPatientChannelId(patientChannelId);
                    }
                }
            } catch (Exception e) {
                logger.error("queryPatientChannelId error:", e);
            }
            req.setSkipMode(giveMode);
            try {
                //获取民科机构登记号
                req.setOrgCode(patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
                logger.info("getRecipeThirdUrl request={}", JSONUtils.toString(req));
                response = hisService.getRecipeThirdUrl(req);
                logger.info("getRecipeThirdUrl res={}", JSONUtils.toString(response));
                if (response != null && "200".equals(response.getMsgCode())) {
                    thirdUrl = response.getData();
                    //前置机传过来的可能是json字符串也可能是非json
                    try {
                        skipThirdBean = JSONObject.parseObject(thirdUrl, SkipThirdBean.class);
                    } catch (Exception e) {
                        //说明不是标准的JSON格式
                        skipThirdBean.setUrl(thirdUrl);
                    }
                } else {
                    throw new DAOException(609, "获取第三方跳转链接异常");
                }
            } catch (Exception e) {
                logger.error("getRecipeThirdUrl error ", e);
                throw new DAOException(609, "获取第三方跳转链接异常");
            }
        }
        return skipThirdBean;
    }

}
