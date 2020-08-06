package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.drug.model.DrugMakingMethodBean;
import com.ngari.recipe.drug.service.IDrugExtService;
import com.ngari.recipe.entity.DecoctionWay;
import com.ngari.recipe.entity.DrugMakingMethod;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.DrugDecoctionWayDao;
import recipe.dao.DrugMakingMethodDao;

import java.util.List;

/**
 * @company: ngarihealth
 * @author: gaomw
 * @date:2020/8/5.
 */
@RpcBean("drugExtService")
public class DrugExtService implements IDrugExtService {
    private static final Logger logger = LoggerFactory.getLogger(DrugExtService.class);
    @Autowired
    DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    DrugDecoctionWayDao drugDecoctionWayDao;

    @RpcService
    @Override
    public DrugMakingMethodBean get(Object id) {
        return null;
    }

    /**
     * 获取机构下所有药品制法
     *
     * @param organId 机构编码
     * @return List<DrugMakingMethod> 药品信息
     */
    @RpcService
    @Override
    public List<DrugMakingMethodBean> findAllDrugMakingMethodByOrganId(Integer organId) {
        List<DrugMakingMethodBean> allUsePathways = drugMakingMethodDao.findAllDrugMakingMethodByOrganId(organId);
        if (allUsePathways.isEmpty()) {
            return Lists.newArrayList();
        }
        return allUsePathways;
    }

    /**
     * 药品制法法存储
     *
     * @param drugMakingMethodBean 制法信息
     * @return
     */
    @RpcService
    @Override
    public Integer saveDrugMakingMethod(DrugMakingMethodBean drugMakingMethodBean) {
        DrugMakingMethod drugMakingMethod = ObjectCopyUtils.convert(drugMakingMethodBean, DrugMakingMethod.class);
        drugMakingMethod = drugMakingMethodDao.save(drugMakingMethod);
        return drugMakingMethod.getMethodId();
    }

    /**
     * 获取机构下所有药品煎法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    @Override
    public List<DecoctionWayBean> findAllDecoctionWayByOrganId(Integer organId) {
        List<DecoctionWayBean> allUsePathways = drugDecoctionWayDao.findAllDecoctionWayByOrganId(organId);
        if (allUsePathways.isEmpty()) {
            return Lists.newArrayList();
        }
        return allUsePathways;
    }

    /**
     * 药品制法存储
     *
     * @param decoctionWayBean 煎法信息
     * @return
     */
    @RpcService
    @Override
    public Integer saveDrugDecoctionWay(DecoctionWayBean decoctionWayBean) {
        DecoctionWay decoctionWay = ObjectCopyUtils.convert(decoctionWayBean, DecoctionWay.class);
        decoctionWay = drugDecoctionWayDao.save(decoctionWay);
        return decoctionWay.getDecoctionId();
    }
}
