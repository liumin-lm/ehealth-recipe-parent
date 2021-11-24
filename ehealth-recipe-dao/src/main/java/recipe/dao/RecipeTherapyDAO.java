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
    @DAOMethod(sql = "from RecipeTherapy where organId=:organId and doctorId=:doctorId order by clinic_id desc")
    public abstract List<RecipeTherapy> findTherapyPageByDoctorId(@DAOParam("organId") int organId, @DAOParam("doctorId") int doctorId
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
     * 根据患者信息获取诊疗列表
     * @param mpiIds  患者信息
     * @param start   start
     * @param limit   limit
     * @return  诊疗列表
     */
    @DAOMethod(sql = "from RecipeTherapy where mpi_id in (:mpiIds) and status != 1 group by clinic_id ,recipe_id  ORDER BY clinic_id desc,gmt_create desc")
    public abstract List<RecipeTherapy> findTherapyPageByMpiIds(@DAOParam("mpiIds") List<String> mpiIds
            , @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = "from RecipeTherapy where mpi_id in (:mpiIds) and status != 1 ", limit = 0)
    public abstract List<RecipeTherapy> findTherapyByMpiIds(@DAOParam("mpiIds") List<String> mpiIds);

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
     * 根据处方ids 获取诊疗信息
     *
     * @param recipeIds 处方ids
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where recipe_id in (:recipeIds)", limit = 0)
    public abstract List<RecipeTherapy> findTherapyByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);


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
    @DAOMethod(sql = "from RecipeTherapy where organ_id=:organId and mpi_id=:mpiId and clinic_id=:clinicId and status != 1 order by id desc")
    public abstract List<RecipeTherapy> findTherapyPageByMpiIdAndClinicId(@DAOParam("organId") int organId, @DAOParam("mpiId") String mpiId,
                                                                          @DAOParam("clinicId") int clinicId, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 根据患者id与复诊id 获取诊疗信息
     *
     * @param organId  机构id
     * @param mpiId    患者id
     * @param clinicId 复诊id
     * @return 诊疗信息
     */
    @DAOMethod(sql = "from RecipeTherapy where organ_id=:organId and mpi_id=:mpiId and clinic_id=:clinicId and status != 1")
    public abstract List<RecipeTherapy> findTherapyByMpiIdAndClinicId(@DAOParam("organId") int organId, @DAOParam("mpiId") String mpiId, @DAOParam("clinicId") int clinicId);

    /**
     * 根据复诊查询诊疗处方
     *
     * @param clinicId 复诊id
     * @return
     */
    @DAOMethod(sql = "from RecipeTherapy where clinic_id=:clinicId")
    public abstract List<RecipeTherapy> findTherapyByClinicId(@DAOParam("clinicId") Integer clinicId);

}