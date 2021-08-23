package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.base.scratchable.model.ScratchableBean;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.client.OrganClient;
import recipe.core.api.IOrganBusinessService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrganBusinessService extends BaseService implements IOrganBusinessService {

    @Autowired
    private OrganClient organClient;
    @Autowired
    private IConfigurationClient configurationClient;

    @Override
    public List<Integer> getOrganForWeb() {
        List<Integer> organIds = organClient.findOrganIdsByCurrentClient();
        if (CollectionUtils.isNotEmpty(organIds)) {
            return organIds;
        }
        return organClient.findAllOrganIds();
    }

    @Override
    public Set<String> getOrganGiveModeConfig(Integer organId){
        Set<String> result = new HashSet<>();
        List<ScratchableBean> scratchableBeans = configurationClient.getOrganGiveMode(organId);
        scratchableBeans.forEach(giveMode->
            result.add(giveMode.getBoxLink())
        );
        logger.info("OrganBusinessService getOrganGiveModeConfig result:{}.", JSON.toJSONString(result));
        return result;
    }
}
