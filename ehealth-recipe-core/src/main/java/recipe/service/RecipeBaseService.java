package recipe.service;

import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.Recipe;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/5/24.
 */
public class RecipeBaseService {

    /** LOGGER */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeBaseService.class);


    /**
     * 获取业务对象bean
     *
     * @param origin
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getBean(Object origin, Class<T> clazz) {
        Object dest = null;
        if (null != origin) {
            try {
                dest = clazz.newInstance();
                copyProperties(dest, origin);
            } catch (InstantiationException e) {
                dest = null;
                LOGGER.error("InstantiationException getBean error ", e);
            } catch (IllegalAccessException e) {
                dest = null;
                LOGGER.error("IllegalAccessException getBean error ", e);
            }
        }

        return (null != dest) ? (T) dest : null;
    }

    /**
     * 复制对象
     *
     * @param dest
     * @param origin
     */
    public void copyProperties(Object dest, Object origin) {
        if (null == origin) {
            dest = null;
            return;
        }
        try {
            BeanUtils.copyProperties(origin, dest);
        } catch (BeansException e) {
            LOGGER.error("BeansException copyProperties error ", e);
        }
    }

    /**
     * 获取业务对象列表
     *
     * @param originList
     * @param clazz
     * @return
     */
    public List getList(List<? extends Object> originList, Class clazz) {
        List list = new ArrayList<>(originList.size());
        if (CollectionUtils.isNotEmpty(originList)) {
            for (Object obj : originList) {
                list.add(getBean(obj, clazz));
            }
        }

        return list;
    }

    public void checkUserHasPermission(Integer recipeId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        UserRoleToken urt = UserRoleToken.getCurrent();
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (recipe != null){
            if ((urt.isPatient() && patientService.isPatientBelongUser(recipe.getMpiid()))||(urt.isDoctor() && urt.isSelfDoctor(recipe.getDoctor()))) {
                return;
            }else{
                LOGGER.error("当前用户没有权限调用recipeId[{}],methodName[{}]", recipeId ,methodName);
                throw new DAOException("当前登录用户没有权限");
            }
        }
    }

    public void checkUserHasPermissionByDoctorId(Integer doctorId){
        UserRoleToken urt = UserRoleToken.getCurrent();
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (!(urt.isSelfDoctor(doctorId))){
            LOGGER.error("当前用户没有权限调用doctorId[{}],methodName[{}]", doctorId ,methodName);
            throw new DAOException("当前登录用户没有权限");
        }
    }

    /**
     * 根据MpiId判断请求接口的患者是否是登录患者或者该患者下的就诊人
     *
     * @param mpiId
     */
    public static void checkUserHasPermissionByMpiId(String mpiId) {
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        if (!patientService.isPatientBelongUser(mpiId)){
            LOGGER.error("当前用户没有权限调用mpiId[{}],methodName[{}]", mpiId ,methodName);
            throw new DAOException("当前登录用户没有权限");
        }
    }

}
