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

    @DAOMethod(sql = "FROM ClinicCart WHERE organId = :organId AND userId = :userId AND deleteFlag = 0", limit = 0)
    public abstract List<ClinicCart> findClinicCartsByOrganIdAndUserId(@DAOParam("organId") Integer organId,
                                                                       @DAOParam("userId") String userId);

    @DAOMethod(sql = "UPDATE ClinicCart SET deleteFlag = :deleteFlag WHERE id IN (:ids)")
    public abstract void deleteClinicCartByIds(@DAOParam("ids") List<Integer> ids,
                                               @DAOParam("deleteFlag") Integer deleteFlag);
}

