package recipe.service.common;

import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeCheckService;
import recipe.service.hospitalrecipe.PrescribeService;
import recipe.util.MapValueUtil;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 获取处方单个数据服务
 * @version： 1.0
 */
@RpcBean("recipeSingleService")
public class RecipeSingleService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RecipeSingleService.class);

    @Autowired
    private RecipeOrderDAO orderDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeDetailDAO detailDAO;

    @RpcService
    public RecipeStandardResTO<Map> getRecipeByConditions(RecipeStandardReqTO request) {
        RecipeStandardResTO<Map> response = RecipeStandardResTO.getRequest(Map.class);
        response.setCode(RecipeCommonBaseTO.FAIL);
        if (request.isNotEmpty()) {
            Map<String, Object> conditions = request.getConditions();
            Integer recipeId = MapValueUtil.getInteger(conditions, "recipeId");
            Recipe dbRecipe;
            if (null != recipeId) {
                dbRecipe = recipeDAO.get(recipeId);
            } else {
                //需要转换组织机构编码
                String organId = MapValueUtil.getString(conditions, "organId");
                String recipeCode = MapValueUtil.getString(conditions, "recipeCode");
                if (StringUtils.isEmpty(organId) || StringUtils.isEmpty(recipeCode)) {
                    response.setMsg("缺少组织机构编码或者处方编号");
                    return response;
                }
                Integer clinicOrgan = null;
                try {
                    IOrganService organService = BaseAPI.getService(IOrganService.class);
                    List<OrganBean> organList = organService.findByOrganizeCode(organId);
                    if (CollectionUtils.isNotEmpty(organList)) {
                        clinicOrgan = organList.get(0).getOrganId();
                    }
                } catch (Exception e) {
                    LOG.warn("getRecipeByConditions 平台未匹配到该组织机构编码. organId={}", organId, e);
                } finally {
                    if (null == clinicOrgan) {
                        response.setMsg("平台未匹配到该组织机构编码");
                        return response;
                    }
                }

                dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, clinicOrgan);
            }

            if (null != dbRecipe) {
                recipeId = dbRecipe.getRecipeId();
                Map<String, Object> other = Maps.newHashMap();
                //组装处方数据
                Map<String, Object> recipeInfo = Maps.newHashMap();
                List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
                recipeInfo.put("recipe", ObjectCopyUtils.convert(dbRecipe, RecipeBean.class));
                recipeInfo.put("detailList", ObjectCopyUtils.convert(detailList, RecipeDetailBean.class));

                //查询患者数据
                PatientDTO patient = null;
                try {
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    patient = patientService.get(dbRecipe.getMpiid());
                } catch (Exception e) {
                    LOG.warn("getRecipeByConditions can't find patient. mpiId={}", dbRecipe.getMpiid(), e);
                } finally {
                    if (null != patient) {
                        other.put("patientAddress", patient.getAddress());
                        other.put("patientTel", patient.getMobile());
                        recipeInfo.put("patient", patient);
                    }
                }
                //设置订单ID
                RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
                if (null != order) {
                    if (1 == order.getEffective()) {
                        //说明已签名，信息从order取
                        other.put("patientAddress", order.getAddress4());
                        other.put("patientTel", order.getRecMobile());
                        other.put("depName", order.getDrugStoreName());
                    }
                    other.put("orderId", order.getOrderId());
                }
                //设置其他数据
                if (RecipeStatusConstant.DELETE == dbRecipe.getStatus()) {
                    other.put("cancelReason", "HIS作废");
                }
                recipeInfo.put("other", other);

                // 根据当前状态返回前端标记，用于前端展示什么页面
                // 分为 -1:查不到处方 0：未签名 1: 其他状态展示详情页  2：药店取药已签名  3: 配送到家已签名-未支付  4:配送到家已签名-已支付 5:审核不通过  6:作废
                int notation = 0;
                switch (dbRecipe.getStatus()) {
                    case RecipeStatusConstant.UNSIGN:
                        notation = 0;
                        break;
                    case RecipeStatusConstant.CHECK_PASS:
                        notation = 3;
                        break;
                    case RecipeStatusConstant.READY_CHECK_YS:
                        if (RecipeBussConstant.GIVEMODE_TFDS.equals(dbRecipe.getGiveMode())) {
                            notation = 2;
                        } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(dbRecipe.getGiveMode())
                                && Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                            notation = 4;
                        } else {
                            notation = 1;
                        }
                        break;
                    case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                        notation = 5;
                        RecipeCheckService service = ApplicationUtils.getRecipeService(RecipeCheckService.class);
                        //获取审核不通过详情
                        List<Map<String, Object>> mapList = service.getCheckNotPassDetail(recipeId);
                        recipeInfo.put("reasonAndDetails", mapList);
                        break;
                    case RecipeStatusConstant.DELETE:
                        notation = 6;
                        break;
                    default:
                        notation = 1;
                }
                recipeInfo.put("notation", notation);
                response.setData(recipeInfo);
                response.setCode(RecipeCommonBaseTO.SUCCESS);
            } else {
                response.setMsg("没有处方匹配");
            }

        } else {
            response.setMsg("请求对象为空");
        }

        return response;
    }

    /**
     * 撤销处方
     *
     * @param recipeId
     */
    @RpcService
    public RecipeStandardResTO revokeRecipe(int recipeId) {
        RecipeStandardResTO response = new RecipeStandardResTO();
        //重置默认为失败
        response.setCode(RecipeCommonBaseTO.FAIL);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        //数据对比
        if (null == dbRecipe) {
            response.setMsg("不存在该处方");
            return response;
        }
        if (RecipeStatusConstant.DELETE == dbRecipe.getStatus()) {
            response.setCode(RecipeCommonBaseTO.SUCCESS);
            response.setMsg("处方状态相同");
            return response;
        }

        PrescribeService prescribeService = RecipeAPI.getService(PrescribeService.class);
        HosRecipeResult result = prescribeService.revokeRecipe(dbRecipe);
        if(HosRecipeResult.SUCCESS.equals(result.getCode())){
            response.setCode(RecipeCommonBaseTO.SUCCESS);
        }else{
            response.setMsg(result.getMsg());
        }
        return response;
    }
}
