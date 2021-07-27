package recipe.client;

import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RevisitClient extends BaseClient {

    @Autowired
    private IRevisitExService revisitExService;


    /**
     * 根据挂号序号获取复诊信息
     *
     * @param registeredId
     * @return
     */
    public RevisitExDTO getByRegisterId(String registeredId) {
        logger.info("RevisitClient getByRegisterId param registeredId:{}",registeredId);
        RevisitExDTO consultExDTO = revisitExService.getByRegisterId(registeredId);
        logger.info("RevisitClient res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }
}
