package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.OrganDTO;
import com.ngari.recipe.dto.ServiceLogDTO;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.client.InfraClient;
import recipe.client.OperationClient;
import recipe.client.OrganClient;
import recipe.core.api.IOrganBusinessService;
import recipe.dao.RecipeParameterDao;
import recipe.enumerate.status.SettleAmountStateEnum;
import recipe.manager.OrderManager;
import recipe.util.ObjectCopyUtils;
import recipe.vo.second.OrganVO;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrganBusinessService extends BaseService implements IOrganBusinessService {
    @Autowired
    private OrganClient organClient;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private InfraClient infraClient;

    @Override
    public List<Integer> getOrganForWeb() {
        List<Integer> organIds = organClient.findOrganIdsByCurrentClient();
        if (CollectionUtils.isNotEmpty(organIds)) {
            return organIds;
        }
        return organClient.findAllOrganIds();
    }

    @Override
    public List<GiveModeButtonBean> getOrganGiveModeConfig(Integer organId){
        List<GiveModeButtonBean> result = new ArrayList<>();
        List<ScratchableBean> scratchableBeans = configurationClient.getOrganGiveMode(organId);
        scratchableBeans.stream().filter(scratchableBean -> !"listItem".equals(scratchableBean.getBoxLink())).forEach(scratchableBean -> {
            GiveModeButtonBean giveModeButtonBean = new GiveModeButtonBean();
            giveModeButtonBean.setShowButtonKey(scratchableBean.getBoxLink());
            giveModeButtonBean.setShowButtonName(scratchableBean.getBoxTxt());
            result.add(giveModeButtonBean);
        });
        logger.info("OrganBusinessService getOrganGiveModeConfig result:{}.", JSON.toJSONString(result));
        return result;
    }


    @Override
    public boolean giveModeValidate(Integer orderId) {
        RecipeOrder recipeOrder = orderManager.getRecipeOrder(null, orderId);
        if (null == recipeOrder) {
            return false;
        }
        return giveModeValidate(recipeOrder.getOrganId(), recipeOrder.getGiveModeKey());
    }

    @Override
    public boolean giveModeValidate(Integer organId, String giveModeKey) {
        List<String> recipeTypes = configurationClient.getValueListCatch(organId, "patientRecipeUploadHis", null);
        if (CollectionUtils.isEmpty(recipeTypes)) {
            return false;
        }
        return recipeTypes.contains(giveModeKey);
    }

    @Override
    public String getRecipeParameterValue(String key) {
        return recipeParameterDao.getByName(key);
    }

    @Override
    public OrganVO getOrganVOByOrganId(Integer organId) {
        OrganDTO organDTO = organClient.organDTO(organId);
        return ObjectCopyUtils.convert(organDTO, OrganVO.class);
    }

    @Override
    public Boolean isAuthorisedOrgan(Integer organId) {
        return operationClient.isAuthorisedOrgan(organId);
    }


    @Override
    public List<GiveModeButtonDTO> organGiveMode(Integer organId) {
        return operationClient.getOrganGiveModeMap(organId);
    }

    @Override
    public Integer getOrderPayFlag(Integer orderId) {
        RecipeOrder recipeOrder = orderManager.getRecipeOrderById(orderId);
        if (null == recipeOrder) {
            throw new DAOException("订单不存在");
        }
        if (null == recipeOrder.getSettleAmountState()) {
            return SettleAmountStateEnum.NONE_SETTLE.getType();
        }
        if (SettleAmountStateEnum.NO_NEED.getType().equals(recipeOrder.getSettleAmountState())) {
            return SettleAmountStateEnum.SETTLE_SUCCESS.getType();
        }
        return recipeOrder.getSettleAmountState();
    }

    @Override
    public void serviceTimeLog(ServiceLogDTO convert) {
        infraClient.serviceTimeLog(convert);
    }
}
