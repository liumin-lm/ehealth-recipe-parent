package recipe.sign;

import com.alibaba.fastjson.JSONObject;
import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.service.DoctorExtendService;
import com.ngari.patient.service.DoctorService;
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
            signDoctorRecipeInfo.setRecipeId(recipeId);
            signDoctorRecipeInfo = signDoctorRecipeInfoDAO.save(signDoctorRecipeInfo);
            return signDoctorRecipeInfo;
        }

        DoctorExtendDTO doctorExtendDTODoc =  doctorExtendService.getByDoctorId(recipeBean.getDoctor());
        if (doctorExtendDTODoc != null) {
            signDoctorRecipeInfo.setSealDataDoc(doctorExtendDTODoc.getSealData());
        }
        DoctorExtendDTO doctorExtendDTOPha = doctorExtendService.getByDoctorId(recipeBean.getChecker());
        if (doctorExtendDTOPha != null) {
            signDoctorRecipeInfo.setSealDataPha(doctorExtendDTOPha.getSealData());
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
}
