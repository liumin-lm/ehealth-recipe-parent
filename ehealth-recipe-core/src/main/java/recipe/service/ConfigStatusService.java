package recipe.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.ConfigStatusCheck;
import com.ngari.recipe.vo.ConfigStatusCheckVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IConfigStatusService;
import recipe.dao.ConfigStatusCheckDAO;

import java.util.List;

/**
 * 获取配置状态服务入口类
 *
 * @author fuzi
 */
@Service
public class ConfigStatusService implements IConfigStatusService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ConfigStatusCheckDAO configStatusCheckDAO;

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
}
