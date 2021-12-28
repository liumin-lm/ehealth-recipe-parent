package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.his.recipe.mode.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.util.ObjectCopyUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description： 患者药品查询入口
 * @author： whf
 * @date： 2021-08-23 18:05
 */
@RpcBean(value = "drugPatientAtop")
public class DrugPatientAtop extends BaseAtop {

    @Resource
    private IDrugBusinessService drugBusinessService;
    @Resource
    private IStockBusinessService stockBusinessService;

    /**
     * 患者端获取药品详情
     *
     * @param searchDrugReqVo
     * @return
     */
    @RpcService
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo) {
        logger.info("DrugPatientAtop findDrugWithEsByPatient outPatientReqVO:{}", JSON.toJSONString(searchDrugReqVo));
        validateAtop(searchDrugReqVo, searchDrugReqVo.getOrganId());
        try {
            List<PatientDrugWithEsDTO> drugWithEsByPatient = drugBusinessService.findDrugWithEsByPatient(searchDrugReqVo);
            logger.info("DrugPatientAtop findDrugWithEsByPatient result:{}", JSONArray.toJSONString(drugWithEsByPatient));
            return drugWithEsByPatient;
        } catch (DAOException e1) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 查询药品说明书
     *
     * @param organId          机构id
     * @param recipeDetailBean 药品数据
     * @return
     */
    @RpcService
    public DrugSpecificationInfoDTO hisDrugBook(Integer organId, RecipeDetailBean recipeDetailBean) {
        validateAtop(organId, recipeDetailBean, recipeDetailBean.getDrugId(), recipeDetailBean.getOrganDrugCode());
        Recipedetail recipedetail = ObjectCopyUtils.convert(recipeDetailBean, Recipedetail.class);
        return drugBusinessService.hisDrugBook(organId, recipedetail);
    }

    /**
     * 下单时获取药品库存
     * @param recipeIds
     * @param enterpriseId
     * @return
     */
    @RpcService
    public Boolean getOrderStockFlag(List<Integer> recipeIds,Integer enterpriseId,String giveModeKey) {
        validateAtop(recipeIds,giveModeKey);
        return stockBusinessService.getOrderStockFlag(recipeIds, enterpriseId,giveModeKey);
    }

}
