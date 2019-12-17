package recipe.drugsenterprise;

import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.hisprescription.model.HospitalDrugDTO;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.ChinaIDNumberUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.util.DateConversion;
import recipe.util.RSAEncryptUtils;

import javax.annotation.Resource;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

/**
 * 对接以大药企
 * @author yinsheng
 * @date 2019\12\13 0013 13:41
 */
@RpcBean("ydRemoteService")
public class YdRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YdRemoteService.class);

    @Resource
    private RecipeDAO recipeDAO;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("YdRemoteService tokenUpdateImpl not implement.");
    }

    @RpcService
    public void test() {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(3050);

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        LOGGER.info("YdRemoteService-pushRecipeInfo hospitalRecipeDTO:{}.", JSONUtils.toString(hospitalRecipeDTO));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();

        Integer depId = enterprise.getId();

        YdRecipeInfoRequest recipeInfoRequest = new YdRecipeInfoRequest();
        if (!ObjectUtils.isEmpty(hospitalRecipeDTO)) {
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
            IOrganService organService = BaseAPI.getService(IOrganService.class);
            OrganBean organ = null;
            List<OrganBean> organList = organService.findByOrganizeCode(hospitalRecipeDTO.getOrganId());
            if (CollectionUtils.isNotEmpty(organList)) {
                organ = organList.get(0);
            }
            if (organ == null) {
                return getDrugEnterpriseResult(result, "机构信息为空");
            }
            recipeInfoRequest.setRecipeno(hospitalRecipeDTO.getRecipeCode());

            recipeInfoRequest.setHospitalid(organ.getOrganizeCode());
            recipeInfoRequest.setCaseno(hospitalRecipeDTO.getPatientNumber()); //病历号
            //TODO 诊疗卡号
            recipeInfoRequest.setHiscardno(""); //诊疗卡号
            recipeInfoRequest.setPatientname(hospitalRecipeDTO.getPatientName());
            recipeInfoRequest.setIdnumber(hospitalRecipeDTO.getCertificate());
            recipeInfoRequest.setMobile(hospitalRecipeDTO.getPatientTel());
            recipeInfoRequest.setOuthospno(hospitalRecipeDTO.getRegisterId());

            recipeInfoRequest.setEmpsex(hospitalRecipeDTO.getPatientSex());
            try{
                String certificate = ChinaIDNumberUtil.convert15To18(hospitalRecipeDTO.getCertificate());
                Integer age = ChinaIDNumberUtil.getAgeFromIDNumber(certificate);
                Date birthday = ChinaIDNumberUtil.getBirthFromIDNumber(certificate);
                recipeInfoRequest.setBirthdate(DateConversion.formatDate(birthday));
                recipeInfoRequest.setAge(age+"");
            }catch(Exception e){
                LOGGER.info("YdRemoteService-pushRecipeInfo");
            }
            recipeInfoRequest.setVisitdate(hospitalRecipeDTO.getCreateDate());
            recipeInfoRequest.setPatienttypename("门诊");
            recipeInfoRequest.setMedicarecategname("");
            recipeInfoRequest.setSignalsourcetypename("");
            DepartmentDTO departmentDTO = departmentService.get(Integer.parseInt(hospitalRecipeDTO.getDepartId()));
            if (departmentDTO == null) {
                return getDrugEnterpriseResult(result, "科室信息为空");
            }
            recipeInfoRequest.setRegistdeptcode(departmentDTO.getProfessionCode());
            recipeInfoRequest.setRegistdeptname(departmentDTO.getName());
            recipeInfoRequest.setRecipestatus("1");
            recipeInfoRequest.setHospital(organ.getName());
            recipeInfoRequest.setRegistdrcode(hospitalRecipeDTO.getDoctorNumber());
            recipeInfoRequest.setRegistdrname(hospitalRecipeDTO.getDoctorName());
            recipeInfoRequest.setDiagcode(hospitalRecipeDTO.getOrganDiseaseId());
            recipeInfoRequest.setDiagname(hospitalRecipeDTO.getOrganDiseaseName());
            List<HospitalDrugDTO> hospitalDrugDTOS = hospitalRecipeDTO.getDrugList();
            List<RecipeDtlVo> recipeDtlVos = new ArrayList<>();
            for (HospitalDrugDTO hospitalDrugDTO : hospitalDrugDTOS) {
                //处方明细
                RecipeDtlVo recipeDtlVo = new RecipeDtlVo();
                recipeDtlVo.setRecipedtlno(hospitalDrugDTO.getRecipedtlno());
                //TODO 药品剂型代码
                recipeDtlVo.setClasstypeno("");
                //TODO 药品剂型名称
                recipeDtlVo.setClasstypename("");
                recipeDtlVo.setDrugcode(hospitalDrugDTO.getDrugCode());
                recipeDtlVo.setDrugname(hospitalDrugDTO.getDrugName());
                recipeDtlVo.setProdarea("无");
                recipeDtlVo.setFactoryname(hospitalDrugDTO.getProducer());
                recipeDtlVo.setDrugspec(hospitalDrugDTO.getSpecification());
                String usingRate ;
                String usePathways ;
                try {
                    usingRate = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(hospitalDrugDTO.getUsingRate());
                    usePathways = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(hospitalDrugDTO.getUsePathways());
                } catch (ControllerException e) {
                    return getDrugEnterpriseResult(result, "药物使用频率使用途径获取失败" + e.getMessage());
                }
                recipeDtlVo.setFreqname(usingRate);
                recipeDtlVo.setSustaineddays(Integer.parseInt(hospitalDrugDTO.getUesDays()));
                recipeDtlVo.setQuantity(Double.parseDouble(hospitalDrugDTO.getUseDose()));
                recipeDtlVo.setDrugunit(hospitalDrugDTO.getUseDoseUnit());
                recipeDtlVo.setUnitprice(Double.parseDouble(hospitalDrugDTO.getDrugFee()));
                recipeDtlVo.setMeasurement("");
                recipeDtlVo.setMeasurementunit(hospitalDrugDTO.getUseDoseUnit());
                recipeDtlVo.setUsagename(usePathways);
                recipeDtlVo.setDosage(usingRate);
                recipeDtlVo.setDosageunit(hospitalDrugDTO.getUseDoseUnit());
                recipeDtlVos.add(recipeDtlVo);
            }
            recipeInfoRequest.setDetaillist(recipeDtlVos);

        }
        String parame = JSONUtils.toString(recipeInfoRequest);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(enterprise.getBusinessUrl());
        httpPost.setHeader("Content-Type", "application/json;charset=utf8");
        StringEntity requestEntry = new StringEntity(getStringEntity(enterprise, parame), ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntry);
        try{
            //获取响应消息
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);

            String decryptResponse = getDecryptResponse(responseStr);
            LOGGER.info("[{}][{}] pushRecipeInfo 返回:{}", depId, enterprise.getName(), decryptResponse);
            if (StringUtils.isNotEmpty(decryptResponse)) {
                YdResponse ydResponse = JSONUtils.parse(decryptResponse, YdResponse.class);
                if ("true".equals(ydResponse.getSuccess())) {
                    result.setCode(DrugEnterpriseResult.SUCCESS);
                } else {
                    result.setCode(DrugEnterpriseResult.FAIL);
                }
            }
        }catch(Exception e){
            LOGGER.info("YdRemoteService-pushRecipeInfo 获取响应失败.");
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return null;
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

    /**
     * 获取Cipher
     * @param drugsEnterprise  药企
     * @param param            请求参数
     * @return                 cipher
     */
    private YdCipher getCipher(DrugsEnterprise drugsEnterprise, String param){
        YdCipher cipher = new YdCipher();
        cipher.setAppid(drugsEnterprise.getUserId());
        cipher.setAppkey(drugsEnterprise.getPassword());
        cipher.setParam(param);
        cipher.setParamLength(param.length()+"");
        cipher.setTimestamp(System.currentTimeMillis()+"");
        cipher.setRandom(generateCode() + "");
        return cipher;
    }

    private String getStringEntity(DrugsEnterprise drugsEnterprise, String param){
        YdRequest request =  new YdRequest();
        request.setAppid(drugsEnterprise.getUserId());
        String encryptCipher = "";
        try {
            RSAPublicKey publicKey = RSAEncryptUtils.loadPublicKeyByFile();
            byte[] cipher = RSAEncryptUtils.encrypt(publicKey, param.getBytes());
            encryptCipher = new String(cipher, "utf-8");
        }catch (Exception e){
            LOGGER.info("YdRemoteService-getStringEntity 公钥加密失败.");
        }

        request.setCipher(encryptCipher);
        request.setSignature(getCipher(drugsEnterprise, param).toMD5String());
        request.setEncryptMode("RSA");
        return JSONUtils.toString(request);
    }

    private String getDecryptResponse(String response){
        try{
            RSAPrivateKey privateKey = RSAEncryptUtils.loadPrivateKeyByFile();
            byte[] decodeStr = RSAEncryptUtils.decrypt(privateKey,response.getBytes());
            return new String(decodeStr, "utf-8");
        }catch (Exception e){
            LOGGER.info("YdRemoteService-getStringEntity 公钥加密失败.");
        }
        return "";
    }

    /**
     * 生成8位随机数
     * @return 8位随机数
     */
    private static synchronized int generateCode() {
        int min = 10000000;
        int max = 99999999;
        Random random = new Random();
        return random.nextInt(max)%(max-min+1) + min;
    }
}
