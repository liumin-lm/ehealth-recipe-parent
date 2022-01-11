package recipe.client;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.WriteDrugRecipeTO;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipe.model.WriteDrugRecipeDTO;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zgy
 * @date 2022/1/10 16:57
 */
@Service
public class WriteRecipeClient extends BaseClient {
    @Autowired
    private IVisitService iVisitService;


    /**
     * @param writeDrugRecipeReqTO 获取院内门诊请求入参
     * @return 院内门诊
     */
    public HisResponseTO<List<WriteDrugRecipeTO>> findWriteDrugRecipeByRevisitFromHis(WriteDrugRecipeReqTO writeDrugRecipeReqTO) {
        HisResponseTO<List<WriteDrugRecipeTO>> hisResponseTOList = new HisResponseTO<>();
        try {
            hisResponseTOList = iVisitService.findWriteDrugRecipeByRevisitFromHis(writeDrugRecipeReqTO);
        }catch (Exception e){
            logger.error("WriteRecipeDoctorAtop findWriteDrugRecipeByRevisitFromHis error={}", JSONUtils.toString(e));
        }
        return hisResponseTOList;
    }
}
