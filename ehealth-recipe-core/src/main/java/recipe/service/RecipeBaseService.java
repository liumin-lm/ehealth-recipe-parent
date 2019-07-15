package recipe.service;

import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.entity.Recipe;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
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
        try {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            UserRoleToken urt = UserRoleToken.getCurrent();
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            if (recipe != null){
                if (urt.isPatient() && urt.isOwnPatient(recipe.getRequestMpiId())) {
                    LOGGER.error("当前用户没有权限调用recipeId[{}],methodName[{}]", recipeId, recipe.getRequestMpiId(),methodName);
                    throw new RuntimeException("当前登录用户没有权限");
                }else if (urt.isDoctor() && urt.isSelfDoctor(recipe.getDoctor())){
                    LOGGER.error("当前用户没有权限调用recipeId[{}],methodName[{}]", recipeId ,methodName);
                    throw new RuntimeException("当前登录用户没有权限");
                }
            }
        }catch (Exception e){
            LOGGER.error("checkUserHasPermission error",e);
            throw new RuntimeException("当前登录用户没有权限");
        }
    }

    public void checkUserHasPermissionByDoctorId(Integer doctorId){
        try {
            UserRoleToken urt = UserRoleToken.getCurrent();
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            if (urt.isDoctor() && urt.isSelfDoctor(doctorId)){
                LOGGER.error("当前用户没有权限调用doctorId[{}],methodName[{}]", doctorId ,methodName);
                throw new RuntimeException("当前登录用户没有权限");
            }
        }catch (Exception e){
            LOGGER.error("checkUserHasPermissionByDoctorId error",e);
            throw new RuntimeException("当前登录用户没有权限");
        }
    }

}
