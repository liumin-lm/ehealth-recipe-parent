package recipe.sign;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;

import java.util.Date;

@RpcBean
public class SignRecipeInfoService {

    @Autowired
    private SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO;

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
