package recipe.dao.sign;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

@RpcSupportDAO
public abstract class SignDoctorRecipeInfoDAO extends HibernateSupportDelegateDAO<SignDoctorRecipeInfo> {

    public SignDoctorRecipeInfoDAO(){
        super();
        this.setEntityName(SignDoctorRecipeInfo.class.getName());
        this.setKeyField("id");
    }
}
