package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.RecipeParameterDao;
import recipe.drugsenterprise.bean.*;
import recipe.drugsenterprise.bean.yd.httpclient.HttpsClientUtils;
import recipe.drugsenterprise.bean.yd.model.*;
import recipe.drugsenterprise.bean.yd.utils.StdInputGenerator;

import java.util.*;

/**
 * 对接以大药企
 * @author yinsheng
 * @date 2019\12\13 0013 13:41
 */
@RpcBean("ydRemoteService")
public class YdRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YdRemoteService.class);

    private String public_key1_path ;
    private String private_key2_path ;

    public YdRemoteService(){
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        private_key2_path = recipeParameterDao.getByName("private_key2_path");
        public_key1_path = recipeParameterDao.getByName("public_key1_path");
    }

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("YdRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    private StdInputVo stdInputVo(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) throws Exception {
        RecipeVo recipeVo = YdRecipeVO.getRecipeVo(hospitalRecipeDTO);
        String params = recipeVo.toJSONString();
        return getStdInputVo(enterprise, params);
    }

    private StdInputVo getStdInputVo(DrugsEnterprise enterprise, String params) throws Exception {
        LOGGER.info("YdRemoteService-stdInputVo params:{}.", params);
        //设置RSA校验
        StdInputVo inputVo = StdInputGenerator.toStdInputVo(enterprise.getUserId(), enterprise.getPassword(), params, EncryptMode.RSA, public_key1_path);

        LOGGER.info("YdRemoteService-stdInputVo 上传数据参数："+inputVo.toJSONString());

        return inputVo;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @RpcService
    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        LOGGER.info("YdRemoteService-pushRecipeInfo hospitalRecipeDTO:{}.", JSONUtils.toString(hospitalRecipeDTO));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();

        if (!ObjectUtils.isEmpty(hospitalRecipeDTO)) {
            try{
                StdInputVo inputVo = stdInputVo(hospitalRecipeDTO, enterprise);
                String outputJson = HttpsClientUtils.doPost(enterprise.getBusinessUrl() + "/api/std/recipe/upload",inputVo.toJSONString());
                LOGGER.info("YdRemoteService-pushRecipeInfo 接口返回值:{}.", outputJson);
                if(outputJson != null){
                    StdOutputVo outputVo = StdOutputVo.fromJson(outputJson);
                    String data = outputVo.decodeStdOutput(EncryptMode.RSA,private_key2_path);
                    LOGGER.info("YdRemoteService-pushRecipeInfo 返回参数解析:{}.", data);
                    YdResponse ydResponse = JSONUtils.parse(data, YdResponse.class);
                    if ("true".equals(ydResponse.getSuccess())) {
                        result.setCode(DrugEnterpriseResult.SUCCESS);
                    } else {
                        result.setCode(DrugEnterpriseResult.FAIL);
                    }
                }
            }catch(Exception e){
                LOGGER.info("YdRemoteService-pushRecipeInfo error:{}.", e.getMessage(), e);
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_YD;
    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        LOGGER.info(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        return result;
    }
}
