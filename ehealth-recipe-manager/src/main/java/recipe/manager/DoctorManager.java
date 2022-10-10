package recipe.manager;

import com.ngari.recipe.entity.DoctorDefault;
import org.springframework.stereotype.Service;
import recipe.dao.DoctorDefaultDAO;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.List;

/**
 * 医生信息通用层
 * @author fuzi
 */
@Service
public class DoctorManager extends BaseManager{

   @Resource
   private DoctorDefaultDAO doctorDefaultDAO;

    /**
     * 获取医生 默认数据
     * @param organId 机构id
     * @param doctorId 医生id
     * @param category 类别
     * @return 医生默认数据
     */
    public List<DoctorDefault> doctorDefaultList(Integer organId, Integer doctorId, Integer category) {
        if(ValidateUtil.integerIsEmpty(category)){
          return doctorDefaultDAO.findByOrganAndDoctor(organId,doctorId);
        }
        return doctorDefaultDAO.findByOrganAndDoctorAndCategory(organId,doctorId,category);
    }

    /**
     * 保存医生 默认数据
     * @param doctorDefault
     */
    public void saveDoctorDefault(DoctorDefault doctorDefault) {
        DoctorDefault dbDefault = doctorDefaultDAO.getByOrganAndDoctorAndCategoryAndType(doctorDefault.getOrganId(), doctorDefault.getDoctorId(), doctorDefault.getCategory(), doctorDefault.getType());
        if (null == dbDefault) {
            doctorDefaultDAO.save(doctorDefault);
            return;
        }
        dbDefault.setIdKey(doctorDefault.getIdKey());
        doctorDefaultDAO.updateNonNullFieldByPrimaryKey(dbDefault);

    }
}
