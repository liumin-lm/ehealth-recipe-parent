package recipe.service;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.HisRecipeDetailVO;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.constant.OrderStatusConstant;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.thread.QueryHisRecipeCallable;
import recipe.thread.RecipeBusiThreadPool;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 19:58
 */
@RpcBean("hisRecipeService")
public class HisRecipeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HisRecipeService.class);

    @Autowired
    private HisRecipeDAO hisRecipeDAO;
    @Autowired
    private HisRecipeExtDAO hisRecipeExtDAO;
    @Autowired
    private HisRecipeDetailDAO hisRecipeDetailDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;

    /**
     * organId 机构编码
     * mpiId 用户mpiId
     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
     * status 1 未处理 2 已处理
     *
     * @param request
     * @return
     */
    @RpcService
    public List<HisRecipeVO> findHisRecipe(Map<String, Object> request) {
        Integer organId = (Integer) request.get("organId");
        String mpiId = (String) request.get("mpiId");
        Integer timeQuantum = (Integer) request.get("timeQuantum");
        String status = (String) request.get("status");
        String cardId = (String) request.get("cardId");
        Integer start = (Integer) request.get("start");
        Integer limit = (Integer) request.get("limit");
        Integer flag = 1;
        if (!"ongoing".equals(status)) {
            flag = 2;
        }
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(mpiId);
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }

        /**查询his线下处方数据*/
        //同步查询待缴费处方
        queryHisRecipeInfo(organId, patientDTO, timeQuantum, 1);
        //异步获取已缴费处方
        RecipeBusiThreadPool.submit(new QueryHisRecipeCallable(organId, mpiId, timeQuantum, 2, patientDTO));

        List<HisRecipe> hisRecipes = hisRecipeDAO.findHisRecipes(organId, mpiId, flag, start, limit);
        List<HisRecipeVO> result = new ArrayList<>();
        //根据status状态查询处方列表
        if ("ongoing".equals(status)) {
            //表示想要查询未处理的处方
            // 1 该处方在平台上不存在,只存在HIS中
            for (HisRecipe hisRecipe : hisRecipes) {
                HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipe, HisRecipeVO.class);
                List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeId(hisRecipe.getHisRecipeID());
                List<HisRecipeDetailVO> hisRecipeDetailVOS = ObjectCopyUtils.convert(hisRecipeDetails, HisRecipeDetailVO.class);
                hisRecipeVO.setRecipeDetail(hisRecipeDetailVOS);
                hisRecipeVO.setOrganDiseaseName(hisRecipe.getDiseaseName());
                Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), organId);
                if (recipe == null) {
                    hisRecipeVO.setOrderStatusText("待支付");
                    hisRecipeVO.setFromFlag(1);
                    hisRecipeVO.setJumpPageType(0);
                    result.add(hisRecipeVO);
                } else {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    if (StringUtils.isEmpty(recipe.getOrderCode())) {
                        if (recipeExtend != null && recipeExtend.getFromFlag() == 0) {
                            //表示该处方来源于HIS
                            hisRecipeVO.setOrderStatusText("待支付");
                            hisRecipeVO.setFromFlag(1);
                            hisRecipeVO.setJumpPageType(0);
                            result.add(hisRecipeVO);
                        } else {
                            //表示该处方来源于平台
                            hisRecipeVO.setOrderStatusText("待支付");
                            hisRecipeVO.setFromFlag(0);
                            hisRecipeVO.setJumpPageType(0);
                            hisRecipeVO.setOrganDiseaseName(recipe.getOrganDiseaseName());
                            hisRecipeVO.setHisRecipeID(recipe.getRecipeId());
                            List<HisRecipeDetailVO> recipeDetailVOS = getHisRecipeDetailVOS(recipe);
                            hisRecipeVO.setRecipeDetail(recipeDetailVOS);
                            result.add(hisRecipeVO);
                        }
                    }
                }
            }
        } else {
            //表示查询已处理的处方
            for (HisRecipe hisRecipe : hisRecipes) {
                HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipe, HisRecipeVO.class);
                List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeId(hisRecipe.getHisRecipeID());
                List<HisRecipeDetailVO> hisRecipeDetailVOS = ObjectCopyUtils.convert(hisRecipeDetails, HisRecipeDetailVO.class);
                hisRecipeVO.setRecipeDetail(hisRecipeDetailVOS);
                Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), organId);
                if (recipe == null) {
                    //表示该处方单患者在his线下已完成
                    hisRecipeVO.setStatusText("已完成");
                    hisRecipeVO.setOrderStatusText("已完成");
                    hisRecipeVO.setFromFlag(1);
                    hisRecipeVO.setJumpPageType(0);
                    result.add(hisRecipeVO);
                } else {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    if (StringUtils.isEmpty(recipe.getOrderCode())) {
                        hisRecipeVO.setStatusText(getRecipeStatusTabText(recipe.getStatus()));
                        if (recipeExtend != null && recipeExtend.getFromFlag() == 0) {
                            hisRecipeVO.setFromFlag(1);
                            hisRecipeVO.setJumpPageType(0);
                            result.add(hisRecipeVO);

                        } else {
                            hisRecipeVO.setFromFlag(0);
                            hisRecipeVO.setOrganDiseaseName(recipe.getOrganDiseaseName());
                            hisRecipeVO.setHisRecipeID(recipe.getRecipeId());
                            List<HisRecipeDetailVO> recipeDetailVOS = getHisRecipeDetailVOS(recipe);
                            hisRecipeVO.setRecipeDetail(recipeDetailVOS);
                            hisRecipeVO.setJumpPageType(0);
                            result.add(hisRecipeVO);
                        }
                    } else {
                        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                        hisRecipeVO.setStatusText(getTipsByStatusForPatient(recipe, recipeOrder));
                        hisRecipeVO.setOrderCode(recipe.getOrderCode());
                        hisRecipeVO.setFromFlag(recipe.getRecipeSourceType()==2?1:0);
                        if (recipe.getFromflag() != 0) {
                            hisRecipeVO.setOrganDiseaseName(recipe.getOrganDiseaseName());
                            List<HisRecipeDetailVO> recipeDetailVOS = getHisRecipeDetailVOS(recipe);
                            hisRecipeVO.setRecipeDetail(recipeDetailVOS);
                        }
                        hisRecipeVO.setJumpPageType(1);
                        result.add(hisRecipeVO);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 查询线下处方 入库操作
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     */
    @RpcService
    public void queryHisRecipeInfo(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag) {
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setBirthday(patientDTO.getBirthday());
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientSex(patientDTO.getPatientSex());
        patientBaseInfo.setMobile(patientDTO.getMobile());
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());

        QueryRecipeRequestTO queryRecipeRequestTO = new QueryRecipeRequestTO();
        queryRecipeRequestTO.setPatientInfo(patientBaseInfo);
        queryRecipeRequestTO.setStartDate(tranDateByFlagNew(timeQuantum.toString()));
        queryRecipeRequestTO.setEndDate(new Date());
        queryRecipeRequestTO.setOrgan(organId);
        queryRecipeRequestTO.setQueryType(flag);

        IRecipeHisService recipeHisService = AppContextHolder.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("queryHisRecipeInfo input:" + JSONUtils.toString(queryRecipeRequestTO, QueryRecipeRequestTO.class));
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = recipeHisService.queryHisRecipeInfo(queryRecipeRequestTO);
        LOGGER.info("queryHisRecipeInfo output:" + JSONUtils.toString(responseTO, HisResponseTO.class));
        if (null == responseTO || null == responseTO.getData()) {
            return;
        }

        try {
            /** 更新数据校验*/
            hisRecipeInfoCheck(responseTO.getData());
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }

        try {
            //数据入库
            saveHisRecipeInfo(responseTO, patientDTO, flag);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }
    }


    @RpcService
    public void saveHisRecipeInfo(HisResponseTO<List<QueryHisRecipResTO>> responseTO, PatientDTO patientDTO, Integer flag) {
        List<QueryHisRecipResTO> queryHisRecipResTOList = responseTO.getData();
        LOGGER.info("saveHisRecipeInfo queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResTOList));
        for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOList) {
            HisRecipe hisRecipe1 = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(
                    patientDTO.getMpiId(), queryHisRecipResTO.getClinicOrgan(), queryHisRecipResTO.getRecipeCode());
            //数据库不存在处方信息，则新增
            if (null == hisRecipe1) {
                HisRecipe hisRecipe = new HisRecipe();
                hisRecipe.setCertificate(patientDTO.getCertificate());
                hisRecipe.setCertificateType(patientDTO.getCertificateType());
                hisRecipe.setMpiId(patientDTO.getMpiId());
                hisRecipe.setPatientName(patientDTO.getPatientName());
                hisRecipe.setPatientAddress(patientDTO.getAddress());
                hisRecipe.setPatientNumber(queryHisRecipResTO.getPatientNumber());
                hisRecipe.setPatientTel(patientDTO.getMobile());
                hisRecipe.setRegisteredId(queryHisRecipResTO.getRegisteredId());
                hisRecipe.setRecipeCode(queryHisRecipResTO.getRecipeCode());
                hisRecipe.setDepartCode(queryHisRecipResTO.getDepartCode());
                hisRecipe.setDepartName(queryHisRecipResTO.getDepartName());
                hisRecipe.setDoctorName(queryHisRecipResTO.getDoctorName());
                hisRecipe.setCreateDate(queryHisRecipResTO.getCreateDate());
                hisRecipe.setStatus(queryHisRecipResTO.getStatus());
                hisRecipe.setMedicalType(1);
                hisRecipe.setRecipeFee(queryHisRecipResTO.getRecipeFee());
                hisRecipe.setRecipeType(queryHisRecipResTO.getRecipeType());
                hisRecipe.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
                hisRecipe.setCreateTime(new Date());
                hisRecipe.setExtensionFlag(1);
                if (queryHisRecipResTO.getExtensionFlag() == null) {
                    hisRecipe.setRecipePayType(0); //设置外延处方的标志
                } else {
                    hisRecipe.setRecipePayType(queryHisRecipResTO.getExtensionFlag()); //设置外延处方的标志
                }

                if(!StringUtils.isEmpty(queryHisRecipResTO.getDiseaseName())){
                    hisRecipe.setDiseaseName(queryHisRecipResTO.getDiseaseName());
                }else {
                    hisRecipe.setDiseaseName("无");
                }
                if(!StringUtils.isEmpty(queryHisRecipResTO.getDoctorCode())){
                    hisRecipe.setDoctorCode(queryHisRecipResTO.getDoctorCode());
                }
                OrganService organService = BasicAPI.getService(OrganService.class);
                OrganDTO organDTO = organService.getByOrganId(queryHisRecipResTO.getClinicOrgan());
                if(null !=organDTO) {
                    hisRecipe.setOrganName(organDTO.getName());
                }
                if (null != queryHisRecipResTO.getMedicalInfo()) {
                    MedicalInfo medicalInfo = queryHisRecipResTO.getMedicalInfo();
                    if(!ObjectUtils.isEmpty(medicalInfo.getMedicalAmount())){
                        hisRecipe.setMedicalAmount(medicalInfo.getMedicalAmount());
                    }
                    if(!ObjectUtils.isEmpty(medicalInfo.getCashAmount())){
                        hisRecipe.setCashAmount(medicalInfo.getCashAmount());
                    }
                    if(!ObjectUtils.isEmpty(medicalInfo.getTotalAmount())){
                        hisRecipe.setTotalAmount(medicalInfo.getTotalAmount());
                    }
                }
                hisRecipe = hisRecipeDAO.save(hisRecipe);
                if (null != queryHisRecipResTO.getExt()) {
                    for (ExtInfoTO extInfoTO : queryHisRecipResTO.getExt()) {
                        HisRecipeExt ext = ObjectCopyUtils.convert(extInfoTO, HisRecipeExt.class);
                        ext.setHisRecipeId(hisRecipe.getHisRecipeID());
                        hisRecipeExtDAO.save(ext);
                    }
                }
                if (null != queryHisRecipResTO.getDrugList()) {
                    for (RecipeDetailTO recipeDetailTO : queryHisRecipResTO.getDrugList()) {
                        HisRecipeDetail detail = ObjectCopyUtils.convert(recipeDetailTO, HisRecipeDetail.class);
                        detail.setHisRecipeId(hisRecipe.getHisRecipeID());
                        detail.setRecipeDeatilCode(recipeDetailTO.getRecipeDeatilCode());
                        detail.setDrugName(recipeDetailTO.getDrugName());
                        detail.setPrice(recipeDetailTO.getPrice());
                        detail.setTotalPrice(recipeDetailTO.getTotalPrice());
                        detail.setUsingRate(recipeDetailTO.getUsingRate());
                        detail.setUsePathways(recipeDetailTO.getUsePathWays());
                        detail.setDrugSpec(recipeDetailTO.getDrugSpec());
                        detail.setDrugUnit(recipeDetailTO.getDrugUnit());
                        //date 20200526
                        //修改线下处方同步用药天数，判断是否有小数类型的用药天数
                        //修改逻辑，UseDays这个字段为老字段只有整数
                        //UseDaysB这个字段为字符类型，可能小数，准备之后用药天数都用这个字段
                        //取对应没有的字段设置传过来的值
                        //这两个值只会有一个没有传
                        if(null != recipeDetailTO.getUseDays()){
                            //设置字符类型的
                            detail.setUseDaysB(recipeDetailTO.getUseDays().toString());
                        }
                        if(null != recipeDetailTO.getUseDaysB()){
                            //设置int类型的
                            //设置字符转向上取整
                            int useDays = new BigDecimal(recipeDetailTO.getUseDaysB()).setScale(0, BigDecimal.ROUND_UP).intValue();
                            detail.setUseDays(useDays);
                        }
//                        detail.setUseDays(recipeDetailTO.getUseDays());
//                        detail.setUseDaysB(recipeDetailTO.getUseDays().toString());
                        detail.setDrugCode(recipeDetailTO.getDrugCode());
                        detail.setUsingRateText(recipeDetailTO.getUsingRateText());
                        detail.setUsePathwaysText(recipeDetailTO.getUsePathwaysText());
                        detail.setUseDose(recipeDetailTO.getUseDose());
                        detail.setUseDoseUnit(recipeDetailTO.getUseDoseUnit());
                        detail.setSaleName(recipeDetailTO.getSaleName());
                        detail.setPack(recipeDetailTO.getPack());
                        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                        if (StringUtils.isNotEmpty(detail.getRecipeDeatilCode())) {
                            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(detail.getDrugCode()));
                            if (CollectionUtils.isEmpty(organDrugLists)) {
                                LOGGER.info("saveHisRecipeInfo organDrugLists his传过来的药品编码没有在对应机构维护,organId:"+hisRecipe.getClinicOrgan()+",organDrugCode:" + detail.getDrugCode());
                            }
                        }
                        detail.setStatus(1);
                        hisRecipeDetailDAO.save(detail);
                    }
                }
            }else{
                //如果已缴费处方在数据库里已存在，且数据里的状态是未缴费，则将数据库里的未缴费状态更新为已缴费状态
                if(2 == flag){
                    if(1 == hisRecipe1.getStatus()){
                        hisRecipe1.setStatus(queryHisRecipResTO.getStatus());
                        hisRecipeDAO.update(hisRecipe1);
                    }
                }
            }
        }
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

    private List<HisRecipeDetailVO> getHisRecipeDetailVOS(Recipe recipe) {
        List<HisRecipeDetailVO> recipeDetailVOS = new ArrayList<>();
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        for (Recipedetail recipedetail : recipedetails) {
            HisRecipeDetailVO hisRecipeDetailVO = new HisRecipeDetailVO();
            hisRecipeDetailVO.setDrugName(recipedetail.getDrugName());
            hisRecipeDetailVO.setDrugSpec(recipedetail.getDrugSpec());
            hisRecipeDetailVO.setDrugUnit(recipedetail.getDrugUnit());
            hisRecipeDetailVO.setPack(recipedetail.getPack());
            hisRecipeDetailVO.setDrugForm(recipedetail.getDrugForm());
            hisRecipeDetailVO.setUseTotalDose(new BigDecimal(recipedetail.getUseTotalDose()));
            recipeDetailVOS.add(hisRecipeDetailVO);
        }
        return recipeDetailVOS;
    }

    @RpcService
    public Map<String, Object> getHisRecipeDetail(Integer hisRecipeId){
        //将线下处方转化成线上处方
        HisRecipe hisRecipe = hisRecipeDAO.get(hisRecipeId);
        if (hisRecipe == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单");
        }
        Recipe recipe = saveRecipeFromHisRecipe(hisRecipe);
        if (recipe != null) {
            saveRecipeExt(recipe.getRecipeId(),hisRecipe);
            //生成处方详情
            savaRecipeDetail(recipe.getRecipeId(),hisRecipe);
        }
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        Map<String,Object> map = recipeService.getPatientRecipeById(recipe.getRecipeId());
        List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeId(hisRecipeId);
        map.put("hisRecipeDetails", hisRecipeDetails);
        List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findByHisRecipeId(hisRecipeId);
        map.put("hisRecipeExts", hisRecipeExts);
        map.put("showText", hisRecipe.getShowText());
        return map;
    }

    private void saveRecipeExt(Integer recipeId, HisRecipe hisRecipe) {
        RecipeExtend haveRecipeExt = recipeExtendDAO.getByRecipeId(recipeId);
        if (haveRecipeExt != null) {
            return;
        }
        RecipeExtend recipeExtend = new RecipeExtend();
        recipeExtend.setRecipeId(recipeId);
        recipeExtend.setFromFlag(0);
        recipeExtend.setRegisterID(hisRecipe.getRegisteredId());
        recipeExtendDAO.save(recipeExtend);
    }

    private Recipe saveRecipeFromHisRecipe(HisRecipe hisRecipe) {
        Recipe haveRecipe = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), hisRecipe.getClinicOrgan());
        if (haveRecipe != null) {
            return haveRecipe;
        }
        Recipe recipe = new Recipe();
        recipe.setBussSource(0);
        //通过挂号序号关联复诊
        try {
            IConsultExService consultExService = ConsultAPI.getService(IConsultExService.class);
            ConsultExDTO consultExDTO = consultExService.getByRegisterId(hisRecipe.getRegisteredId());
            if (consultExDTO != null){
                recipe.setBussSource(2);
                recipe.setClinicId(consultExDTO.getConsultId());
            }
        }catch (Exception e){
            LOGGER.error("线下处方转线上通过挂号序号关联复诊 error",e);
        }

        recipe.setClinicOrgan(hisRecipe.getClinicOrgan());
        recipe.setMpiid(hisRecipe.getMpiId());
        recipe.setPatientName(hisRecipe.getPatientName());
        recipe.setPatientID(hisRecipe.getPatientNumber());
        recipe.setPatientStatus(1);
        recipe.setOrganName(hisRecipe.getOrganName());
        recipe.setRecipeCode(hisRecipe.getRecipeCode());
        recipe.setRecipeType(hisRecipe.getRecipeType());
        recipe.setDepart(Integer.parseInt(hisRecipe.getDepartCode()));
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        if (StringUtils.isNotEmpty(hisRecipe.getDoctorCode())) {
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(hisRecipe.getDoctorCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setDoctor(employmentDTO.getDoctorId());
            } else {
                LOGGER.error("请确认医院的医生工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "请将医院的医生工号和纳里维护的医生工号保持一致");
            }
        }

        recipe.setDoctorName(hisRecipe.getDoctorName());
        recipe.setCreateDate(hisRecipe.getCreateDate());
        recipe.setSignDate(hisRecipe.getCreateDate());
        recipe.setOrganDiseaseName(hisRecipe.getDiseaseName());
        recipe.setOrganDiseaseId(hisRecipe.getDisease());
        recipe.setTotalMoney(hisRecipe.getRecipeFee());
        recipe.setActualPrice(hisRecipe.getRecipeFee());
        recipe.setMemo(hisRecipe.getMemo()==null?"无":hisRecipe.getMemo());
        recipe.setPayFlag(0);
        if (hisRecipe.getStatus() == 2) {
            recipe.setStatus(6);
        } else {
            recipe.setStatus(2);
        }

        recipe.setReviewType(0);
        recipe.setChooseFlag(0);
        recipe.setRemindFlag(0);
        recipe.setPushFlag(0);
        recipe.setTakeMedicine(0);
        recipe.setGiveFlag(0);
        recipe.setRecipeMode("ngarihealth");
        recipe.setCopyNum(1);
        recipe.setValueDays(3);
        recipe.setFromflag(1);
        recipe.setRecipeSourceType(2);
        recipe.setRecipePayType(hisRecipe.getRecipePayType());
        recipe.setRequestMpiId(hisRecipe.getMpiId());

        return recipeDAO.saveRecipe(recipe);

    }

    private void savaRecipeDetail(Integer recipeId, HisRecipe hisRecipe) {
        List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeId(hisRecipe.getHisRecipeID());
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipedetails)) {
            return;
        }
        for (HisRecipeDetail hisRecipeDetail : hisRecipeDetails) {
            LOGGER.info("hisRecipe.getClinicOrgan(): "+hisRecipe.getClinicOrgan()+"");
            LOGGER.info("Arrays.asList(hisRecipeDetail.getDrugCode()):"+hisRecipeDetail.getDrugCode());
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(hisRecipeDetail.getDrugCode()));
            if (CollectionUtils.isEmpty(organDrugLists)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "请将医院的药品信息维护到纳里机构药品目录");
            }
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setRecipeId(recipeId);
            recipedetail.setUseDose(StringUtils.isEmpty(hisRecipeDetail.getUseDose())?null:Double.valueOf(hisRecipeDetail.getUseDose()));
            recipedetail.setUseDoseUnit(hisRecipeDetail.getUseDoseUnit());
            if (StringUtils.isNotEmpty(hisRecipeDetail.getUseDose())) {
                recipedetail.setUseDose(Double.parseDouble(hisRecipeDetail.getUseDose()));
            }
            if (StringUtils.isNotEmpty(hisRecipeDetail.getDrugSpec())) {
                recipedetail.setDrugSpec(hisRecipeDetail.getDrugSpec());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugSpec(organDrugLists.get(0).getDrugSpec());
                }
            }
            if (StringUtils.isNotEmpty(hisRecipeDetail.getDrugName())) {
                recipedetail.setDrugName(hisRecipeDetail.getDrugName());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugName(organDrugLists.get(0).getDrugName());
                }
            }
            if (StringUtils.isNotEmpty(hisRecipeDetail.getDrugUnit())) {
                recipedetail.setDrugUnit(hisRecipeDetail.getDrugUnit());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugUnit(organDrugLists.get(0).getUnit());
                }
            }
            if (hisRecipeDetail.getPack() != null) {
                recipedetail.setPack(hisRecipeDetail.getPack());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setPack(organDrugLists.get(0).getPack());
                }
            }
            if (hisRecipeDetail.getPrice() != null) {
                recipedetail.setSalePrice(hisRecipeDetail.getPrice());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setSalePrice(organDrugLists.get(0).getSalePrice());
                }
            }
            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                recipedetail.setDrugId(organDrugLists.get(0).getDrugId());
                recipedetail.setOrganDrugCode(hisRecipeDetail.getDrugCode());
                recipedetail.setUsingRate(organDrugLists.get(0).getUsingRate());
                recipedetail.setUsePathways(organDrugLists.get(0).getUsePathways());
                if (StringUtils.isEmpty(recipedetail.getUseDoseUnit())) {
                    recipedetail.setUseDoseUnit(organDrugLists.get(0).getUseDoseUnit());
                }
                if (recipedetail.getUseDose() == null) {
                    recipedetail.setUseDose(organDrugLists.get(0).getUseDose());
                }
            }
            recipedetail.setUsingRateTextFromHis(hisRecipeDetail.getUsingRateText());
            recipedetail.setUsePathwaysTextFromHis(hisRecipeDetail.getUsePathwaysText());
            if (hisRecipeDetail.getUseTotalDose() != null) {
                recipedetail.setUseTotalDose(hisRecipeDetail.getUseTotalDose().doubleValue());
            }
            recipedetail.setUseDays(hisRecipeDetail.getUseDays());
            //date 20200528
            //设置线上处方的信息
            recipedetail.setUseDaysB(hisRecipeDetail.getUseDaysB());
            recipedetail.setStatus(1);

            if (hisRecipeDetail.getUseTotalDose() != null && hisRecipeDetail.getPrice() != null) {
                recipedetail.setDrugCost(hisRecipeDetail.getUseTotalDose().multiply(hisRecipeDetail.getPrice()));
            } else {
                recipedetail.setDrugCost(hisRecipeDetail.getUseTotalDose().multiply(organDrugLists.get(0).getSalePrice()));
            }
            recipeDetailDAO.save(recipedetail);
        }
    }

    private String getRecipeStatusTabText(int status) {
        String msg;
        switch (status) {
            case RecipeStatusConstant.FINISH:
                msg = "已完成";
                break;
            case RecipeStatusConstant.HAVE_PAY:
                msg = "已支付，待取药";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                msg = "待处理";
                break;
            case RecipeStatusConstant.NO_PAY:
                msg = "未支付";
                break;
            case RecipeStatusConstant.NO_OPERATOR:
                msg = "未处理";
                break;
            //已撤销从已取消拆出来
            case RecipeStatusConstant.REVOKE:
                msg = "已撤销";
                break;
            //已撤销从已取消拆出来
            case RecipeStatusConstant.DELETE:
                msg = "已删除";
                break;
            //写入his失败从已取消拆出来
            case RecipeStatusConstant.HIS_FAIL:
                msg = "写入his失败";
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                msg = "审核不通过";
                break;
            case RecipeStatusConstant.IN_SEND:
                msg = "配送中";
                break;
            case RecipeStatusConstant.WAIT_SEND:
                msg = "待配送";
                break;
            case RecipeStatusConstant.READY_CHECK_YS:
                msg = "待审核";
                break;
            case RecipeStatusConstant.CHECK_PASS_YS:
                msg = "审核通过";
                break;
            //这里患者取药失败和取药失败都判定为失败
            case RecipeStatusConstant.NO_DRUG:
            case RecipeStatusConstant.RECIPE_FAIL:
                msg = "失败";
                break;
            case RecipeStatusConstant.RECIPE_DOWNLOADED:
                msg = "待取药";
                break;
            case RecipeStatusConstant.USING:
                msg = "处理中";
                break;
            default:
                msg = "未知状态";
        }

        return msg;
    }


    /**
     * 状态文字提示（患者端）
     *
     * @param recipe
     * @return
     */
    public static String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payMode = recipe.getPayMode();
        Integer payFlag = recipe.getPayFlag();
        Integer giveMode = recipe.getGiveMode();
        Integer orderStatus = order.getStatus();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.NO_PAY:
            case RecipeStatusConstant.NO_OPERATOR:
            case RecipeStatusConstant.REVOKE:
            case RecipeStatusConstant.NO_DRUG:
            case RecipeStatusConstant.DELETE:
            case RecipeStatusConstant.HIS_FAIL:
                tips = "已取消";
                break;
            case RecipeStatusConstant.FINISH:
                tips = "已完成";
                break;
            case RecipeStatusConstant.IN_SEND:
                tips = "配送中";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "待处理";
                } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
                    if (new Integer(1).equals(recipe.getRecipePayType()) && payFlag == 1) {
                        tips = "已支付";
                    } else if (payFlag == 0){
                        tips = "待支付";
                    } else {
                        tips = "待取药";
                    }
                } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                    if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                        if (payFlag == 0) {
                            tips = "待支付";
                        } else {
                            if (OrderStatusConstant.READY_SEND.equals(orderStatus)) {
                                tips = "待配送";
                            } else if (OrderStatusConstant.SENDING.equals(orderStatus)) {
                                tips = "配送中";
                            } else if (OrderStatusConstant.FINISH.equals(orderStatus)) {
                                tips = "已完成";
                            }
                        }
                    }

                } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                    if (OrderStatusConstant.HAS_DRUG.equals(orderStatus)) {
                        if (payFlag == 0) {
                            tips = "待支付";
                        } else {
                            tips = "待取药";
                        }
                    }
                } else if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(giveMode)) {
                    tips = "已完成";
                }
                break;
            default:
                tips = "待取药";
        }
        return tips;
    }

    @RpcService
    public Integer getCardType(Integer organId) {
        //卡类型 1 表示身份证  2 表示就诊卡
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (Integer) configurationCenterUtilsService.getConfiguration(organId, "getCardTypeForHis");
    }

    /**
     * 校验 his线下处方是否有更改
     *
     * @param hisRecipeTO
     */
    private void hisRecipeInfoCheck(List<QueryHisRecipResTO> hisRecipeTO) {
        LOGGER.info("hisRecipeInfoCheck hisRecipeTO = {}", JSONUtils.toString(hisRecipeTO));
        Integer clinicOrgan = hisRecipeTO.get(0).getClinicOrgan();
        if (null == clinicOrgan) {
            LOGGER.info("hisRecipeInfoCheck his data error clinicOrgan is null");
            return;
        }
        List<String> recipeCodeList = hisRecipeTO.stream().map(QueryHisRecipResTO::getRecipeCode).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeCodeList)) {
            LOGGER.info("hisRecipeInfoCheck his data error recipeCodeList is null");
            return;
        }
        //2 判断Recipe 是否有订单
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        LOGGER.info("hisRecipeInfoCheck recipeList = {}", JSONUtils.toString(recipeList));

        if (CollectionUtils.isNotEmpty(recipeList)) {
            List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).distinct().collect(Collectors.toList());
            //3 判断 订单 是否支付
            if (CollectionUtils.isNotEmpty(orderCodeList)) {
                List<RecipeOrder> recipeOrderList = recipeOrderDAO.findByOrderCode(orderCodeList);
                LOGGER.info("hisRecipeInfoCheck recipeOrderList = {}", JSONUtils.toString(recipeOrderList));
                List<String> recipeOrderCode = recipeOrderList.stream().filter(a -> a.getPayFlag().equals(PayConstant.PAY_FLAG_PAY_SUCCESS)).map(RecipeOrder::getOrderCode).collect(Collectors.toList());
                List<String> recipeCodeExclude = recipeList.stream().filter(a -> recipeOrderCode.contains(a.getOrderCode())).map(Recipe::getRecipeCode).distinct().collect(Collectors.toList());
                //排除支付订单处方
                recipeCodeList = recipeCodeList.stream().filter(a -> !recipeCodeExclude.contains(a)).collect(Collectors.toList());
                LOGGER.info("hisRecipeInfoCheck recipeCodeList = {}", JSONUtils.toString(recipeCodeList));
                if (CollectionUtils.isEmpty(recipeCodeList)) {
                    return;
                }
            }
        }

        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        LOGGER.info("hisRecipeInfoCheck hisRecipeList = {}", JSONUtils.toString(hisRecipeList));
        if (CollectionUtils.isEmpty(hisRecipeList)) {
            return;
        }
        //判断 hisRecipe 诊断不一致 更新
        Map<String, HisRecipe> hisRecipeMap = updateHisRecipe(hisRecipeTO, recipeList, hisRecipeList);

        /**判断处方是否删除*/
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).distinct().collect(Collectors.toList());
        List<HisRecipeDetail> hisRecipeDetailList = hisRecipeDetailDAO.findByHisRecipeIds(hisRecipeIds);
        LOGGER.info("hisRecipeInfoCheck hisRecipeDetailList = {}", JSONUtils.toString(hisRecipeDetailList));
        if (CollectionUtils.isEmpty(hisRecipeDetailList)) {
            return;
        }
        //1 判断是否delete 处方相关表 / RecipeDetailTO 数量 ，药品，开药总数
        Set<String> deleteSetRecipeCode = new HashSet<>();
        Map<Integer, List<HisRecipeDetail>> hisRecipeIdDetailMap = hisRecipeDetailList.stream().collect(Collectors.groupingBy(HisRecipeDetail::getHisRecipeId));
        hisRecipeTO.forEach(a -> {
            String recipeCode = a.getRecipeCode();
            HisRecipe hisRecipe = hisRecipeMap.get(recipeCode);
            if (null == hisRecipe) {
                return;
            }
            List<HisRecipeDetail> hisDetailList = hisRecipeIdDetailMap.get(hisRecipe.getHisRecipeID());
            if (CollectionUtils.isEmpty(a.getDrugList()) || CollectionUtils.isEmpty(hisDetailList)) {
                deleteSetRecipeCode.add(recipeCode);
                return;
            }
            if (a.getDrugList().size() != hisDetailList.size()) {
                deleteSetRecipeCode.add(recipeCode);
                return;
            }
            Map<String, BigDecimal> drugTotalDoseMap = hisDetailList.stream().collect(Collectors.toMap(HisRecipeDetail::getDrugCode, HisRecipeDetail::getUseTotalDose));
            for (RecipeDetailTO recipeDetailTO : a.getDrugList()) {
                BigDecimal useTotalDose = drugTotalDoseMap.get(recipeDetailTO.getDrugCode());
                if (null == useTotalDose || 0 != useTotalDose.compareTo(recipeDetailTO.getUseTotalDose())) {
                    deleteSetRecipeCode.add(recipeCode);
                    break;
                }
            }
        });
        //删除
        deleteSetRecipeCode(clinicOrgan, deleteSetRecipeCode);
    }

    /**
     * 删除线下处方相关数据
     *
     * @param clinicOrgan         机构id
     * @param deleteSetRecipeCode 要删除的
     */
    private void deleteSetRecipeCode(Integer clinicOrgan, Set<String> deleteSetRecipeCode) {
        LOGGER.info("deleteSetRecipeCode clinicOrgan = {},deleteSetRecipeCode = {}", clinicOrgan, JSONUtils.toString(deleteSetRecipeCode));
        if (CollectionUtils.isEmpty(deleteSetRecipeCode)) {
            return;
        }
        List<String> recipeCodeList = new ArrayList<>(deleteSetRecipeCode);
        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).collect(Collectors.toList());
        hisRecipeExtDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDetailDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDAO.deleteByHisRecipeIds(hisRecipeIds);
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(orderCodeList)) {
            recipeOrderDAO.deleteByRecipeIds(orderCodeList);
        }
        recipeExtendDAO.deleteByRecipeIds(recipeIds);
        recipeDetailDAO.deleteByRecipeIds(recipeIds);
        recipeDAO.deleteByRecipeIds(recipeIds);
        LOGGER.info("deleteSetRecipeCode is delete end ");
    }

    /**
     * 更新诊断字段
     *
     * @param hisRecipeTO
     * @param recipeList
     * @param hisRecipeList
     */
    private Map<String, HisRecipe> updateHisRecipe(List<QueryHisRecipResTO> hisRecipeTO, List<Recipe> recipeList, List<HisRecipe> hisRecipeList) {
        Map<String, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        Map<String, HisRecipe> hisRecipeMap = hisRecipeList.stream().collect(Collectors.toMap(HisRecipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        hisRecipeTO.forEach(a -> {
            String disease = null != a.getDisease() ? a.getDisease() : "";
            String diseaseName = null != a.getDiseaseName() ? a.getDiseaseName() : "";
            Recipe recipe = recipeMap.get(a.getRecipeCode());
            if (null != recipe) {
                if (!disease.equals(recipe.getOrganDiseaseId()) || !diseaseName.equals(recipe.getOrganDiseaseName())) {
                    recipe.setOrganDiseaseId(disease);
                    recipe.setOrganDiseaseName(diseaseName);
                    recipeDAO.update(recipe);
                    LOGGER.info("updateHisRecipe recipe = {}", JSONUtils.toString(recipe));
                }
            }
            HisRecipe hisRecipe = hisRecipeMap.get(a.getRecipeCode());
            if (null != hisRecipe) {
                if (!disease.equals(hisRecipe.getDisease()) || !diseaseName.equals(hisRecipe.getDiseaseName())) {
                    hisRecipe.setDisease(disease);
                    hisRecipe.setDiseaseName(diseaseName);
                    hisRecipeDAO.update(hisRecipe);
                    LOGGER.info("updateHisRecipe hisRecipe = {}", JSONUtils.toString(hisRecipe));
                }
            }
        });
        return hisRecipeMap;
    }

}
