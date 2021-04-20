//package recipe.ca.impl;
//
//import com.ngari.his.ca.model.*;
//import com.ngari.patient.dto.DoctorDTO;
//import com.ngari.patient.dto.DoctorExtendDTO;
//import com.ngari.patient.service.BasicAPI;
//import com.ngari.patient.service.DoctorExtendService;
//import com.ngari.patient.service.DoctorService;
//import com.ngari.patient.service.EmploymentService;
//import com.ngari.recipe.common.RecipeResultBean;
//import com.ngari.recipe.entity.Recipe;
//import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
//import ctd.persistence.DAOFactory;
//import ctd.util.AppContextHolder;
//import ctd.util.JSONUtils;
//import ctd.util.annotation.RpcBean;
//import ctd.util.annotation.RpcService;
//import ctd.util.event.GlobalEventExecFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.StringUtils;
//import recipe.ApplicationUtils;
//import recipe.ca.CAInterface;
//import recipe.ca.ICommonCAServcie;
//import recipe.ca.vo.CaSignResultVo;
//import recipe.dao.sign.SignDoctorCaInfoDAO;
//import recipe.service.RecipeService;
//
//import java.util.Date;
//import java.util.List;
//
///**
// * CA标准化对接文档
// */
//@RpcBean("shanghaiCA")
//public class ShanghaiCAImpl implements CAInterface {
//    private static final Logger LOGGER = LoggerFactory.getLogger(ShanghaiCAImpl.class);
//
//    private ICommonCAServcie iCommonCAServcie= AppContextHolder.getBean("iCommonCAServcie", ICommonCAServcie.class);
//
//    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
//    private RecipeService recipeService = AppContextHolder.getBean("recipeService", RecipeService.class);
//    private SignDoctorCaInfoDAO signDoctorCaInfoDAO = DAOFactory.getDAO(SignDoctorCaInfoDAO.class);
//
//    /**
//     * CA用户注册、申请证书接口
//     * @param doctorId
//     * @return
//     */
//    @RpcService
//    public boolean caUserLoginAndGetCertificate(Integer doctorId){
//        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
//
//        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
//        caAccountRequestTO.setOrganId(doctorDTO.getOrgan());
//        caAccountRequestTO.setUserName(doctorDTO.getName());
//        return iCommonCAServcie.caUserBusiness(caAccountRequestTO);
//    }
//
//    /**
//     * CA密码接口
//     * @param requestTO
//     * @return
//     */
//    @RpcService
//    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO) {
//        return true;
//    }
//
//    /**
//     * 标准化CA签名及签章接口
//     * 上海胸科有签章，而且签名、签章都是异步推送。
//     * 上海其他地方只有签名
//     * @param requestSealTO
//     * @param organId
//     * @param userAccount
//     * @param caPassword
//     */
//    @RpcService
//    public CaSignResultVo commonCASignAndSeal(CaSealRequestTO requestSealTO, Recipe recipe, Integer organId, String userAccount, String caPassword) {
//        LOGGER.info("shanghaiCA commonCASignAndSeal start requestSealTO={},recipeId={},organId={},userAccount={},caPassword={}",
//                JSONUtils.toString(requestSealTO), recipe.getRecipeId(),organId, userAccount, caPassword);
//        CaSignResultVo signResultVo = new CaSignResultVo();
//        signResultVo.setRecipeId(recipe.getRecipeId());
//        try {
//            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
//            List<String> jobNumbers = employmentService.findJobNumberByDoctorIdAndOrganId(recipe.getDoctor(), recipe.getClinicOrgan());
//            //电子签名
//            CaSignRequestTO caSignRequestTO = new CaSignRequestTO();
//            caSignRequestTO.setCretMsg(null);
//            caSignRequestTO.setOrganId(organId);
//            if (!CollectionUtils.isEmpty(jobNumbers)) {
//                caSignRequestTO.setJobnumber(jobNumbers.get(0));
//            }
//            caSignRequestTO.setSignMsg(JSONUtils.toString(recipe));
//            caSignRequestTO.setUserAccount(userAccount);
//            CaSignResponseTO responseTO = iCommonCAServcie.caSignBusiness(caSignRequestTO);
//            if (responseTO == null || responseTO.getCode() != 200) {
//                signResultVo.setCode(responseTO.getCode());
//                signResultVo.setMsg(responseTO.getMsg());
//                signResultVo.setResultCode(0);
//                return signResultVo;
//            }
//            signResultVo.setSignRecipeCode(responseTO.getSignValue());
//            //上传手签图片(暂不实现)
//
//            //获取时间戳数据
//            CaSignDateRequestTO caSignDateRequestTO = new CaSignDateRequestTO();
//            caSignDateRequestTO.setOrganId(organId);
//            caSignDateRequestTO.setUserAccount(userAccount);
//            caSignDateRequestTO.setSignMsg(JSONUtils.toString(recipe));
//
//            CaSignDateResponseTO responseDateTO = iCommonCAServcie.caSignDateBusiness(caSignDateRequestTO);
//            if(responseDateTO == null){
//                signResultVo.setCode(RecipeResultBean.FAIL);
//                signResultVo.setResultCode(0);
//                signResultVo.setMsg("caSignDateBusiness res is null");
//                return signResultVo;
//            }
//            if (responseDateTO.getCode() != 200) {
//                signResultVo.setResultCode(0);
//                signResultVo.setMsg(responseDateTO.getMsg());
//                signResultVo.setCode(responseDateTO.getCode());
//                return signResultVo;
//            }
//            signResultVo.setSignCADate(responseDateTO.getSignDate());
//            signResultVo.setCode(200);
//
//            // 电子签章
//            requestSealTO.setOrganId(organId);
//            requestSealTO.setUserPin(caPassword);
//            requestSealTO.setUserAccount(userAccount);
//            requestSealTO.setDoctorType(null == recipe.getChecker() ? "0" : "1");
//            requestSealTO.setSignMsg(JSONUtils.toString(recipe));
//            if (!CollectionUtils.isEmpty(jobNumbers)) {
//                requestSealTO.setJobnumber(jobNumbers.get(0));
//            }
//            CaSealResponseTO responseSealTO = iCommonCAServcie.caSealBusiness(requestSealTO);
//            if (responseSealTO == null){
//                signResultVo.setCode(RecipeResultBean.FAIL);
//                signResultVo.setResultCode(0);
//                signResultVo.setMsg("caSealBusiness res is null");
//                return signResultVo;
//            }
//            if (responseSealTO.getCode() != 200
//                    && requestSealTO.getCode() != 404 && requestSealTO.getCode() != 405){
//                signResultVo.setCode(responseSealTO.getCode());
//                signResultVo.setResultCode(0);
//                signResultVo.setMsg(responseSealTO.getMsg());
//                return signResultVo;
//            }
//            signResultVo.setPdfBase64(responseSealTO.getPdfBase64File());
//            if (StringUtils.isEmpty(signResultVo.getSignRecipeCode()) && StringUtils.isEmpty(signResultVo.getPdfBase64())) {
//                // 上海胸科异步推送返回的签名签章都为空
//                signResultVo.setResultCode(-1);
//            }else {
//                signResultVo.setResultCode(1);
//            }
//            // 启动异步线程对证书号进行获取保存（上海六院）
//            GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
//                @Override
//                public void run() {
//                    getAndSaveCertificate(recipe.getDoctor(), recipe.getClinicOrgan(),userAccount);
//                }
//            });
//        } catch (Exception e){
//            signResultVo.setResultCode(0);
//            LOGGER.error("ShanghaiCAImpl commonCASignAndSeal 调用前置机失败 requestSealTO={},recipeId={},organId={},userAccount={},caPassword={}",
//                    JSONUtils.toString(requestSealTO), recipe.getRecipeId(),organId, userAccount, caPassword,e);
//            LOGGER.error("commonCASignAndSeal Exception", e);
//        }finally {
//            LOGGER.error("ShanxiCAImpl finally callback signResultVo={}", JSONUtils.toString(signResultVo));
//            this.callbackRecipe(signResultVo, null == recipe.getChecker());
//        }
//        LOGGER.info("shanghaiCA commonCASignAndSeal end recipeId={},params: {}", recipe.getRecipeId(),JSONUtils.toString(signResultVo));
//        return signResultVo;
//    }
//
//    private void callbackRecipe(CaSignResultVo signResultVo, boolean isDoctor) {
//        if (isDoctor) {
//            recipeService.retryCaDoctorCallBackToRecipe(signResultVo);
//        }else {
//            recipeService.retryCaPharmacistCallBackToRecipe(signResultVo);
//        }
//    }
//
//    /**
//     *  保存证书序列号 供监管平台调用（上海六院取得是userAccount的值(身份证)作为CA用户认证  其他的为工号作为认证）
//     * @param doctorId
//     * @param organId
//     * @return
//     */
//    @RpcService
//    public void getAndSaveCertificate(Integer doctorId, Integer organId,String userAccount) {
//        DoctorExtendService doctorExtendService = BasicAPI.getService(DoctorExtendService.class);
//        SignDoctorCaInfo result = signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(doctorId, "shanghaiCa");
//        if (result == null) {
//            CaCertificateRequestTO requestTO = new CaCertificateRequestTO();
//            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
//            List<String> jobNumbers = employmentService.findJobNumberByDoctorIdAndOrganId(doctorId, organId);
//            if (!CollectionUtils.isEmpty(jobNumbers)) {
//                requestTO.setJobNubmer(jobNumbers.get(0));
//            }
//            requestTO.setUserAccount(userAccount);
//            requestTO.setOrganId(organId);
//            CaCertificateResponseTO responseTO = iCommonCAServcie.caCertificateBusiness(requestTO);
//            if (responseTO != null && responseTO.getCode() == 200) {
//                SignDoctorCaInfo signDoctorCaInfo = new SignDoctorCaInfo();
//                DoctorExtendDTO doctorExtendDTO = new DoctorExtendDTO();
//                signDoctorCaInfo.setDoctorId(doctorId);
//                doctorExtendDTO.setDoctorId(doctorId);
//                signDoctorCaInfo.setCaType("shanghaiCa");
//                signDoctorCaInfo.setCert_voucher(responseTO.getCretBody());
//                signDoctorCaInfo.setCertSerial(responseTO.getCretSerial());
//                doctorExtendDTO.setSerialNumCA(responseTO.getCretSerial());
//                signDoctorCaInfo.setCreateDate(new Date());
//                signDoctorCaInfo.setLastmodify(new Date());
//                signDoctorCaInfoDAO.save(signDoctorCaInfo);
//                doctorExtendService.updateCertificateByDocId(doctorExtendDTO);
//            }
//        }
//    }
//}
