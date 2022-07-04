package recipe.caNew;

import ca.service.ICaSignService;
import ca.vo.CommonSignRequest;
import com.alibaba.fastjson.JSON;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.SignEnum;
import recipe.service.RecipeCAService;

import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

//JRK
//后置处方签名实现
@Service
public class CaAfterProcessType extends AbstractCaProcessType {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaAfterProcessType.class);

    private ICaSignService caSignService = AppDomainContext.getBean("ca.iCaSignService", ICaSignService.class);

    //我们将开方的流程拆开：
    //后置CA操作：1.保存处方（公共操作），推送处方到his=》2.获取his推送结果=》3.成功后触发CA结果 =》4.CA成功后将处方向下流
    @Override
    public void signCABeforeRecipeFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        LOGGER.info("After---signCABeforeRecipeFunction 当前CA执行签名之前特应性行为，入参：recipeBean：{}，detailBeanList：{} ", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList));
        try {
            super.recipeHisResultBeforeCAFunction(recipeBean, detailBeanList);
        } catch (Exception e) {
            LOGGER.error("CaAfterProcessType signCABeforeRecipeFunction recipeBean= {}", JSON.toJSONString(recipeBean), e);
        }
    }

    @Override
    public void signCAAfterRecipeCallBackFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        LOGGER.info("After---signCAAfterRecipeCallBackFunction 当前CA执行签名之后回调特应性行为，入参：recipeBean：{}，detailBeanList：{} ", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList));
        try {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeBean.getRecipeId());
            Integer caType = caManager.caProcessType(recipe);
            if (null == caType) {
                return;
            }
            String memo = "HIS审核返回：写入his成功，审核通过---" + caType;
            super.caComplete(recipe, memo);
        } catch (Exception e) {
            LOGGER.error("CaAfterProcessType signCAAfterRecipeCallBackFunction recipeBean= {}", JSON.toJSONString(recipeBean), e);
        }
    }

    @Override
    public RecipeResultBean hisCallBackCARecipeFunction(Integer recipeId) {
        LOGGER.info("After---当前CA执行his回调之后组装CA响应特应性行为，入参：recipeId：{}", recipeId);
        //后置CA:首先组装CA请求 =》请求CA =》封装一个异步请求CA结果
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.warn("当前处方{}信息不存在，无法进行签名操作!", recipeId);
            return RecipeResultBean.getFail();
        }
        //设置处方状态为：签名中
        stateManager.updateStatus(recipeId, RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_DOC, SignEnum.sign_STATE_SUBMIT);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
        //1.调用组装CA请求
        RecipeCAService recipeCAService = ApplicationUtils.getRecipeService(RecipeCAService.class);
        CommonSignRequest commonSignRequest = recipeCAService.packageCAFromRecipe(recipeId, recipe.getDoctor(), true);
        LOGGER.info("当前请求CA的组装数据：{}", JSONUtils.toString(commonSignRequest));
        //2.请求后台的CA
        try {
            caSignService.commonCaSignAndSeal(commonSignRequest);
        } catch (Exception e) {
            LOGGER.error("请求CA异常 recipeId={}", recipeId, e);
            return RecipeResultBean.getFail();
        }
        return RecipeResultBean.getSuccess();
    }

}