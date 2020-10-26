package recipe.service;

import com.ngari.recipe.entity.ConfigStatusCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.ConfigStatusCheckDAO;

import java.util.List;

/**
 * 获取配置状态服务入口类
 *
 * @author fuzi
 */
@Service
public class ConfigStatusService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ConfigStatusCheckDAO configStatusCheckDAO;

    public List<ConfigStatusCheck> getConfigStatus(int location) {
        return configStatusCheckDAO.findByLocation(location);
    }
}
