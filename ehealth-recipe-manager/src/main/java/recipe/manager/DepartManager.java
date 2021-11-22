package recipe.manager;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DepartClient;
import recipe.dao.RecipeExtendDAO;

/**
 * 科室通用业务处理类
 *
 * @author liumin
 * @date 2021\11\19 0018 08:57
 */
@Service
public class DepartManager extends BaseManager {

    @Autowired
    private DepartClient departClient;

    @Autowired
    private RecipeExtendDAO recipeExtendDao;

    /**
     * 获取挂号科室
     *
     * @param recipe
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Recipe recipe) {
        AppointDepartDTO appointDepartDTO = new AppointDepartDTO();
        if (recipe == null) {
            return appointDepartDTO;
        }
        RecipeExtend recipeExtend = recipeExtendDao.getByRecipeId(recipe.getRecipeId());
        if (recipeExtend != null && recipeExtend.getDocIndexId() != null) {
            appointDepartDTO.setAppointDepartCode(recipeExtend.getCardNo());
            appointDepartDTO.setAppointDepartName(recipeExtend.getCardTypeName());
        } else {
            appointDepartDTO = departClient.getAppointDepartByOrganIdAndDepart(recipe.getClinicOrgan(), recipe.getDepart());
        }
        return appointDepartDTO;
    }

    /**
     * 获取行政科室
     *
     * @param depart
     * @return
     */
    public DepartmentDTO getDepartmentByDepart(Integer depart) {
        DepartmentDTO departmentDTO = new DepartmentDTO();
        if (depart == null) {
            return departmentDTO;
        }
        departmentDTO = departClient.getDepartmentByDepart(depart);
        return departmentDTO;
    }

}
