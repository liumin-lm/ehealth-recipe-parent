package recipe.atop.open;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.recipe.dto.RecipeTherapyOpDTO;
import com.ngari.recipe.dto.RecipeTherapyOpQueryDTO;
import com.ngari.recipe.entity.RecipeTherapy;
import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import com.ngari.recipe.recipe.model.RecipeTherapyOpQueryVO;
import com.ngari.recipe.recipe.model.RecipeTherapyOpVO;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.api.open.ITherapyRecipeOpenService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.enumerate.type.TherapyCancellationTypeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.RecipeTherapyVO;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 诊疗处方open接口类
 *
 * @author yinsheng
 * @date 2021\8\30 0030 10:09
 */
@RpcBean("remoteTherapyRecipeOpenService")
public class TherapyRecipeOpenAtop extends BaseAtop implements ITherapyRecipeOpenService {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

    @Override
    public boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId) {
        logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose bussSource={} clinicID={}", bussSource, clinicId);
        validateAtop(bussSource, clinicId);
        try {
            //接口结果 True存在 False不存在
            Boolean result = therapyRecipeBusinessService.abolishTherapyRecipeForRevisitClose(bussSource, clinicId);
            logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public boolean abolishTherapyRecipeForHis(Integer organId, String recipeCode) {
        logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForHis organId={} recipeCode={}", organId, recipeCode);
        validateAtop(organId, recipeCode);
        try {
            RecipeTherapyDTO recipeTherapyDTO = new RecipeTherapyDTO();
            recipeTherapyDTO.setStatus(TherapyStatusEnum.HADECANCEL.getType());
            recipeTherapyDTO.setTherapyCancellationType(TherapyCancellationTypeEnum.HIS_ABOLISH.getType());
            Boolean result = therapyRecipeBusinessService.updateTherapyRecipe(organId, recipeCode, recipeTherapyDTO);
            logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForHis result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForHis error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForHis error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public boolean therapyPayNotice(Integer organId, String recipeCode, RecipeTherapyDTO recipeTherapyDTO) {
        logger.info("TherapyRecipeOpenAtop therapyPayNotice organId={} recipeCode={}, recipeTherapyDTO:{}.", organId, recipeCode, JSON.toJSON(recipeTherapyDTO));
        validateAtop(organId, recipeCode, recipeTherapyDTO);
        try {
            recipeTherapyDTO.setStatus(TherapyStatusEnum.HADEPAY.getType());
            Boolean result = therapyRecipeBusinessService.updateTherapyRecipe(organId, recipeCode, recipeTherapyDTO);
            logger.info("TherapyRecipeOpenAtop therapyPayNotice result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop therapyPayNotice error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop therapyPayNotice error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public List<RecipeTherapyVO> findTherapyByClinicId(Integer clinicId) {
        List<RecipeTherapy> recipeTherapyList = therapyRecipeBusinessService.findTherapyByClinicId(clinicId);
        return ObjectCopyUtils.convert(recipeTherapyList, RecipeTherapyVO.class);
    }

    @Override
    @LogRecord
    public QueryResult<RecipeTherapyOpVO> findTherapyByInfo(RecipeTherapyOpQueryVO recipeTherapyOpQueryVO) {
        validateAtop(recipeTherapyOpQueryVO);
        try {
            RecipeTherapyOpQueryDTO recipeTherapyOpQueryDTO = ObjectCopyUtils.convert(recipeTherapyOpQueryVO, RecipeTherapyOpQueryDTO.class);
            QueryResult<RecipeTherapyOpDTO> queryResult = therapyRecipeBusinessService.findTherapyByInfo(recipeTherapyOpQueryDTO);
            List<RecipeTherapyOpDTO> items = queryResult.getItems();
            List<RecipeTherapyOpVO> records = new ArrayList<>();
            for (RecipeTherapyOpDTO item : items){
                RecipeTherapyOpVO recipeTherapyOpVO = new RecipeTherapyOpVO();
                recipeTherapyOpVO.setRecipeId(item.getRecipeId());
                recipeTherapyOpVO.setRecipeCode(item.getRecipeCode());
                recipeTherapyOpVO.setStatus(item.getStatus());
                recipeTherapyOpVO.setCreateTime(item.getCreateTime());
                recipeTherapyOpVO.setDoctorName(item.getDoctorName());
                recipeTherapyOpVO.setOrganName(item.getOrganName());
                recipeTherapyOpVO.setAppointDepartName(item.getAppointDepartName());
                recipeTherapyOpVO.setPatientMobile(item.getPatientMobile());
                recipeTherapyOpVO.setPatientName(item.getPatientName());
                records.add(recipeTherapyOpVO);
            }
            QueryResult<RecipeTherapyOpVO> result = new QueryResult<>();
            if(queryResult.getProperties() != null){
                Map<String, Object> properties = queryResult.getProperties();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    result.setProperty(key, entry.getValue());
                }
            }
            result.setItems(records);
            result.setLimit((int) queryResult.getLimit());
            result.setStart(queryResult.getStart());
            result.setTotal(queryResult.getTotal());
            return result;
        }catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop findTherapyByInfo error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop findTherapyByInfo error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }

    }
}