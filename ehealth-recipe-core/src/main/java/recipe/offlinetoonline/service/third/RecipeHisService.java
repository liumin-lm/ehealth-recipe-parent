package recipe.offlinetoonline.service.third;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.QueryRecipeRequestTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author liumin
 * @Date 2021/6/8 下午2:08
 * @Description
 */
public class RecipeHisService {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

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
        //TODO question 查询条件带recipeCode
        //TODO question 让前置机去过滤数据
        LOGGER.info("HisRecipeService HisResponseTO.queryData:organId:{},patientDTO:{}", organId, JSONUtils.toString(patientDTO));
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setBirthday(patientDTO.getBirthday());
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientSex(patientDTO.getPatientSex());
        patientBaseInfo.setMobile(patientDTO.getMobile());
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        patientBaseInfo.setCardID(patientDTO.getCardId());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());

        QueryRecipeRequestTO queryRecipeRequestTO = new QueryRecipeRequestTO();
        queryRecipeRequestTO.setPatientInfo(patientBaseInfo);
        queryRecipeRequestTO.setStartDate(tranDateByFlagNew(timeQuantum.toString()));
        queryRecipeRequestTO.setEndDate(new Date());
        queryRecipeRequestTO.setOrgan(organId);
        queryRecipeRequestTO.setQueryType(flag);
        if (StringUtils.isNotEmpty(recipeCode)) {
            queryRecipeRequestTO.setRecipeCode(recipeCode);
        }
        IRecipeHisService recipeHisService = AppContextHolder.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("queryHisRecipeInfo input:" + JSONUtils.toString(queryRecipeRequestTO, QueryRecipeRequestTO.class));
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = recipeHisService.queryHisRecipeInfo(queryRecipeRequestTO);
        LOGGER.info("queryHisRecipeInfo output:" + JSONUtils.toString(responseTO, HisResponseTO.class));
        //过滤数据
        responseTO = filterData(responseTO,recipeCode);
        LOGGER.info("queryHisRecipeInfo queryData:{}.", JSONUtils.toString(responseTO));
        return responseTO;
    }


    /**
     * @param responseTO
     * @return
     * @author liumin
     * @Description 数据过滤
     */
    private HisResponseTO<List<QueryHisRecipResTO>> filterData(HisResponseTO<List<QueryHisRecipResTO>> responseTO,String recipeCode) {
        //获取详情时防止前置机没过滤数据，做过滤处理
        if(responseTO!=null&&recipeCode!=null){
            LOGGER.info("queryHisRecipeInfo recipeCode:{}",recipeCode);
            List<QueryHisRecipResTO> queryHisRecipResTOs=responseTO.getData();
            List<QueryHisRecipResTO> queryHisRecipResTOFilters=new ArrayList<>();
            if(!CollectionUtils.isEmpty(queryHisRecipResTOs)&&queryHisRecipResTOs.size()>1){
                for(QueryHisRecipResTO queryHisRecipResTO:queryHisRecipResTOs){
                    if(recipeCode.equals(queryHisRecipResTO.getRecipeCode())){
                        queryHisRecipResTOFilters.add(queryHisRecipResTO);
                        continue;
                    }
                }
            }
            responseTO.setData(queryHisRecipResTOFilters);
        }
        return responseTO;
    }


}
