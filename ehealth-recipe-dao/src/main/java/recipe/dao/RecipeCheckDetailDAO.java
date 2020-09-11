//package recipe.dao;
//
//import com.ngari.recipe.entity.RecipeCheckDetail;
//import ctd.persistence.annotation.DAOMethod;
//import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
//import ctd.util.annotation.RpcSupportDAO;
//
//import java.util.List;
//
///**
// * @author zhongzx
// * 审方详情dao
// */
//@RpcSupportDAO
//public abstract class RecipeCheckDetailDAO extends HibernateSupportDelegateDAO<RecipeCheckDetail> {
//
//    public RecipeCheckDetailDAO() {
//        super();
//        this.setEntityName(RecipeCheckDetail.class.getName());
//        this.setKeyField("checkDetailId");
//    }
//
//    /**
//     * 根据id获取
//     * @param checkId
//     * @return
//     */
//    @DAOMethod(limit = 0)
//    public abstract List<RecipeCheckDetail> findByCheckId(Integer checkId);
//
//}
