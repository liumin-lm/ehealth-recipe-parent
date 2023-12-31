package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.ConfigStatusCheck;
import com.ngari.recipe.vo.ConfigStatusCheckVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.core.api.IConfigStatusBusinessService;
import recipe.dao.ConfigStatusCheckDAO;

import java.util.List;

/**
 * 获取配置状态服务入口类
 *
 * @author fuzi
 */
@Service
public class ConfigStatusBusinessService implements IConfigStatusBusinessService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ConfigStatusCheckDAO configStatusCheckDAO;

    @Autowired
    private IConfigurationClient configurationClient;

    @Override
    public List<ConfigStatusCheckVO> getConfigStatus(Integer location) {
        List<ConfigStatusCheck> list = configStatusCheckDAO.findByLocation(location);
        return ObjectCopyUtils.convert(list, ConfigStatusCheckVO.class);
    }

    @Override
    public List<ConfigStatusCheckVO> findByLocationAndSource(Integer location, Integer source) {
        List<ConfigStatusCheck> list = configStatusCheckDAO.findByLocationAndSource(location, source);
        return ObjectCopyUtils.convert(list, ConfigStatusCheckVO.class);
    }

    @Override
    public Boolean getOpenRecipeHideDrugManufacturer(Integer organId,String openRecipeHideDrugManufacturer) {
        return configurationClient.getValueBooleanCatch(organId,openRecipeHideDrugManufacturer,false);
    }
}
