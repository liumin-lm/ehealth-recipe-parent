package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.model.GetPriorityDrugsTO;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.patient.dto.ConsultSetDTO;
import com.ngari.patient.service.ConsultSetService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.PriortyDrug;
import com.ngari.recipe.entity.PriortyDrugBindDoctor;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import recipe.ApplicationUtils;
import recipe.dao.DrugListDAO;
import recipe.dao.PriortyDrugsBindDoctorDao;
import recipe.dao.PriortyDrugsDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 重点药品选择医生开处方(药品和医生均在数据库配置)
 *
 * 在线续方首页增加药品重点展示栏目或，点击该栏找医生开处方按钮，
 * 进入指定的医生列表，若未人工指定，则进入所有开方医生列表。
 * @author jiangtingfeng
 * @date 2017/10/23.
 */
@RpcBean(value = "priorityDrugBindDoctorService", mvc_authentication = false)
public class PriorityDrugBindDoctorService
{
    private static final Log LOGGER = LogFactory.getLog(PriorityDrugBindDoctorService.class);

    /**
     * 获取重点药品信息
     * @return
     */
    @RpcService
    public List<Map<String, Object>> getPriortyDrugs() {
        PriortyDrugsDao priortyDrugsDao = DAOFactory.getDAO(PriortyDrugsDao.class);
        List<PriortyDrug> priortyDrugs = priortyDrugsDao.findPriortyDrugs();
        List<Map<String, Object>> result = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(priortyDrugs)) {
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

            for (PriortyDrug priortyDrug : priortyDrugs) {
                Map<String, Object> map = Maps.newHashMap();
                Integer drugId = priortyDrug.getDrugId();
                // 排除机构不支持开的药
                DrugList drug = drugListDAO.findByDrugIdAndOrganId(drugId);
                if (null != drug) {
                    map.put("drug", ObjectCopyUtils.convert(drug, DrugListBean.class));
                    map.put("drugPicId",priortyDrug.getDrugPicId());
                    result.add(map);
                }
            }
        }
        return result;
    }

//    2019/12/16去除废弃接口
//     /**
//     * 获取重点药品的开药医生列表
//     * @param getPriorityDrugsTO
//     * @return
//     */
//    @Deprecated
//    @RpcService
//    public List<Map<String, Object>> getDoctorsForDishingOutDrug(GetPriorityDrugsTO getPriorityDrugsTO) {
//        PriortyDrugsBindDoctorDao priortyDrugsBindDoctorDao = DAOFactory.getDAO(PriortyDrugsBindDoctorDao.class);
//        IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);
//        IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
//        List<Map<String, Object>> result = Lists.newArrayList();
//
//        LOGGER.info("Enter PriorityDrugBindDoctorService.getDoctorsForDishingOutDrug " +
//            "GetPriorityDrugsTO = " + getPriorityDrugsTO);
//
//        if (null == getPriorityDrugsTO || null == getPriorityDrugsTO.getDrugId()){
//            return result;
//        }
//        Integer drugId = getPriorityDrugsTO.getDrugId();
//        List<Integer> doctorIds =
//            priortyDrugsBindDoctorDao.findPriortyDrugBindDoctors(drugId);
//
//        if (CollectionUtils.isEmpty(doctorIds)) {
//            return null;
//        }
//        List<DoctorBean> doctors =
//            iDoctorService.findDoctorsByConditions(getPriorityDrugsTO,
//                doctorIds);
//
//        for (DoctorBean doctor : doctors) {
//            getResult(result, iEmploymentService, doctor);
//        }
//        return result;
//    }

    /**
     * 配置重点药品
     * 插入的药品Id必须在药品表中存在（base_druglist）
     * @param drugId
     * @param sort 排序字段，后台根据该字段做排序(比如药品的销量，具体由运营控制)
     */
    @RpcService
    public void addPriortyDrug(Integer drugId,Integer sort,String drugPicId) {
        PriortyDrug priortyDrug = new PriortyDrug();
        PriortyDrugsDao priortyDrugsDao = DAOFactory.getDAO(PriortyDrugsDao.class);
        priortyDrug.setDrugId(drugId);
        priortyDrug.setSort(sort);
        priortyDrug.setDrugPicId(drugPicId);
        priortyDrug.setCreateTime(new Date());
        priortyDrug.setLastModify(new Date());
        priortyDrugsDao.save(priortyDrug);
    }

    /**
     * 配置绑定的医生
     * @param drugId
     * @param doctorId
     */
    @RpcService
    public void addDrugBindDoctors(Integer drugId,Integer doctorId) {
        PriortyDrugsBindDoctorDao priortyDrugsBindDoctorDao = DAOFactory.getDAO(PriortyDrugsBindDoctorDao.class);
        PriortyDrugBindDoctor priortyDrugBindDoctor = new PriortyDrugBindDoctor();
        priortyDrugBindDoctor.setDrugId(drugId);
        priortyDrugBindDoctor.setDoctorId(doctorId);
        priortyDrugBindDoctor.setCreateTime(new Date());
        priortyDrugsBindDoctorDao.save(priortyDrugBindDoctor);
    }

    /**
     * 封装返回信息
     * @param docInfoList
     * @param iEmploymentService
     * @param doctor
     * @return
     */
    public List<Map<String, Object>> getResult(List<Map<String, Object>> docInfoList,
        IEmploymentService iEmploymentService, DoctorBean doctor){
        int doctorId = doctor.getDoctorId();
        Map<String, Object> docInfo = Maps.newHashMap();
        EmploymentBean employment = iEmploymentService.getPrimaryEmpByDoctorId(doctorId);
        if (employment != null) {
            doctor.setDepartment(employment.getDepartment());
        }
        ConsultSetService consultSetService = ApplicationUtils.getBasicService(ConsultSetService.class);
        ConsultSetDTO consultSet = consultSetService.getNoDisCountSet(doctorId);
        docInfo.put("doctor", doctor);
        docInfo.put("consultSet", consultSet);
        docInfoList.add(docInfo);
        return docInfoList;
    }
}
