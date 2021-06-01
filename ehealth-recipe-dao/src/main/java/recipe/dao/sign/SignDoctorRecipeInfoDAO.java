package recipe.dao.sign;

import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

@RpcSupportDAO
public abstract class SignDoctorRecipeInfoDAO extends HibernateSupportDelegateDAO<SignDoctorRecipeInfo> {

    public SignDoctorRecipeInfoDAO(){
        super();
        this.setEntityName(SignDoctorRecipeInfo.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType = 1")
    public abstract SignDoctorRecipeInfo getRecipeInfoByRecipeId(@DAOParam("recipeId")Integer recipeId);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType =:serverType")
    public abstract SignDoctorRecipeInfo getRecipeInfoByRecipeIdAndServerType(@DAOParam("recipeId") Integer recipeId, @DAOParam("serverType") Integer serverType);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType=:serviceType")
    public abstract SignDoctorRecipeInfo getInfoByRecipeIdAndServiceType(@DAOParam("recipeId") Integer recipeId, @DAOParam("serviceType") Integer serviceType);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and type=:type and serverType = 1")
    public abstract SignDoctorRecipeInfo getRecipeInfoByRecipeIdAndType(@DAOParam("recipeId") Integer recipeId, @DAOParam("type") String type);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType =:serverType order by id desc")
    public abstract List<SignDoctorRecipeInfo> findRecipeInfoByRecipeIdAndServerType(@DAOParam("recipeId") Integer recipeId, @DAOParam("serverType") Integer serverType);

    @DAOMethod(sql = " from SignDoctorRecipeInfo where recipeId=:recipeId and serverType =:serverType and signDoctor =:doctorId  order by id desc")
    public abstract List<SignDoctorRecipeInfo> findSignInfoByRecipeIdAndDoctorId(@DAOParam("recipeId") Integer recipeId, @DAOParam("doctorId") Integer doctorId, @DAOParam("serverType") Integer serverType);
}
