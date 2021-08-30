package recipe.client;

import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import com.ngari.revisit.traces.service.IRevisitTracesSortService;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 复诊相关服务
 *
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RevisitClient extends BaseClient {

    @Autowired
    private IRevisitExService revisitExService;

    @Autowired
    private IRevisitTracesSortService revisitTracesSortService;

    @Autowired
    private IRevisitService revisitService;

    /**
     * 根据挂号序号获取复诊信息
     *
     * @param registeredId 挂号序号
     * @return 复诊信息
     */
    public RevisitExDTO getByRegisterId(String registeredId) {
        logger.info("RevisitClient getByRegisterId param registeredId:{}", registeredId);
        RevisitExDTO consultExDTO = revisitExService.getByRegisterId(registeredId);
        logger.info("RevisitClient res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }

    public RevisitBean getRevisitByClinicId(Integer clinicId) {
        logger.info("RevisitClient getRevisitByClinicId param clinicId:{}", clinicId);
        RevisitBean revisitBean = revisitService.getById(clinicId);
        logger.info("RevisitClient getRevisitByClinicId param clinicId:{}", clinicId);
        return revisitBean;
    }

    /**
     * 根据复诊单号获取复诊信息
     *
     * @param clinicId 复诊单号
     * @return 复诊信息
     */
    public RevisitExDTO getByClinicId(Integer clinicId) {
        logger.info("RevisitClient getByClinicId param clinicId:{}", clinicId);
        RevisitExDTO consultExDTO = revisitExService.getByConsultId(clinicId);
        logger.info("RevisitClient getByClinicId res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }

    /**
     * 通知复诊——删除处方追溯数据
     *
     * @param recipeId
     */
    public void deleteByBusIdAndBusNumOrder(Integer recipeId) {
        try {
            if (recipeId == null) {
                return;
            }
            logger.info("RevisitClient deleteByBusIdAndBusNumOrder request recipeId:{}", recipeId);
            revisitTracesSortService.deleteByBusIdAndBusNumOrder(recipeId + "", 10);
            logger.info("RevisitClient deleteByBusIdAndBusNumOrder response recipeId:{}", recipeId);
            //TODO  复诊的接口返回没有成功或失败 无法加标志 无法失败重试或批量处理失败数据
        } catch (Exception e) {
            logger.error("RevisitClient deleteByBusIdAndBusNumOrder error recipeId:{},{}", recipeId, e);
            e.printStackTrace();
        }
    }

}
