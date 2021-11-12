package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.vo.SearchDrugReqVo;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IDrugBusinessService;

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

    /**
     * 患者端获取药品详情
     *
     * @param searchDrugReqVo
     * @return
     */
    @RpcService
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVo searchDrugReqVo) {
        logger.info("DrugPatientAtop findDrugWithEsByPatient outPatientReqVO:{}", JSON.toJSONString(searchDrugReqVo));
        validateAtop(searchDrugReqVo, searchDrugReqVo.getOrganId());
        try {
            List<PatientDrugWithEsDTO> drugWithEsDTOS = drugBusinessService.findDrugWithEsByPatient(searchDrugReqVo);
            logger.info("DrugPatientAtop findDrugWithEsByPatient result:{}", JSONArray.toJSONString(drugWithEsDTOS));
            return drugWithEsDTOS;
        } catch (DAOException e1) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
