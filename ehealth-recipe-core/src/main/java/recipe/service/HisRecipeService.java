package recipe.service;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.thread.QueryHisRecipeCallable;
import recipe.thread.RecipeBusiThreadPool;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 19:58
 */
@RpcBean("hisRecipeService")
public class HisRecipeService {
    private static final Log LOGGER = LogFactory.getLog(HisRecipeService.class);

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
     * @param request
     * @return
     */
    @RpcService
    public List<HisRecipeVO>  findHisRecipe(Map<String, Object> request){
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
        if(null == patientDTO){
            throw new DAOException(609,"患者信息不存在");
        }
       //查询his线下处方数据
        //同步查询待缴费处方
        try {
            HisResponseTO<List<QueryHisRecipResTO>> responseTO = queryHisRecipeInfo(organId, patientDTO, timeQuantum, 1);
            if (null != responseTO) {
                if (null != responseTO.getData()) {
                    saveHisRecipeInfo(responseTO,patientDTO,1);
                }
            }
        }catch (Exception ex){
            LOGGER.error("queryHisRecipeInfo error:",ex);
        }
        //异步获取已缴费处方
        QueryHisRecipeCallable callable = new QueryHisRecipeCallable(organId,mpiId,timeQuantum,2,patientDTO);
        RecipeBusiThreadPool.submit(callable);

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

    @RpcService
    public HisResponseTO<List<QueryHisRecipResTO>> queryHisRecipeInfo(Integer organId,PatientDTO patientDTO,Integer timeQuantum,Integer flag){
        IRecipeHisService recipeHisService = AppContextHolder.getBean("his.iRecipeHisService",IRecipeHisService.class);
        QueryRecipeRequestTO queryRecipeRequestTO = new QueryRecipeRequestTO();
        Date startDate = tranDateByFlagNew(timeQuantum.toString());
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setBirthday(patientDTO.getBirthday());
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientSex(patientDTO.getPatientSex());
        patientBaseInfo.setMobile(patientDTO.getMobile());
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());
        queryRecipeRequestTO.setStartDate(startDate);
        queryRecipeRequestTO.setEndDate(new Date());
        queryRecipeRequestTO.setOrgan(organId);
        queryRecipeRequestTO.setQueryType(flag);
        queryRecipeRequestTO.setPatientInfo(patientBaseInfo);
        LOGGER.info("queryHisRecipeInfo input:" + JSONUtils.toString(queryRecipeRequestTO,QueryRecipeRequestTO.class));
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = recipeHisService.queryHisRecipeInfo(queryRecipeRequestTO);
        LOGGER.info("queryHisRecipeInfo output:" + JSONUtils.toString(responseTO,HisResponseTO.class));
        return responseTO;
    }

    @RpcService
    public void saveHisRecipeInfo(HisResponseTO<List<QueryHisRecipResTO>> responseTO,PatientDTO patientDTO,Integer flag){
        List<QueryHisRecipResTO> queryHisRecipResTOList = responseTO.getData();
        for(QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOList){
            HisRecipe hisRecipe1 = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(
                    patientDTO.getMpiId(),queryHisRecipResTO.getClinicOrgan(),queryHisRecipResTO.getRecipeCode());
            //数据库不存在处方信息，则新增
            if(null == hisRecipe1) {
                HisRecipe hisRecipe = new HisRecipe();
//                hisRecipe = ObjectCopyUtils.convert(queryHisRecipResTO, HisRecipe.class);
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
                hisRecipe.setExtensionFlag(1);
                hisRecipe.setMedicalType(1);
                hisRecipe.setRecipeFee(queryHisRecipResTO.getRecipeFee());
                hisRecipe.setRecipeType(queryHisRecipResTO.getRecipeType());
                hisRecipe.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
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
                        detail.setDrugSpec(recipeDetailTO.getDrugSpec());
                        detail.setDrugUnit(recipeDetailTO.getDrugUnit());
                        detail.setUseDays(recipeDetailTO.getUseDays());
                        detail.setDrugCode(recipeDetailTO.getDrugCode());
                        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                        if (StringUtils.isNotEmpty(detail.getRecipeDeatilCode())) {
                            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(detail.getDrugCode()));
                            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                                OrganDrugList organDrugList = organDrugLists.get(0);
                                detail.setDrugName(organDrugList.getDrugName());
                                detail.setSaleName(organDrugList.getSaleName());
                                detail.setPack(organDrugList.getPack());
                                detail.setPrice(organDrugList.getSalePrice());
                                detail.setUsingRate(organDrugList.getUsingRate());
                                detail.setUsePathways(organDrugList.getUsePathways());
                            } else {
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
                        hisRecipe1.setStatus(2);
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
            if (employmentDTO != null) {
                recipe.setDoctor(employmentDTO.getDoctorId());
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
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(hisRecipeDetail.getDrugCode()));
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setRecipeId(recipeId);
            recipedetail.setDrugName(hisRecipeDetail.getDrugName());
            recipedetail.setDrugSpec(hisRecipeDetail.getDrugSpec());
            recipedetail.setDrugUnit(hisRecipeDetail.getDrugUnit());
            recipedetail.setPack(hisRecipeDetail.getPack());
            recipedetail.setOrganDrugCode(hisRecipeDetail.getDrugCode());
            if (StringUtils.isNotEmpty(hisRecipeDetail.getUseDose())) {
                recipedetail.setUseDose(Double.parseDouble(hisRecipeDetail.getUseDose()));
            }
            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                recipedetail.setDrugId(organDrugLists.get(0).getDrugId());
            }
            recipedetail.setUsingRate(hisRecipeDetail.getUsingRate());
            recipedetail.setUsePathways(hisRecipeDetail.getUsePathways());
            if (hisRecipeDetail.getUseTotalDose() != null) {
                recipedetail.setUseTotalDose(hisRecipeDetail.getUseTotalDose().doubleValue());
            }
            recipedetail.setUseDays(hisRecipeDetail.getUseDays());
            recipedetail.setStatus(1);
            recipedetail.setSalePrice(hisRecipeDetail.getPrice());
            if (hisRecipeDetail.getUseTotalDose() != null && hisRecipeDetail.getPrice() != null) {
                recipedetail.setDrugCost(hisRecipeDetail.getUseTotalDose().multiply(hisRecipeDetail.getPrice()));
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
            case RecipeStatusConstant.CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "待处理";
                } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
                    tips = "待取药";
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
    public Integer getCardType(Integer organId){
        //卡类型 1 表示身份证  2 表示就诊卡
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (Integer)configurationCenterUtilsService.getConfiguration(organId, "getCardTypeForHis");
    }
}
