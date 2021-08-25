package recipe.dao;

import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * 诊疗处方数据表
 *
 * @author fuzi
 */
@RpcSupportDAO
public abstract class RecipeTherapyDAO extends HibernateSupportDelegateDAO<RecipeTherapy> implements ExtendDao<RecipeTherapy> {

    public RecipeTherapyDAO() {
        super();
        this.setEntityName(RecipeTherapy.class.getName());
        this.setKeyField(SQL_KEY_ID);
    }


    @Override
    public boolean updateNonNullFieldByPrimaryKey(RecipeTherapy recipeTherapy) {
        return updateNonNullFieldByPrimaryKey(recipeTherapy, SQL_KEY_ID);
    }

    /**
     * 根据id查询诊疗处方数据
     *
     * @param id 诊疗id
     * @return
     */
    @DAOMethod
    public abstract RecipeTherapy getById(int id);

    /**
     * 根据处方id查询诊疗处方数据
     *
     * @param recipeId 处方id
     * @return
     */
    @DAOMethod
    public abstract RecipeTherapy getByRecipeId(Integer recipeId);

    /**
     * 分页 根据医生id 获取诊疗信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param start    页数
     * @param limit    条数
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organId=:organId and doctorId=:doctorId")
    public abstract List<RecipeTherapy> findTherapyByDoctorId(@DAOParam("organId") int organId, @DAOParam("doctorId") int doctorId
            , @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 根据医生id 获取诊疗信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organId=:organId and doctorId=:doctorId")
    public abstract List<RecipeTherapy> findTherapyByDoctorId(@DAOParam("organId") int organId, @DAOParam("doctorId") int doctorId);

    /**
     * 分页  根据医生id与复诊id 获取诊疗信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param clinicId 复诊id
     * @param start    页数
     * @param limit    条数
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organ_id=:organId and doctor_id=:doctorId and clinic_id=:clinicId")
    public abstract List<RecipeTherapy> findTherapyByDoctorIdAndClinicId(@DAOParam("organId") int organId, @DAOParam("doctorId") int doctorId,
                                                                         @DAOParam("clinicId") int clinicId, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 根据医生id与复诊id 获取诊疗信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param clinicId 复诊id
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organ_id=:organId and doctor_id=:doctorId and clinic_id=:clinicId")
    public abstract List<RecipeTherapy> findTherapyByDoctorIdAndClinicId(@DAOParam("organId") int organId, @DAOParam("doctorId") int doctorId, @DAOParam("clinicId") int clinicId);


    /**
     * 分页  根据患者id与复诊id 获取诊疗信息
     *
     * @param organId  机构id
     * @param mpiId    患者id
     * @param clinicId 复诊id
     * @param start    页数
     * @param limit    条数
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organ_id=:organId and mpi_id=:mpiId and clinic_id=:clinicId")
    public abstract List<RecipeTherapy> findTherapyByMpiIdAndClinicId(@DAOParam("organId") int organId, @DAOParam("mpiId") String mpiId,
                                                                      @DAOParam("clinicId") int clinicId, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 根据患者id与复诊id 获取诊疗信息
     *
     * @param organId  机构id
     * @param mpiId    患者id
     * @param clinicId 复诊id
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organ_id=:organId and mpi_id=:mpiId and clinic_id=:clinicId")
    public abstract List<RecipeTherapy> findTherapyByMpiIdAndClinicId(@DAOParam("organId") int organId, @DAOParam("mpiId") String mpiId, @DAOParam("clinicId") int clinicId);


}