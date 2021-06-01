//package recipe.dao;
//
//import com.ngari.recipe.entity.Recipe;
//import com.ngari.recipe.entity.RecipeCheck;
//import com.ngari.recipe.entity.RecipeCheckDetail;
//import ctd.persistence.DAOFactory;
//import ctd.persistence.annotation.DAOMethod;
//import ctd.persistence.annotation.DAOParam;
//import ctd.persistence.exception.DAOException;
//import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
//import ctd.persistence.support.hibernate.HibernateSupportWriteDAO;
//import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
//import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
//import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
//import ctd.util.annotation.RpcSupportDAO;
//import org.apache.commons.lang3.StringUtils;
//import org.hibernate.Query;
//import org.hibernate.StatelessSession;
//import recipe.constant.ErrorCode;
//
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
///**
// * @author zhongzx
// * 审方dao
// */
//@RpcSupportDAO
//public abstract class RecipeCheckDAO extends HibernateSupportDelegateDAO<RecipeCheck> {
//
//    /**
//     * 2-患者姓名
//     */
//    private static final int SEARCH_FLAG_PN = 2;
//
//    /**
//     * 3-病历号
//     */
//    private static final int SEARCH_FLAG_BL = 3;
//
//    public RecipeCheckDAO() {
//        super(new HibernateSupportWriteDAO<RecipeCheck>(){
//            @Override
//            protected void beforeSave(RecipeCheck recipeCheck) throws DAOException {
//                recipeCheck.setUpdateTime(new Date());
//            }
//            @Override
//            protected void beforeUpdate(RecipeCheck recipeCheck) throws DAOException {
//                recipeCheck.setUpdateTime(new Date());
//            }
//        });
//        this.setEntityName(RecipeCheck.class.getName());
//        this.setKeyField("checkId");
//    }
//
//    /**
//     * 保存审方记录和详情记录
//     * zhongzx
//     *
//     * @param recipeCheck
//     * @param recipeCheckDetails
//     */
//    public void saveRecipeCheckAndDetail(RecipeCheck recipeCheck, List<RecipeCheckDetail> recipeCheckDetails) {
//        recipeCheck = save(recipeCheck);
//        RecipeCheckDetailDAO
//                recipeCheckDetailDAO = DAOFactory.getDAO(RecipeCheckDetailDAO.class);
//        if (null != recipeCheckDetails) {
//            for (RecipeCheckDetail recipeCheckDetail : recipeCheckDetails) {
//                recipeCheckDetail.setCheckId(recipeCheck.getCheckId());
//                recipeCheckDetailDAO.save(recipeCheckDetail);
//            }
//        }
//    }
//
//    /**
//     * 根据处方Id查询不通过审核记录
//     *
//     * @param recipeId
//     * @return
//     */
//    @DAOMethod(sql = "from RecipeCheck where recipeId=:recipeId and checkStatus=0")
//    public abstract RecipeCheck getByRecipeIdAndCheckStatus(@DAOParam("recipeId") Integer recipeId);
//
//    /**
//     * 药师搜索方法 开方医生 审方医生 患者姓名 患者patientId
//     *
//     * @param organs
//     * @param searchString
//     * @param searchFlag   0-开方医生 1-审方医生 2-患者姓名 3-病历号
//     * @param start
//     * @param limit
//     * @return
//     * @author zhongzx
//     */
//    public List<Recipe> searchRecipe(final Set<Integer> organs,
//                                     final Integer searchFlag, final String searchString,
//                                     final Integer start, final Integer limit) {
//        HibernateStatelessResultAction<List<Recipe>> action =
//            new AbstractHibernateStatelessResultAction<List<Recipe>>() {
//                @Override
//                public void execute(StatelessSession ss)
//                    throws Exception {
//                    StringBuilder hql = new StringBuilder();
//                    if (1 == searchFlag) {
//                        //按照药师搜索
//                        hql.append("select r from Recipe r , RecipeCheck w where r.recipeId = w.recipeId and w.checkerName like:searchString");
//                        hql.append(" and r.clinicOrgan in (:organs) order by w.checkDate desc");
//                    } else {
//                        hql.append("select distinct r from Recipe r");
//                        if (0 == searchFlag) {
//                            hql.append(" where r.doctorName like:searchString ");
//                        }
//                        else if (SEARCH_FLAG_PN == searchFlag) {
//                            hql.append(" where r.patientName like:searchString ");
//                        }
//                        else if (SEARCH_FLAG_BL == searchFlag) {
//                            hql.append(" where r.patientID like:searchString ");
//                        }
//                        else {
//                            throw new DAOException(ErrorCode.SERVICE_ERROR,
//                                    "searchFlag is invalid");
//                        }
//                        hql.append("and (r.checkDateYs is not null or r.status = 8) " +
//                                "and r.clinicOrgan in (:organs) order by r.signDate desc");
//                    }
//
//                    Query q = ss.createQuery(hql.toString());
//                    q.setParameter("searchString", "%" + searchString + "%");
//                    q.setParameterList("organs", organs);
//                    if (null != start && null != limit) {
//                        q.setFirstResult(start);
//                        q.setMaxResults(limit);
//                    }
//                    setResult(q.list());
//                }
//            };
//        HibernateSessionTemplate.instance().execute(action);
//        return action.getResult();
//    }
//
//    /**
//     * 根据处方id获取处方审核记录
//     * @param recipeId
//     * @return
//     */
//    @DAOMethod(sql = "from RecipeCheck where recipeId=:recipeId order by checkDate desc")
//    public abstract List<RecipeCheck> findByRecipeId(@DAOParam("recipeId") Integer recipeId);
//
//    @DAOMethod(sql = "from RecipeCheck ", limit = 0)
//    public abstract List<RecipeCheck> findAllRecipeCheck();
//
//    @DAOMethod(sql = "from RecipeCheck where checker is null AND checkStatus=0 AND grabOrderStatus=1 AND localLimitDate<now()")
//    public abstract List<RecipeCheck> findOverTimeRecipeCheck();
//
//    @DAOMethod(sql = "from RecipeCheck where recipeId=:recipeId")
//    public abstract RecipeCheck getByRecipeId(@DAOParam("recipeId") Integer recipeId);
//    /**
//     * 根据处方Id查询最新的一条审核记录
//     *
//     * @param recipeId
//     * @return
//     */
//    @DAOMethod(sql = "from RecipeCheck c where c.checkId = (select max(c.checkId) from c where c.recipeId=:recipeId)")
//    public abstract RecipeCheck getNowCheckResultByRecipeId(@DAOParam("recipeId") Integer recipeId);
//
//    /**
//     * 更新处方自定义字段
//     *
//     * @param recipeId
//     * @param changeAttr
//     * @return
//     */
//    public Boolean updateRecipeExInfoByRecipeId(final int recipeId, final Map<String, ?> changeAttr) {
//        if (null == changeAttr || changeAttr.isEmpty()) {
//            return true;
//        }
//
//        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
//            @Override
//            public void execute(StatelessSession ss) throws Exception {
//                StringBuilder hql = new StringBuilder("update RecipeCheck set ");
//                StringBuilder keyHql = new StringBuilder();
//                for (String key : changeAttr.keySet()) {
//                    keyHql.append("," + key + "=:" + key);
//                }
//                hql.append(keyHql.toString().substring(1)).append(" where recipeId=:recipeId");
//                Query q = ss.createQuery(hql.toString());
//
//                q.setParameter("recipeId", recipeId);
//                for (String key : changeAttr.keySet()) {
//                    q.setParameter(key, changeAttr.get(key));
//                }
//
//                int flag = q.executeUpdate();
//                setResult(flag == 1);
//            }
//        };
//        HibernateSessionTemplate.instance().execute(action);
//        return action.getResult();
//    }
//}
