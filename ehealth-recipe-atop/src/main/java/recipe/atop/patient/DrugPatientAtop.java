package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.dto.PatientOpenDrugWithEsDTO;
import com.ngari.recipe.vo.SearchDrugReqVo;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.util.CollectionUtils;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IDrugBusinessService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

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
            List<PatientOpenDrugWithEsDTO> drugWithEsByPatient = drugBusinessService.findDrugWithEsByPatient(searchDrugReqVo);
            if(CollectionUtils.isEmpty(drugWithEsByPatient)){
                return Lists.newArrayList();
            }
            List<PatientDrugWithEsDTO> collect = drugWithEsByPatient.stream().map(patientOpenDrugWithEsDTO -> {
                PatientDrugWithEsDTO patientDrugWithEsDTO = new PatientDrugWithEsDTO();
                BeanUtils.copy(patientOpenDrugWithEsDTO, patientDrugWithEsDTO);
                return patientDrugWithEsDTO;
            }).collect(Collectors.toList());
            logger.info("DrugPatientAtop findDrugWithEsByPatient result:{}", JSONArray.toJSONString(collect));
            return collect;
        } catch (DAOException e1) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 可开方列表
     *
     * @param searchDrugReqVo
     * @return
     */
    @RpcService
    public List<PatientOpenDrugWithEsDTO> findOpenRecipeDrugWithEsByPatient(SearchDrugReqVo searchDrugReqVo) {
        logger.info("DrugPatientAtop findOpenRecipeDrugWithEsByPatient outPatientReqVO:{}", JSON.toJSONString(searchDrugReqVo));
        validateAtop(searchDrugReqVo, searchDrugReqVo.getOrganId());
        try {
            List<PatientOpenDrugWithEsDTO> drugWithEsDTOS = drugBusinessService.findDrugWithEsByPatient(searchDrugReqVo);
            logger.info("DrugPatientAtop findOpenRecipeDrugWithEsByPatient result:{}", JSONArray.toJSONString(drugWithEsDTOS));
            return drugWithEsDTOS;
        } catch (DAOException e1) {
            logger.error("DrugPatientAtop findOpenRecipeDrugWithEsByPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugPatientAtop findOpenRecipeDrugWithEsByPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
