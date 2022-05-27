//package recipe.dao;
//
//import com.ngari.recipe.entity.DrugSaleStrategy;
//import com.ngari.recipe.entity.DrugsEnterprise;
//import ctd.persistence.annotation.DAOMethod;
//import ctd.persistence.annotation.DAOParam;
//import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
//import ctd.util.annotation.RpcSupportDAO;
//
//import java.util.List;
//
///**
// * 药品销售策略
// */
//@RpcSupportDAO
//public abstract class DrugSaleStrategyDAO extends HibernateSupportDelegateDAO<DrugSaleStrategy> {
//
//    public DrugSaleStrategyDAO() {
//        super();
//        this.setEntityName(DrugSaleStrategy.class.getName());
//        this.setKeyField("id");
//    }
//
//    @DAOMethod(sql = "from DrugSaleStrategy where status=1 and drugId in(:drugIds)")
//    public abstract List<DrugSaleStrategy> findByDrugIds(@DAOParam("drugIds") List<Integer> drugIds);
//
//    @DAOMethod(sql = "from DrugSaleStrategy where status=1 and id=:id")
//    public abstract DrugSaleStrategy getDrugSaleStrategyById(@DAOParam("id") Integer id);
//
//}
