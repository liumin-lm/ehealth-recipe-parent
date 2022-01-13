package recipe.dao;

import com.ngari.recipe.dto.RecipeTherapyOpBean;
import com.ngari.recipe.dto.RecipeTherapyOpQueryDTO;
import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import recipe.dao.comment.ExtendDao;

import java.text.SimpleDateFormat;
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
    @DAOMethod(sql = "from RecipeTherapy where organId=:organId and doctorId=:doctorId order by clinic_id desc,id desc")
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

    /**
     * 运营平台展示诊疗处方列表
     *
     * @param recipeTherapyOpQueryVO
     * @return
     *
     */
    public QueryResult<RecipeTherapyOpBean> findTherapyByInfo(RecipeTherapyOpQueryDTO recipeTherapyOpQueryVO){
        final StringBuilder sbHql = this.generateRecipeTherapyHQLforStatistics(recipeTherapyOpQueryVO);
        final StringBuilder sbHqlCount = this.generateRecipeTherapyHQLforStatisticsCount(recipeTherapyOpQueryVO);
        logger.info("RecipeTherapyDAO findTherapyByInfo sbHql:{}", sbHql.toString());
        HibernateStatelessResultAction<QueryResult<RecipeTherapyOpBean>> action = new AbstractHibernateStatelessResultAction<QueryResult<RecipeTherapyOpBean>>(){
            @Override
            public void execute(StatelessSession ss) throws Exception {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                // 查询总记录数
                SQLQuery sqlQuery = ss.createSQLQuery(sbHqlCount.toString());
                sqlQuery.setParameter("startTime", sdf.format(recipeTherapyOpQueryVO.getBDate()));
                sqlQuery.setParameter("endTime", sdf.format(recipeTherapyOpQueryVO.getEDate()));
                Long total = Long.valueOf(String.valueOf((sqlQuery.uniqueResult())));
                // 查询结果
                Query query = ss.createSQLQuery(sbHql.append(" order by CreateDate DESC").toString()).addEntity(RecipeTherapyOpBean.class);
                query.setParameter("startTime", sdf.format(recipeTherapyOpQueryVO.getBDate()));
                query.setParameter("endTime", sdf.format(recipeTherapyOpQueryVO.getEDate()));
                query.setFirstResult(recipeTherapyOpQueryVO.getStart());
                query.setMaxResults(recipeTherapyOpQueryVO.getLimit());
                List<RecipeTherapyOpBean> recipeTherapyList = query.list();
                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), recipeTherapyList));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    protected StringBuilder generateRecipeTherapyHQLforStatistics(RecipeTherapyOpQueryDTO recipeTherapyOpQueryVO){
        StringBuilder hql = new StringBuilder("select r.RecipeID,r.RecipeCode,r.patientName,r.mpiId,r.doctorName,r.appoint_depart_name," +
                "r.organName,cr.status,cr.gmt_create from cdr_recipe r ");
        hql.append(" INNER JOIN cdr_recipe_therapy cr on r.RecipeID = cr.recipe_id ");
        hql.append(" where r.recipeSourceType=3 and cr.status!=1 ");
        return generateRecipeTherapyHQLforStatisticsV1(hql,recipeTherapyOpQueryVO);
    }

    protected StringBuilder generateRecipeTherapyHQLforStatisticsCount(RecipeTherapyOpQueryDTO recipeTherapyOpQueryVO){
        StringBuilder hql = new StringBuilder("select count(1) from cdr_recipe r ");
        hql.append(" INNER JOIN cdr_recipe_therapy cr on r.RecipeID = cr.recipe_id ");
        hql.append(" where r.recipeSourceType=3 and cr.status!=1 ");
        return generateRecipeTherapyHQLforStatisticsV1(hql,recipeTherapyOpQueryVO);
    }

    private StringBuilder generateRecipeTherapyHQLforStatisticsV1(StringBuilder hql, RecipeTherapyOpQueryDTO recipeTherapyOpQueryVO) {
        //默认查询所有
        if (recipeTherapyOpQueryVO.getOrganId() != null) {
            hql.append(" and r.clinicOrgan =").append(recipeTherapyOpQueryVO.getOrganId());
        }

        hql.append(" and cr.gmt_create BETWEEN :startTime" + " and :endTime ");

        if(recipeTherapyOpQueryVO.getStatus() != null ){
            hql.append(" and cr.status =").append(recipeTherapyOpQueryVO.getStatus());
        }
        if(recipeTherapyOpQueryVO.getMpiId() != null ){
            hql.append(" and r.mpiId =").append(recipeTherapyOpQueryVO.getMpiId());
        }
        if(recipeTherapyOpQueryVO.getDoctorInfoSearch() != null ){
            hql.append("and r.doctorName like ").append(recipeTherapyOpQueryVO.getDoctorInfoSearch()).append("%");
            hql.append("or r.appoint_depart_name like ").append(recipeTherapyOpQueryVO.getDoctorInfoSearch()).append("%");
        }
        return hql;
    }


}