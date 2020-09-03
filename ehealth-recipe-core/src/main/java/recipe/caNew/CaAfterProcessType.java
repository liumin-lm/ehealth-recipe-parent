package recipe.caNew;

import com.google.common.collect.ImmutableMap;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.audit.service.PrescriptionService;
import recipe.bean.DrugEnterpriseResult;
import recipe.ca.vo.CaSignResultVo;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.DrugDistributionService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.SaveAutoReviewRunable;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;

//JRK
//后置处方签名实现
@Service("caAfterProcessType")
public class CaAfterProcessType extends AbstractCaProcessType{
    private static final Logger LOGGER = LoggerFactory.getLogger(CaAfterProcessType.class);

    //我们将开方的流程拆开：
    //后置CA操作：1.保存处方（公共操作），推送处方到his=》2.获取his推送结果=》3.成功后触发CA结果 =》4.CA成功后将处方向下流
    @Override
    public void signCABeforeRecipeFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList){
        LOGGER.info("After---signCABeforeRecipeFunction 当前CA执行签名之前特应性行为，入参：recipeBean：{}，detailBeanList：{} ",  JSONUtils.toString(recipeBean),  JSONUtils.toString(detailBeanList));
        recipeHisResultBeforeCAFunction(recipeBean, detailBeanList);
    }

    @Override
    public void signCAAfterRecipeCallBackFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList){
        LOGGER.info("After---signCAAfterRecipeCallBackFunction 当前CA执行签名之后回调特应性行为，入参：recipeBean：{}，detailBeanList：{} ",  JSONUtils.toString(recipeBean),  JSONUtils.toString(detailBeanList));
        recipeHisResultAfterCAFunction(recipeBean.getRecipeId());
    }

    @Override
    public RecipeResultBean hisCallBackCARecipeFunction(Integer recipeId) {
        LOGGER.info("After---当前CA执行his回调之后组装CA响应特应性行为，入参：recipeId：{}", recipeId);
        //后置CA:首先组装CA请求 =》请求CA =》封装一个异步请求CA结果

        //设置处方状态为：签名中
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusConstant.SIGN_ING_CODE_DOC));
        LOGGER.info("当前处方{}设置成CA签名中", recipeId);
        //1.调用组装CA请求

        //2.请求后台的CA

        //3.返回一个异步操作的CA,中断状态
        RecipeResultBean recipeResultBean = new RecipeResultBean();
        recipeResultBean.setCode(RecipeResultBean.NO_ADDRESS);
        //将返回的CA结果给处方，设置处方流转
        return recipeResultBean;
    }

}