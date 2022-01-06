package recipe.atop.greenroom;

import com.ngari.recipe.dto.RecipeTherapyOpDTO;
import com.ngari.recipe.dto.RecipeTherapyOpQueryDTO;
import com.ngari.recipe.recipe.model.RecipeTherapyOpQueryVO;
import com.ngari.recipe.recipe.model.RecipeTherapyOpVO;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.util.ObjectCopyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description： 诊疗处方运营平台入口
 * @author： whf
 * @date： 2022-01-06 9:39
 */
@RpcBean("therapyRecipeGMAtop")
public class TherapyRecipeGMAtop  extends BaseAtop {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

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
