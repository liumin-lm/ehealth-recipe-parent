package recipe.sign;

import com.alibaba.fastjson.JSONObject;
import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.service.DoctorExtendService;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;
import recipe.service.RecipeService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RpcBean
public class SignRecipeInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SignDoctorRecipeInfo.class);

    @Autowired
    private DoctorExtendService doctorExtendService;

    @Autowired
    private SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO;

    @RpcService
    public boolean updateSignInfoByRecipeId(SignDoctorRecipeInfo signDoctorRecipeInfo){
        logger.info("SignRecipeInfoService updateSignInfoByRecipeId info[{}]", JSONObject.toJSONString(signDoctorRecipeInfo));

        if (signDoctorRecipeInfo == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "signDoctorRecipeInfo is not null!");
        }
        SignDoctorRecipeInfo recipeInfo = signDoctorRecipeInfoDAO.getInfoByRecipeId(signDoctorRecipeInfo.getRecipeId());
        if (signDoctorRecipeInfo == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "处方订单不存在");
        }

        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignCaDateDoc())) {
            recipeInfo.setSignCaDateDoc(signDoctorRecipeInfo.getSignCaDateDoc());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignCodeDoc())) {
            recipeInfo.setSignCodeDoc(signDoctorRecipeInfo.getSignCodeDoc());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignFileDoc())) {
            recipeInfo.setSignFileDoc(signDoctorRecipeInfo.getSignFileDoc());
        }
        if (signDoctorRecipeInfo.getSignDate() != null) {
            recipeInfo.setSignDate(signDoctorRecipeInfo.getSignDate());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignCaDatePha())) {
            recipeInfo.setSignCaDatePha(signDoctorRecipeInfo.getSignCaDatePha());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignCodePha())) {
            recipeInfo.setSignCodePha(signDoctorRecipeInfo.getSignCodePha());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignFilePha())) {
            recipeInfo.setSignFilePha(signDoctorRecipeInfo.getSignFilePha());
        }
        if (signDoctorRecipeInfo.getCheckDatePha() != null) {
            recipeInfo.setCheckDatePha(signDoctorRecipeInfo.getCheckDatePha());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignRemarkDoc())) {
            recipeInfo.setSignRemarkDoc(signDoctorRecipeInfo.getSignRemarkDoc());
        }
        if (StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignRemarkPha())) {
            recipeInfo.setSignRemarkPha(signDoctorRecipeInfo.getSignRemarkPha());
        }
        if(StringUtils.isNotEmpty(signDoctorRecipeInfo.getSignBefText())){
            recipeInfo.setSignBefText(signDoctorRecipeInfo.getSignBefText());
        }

        signDoctorRecipeInfoDAO.update(recipeInfo);
        return true;
    }


    @RpcService
    public SignDoctorRecipeInfo getSignInfoByRecipeId(Integer recipeId){
        logger.info("getSignInfoByRecipeId start recipeId=" + recipeId);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        if (recipeBean == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "处方订单不存在");
        }

        SignDoctorRecipeInfo signDoctorRecipeInfo = signDoctorRecipeInfoDAO.getInfoByRecipeId(recipeId);
        if (signDoctorRecipeInfo == null) {
            signDoctorRecipeInfo = new SignDoctorRecipeInfo();
            signDoctorRecipeInfo.setRecipeId(recipeId);
            signDoctorRecipeInfo.setCreateDate(new Date());
            signDoctorRecipeInfo.setLastmodify(new Date());
            signDoctorRecipeInfo = signDoctorRecipeInfoDAO.save(signDoctorRecipeInfo);
            return signDoctorRecipeInfo;
        }

        if(recipeBean.getDoctor() != null) {
            DoctorExtendDTO doctorExtendDTODoc =  doctorExtendService.getByDoctorId(recipeBean.getDoctor());
            if (doctorExtendDTODoc != null) {
                signDoctorRecipeInfo.setSealDataDoc(doctorExtendDTODoc.getSealData());
            }
        }

        if(recipeBean.getChecker() != null){
            DoctorExtendDTO doctorExtendDTOPha = doctorExtendService.getByDoctorId(recipeBean.getChecker());
            if (doctorExtendDTOPha != null) {
                signDoctorRecipeInfo.setSealDataPha(doctorExtendDTOPha.getSealData());
            }
        }
        return signDoctorRecipeInfo;
    }

    @RpcService
    public SignDoctorRecipeInfo setSignRecipeInfo(Integer recipeId, boolean isDoctor, String serCode){
        SignDoctorRecipeInfo signDoctorRecipeInfo = new SignDoctorRecipeInfo();
        if (isDoctor) {
            signDoctorRecipeInfo.setCaSerCodeDoc(serCode);
        } else {
            signDoctorRecipeInfo.setCaSerCodePha(serCode);
        }
        signDoctorRecipeInfo.setCreateDate(new Date());
        signDoctorRecipeInfo.setLastmodify(new Date());
        signDoctorRecipeInfo.setRecipeId(recipeId);
        return signDoctorRecipeInfoDAO.save(signDoctorRecipeInfo);
    }

    @RpcService
    public Map getSignInfoByRegisterID(Integer recipeId, String type){
        logger.info("getSignInfoByRegisterID start recipeId={}=,type={}=", recipeId, type);
        SignDoctorRecipeInfo signDoctorRecipeInfo =signDoctorRecipeInfoDAO.getInfoByRecipeIdAndType(recipeId, type);
        Map map = new HashMap();
        if (signDoctorRecipeInfo != null) {
            map.put("signCodeDoc",signDoctorRecipeInfo.getSignCodeDoc());
            map.put("signRemarkDoc",signDoctorRecipeInfo.getSignRemarkDoc());
            map.put("signCodePha",signDoctorRecipeInfo.getSignCodePha());
            map.put("signRemarkPha",signDoctorRecipeInfo.getSignRemarkPha());
        }
        return map;
    }

    /**
     * ca保存ca信息
     * @param recipeId 处方ID
     * @param signCode 签名摘要
     * @param signCrt 签名值
     * @param isDoctor true 医生 false 药师
     */
    @RpcService
    public void saveSignInfoByRecipe(Integer recipeId, String signCode, String signCrt, boolean isDoctor, String type){

        logger.info("saveSignInfoByRecipe infos recipeId={}=,signCode={}=,signCrt={}=,isDoctor={}=, type={}=",recipeId, signCode, signCrt, isDoctor, type);
        SignDoctorRecipeInfo signDoctorRecipeInfo = signDoctorRecipeInfoDAO.getInfoByRecipeIdAndType(recipeId, type);
        if (signDoctorRecipeInfo == null) {
            signDoctorRecipeInfo = new SignDoctorRecipeInfo();
            signDoctorRecipeInfo.setRecipeId(recipeId);
            signDoctorRecipeInfo.setCreateDate(new Date());
            signDoctorRecipeInfo = getInfo(signDoctorRecipeInfo, signCode, signCrt,isDoctor, type);
            signDoctorRecipeInfoDAO.save(signDoctorRecipeInfo);
        } else {
            signDoctorRecipeInfo = getInfo(signDoctorRecipeInfo, signCode, signCrt,isDoctor, type);
            signDoctorRecipeInfoDAO.update(signDoctorRecipeInfo);
        }
    }

    private SignDoctorRecipeInfo getInfo (SignDoctorRecipeInfo signDoctorRecipeInfo, String signCode, String signCrt, boolean isDoctor,String type) {
        if (isDoctor) {
            signDoctorRecipeInfo.setSignRemarkDoc(signCrt);
            signDoctorRecipeInfo.setSignCodeDoc(signCode);
        } else {
            signDoctorRecipeInfo.setSignRemarkPha(signCrt);
            signDoctorRecipeInfo.setSignCodePha(signCode);
        }
        signDoctorRecipeInfo.setLastmodify(new Date());
        signDoctorRecipeInfo.setType(type);
        return signDoctorRecipeInfo;
    }
}
