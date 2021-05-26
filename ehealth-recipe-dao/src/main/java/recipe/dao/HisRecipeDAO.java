package recipe.dao;

import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.dao.bean.HisRecipeListBean;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:23
 */
@RpcSupportDAO
public abstract class HisRecipeDAO extends HibernateSupportDelegateDAO<HisRecipe> {

    public HisRecipeDAO() {
        super();
        this.setEntityName(HisRecipe.class.getName());
        this.setKeyField("hisRecipeID");
    }

    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and mpiId=:mpiId and status=:status order by createDate desc")
    public abstract List<HisRecipe> findHisRecipes(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("mpiId") String mpiId, @DAOParam("status") int status, @DAOParam(pageStart = true) int start,
                                                   @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = " update HisRecipe set status=:status where clinicOrgan=:clinicOrgan and recipeCode=:recipeCode ")
    public abstract void updateHisRecieStatus(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode, @DAOParam("status") int status);

    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and recipeCode=:recipeCode")
    public abstract HisRecipe getHisRecipeByRecipeCodeAndClinicOrgan(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode);

    /**
     * 根据 机构编号 和 处方单号 批量查询数据
     *
     * @param clinicOrgan    机构编号
     * @param recipeCodeList 处方单号
     * @return
     */
    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and recipeCode in (:recipeCodeList)")
    public abstract List<HisRecipe> findHisRecipeByRecipeCodeAndClinicOrgan(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCodeList") List<String> recipeCodeList);

    /**
     * 查询
     * @param
     * @return
     */
    public List<Integer> findHisRecipeByPayFlag(@DAOParam("recipeCodeList") List<String> recipeCodeList,
                                                @DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam("mpiid") String mpiid) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r.hisRecipeID from cdr_his_recipe r where  not exists " +
                        "(select o.recipeCode,o.clinicOrgan,o.mpiid from cdr_recipe o where   r.recipeCode=o.recipeCode and r.clinicOrgan=o.clinicOrgan and r.mpiid=o.mpiid and o.payFlag=1) ");
                hql.append(" and r.recipeCode in (:recipeCodeList) and r.clinicOrgan=:clinicOrgan and r.mpiid=:mpiid ");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("recipeCodeList", recipeCodeList);
                q.setParameter("clinicOrgan",clinicOrgan);
                q.setParameter("mpiid", mpiid);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
    @DAOMethod(sql = "from Recipe where recipeCode in (:recipeCodeList) and clinicOrgan=:clinicOrgan and mpiid=:mpiid and payFlag!=1")
    public abstract List<Recipe> findByRecipeCodeAndClinicOrganAndMpiid(@DAOParam("recipeCodeList") List<String> recipeCodeList,
                                                                        @DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam("mpiid") String mpiid);


    @DAOMethod(sql = " From HisRecipe where mpiId=:mpiId and clinicOrgan=:clinicOrgan and recipeCode=:recipeCode")
    public abstract HisRecipe getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(@DAOParam("mpiId") String mpiId, @DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode);

    @DAOMethod(sql = " From HisRecipe where mpiId=:mpiId and clinicOrgan=:clinicOrgan and recipeCode=:recipeCode")
    public abstract HisRecipe getHisRecipeRecipeCodeAndClinicOrgan( @DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCode") String recipeCode);

    /**
     * 根据处方id批量删除
     *
     * @param hisRecipeIds
     */
    @DAOMethod(sql = "delete from HisRecipe where hisRecipeId in (:hisRecipeIds)")
    public abstract void deleteByHisRecipeIds(@DAOParam("hisRecipeIds") List<Integer> hisRecipeIds);

    @DAOMethod(sql = " From HisRecipe where hisRecipeId in (:hisRecipeIds)")
    public abstract List<HisRecipe> findHisRecipeByhisRecipeIds(@DAOParam("hisRecipeIds") List<Integer> hisRecipeIds);

    /**
     * 批量查询已处理his处方
     * @param allMpiIds
     * @param start
     * @param limit
     */
    public List<HisRecipeListBean> findHisRecipeListByMPIIds(List<String> allMpiIds, Integer start, Integer limit){
        HibernateStatelessResultAction<List<HisRecipeListBean>> action = new AbstractHibernateStatelessResultAction<List<HisRecipeListBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select new recipe.dao.bean.HisRecipeListBean(h.diseaseName,h.hisRecipeID,h.registeredId, h.mpiId, h.recipeCode, h.clinicOrgan, h.departCode, h.departName, h.createDate, h.doctorCode, h.doctorName, h.chronicDiseaseCode, h.chronicDiseaseName, h.patientName, h.memo,h.recipeType,r.fromflag,r.recipeId, r.orderCode, r.status)  FROM HisRecipe h,Recipe r where h.status = 2 and h.clinicOrgan=r.clinicOrgan and h.recipeCode=r.recipeCode and h.mpiId in (:allMpiIds) ORDER BY h.createDate DESC");
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("allMpiIds", allMpiIds);
                q.setMaxResults(limit);
                q.setFirstResult(start);

                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 批量查询进行中的his处方
     * @param allMpiIds
     * @param start
     * @param limit
     */
    public List<HisRecipeListBean> findOngoingHisRecipeListByMPIIds(List<String> allMpiIds, Integer start, Integer limit){
        HibernateStatelessResultAction<List<HisRecipeListBean>> action = new AbstractHibernateStatelessResultAction<List<HisRecipeListBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select new recipe.dao.bean.HisRecipeListBean(h.hisRecipeID,h.registeredId, h.mpiId, h.recipeCode, h.clinicOrgan, h.departCode, h.departName, h.createDate, h.doctorCode, h.doctorName, h.chronicDiseaseCode, h.chronicDiseaseName, h.patientName, h.memo,h.recipeType,r.fromflag,r.recipeId, r.orderCode, r.status)  FROM HisRecipe h,Recipe r where h.status = 1 and h.clinicOrgan=r.clinicOrgan and h.recipeCode=r.recipeCode and h.mpiId in (:allMpiIds) and r.orderCode is not null ORDER BY h.createDate DESC");
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("allMpiIds", allMpiIds);
                q.setMaxResults(limit);
                q.setFirstResult(start);

                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
