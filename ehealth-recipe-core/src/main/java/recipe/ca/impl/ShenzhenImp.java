//package recipe.ca.impl;
//
//import com.ngari.his.ca.model.*;
//import com.ngari.patient.dto.DoctorDTO;
//import com.ngari.patient.dto.EmploymentDTO;
//import com.ngari.patient.service.DoctorService;
//import com.ngari.patient.service.EmploymentService;
//import com.ngari.recipe.common.RecipeResultBean;
//import com.ngari.recipe.entity.Recipe;
//import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
//import ctd.account.user.support.SM3;
//import ctd.persistence.exception.DAOException;
//import ctd.util.AppContextHolder;
//import ctd.util.JSONUtils;
//import ctd.util.annotation.RpcBean;
//import ctd.util.annotation.RpcService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//import recipe.ApplicationUtils;
//import recipe.ca.CAInterface;
//import recipe.ca.ICommonCAServcie;
//import recipe.ca.vo.CaSignResultVo;
//import recipe.dao.sign.SignDoctorRecipeInfoDAO;
//import recipe.service.RecipeService;
//import recipe.sign.SignRecipeInfoService;
//import recipe.util.RedisClient;
//
//import java.util.Date;
//
///**
// * CA新对接模式已迁移miscellany项目中 后期不将recipe维护
// */
//
//@Deprecated
//@RpcBean("shenzhenCA")
//public class ShenzhenImp implements CAInterface {
//
//    private Logger logger = LoggerFactory.getLogger(ShenzhenImp.class);
//    private ICommonCAServcie iCommonCAServcie = AppContextHolder.getBean("iCommonCAServcie", ICommonCAServcie.class);
//    private RedisClient redisClient = AppContextHolder.getBean("redisClient", RedisClient.class);
//    private RecipeService recipeService = AppContextHolder.getBean("recipeService", RecipeService.class);
//    private SignRecipeInfoService signRecipeInfoService = AppContextHolder.getBean("signRecipeInfoService", SignRecipeInfoService.class);
//    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
//    private EmploymentService employmentService = ApplicationUtils.getBasicService(EmploymentService.class);
//    @Autowired
//    private SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO;
//
//    /**
//     * 深圳CA用户由人工维护（此接口判断是否有令牌）
//     *
//     * @param doctorId
//     * @return
//     */
//    @Override
//    public boolean caUserLoginAndGetCertificate(Integer doctorId) {
//        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
//        EmploymentDTO employmentDTO = employmentService.getByDoctorIdAndOrganId(doctorId,doctorDTO.getOrgan());
//        if (null != redisClient.get("encryptedToken_" + employmentDTO.getJobNumber())) {
//            return true;
//        };
//        return false;
//    }
//
//    /**
//     * 根据pin码获取令牌
//     *
//     * @param requestTO
//     * @return
//     */
//    @Override
//    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO) {
//        CaPasswordResponseTO responseTO = iCommonCAServcie.caTokenBusiness(requestTO);
//        String userAccount = requestTO.getUserAccount();
//        if (!StringUtils.isEmpty(responseTO.getValue())) {
//            //redisClient.set("encryptedToken_"+userAccount, responseTO.getValue());
//            redisClient.setEX("encryptedToken_"+userAccount,8*3600L,responseTO.getValue());
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public CaSignResultVo commonCASignAndSeal(CaSealRequestTO requestSealTO, Recipe recipe, Integer organId, String userAccount, String caPassword) {
//        logger.info("ShenzhenCA commonCASignAndSeal start requestSealTO=[{}],requestSealTO=[{}],organId=[{}],userAccount=[{}]",JSONUtils.toString(requestSealTO),JSONUtils.toString(recipe),organId,userAccount);
//        CaSignResultVo caSignResultVo = new CaSignResultVo();
//        caSignResultVo.setRecipeId(recipe.getRecipeId());
//        try {
//            //深圳CA为工号 recipe.getChecker() 为null的为医生
//            Integer doctorId;
//            if(recipe.getChecker() == null) {
//             doctorId =recipe.getDoctor();
//            }else {
//                doctorId =recipe.getChecker();
//            }
//            DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
//            EmploymentDTO employmentDTO = employmentService.getByDoctorIdAndOrganId(doctorId,doctorDTO.getOrgan());
//            userAccount = employmentDTO.getJobNumber();
//            logger.info("shenzhenCA commonCASignAndSeal the userAccount=[{}]",userAccount);
//            //获取手写图片
//            CaPictureRequestTO requestTO = new CaPictureRequestTO();
//            requestTO.setUserAccount(userAccount);
//            requestTO.setOrganId(organId);
//            CaPictureResponseTO caPictureResponseTO = iCommonCAServcie.newCaPictureBusiness(requestTO);
//            if (caPictureResponseTO == null || caPictureResponseTO.getCode() != 200) {
//                caSignResultVo.setCode(caPictureResponseTO.getCode());
//                caSignResultVo.setResultCode(0);
//                caSignResultVo.setMsg(caPictureResponseTO.getMsg());
//                return caSignResultVo;
//            }
//            caSignResultVo.setSignPicture(caPictureResponseTO.getCaPicture());
//
//            //数据签名
//            CaSignRequestTO caSignRequestTO = new CaSignRequestTO();
//            caSignRequestTO.setType("002");
//            if (!StringUtils.isEmpty(redisClient.get("encryptedToken_"+userAccount))) {
//                String token = redisClient.get("encryptedToken_"+userAccount);
//                logger.info("shenzhenCA commonCASignAndSeal token = [{}]",token);
//                caSignRequestTO.setCertVoucher(token);
//            } else {
//                throw new DAOException(505, "令牌已过期，请重新获取");
//            }
//            //签名原文
//            caSignRequestTO.setSignMsg(JSONUtils.toString(recipe));
//            caSignRequestTO.setOrganId(organId);
//            CaSignResponseTO caSignResponseTO = iCommonCAServcie.caSignBusiness(caSignRequestTO);
//            if (caSignResponseTO == null || caSignResponseTO.getCode() != 200) {
//                caSignResultVo.setCode(caSignResponseTO.getCode());
//                caSignResultVo.setResultCode(0);
//
//                caSignResultVo.setMsg(caSignResponseTO.getMsg());
//                return caSignResultVo;
//            }
//            caSignResultVo.setSignRecipeCode(caSignResponseTO.getSignValue());
//            caSignResultVo.setSignCADate(caSignResponseTO.getUserAccount());
//
//            //获取base64位证书
//            CaCertificateRequestTO caCertificateRequestTO = new CaCertificateRequestTO();
//            caCertificateRequestTO.setUserAccount(userAccount);
//            caCertificateRequestTO.setOrganId(organId);
//
//            CaCertificateResponseTO caCertificateResponseTO = iCommonCAServcie.caCertificateBusiness(caCertificateRequestTO);
//            if (caCertificateResponseTO == null || caCertificateResponseTO.getCode() != 200) {
//                caSignResultVo.setCode(caSignResponseTO.getCode());
//                caSignResultVo.setResultCode(0);
//                caSignResultVo.setMsg(caSignResponseTO.getMsg());
//                return caSignResultVo;
//            }
//            caSignResultVo.setCertificate(caCertificateResponseTO.getCretBody());
//            caSignResultVo.setCode(200);
//            caSignResultVo.setResultCode(1);
//            //保存信息
//           // saveSignDoctorRecipeInfo(caSignResultVo);
//        } catch (Exception e) {
//            caSignResultVo.setResultCode(0);
//            logger.error("shenzhenCAImpl commonCASignAndSeal 调用前置机失败 requestTO={}", e);
//        } finally {
//            logger.error("shenzhenCAImpl finally callback signResultVo={}", JSONUtils.toString(caSignResultVo));
//            // ca结果失败 删除token进行重新签名
//            if(caSignResultVo.getResultCode() != 1){
//                redisClient.del("encryptedToken_"+userAccount);
//            }
//            this.callbackRecipe(caSignResultVo, null == recipe.getChecker());
//        }
//        logger.info("ShanxiCAImpl commonCASignAndSeal end recipeId={},params: {}", recipe.getRecipeId(), JSONUtils.toString(caSignResultVo));
//        return caSignResultVo;
//
//    }
//
//    private void callbackRecipe(CaSignResultVo signResultVo, boolean isDoctor) {
//        if (isDoctor) {
//            recipeService.retryCaDoctorCallBackToRecipe(signResultVo);
//        } else {
//            recipeService.retryCaPharmacistCallBackToRecipe(signResultVo);
//        }
//    }
//}
