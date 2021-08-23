package recipe.client;

import com.ngari.recipe.entity.Recipe;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.traces.requ.RevisitTracesSortRequest;
import com.ngari.revisit.traces.resp.RevisitTracesSortResponse;
import com.ngari.revisit.traces.service.IRevisitTracesSortService;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
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
     * 通知复诊——处方追溯数据
     *
     * @param recipe
     */
    public void saveRevisitTracesList(Recipe recipe) {
        try {
            if (recipe == null) {
                return;
            }
            RevisitTracesSortRequest revisitTracesSortRequest = new RevisitTracesSortRequest();
            revisitTracesSortRequest.setBusId(recipe.getRecipeId().toString());
            revisitTracesSortRequest.setBusNumOrder(10);
            revisitTracesSortRequest.setBusOccurredTime(new Date());
            revisitTracesSortRequest.setBusType(1);
            revisitTracesSortRequest.setConsultId(recipe.getClinicId());
            revisitTracesSortRequest.setFrequency(0);
            revisitTracesSortRequest.setOrganId(recipe.getClinicOrgan());
            logger.info("RevisitClient saveRevisitTracesList request revisitTracesSortRequest:{}", JSONUtils.toString(revisitTracesSortRequest));
            RevisitTracesSortResponse revisitTracesSortResponse = revisitTracesSortService.saveOrUpdate(revisitTracesSortRequest);
            logger.info("RevisitClient saveRevisitTracesList response revisitTracesSortResponse:{}", JSONUtils.toString(revisitTracesSortResponse));
            //TODO  复诊的接口返回没有成功或失败 无法加标志 无法失败重试或批量处理失败数据
        } catch (Exception e) {
            logger.error("RevisitClient saveRevisitTracesList error recipeId:{},{}", recipe.getRecipeId(), e);
            e.printStackTrace();
        }
    }
}
