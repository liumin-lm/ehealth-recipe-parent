package recipe.client;

import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.service.HealthCardService;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 健康卡服务
 * @author yinsheng
 * @date 2021\7\29 0029 19:39
 */
@Service
public class HealthCardClient extends BaseClient{

    @Autowired
    private HealthCardService healthCardService;

    /**
     * 获取健康卡
     * @param mpiId 患者唯一号
     * @return 健康卡列表
     */
    public Set<String> findHealthCard(String mpiId){
        logger.info("HealthCardClient findHealthCard mpiId:{}.", mpiId);
        List<HealthCardDTO> healthCards = healthCardService.findByMpiId(mpiId);
        Set<String> result = healthCards.stream().map(HealthCardDTO::getCardId).collect(Collectors.toSet());
        logger.info("HealthCardClient findHealthCard result:{}.", JSONUtils.toString(result));
        return result;
    }
}
