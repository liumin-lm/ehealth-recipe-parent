package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.drugsenterprise.bean.yd.httpclient.HttpsClientUtils;
import recipe.drugsenterprise.bean.yd.model.*;
import recipe.drugsenterprise.bean.yd.utils.StdInputGenerator;

import javax.annotation.Resource;
import java.util.*;

/**
 * 对接以大药企
 * @author yinsheng
 * @date 2019\12\13 0013 13:41
 */
@RpcBean("ydRemoteService")
public class YdRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YdRemoteService.class);

    private static final String public_key1_path = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrfoEAF7+NkAfTqOrakgfH3u9xsaEZxJ/3QB/m3iGSDuolmSaajsBBH1AD4Op9yOhN1mE92Fx6sosBy33XGd2YVfWxSXDFTR3vPPbDJZpJgMYeZw4tz1xn6sVP/dUg28A3w4rVQ4FuYLJ2WvdfOjiiZtWghpIBynQxcHgBW61xHQIDAQAB";
    private static final String private_key2_path = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALGcz+NKE50fJDY0D8i4ysApvv/4/wwihFvSAZzWXnUBGkLSx/mTIeV/QSHUutcmtuoZhACruKI3VB2RBXpjjvXeZF9P4FuUmuuB91A4fOW66+EpxRPq1OoZaB5B9O60Y8AZh+V+nDK+udgI4Thl77vC6dwaZvjeEp44LdQFxBzZAgMBAAECgYAF4YVYp0lC+JcAXHTxVn0QI9G5NAtt4W60g52eDdMO2Lx/3e7VKrQCn1YOwrZ1DUkdMz8VrpnsdRyJ5hViWg2PtGstI956jcXESppeCWDP+peoG/2RBjC7wK3LAVb5qwTukxDzNJfcUVtdJBUirEcJb1PyGS03HJtGEUAMdD1KAQJBAOELWz3Nwa/Bn1Dz+zHn1gmyM8llBTkbw5AWrzTngKorVREFpGPzsXSkg8vKsNtjoRWaeCOgqTm4VQj+Gkul2uECQQDKCzTJrr8kwIQRFTA/lKpOaE+/f8pMiBSshwfPPLxfzCrbQ3YCd1IxWMAhcyql83tfDK+R3w0gagPBOPZUsTj5AkAzznh3ttlCy7EQYspOB8/nNYXkdAQKzJBtqDs3U5/0DLutin34oI4WixToIkYqizn3DjNgCElMx1mUE2McTRchAkEAtS3BY44pZ/qfM3ZtssZMxkzyHoao0WJCL8hSr3sGbV13nPHs1B9N/GRavmQ4/WHO4xhMJKIBcmy++zlqY94ceQJBALZdU+2lYEROKZ7B8C5SPHYVQzAgIwgKkbxdUT1Ztv+xMIUJRa8iAjZUGIxKk53dHMfU58kP5/ah/M2TmeAlfbc=";

    @Resource
    private RecipeDAO recipeDAO;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("YdRemoteService tokenUpdateImpl not implement.");
    }

    private StdInputVo stdInputVo(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) throws Exception {
        RecipeVo recipeVo = YdRecipeVO.getRecipeVo(hospitalRecipeDTO);
        String params = recipeVo.toJSONString();
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
    public DrugEnterpriseResult pushRecipeInfo(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        LOGGER.info("YdRemoteService-pushRecipeInfo hospitalRecipeDTO:{}.", JSONUtils.toString(hospitalRecipeDTO));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();

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
