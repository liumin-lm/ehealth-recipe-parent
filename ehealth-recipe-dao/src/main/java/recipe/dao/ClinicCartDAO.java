package recipe.dao;

import com.ngari.recipe.entity.ClinicCart;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@RpcSupportDAO
public abstract class ClinicCartDAO extends HibernateSupportDelegateDAO<ClinicCart> {

    public ClinicCartDAO() {
        super();
        this.setEntityName(ClinicCart.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "from ClinicCart where organId = :organId and userId = :userId and deleteFlag = 0")
    public abstract List<ClinicCart> findClinicCartsByOrganIdAndUserId(@DAOParam("organId") Integer organId,
                                                                       @DAOParam("userId") String userId);

    @DAOMethod(sql = "UPDATE ClinicCart SET deleteFlag = :deleteFlag where id = :id")
    public abstract void updateDeleteFlagById(@DAOParam("id") Integer id,
                                              @DAOParam("deleteFlag") Integer deleteFlag);
}

