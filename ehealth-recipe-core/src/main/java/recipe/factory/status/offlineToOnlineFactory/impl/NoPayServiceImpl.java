package recipe.factory.status.offlineToOnlineFactory.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import com.ngari.recipe.vo.FindHisRecipeDetailVO;
import com.ngari.recipe.vo.FindHisRecipeListVO;
import ctd.persistence.exception.DAOException;
import ctd.util.event.GlobalEventExecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import recipe.factory.status.constant.OfflineToOnlineEnum;
import recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService;
import recipe.service.OfflineToOnlineService;
import recipe.service.RecipeHisService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上待缴费处方实现类
 */
@Service
public class NoPayServiceImpl implements IOfflineToOnlineService {

    @Autowired
    OfflineToOnlineService offlineToOnlineService;

    @Autowired
    RecipeHisService recipeHisService;

    @Autowired
    @Qualifier("basic.patientService")
    PatientService patientService;

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Override
    public List<HisRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        //转换成前端所需对象
        List<HisRecipeVO> noPayHisRecipeVO=offlineToOnlineService.covertToHisRecipeObjectForNoPay(hisRecipeInfos, patientDTO);
        //异步删除his未返回数据数据
        GlobalEventExecFactory.instance().getExecutor().execute(()->{
            offlineToOnlineService.deleteOnlyExistnoHisRecipeVOs(noPayHisRecipeVO,request);
        });
        return noPayHisRecipeVO;
    }

    @Override
    public Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request) {
        // 1获取his数据
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos=new HisResponseTO<List<QueryHisRecipResTO>>();
        //hisRecipeInfos=recipeHisService.queryHisRecipeInfo(request.getOrganId(),patientDTO,180,1,request.getCardId(),null);
        try {
            // 2更新数据校验
            offlineToOnlineService.hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        List<HisRecipe> recipes=new ArrayList<>();
        try {
            // 3保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            recipes=offlineToOnlineService.saveHisRecipeInfo(hisRecipeInfos, patientDTO, 1);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }

        // 4.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        Integer recipeId=offlineToOnlineService.saveRecipeInfo(recipes.get(0).getHisRecipeID());
        // 5.通过cdrHisRecipeId返回数据详情
        return offlineToOnlineService.getHisRecipeDetailByHisRecipeIdAndRecipeId(request.getHisRecipeId(),recipeId);
    }

    @Override
    public Integer getPayMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType();
    }


}
