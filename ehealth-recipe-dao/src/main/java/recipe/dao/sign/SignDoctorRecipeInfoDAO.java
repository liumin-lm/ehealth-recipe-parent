package recipe.dao.sign;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

@RpcSupportDAO
public abstract class SignDoctorRecipeInfoDAO extends HibernateSupportDelegateDAO<SignDoctorRecipeInfo> {

    public SignDoctorRecipeInfoDAO(){
        super();
        this.setEntityName(SignDoctorRecipeInfo.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType = 1")
    public abstract SignDoctorRecipeInfo getRecipeInfoByRecipeId(@DAOParam("recipeId")Integer recipeId);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType=:serviceType")
    public abstract SignDoctorRecipeInfo getInfoByRecipeIdAndServiceType(@DAOParam("recipeId")Integer recipeId, @DAOParam("serviceType")Integer serviceType);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and type=:type and serverType = 1")
    public abstract SignDoctorRecipeInfo getRecipeInfoByRecipeIdAndType(@DAOParam("recipeId")Integer recipeId, @DAOParam("type")String type);
}
