package recipe.business;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OrganClient;
import recipe.core.api.IOrganBusinessService;

import java.util.List;

@Service
public class OrganBusinessService extends BaseService implements IOrganBusinessService {

    @Autowired
    private OrganClient organClient;

    @Override
    public List<Integer> getOrganForWeb() {
        List<Integer> organIds = organClient.findOrganIdsByCurrentClient();
        if (CollectionUtils.isNotEmpty(organIds)) {
            return organIds;
        }
        return organClient.findAllOrganIds();
    }
}
