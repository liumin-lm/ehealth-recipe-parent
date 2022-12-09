package recipe.atop.open;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.MedicationInfoResTO;
import com.ngari.platform.recipe.mode.ListOrganDrugReq;
import com.ngari.platform.recipe.mode.ListOrganDrugRes;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.RecipeRulesDrugCorrelation;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import recipe.api.open.IDrugAtopService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IDrugBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.DrugBookVo;
import recipe.vo.second.RecipeRulesDrugCorrelationVO;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description： 药品 open atop
 * @author： whf
 * @date： 2021-08-26 9:45
 */
@RpcBean("drugOpenAtop")
public class DrugOpenAtop extends BaseAtop implements IDrugAtopService {

    @Resource
    private IDrugBusinessService drugBusinessService;

    @Override
    public DrugBookVo getDrugBook(Integer organId, String organDrugCode) {
        logger.info("DrugOpenAtop getDrugBook organId={} organDrugCode={}", organId, organDrugCode);
        validateAtop(organId, organDrugCode);
        try {
            Dispensatory dispensatory = drugBusinessService.getDrugBook(organId, organDrugCode);
            DrugBookVo drugBookVo = ObjectCopyUtils.convert(dispensatory, DrugBookVo.class);
            logger.info("DrugOpenAtop getDrugBook result = {}", drugBookVo);
            return drugBookVo;
        } catch (DAOException e1) {
            logger.error("DrugOpenAtop getDrugBook error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugOpenAtop getDrugBook error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public List<RecipeRulesDrugCorrelationVO> getListDrugRules(List<Integer> list, Integer ruleId) {
        List<RecipeRulesDrugCorrelation> result = drugBusinessService.getListDrugRules(list, ruleId);
        return CollectionUtils.isEmpty(result) ? new ArrayList<>() : ObjectCopyUtils.convert(result, RecipeRulesDrugCorrelationVO.class);
    }

    @Override
    public List<ListOrganDrugRes> listOrganDrug(ListOrganDrugReq listOrganDrugReq) {
        List<OrganDrugList> listOrganDrugRes = drugBusinessService.listOrganDrug(listOrganDrugReq);
        return ObjectCopyUtils.convert(listOrganDrugRes, ListOrganDrugRes.class);
    }

    @Override
    public List<RecipeRulesDrugCorrelationVO> findRulesByDrugIdAndRuleId(Integer drugId, Integer ruleId) {
        List<RecipeRulesDrugCorrelation> recipeRulesDrugCorrelations = drugBusinessService.findRulesByDrugIdAndRuleId(drugId, ruleId);
        return ObjectCopyUtils.convert(recipeRulesDrugCorrelations, RecipeRulesDrugCorrelationVO.class);
    }

    @Override
    public List<RecipeRulesDrugCorrelationVO> findRulesByCorrelationDrugIdAndRuleId(Integer correlationDrugId, Integer ruleId) {
        List<RecipeRulesDrugCorrelation> recipeRulesDrugCorrelations = drugBusinessService.findRulesByCorrelationDrugIdAndRuleId(correlationDrugId, ruleId);
        return ObjectCopyUtils.convert(recipeRulesDrugCorrelations, RecipeRulesDrugCorrelationVO.class);
    }

    @Override
    public HisResponseTO medicationInfoSyncTaskForHis(List<MedicationInfoResTO> medicationInfoResTOList) {
        return drugBusinessService.medicationInfoSyncTaskForHis(medicationInfoResTOList);
    }

    @Override
    public List<DrugListBean> findDrugListByInfo(DrugListBean drugListBean) {
        validateAtop(drugListBean.getDrugName(),drugListBean.getDrugSpec(),drugListBean.getProducer(),drugListBean.getPack(),drugListBean.getUnit());
        List<DrugList> drugLists = drugBusinessService.findDrugListByInfo(drugListBean);
        return ObjectCopyUtils.convert(drugLists,DrugListBean.class);
    }

    /**
     * 定时 获取用药提醒的线下处方
     */
    @RpcService(timeout = 3600)
    public void queryRemindRecipe() {
        drugBusinessService.queryRemindRecipe("");
    }

}
