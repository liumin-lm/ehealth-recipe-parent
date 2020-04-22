package recipe.sign;

import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.service.DoctorExtendService;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;
import recipe.service.RecipeService;

import java.util.Date;

@RpcBean
public class SignRecipeInfoService {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private DoctorExtendService doctorExtendService;

    @Autowired
    private SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO;

    @RpcService
    public SignDoctorRecipeInfo getSignInfoByRecipeId(Integer recipeId){
        SignDoctorRecipeInfo signDoctorRecipeInfo = signDoctorRecipeInfoDAO.getInfoByRecipeId(recipeId);
        if (signDoctorRecipeInfo == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "咨询订单不存在");
        }
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        if (recipeBean == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "咨询订单不存在");
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
