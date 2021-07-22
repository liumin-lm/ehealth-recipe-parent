package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.DiseaseInfoDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import com.ngari.recipe.dto.OutPatientRecipeDTO;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * his处方 交互处理类
 *
 * @author fuzi
 */
@Service
public class OfflineRecipeClient extends BaseClient {
    /**
     * @param organId   机构id
     * @param doctorDTO 医生信息
     * @return 线下常用方对象
     */
    public List<CommonDTO> offlineCommonRecipe(Integer organId, DoctorDTO doctorDTO) {
        logger.info("OfflineRecipeClient offlineCommonRecipe organId:{}，doctorDTO:{}", organId, JSON.toJSONString(doctorDTO));
        OfflineCommonRecipeRequestTO request = new OfflineCommonRecipeRequestTO();
        request.setOrganId(organId);
        request.setDoctorId(doctorDTO.getDoctorId());
        request.setJobNumber(doctorDTO.getJobNumber());
        request.setName(doctorDTO.getName());
        try {
            HisResponseTO<List<CommonDTO>> hisResponse = recipeHisService.offlineCommonRecipe(request);
            return getResponse(hisResponse);
        } catch (Exception e) {
            logger.error("OfflineRecipeClient offlineCommonRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 查询线下门诊处方诊断信息
     * @param organId 机构ID
     * @param patientName 患者名称
     * @param registerID 挂号序号
     * @param patientId 病历号
     * @return  诊断列表
     */
    public List<DiseaseInfoDTO> queryPatientDisease(Integer organId, String patientName, String registerID, String patientId){
        logger.info("OfflineRecipeClient queryPatientDisease organId:{}, patientName:{},registerID:{},patientId:{}.",organId, patientName, registerID, patientId);
        try{
            PatientDiseaseInfoTO patientDiseaseInfoTO = new PatientDiseaseInfoTO();
            patientDiseaseInfoTO.setOrganId(organId);
            patientDiseaseInfoTO.setPatientName(patientName);
            patientDiseaseInfoTO.setRegisterID(registerID);
            patientDiseaseInfoTO.setPatientId(patientId);
            HisResponseTO<List<DiseaseInfo>>  hisResponse = recipeHisService.queryDiseaseInfo(patientDiseaseInfoTO);
            List<DiseaseInfo> result = getResponse(hisResponse);
            return ObjectCopyUtils.convert(result, DiseaseInfoDTO.class);
        } catch (Exception e){
            logger.error("OfflineRecipeClient queryPatientDisease hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 查询门诊处方
     * @param outPatientRecipeReq 患者信息
     * @return 门诊处方列表
     */
    public List<OutPatientRecipeDTO> queryOutPatientRecipe(OutPatientRecipeReq outPatientRecipeReq){
        logger.info("OfflineRecipeClient queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReq));
        try {
            HisResponseTO<List<OutPatientRecipeTO>> hisResponse = recipeHisService.queryOutPatientRecipe(outPatientRecipeReq);
            List<OutPatientRecipeTO> result = getResponse(hisResponse);
            return ObjectCopyUtils.convert(result, OutPatientRecipeDTO.class);
        } catch (Exception e) {
            logger.error("OfflineRecipeClient queryOutPatientRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @return
     * @Author liumin
     * @Desciption 从 his查询待缴费已缴费的处方信息
     */
    public HisResponseTO<List<QueryHisRecipResTO>> queryData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        logger.info("queryData organId:{},patientDTO:{}", organId, JSONUtils.toString(patientDTO));
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setBirthday(patientDTO.getBirthday());
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientSex(patientDTO.getPatientSex());
        patientBaseInfo.setMobile(patientDTO.getMobile());
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        patientBaseInfo.setCardID(patientDTO.getCardId());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());

        QueryRecipeRequestTO queryRecipeRequestTo = new QueryRecipeRequestTO();
        queryRecipeRequestTo.setPatientInfo(patientBaseInfo);
        if (timeQuantum != null) {
            queryRecipeRequestTo.setStartDate(tranDateByFlagNew(timeQuantum.toString()));
        }
        queryRecipeRequestTo.setEndDate(new Date());
        queryRecipeRequestTo.setOrgan(organId);
        queryRecipeRequestTo.setQueryType(flag);
        if (StringUtils.isNotEmpty(recipeCode)) {
            queryRecipeRequestTo.setRecipeCode(recipeCode);
        }
        IRecipeHisService recipeHisService = AppContextHolder.getBean("his.iRecipeHisService", IRecipeHisService.class);
        logger.info("queryHisRecipeInfo input:" + JSONUtils.toString(queryRecipeRequestTo, QueryRecipeRequestTO.class));
        HisResponseTO<List<QueryHisRecipResTO>> responseTo = recipeHisService.queryHisRecipeInfo(queryRecipeRequestTo);
        logger.info("queryHisRecipeInfo output:" + JSONUtils.toString(responseTo, HisResponseTO.class));
        //过滤数据
        responseTo = filterData(responseTo,recipeCode);
        logger.info("queryHisRecipeInfo queryData:{}.", JSONUtils.toString(responseTo));
        return responseTo;
    }

    /**
     * @param flag 根据flag转化日期 查询标志 0-近一个月数据;1-近三个月;2-近半年;3-近一年
     *             1 代表一个月  3 代表三个月 6 代表6个月
     * @return
     */
    private Date tranDateByFlagNew(String flag) {
        Date beginTime = new Date();
        Calendar ca = Calendar.getInstance();
        //得到当前日期
        ca.setTime(new Date());
        if ("6".equals(flag)) {  //近半年数据
            ca.add(Calendar.MONTH, -6);//月份减6
            Date resultDate = ca.getTime(); //结果
            beginTime = resultDate;
        } else if ("3".equals(flag)) {  //近三个月数据
            ca.add(Calendar.MONTH, -3);//月份减3
            Date resultDate = ca.getTime(); //结果
            beginTime = resultDate;
        } else if ("1".equals(flag)) { //近一个月数据
            ca.add(Calendar.MONTH, -1);//月份减1
            Date resultDate = ca.getTime(); //结果
            beginTime = resultDate;
        }
        return beginTime;
    }

    /**
     * @param responseTo
     * @return
     * @author liumin
     * @Description 数据过滤
     */
    private HisResponseTO<List<QueryHisRecipResTO>> filterData(HisResponseTO<List<QueryHisRecipResTO>> responseTo,String recipeCode) {
        //获取详情时防止前置机没过滤数据，做过滤处理
        if(responseTo!=null&&recipeCode!=null){
            logger.info("queryHisRecipeInfo recipeCode:{}",recipeCode);
            List<QueryHisRecipResTO> queryHisRecipResTos=responseTo.getData();
            List<QueryHisRecipResTO> queryHisRecipResToFilters=new ArrayList<>();
            if(!CollectionUtils.isEmpty(queryHisRecipResTos)&&queryHisRecipResTos.size()>1){
                for(QueryHisRecipResTO queryHisRecipResTo:queryHisRecipResTos){
                    if(recipeCode.equals(queryHisRecipResTo.getRecipeCode())){
                        queryHisRecipResToFilters.add(queryHisRecipResTo);
                        continue;
                    }
                }
            }
            responseTo.setData(queryHisRecipResToFilters);
        }
        return responseTo;
    }

}
