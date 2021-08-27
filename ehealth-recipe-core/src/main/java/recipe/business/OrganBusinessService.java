package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.client.OrganClient;
import recipe.core.api.IOrganBusinessService;

import java.util.ArrayList;
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
    public List<GiveModeButtonBean> getOrganGiveModeConfig(Integer organId){
        List<GiveModeButtonBean> result = new ArrayList<>();
        List<ScratchableBean> scratchableBeans = configurationClient.getOrganGiveMode(organId);
        scratchableBeans.stream().filter(scratchableBean->!"listItem".equals(scratchableBean.getBoxLink())).forEach(scratchableBean->{
            GiveModeButtonBean giveModeButtonBean = new GiveModeButtonBean();
            giveModeButtonBean.setShowButtonKey(scratchableBean.getBoxLink());
            giveModeButtonBean.setShowButtonName(scratchableBean.getBoxTxt());
            result.add(giveModeButtonBean);
        });
        logger.info("OrganBusinessService getOrganGiveModeConfig result:{}.", JSON.toJSONString(result));
        return result;
    }
}
