package recipe.service;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.doctor.model.RelationDoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.operationrecords.model.OperationRecordsBean;
import com.ngari.base.operationrecords.service.IOperationRecordsService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.sysparamter.service.ISysParamterService;
import com.ngari.bus.consult.model.ConsultBean;
import com.ngari.bus.consult.model.ConsultSetBean;
import com.ngari.bus.consult.model.RecipeTagMsgBean;
import com.ngari.bus.consult.service.IConsultService;
import com.ngari.recipe.entity.*;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.RecipeValidateUtil;
import recipe.constant.*;
import recipe.dao.*;
import recipe.util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 供recipeService调用
 * @author liuya
 */
public class RecipeServiceSub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceSub.class);

    private static final String UNSIGN = "unsign";

    private static final String UNCHECK = "uncheck";

    private static IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

    private static IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);

    private static IOrganService iOrganService = ApplicationUtils.getBaseService(IOrganService.class);

    private static ISysParamterService iSysParamterService = ApplicationUtils.getBaseService(ISysParamterService.class);

    /**
     * @param recipe
     * @param details
     * @param flag(recipe的fromflag) 0：HIS处方  1：平台处方
     * @return
     */
    public static Integer saveRecipeDataImpl(Recipe recipe, List<Recipedetail> details, Integer flag) {
        if (null != recipe && recipe.getRecipeId() != null && recipe.getRecipeId() > 0) {
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            return recipeService.updateRecipeAndDetail(recipe, details);
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        IOperationRecordsService iOperationRecordsService = ApplicationUtils.getBaseService(IOperationRecordsService.class);
        if(null == recipe){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is required!");
        }
        RecipeValidateUtil.validateSaveRecipeData(recipe);
        RecipeUtil.setDefaultData(recipe);

        if (null == details) {
            details = new ArrayList<>(0);
        }
        for (Recipedetail recipeDetail : details) {
            RecipeValidateUtil.validateRecipeDetailData(recipeDetail, recipe);
        }

        if (1 == flag) {
            boolean isSucc = setDetailsInfo(recipe, details);
            if (!isSucc) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "药品详情数据有误");
            }
        } else if (0 == flag) {
            //处方总价未计算
            BigDecimal totalMoney = new BigDecimal(0d);
            for (Recipedetail detail : details) {
                if (null != detail.getDrugCost()) {
                    totalMoney = totalMoney.add(detail.getDrugCost());
                }
            }
            recipe.setTotalMoney(totalMoney);
            recipe.setActualPrice(totalMoney);
        }
        String mpiId = recipe.getMpiid();
        if (StringUtils.isEmpty(mpiId)) {
            throw new DAOException(DAOException.VALUE_NEEDED,
                    "mpiId is required");
        }

        String patientName = iPatientService.getNameByMpiId(mpiId);
        if (StringUtils.isEmpty(patientName)) {
            patientName = "未知";
        }
        recipe.setPatientName(patientName);
        recipe.setDoctorName(iDoctorService.getNameById(recipe.getDoctor()));
        OrganBean organBean = iOrganService.get(recipe.getClinicOrgan());
        recipe.setOrganName(organBean.getShortName());

        Integer recipeId = recipeDAO.updateOrSaveRecipeAndDetail(recipe, details, false);
        recipe.setRecipeId(recipeId);

        //加入历史患者
        OperationRecordsBean record = new OperationRecordsBean();


        record.setMpiId(mpiId);
        record.setRequestMpiId(mpiId);
        record.setPatientName(patientName);
        record.setBussType(BussTypeConstant.RECIPE);
        record.setBussId(recipe.getRecipeId());
        record.setRequestDoctor(recipe.getDoctor());
        record.setExeDoctor(recipe.getDoctor());
        record.setRequestTime(recipe.getCreateDate());
        iOperationRecordsService.saveOperationRecordsForRecipe(record);

        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "暂存处方单");
        return recipeId;
    }


    /**
     * 设置药品详情数据
     *
     * @param recipe        处方
     * @param recipedetails 处方ID
     */
    public static boolean setDetailsInfo(Recipe recipe, List<Recipedetail> recipedetails) {
        boolean success = false;
        int organId = recipe.getClinicOrgan();
        //药品总金额
        BigDecimal totalMoney = new BigDecimal(0d);
        List<Integer> drugIds = new ArrayList<>(0);
        Date nowDate = DateTime.now().toDate();
        for (Recipedetail detail : recipedetails) {
            //设置药品详情基础数据
            detail.setStatus(1);
            detail.setRecipeId(recipe.getRecipeId());
            detail.setCreateDt(nowDate);
            detail.setLastModify(nowDate);
            if (null != detail.getDrugId()) {
                drugIds.add(detail.getDrugId());
            }
        }

        if (CollectionUtils.isNotEmpty(drugIds)) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);

            List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(organId, drugIds);
            List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
            if (CollectionUtils.isNotEmpty(organDrugList) && CollectionUtils.isNotEmpty(drugList)) {
                Map<Integer, OrganDrugList> organDrugListMap = Maps.newHashMap();
                Map<Integer, DrugList> drugListMap = Maps.newHashMap();
                for (DrugList obj : drugList) {
                    drugListMap.put(obj.getDrugId(), obj);
                }

                int takeMedicineSize = 0;
                List<String> takeOutDrugName = Lists.newArrayList();
                for (OrganDrugList obj : organDrugList) {
                    //检验是否都为外带药
                    if (Integer.valueOf(1).equals(obj.getTakeMedicine())) {
                        takeMedicineSize++;
                        takeOutDrugName.add(drugListMap.get(obj.getDrugId()).getDrugName());
                    }
                    organDrugListMap.put(obj.getDrugId(), obj);
                }

                if (takeMedicineSize > 0) {
                    if (takeMedicineSize != organDrugList.size()) {
                        String errorDrugName = Joiner.on(",").join(takeOutDrugName);
                        //外带药和处方药混合开具是不允许的
                        LOGGER.error("setDetailsInfo 存在外带药且混合开具. recipeId=[{}], drugIds={}, 外带药={}", recipe.getRecipeId(),
                                JSONUtils.toString(drugIds), errorDrugName);
                        throw new DAOException(ErrorCode.SERVICE_ERROR, errorDrugName+"不能开具在一张处方上");
                    } else {
                        //外带处方， 同时也设置成只能配送处方
                        recipe.setTakeMedicine(1);
                        recipe.setDistributionFlag(1);
                    }
                }

                //供应商一致性校验，取第一个药品能配送的药企作为标准
                Map<Integer, List<String>> drugDepRel = saleDrugListDAO.findDrugDepRelation(drugIds);
                //无法配送药品校验
                List<String> noFilterDrugName = new ArrayList<>();
                for(Integer drugId : drugIds){
                    if(CollectionUtils.isEmpty(drugDepRel.get(drugId))){
                        noFilterDrugName.add(drugListMap.get(drugId).getDrugName());
                    }
                }
                if(CollectionUtils.isNotEmpty(noFilterDrugName)){
                    LOGGER.error("setDetailsInfo 存在无法配送的药品. recipeId=[{}], drugIds={}, noFilterDrugName={}",
                            recipe.getRecipeId(), JSONUtils.toString(drugIds), JSONUtils.toString(noFilterDrugName));
                    throw new DAOException(ErrorCode.SERVICE_ERROR, Joiner.on(",").join(noFilterDrugName)+"无法配送！");
                }

                noFilterDrugName.clear();
                List<String> firstDrugDepIds = drugDepRel.get(drugIds.get(0));
                for(Integer drugId : drugDepRel.keySet()){
                    List<String> depIds = drugDepRel.get(drugId);
                    boolean filterFlag = false;
                    for(String depId : depIds){
                        //匹配到一个药企相同则可跳过
                        if(firstDrugDepIds.contains(depId)){
                            filterFlag = true;
                            break;
                        }
                    }
                    if(!filterFlag){
                        noFilterDrugName.add(drugListMap.get(drugId).getDrugName());
                    }else{
                        firstDrugDepIds.retainAll(depIds);
                    }
                }
                if(CollectionUtils.isNotEmpty(noFilterDrugName)){
                    LOGGER.error("setDetailsInfo 存在无法一起配送的药品. recipeId=[{}], drugIds={}, noFilterDrugName={}",
                            recipe.getRecipeId(), JSONUtils.toString(drugIds), JSONUtils.toString(noFilterDrugName));
                    throw new DAOException(ErrorCode.SERVICE_ERROR, Joiner.on(",").join(noFilterDrugName)+"不能开具在一张处方上！");
                }

                for (Recipedetail detail : recipedetails) {
                    //设置药品基础数据
                    DrugList drug = drugListMap.get(detail.getDrugId());
                    if (null != drug) {
                        detail.setDrugName(drug.getDrugName());
                        detail.setDrugSpec(drug.getDrugSpec());
                        detail.setDrugUnit(drug.getUnit());
                        detail.setDefaultUseDose(drug.getUseDose());
                        detail.setUseDoseUnit(drug.getUseDoseUnit());
                        detail.setDosageUnit(drug.getUseDoseUnit());
                        //设置药品包装数量
                        detail.setPack(drug.getPack());
                        //中药基础数据处理
                        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                            detail.setUsePathways(recipe.getTcmUsePathways());
                            detail.setUsingRate(recipe.getTcmUsingRate());
                            detail.setUseDays(recipe.getCopyNum());
                            detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                        } else if (RecipeBussConstant.RECIPETYPE_HP.equals(recipe.getRecipeType())) {
                            detail.setUseDays(recipe.getCopyNum());
                            detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                        }
                    }

                    //设置药品价格
                    OrganDrugList organDrug = organDrugListMap.get(detail.getDrugId());
                    if (null != organDrug) {
                        detail.setOrganDrugCode(organDrug.getOrganDrugCode());
                        BigDecimal price = organDrug.getSalePrice();
                        if (null == price) {
                            LOGGER.error("setDetailsInfo 药品ID：" + drug.getDrugId() + " 在医院(ID为" + organId + ")的价格为NULL！");
                            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品数据异常！");
                        }
                        detail.setSalePrice(price);
                        //保留3位小数
                        BigDecimal drugCost = price.multiply(new BigDecimal(detail.getUseTotalDose()))
                                .divide(BigDecimal.ONE, 3, RoundingMode.UP);
                        detail.setDrugCost(drugCost);
                        totalMoney = totalMoney.add(drugCost);
                    }
                }
                success = true;
            } else {
                LOGGER.error("setDetailsInfo organDrugList或者drugList为空. recipeId=[{}], drugIds={}", recipe.getRecipeId(), JSONUtils.toString(drugIds));
            }
        } else {
            LOGGER.error("setDetailsInfo 详情里没有药品ID. recipeId=[{}]", recipe.getRecipeId());
        }

        recipe.setTotalMoney(totalMoney);
        recipe.setActualPrice(totalMoney);
        return success;
    }

    /**
     * 组装生成pdf的参数集合
     * zhongzx
     *
     * @param recipe  处方对象
     * @param details 处方详情
     * @return Map<String, Object>
     */
    public static Map<String, Object> createParamMap(Recipe recipe, List<Recipedetail> details, String fileName) {
        DrugListDAO dDao = DAOFactory.getDAO(DrugListDAO.class);
        Map<String, Object> paramMap = Maps.newHashMap();
        try {
            PatientBean p = iPatientService.get(recipe.getMpiid());
            if (null == p) {
                LOGGER.error("createParamMap 病人不存在. recipeId={}, mpiId={}", recipe.getRecipeId(), recipe.getMpiid());
                return paramMap;
            }
            //模板类型，西药模板
            paramMap.put("templateType", "wm");
            //生成pdf文件的入参
            paramMap.put("fileName", fileName);
            paramMap.put("recipeType", recipe.getRecipeType());
            String recipeType = DictionaryController.instance().get("eh.cdr.dictionary.RecipeType").getText(recipe.getRecipeType());
            paramMap.put("title", recipeType + "处方笺");
            paramMap.put("pName", p.getPatientName());
            paramMap.put("pGender", DictionaryController.instance().get("eh.base.dictionary.Gender").getText(p.getPatientSex()));
            paramMap.put("pAge", DateConversion.getAge(p.getBirthday()) + "岁");
            paramMap.put("pType", DictionaryController.instance().get("eh.mpi.dictionary.PatientType").getText(p.getPatientType()));
            paramMap.put("doctor", DictionaryController.instance().get("eh.base.dictionary.Doctor").getText(recipe.getDoctor()));
            String organ = DictionaryController.instance().get("eh.base.dictionary.Organ").getText(recipe.getClinicOrgan());
            String depart = DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart());
            paramMap.put("organInfo", organ);
            paramMap.put("departInfo", depart);
            paramMap.put("disease", recipe.getOrganDiseaseName());
            paramMap.put("cDate", DateConversion.getDateFormatter(recipe.getSignDate(), "yyyy-MM-dd HH:mm"));
            paramMap.put("diseaseMemo", recipe.getMemo());
            paramMap.put("recipeCode", recipe.getRecipeCode().startsWith("ngari") ? "" : recipe.getRecipeCode());
            paramMap.put("patientId", recipe.getPatientID());
            paramMap.put("mobile", p.getMobile());
            paramMap.put("label", recipeType + "处方");
            int i = 0;
            List<Integer> drugIds = Lists.newArrayList();
            for (Recipedetail d : details) {
                drugIds.add(d.getDrugId());
            }
            List<DrugList> dlist = dDao.findByDrugIds(drugIds);
            Map<Integer, DrugList> dMap = Maps.newHashMap();
            for (DrugList d : dlist) {
                dMap.put(d.getDrugId(), d);
            }
            ctd.dictionary.Dictionary usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
            Dictionary usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
            for (Recipedetail d : details) {
                DrugList drug = dMap.get(d.getDrugId());
                String dName = (i + 1) + "、" + drug.getDrugName();
                //规格+药品单位
                String dSpec = drug.getDrugSpec() + "/" + drug.getUnit();
                //使用天数
                String useDay = d.getUseDays() + "天";
                //每次剂量+剂量单位
                String uDose = "Sig: " + "每次" + d.getUseDose() + drug.getUseDoseUnit();
                //开药总量+药品单位
                String dTotal = "X" + d.getUseTotalDose() + drug.getUnit();
                //用药频次
                String dRateName = d.getUsingRate() + "(" + usingRateDic.getText(d.getUsingRate()) + ")";
                //用法
                String dWay = d.getUsePathways() + "(" + usePathwaysDic.getText(d.getUsePathways()) + ")";
                paramMap.put("drugInfo" + i, dName + dSpec);
                paramMap.put("dTotal" + i, dTotal);
                paramMap.put("useInfo" + i, uDose + "    " + dRateName + "    " + dWay + "    " + useDay);
                if (!StringUtils.isEmpty(d.getMemo())) {
                    //备注
                    paramMap.put("dMemo" + i, "备注:" + d.getMemo());
                }
                i++;
            }
        } catch (Exception e) {
            LOGGER.error("createParamMap 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }
        return paramMap;
    }

    /**
     * 中药处方pdf模板
     *
     * @param recipe
     * @param details
     * @param fileName
     * @return
     * @Author liuya
     */
    public static Map<String, Object> createParamMapForChineseMedicine(Recipe recipe, List<Recipedetail> details, String fileName) {
        DrugListDAO dDao = DAOFactory.getDAO(DrugListDAO.class);
        Map<String, Object> paramMap = Maps.newHashMap();
        try {
            PatientBean p = iPatientService.get(recipe.getMpiid());
            if (null == p) {
                LOGGER.error("createParamMapForChineseMedicine 病人不存在. recipeId={}, mpiId={}", recipe.getRecipeId(), recipe.getMpiid());
                return paramMap;
            }
            //模板类型，中药类模板
            paramMap.put("templateType", "tcm");
            //生成pdf文件的入参
            paramMap.put("fileName", fileName);
            paramMap.put("recipeType", recipe.getRecipeType());
            String recipeType = DictionaryController.instance().get("eh.cdr.dictionary.RecipeType").getText(recipe.getRecipeType());
            paramMap.put("title", recipeType + "处方笺");
            paramMap.put("pName", p.getPatientName());
            paramMap.put("pGender", DictionaryController.instance().get("eh.base.dictionary.Gender").getText(p.getPatientSex()));
            paramMap.put("pAge", DateConversion.getAge(p.getBirthday()) + "岁");
            paramMap.put("pType", DictionaryController.instance().get("eh.mpi.dictionary.PatientType").getText(p.getPatientType()));
            paramMap.put("doctor", DictionaryController.instance().get("eh.base.dictionary.Doctor").getText(recipe.getDoctor()));
            String organ = DictionaryController.instance().get("eh.base.dictionary.Organ").getText(recipe.getClinicOrgan());
            String depart = DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart());
            paramMap.put("organInfo", organ);
            paramMap.put("departInfo", depart);
            paramMap.put("disease", recipe.getOrganDiseaseName());
            paramMap.put("cDate", DateConversion.getDateFormatter(recipe.getSignDate(), "yyyy-MM-dd HH:mm"));
            paramMap.put("diseaseMemo", recipe.getMemo());
            paramMap.put("recipeCode", recipe.getRecipeCode().startsWith("ngari") ? "" : recipe.getRecipeCode());
            paramMap.put("patientId", recipe.getPatientID());
            paramMap.put("mobile", p.getMobile());
            paramMap.put("label", recipeType + "处方");
            paramMap.put("copyNum", recipe.getCopyNum() + "剂");
            paramMap.put("recipeMemo", recipe.getRecipeMemo());
            int i = 0;
            List<Integer> drugIds = Lists.newArrayList();
            for (Recipedetail d : details) {
                drugIds.add(d.getDrugId());
            }
            List<DrugList> dlist = dDao.findByDrugIds(drugIds);
            Map<Integer, DrugList> dMap = Maps.newHashMap();
            for (DrugList d : dlist) {
                dMap.put(d.getDrugId(), d);
            }
            for (Recipedetail d : details) {
                DrugList drug = dMap.get(d.getDrugId());
                String dName = drug.getDrugName();
                //开药总量+药品单位
                String dTotal = "";
                //增加判断条件  如果用量小数位为零，则不显示小数点
                if ((d.getUseDose() - d.getUseDose().intValue()) == 0d) {
                    dTotal = d.getUseDose().intValue() + drug.getUseDoseUnit();
                } else {
                    dTotal = d.getUseDose() + drug.getUseDoseUnit();
                }
                if (!StringUtils.isEmpty(d.getMemo())) {
                    //备注
                    dTotal = dTotal + "*" + d.getMemo();
                }
                paramMap.put("drugInfo" + i, dName + "¨" + dTotal);
                paramMap.put("tcmUsePathways", d.getUsePathways());
                paramMap.put("tcmUsingRate", d.getUsingRate());
                i++;
            }
        } catch (Exception e) {
            LOGGER.error("createParamMapForChineseMedicine 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }
        return paramMap;
    }

    /**
     * 处方列表服务
     *
     * @param doctorId 开方医生
     * @param start    分页开始位置
     * @param limit    每页限制条数
     * @param mark     标志 --0新处方1历史处方
     * @return List
     */
    public static List<HashMap<String, Object>> findRecipesAndPatientsByDoctor(
            final int doctorId, final int start, final int limit, final int mark) {
        if (0 == limit) {
            return null;
        }

        List<Recipe> recipes;
        // 是否含有未签名的数据
        boolean hasUnsignRecipe = false;
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        if (0 == mark) {
            recipes = new ArrayList<>(0);
            int endIndex = start + limit;
            //先查询未签名处方的数量
            int unsignCount = recipeDAO.getCountByDoctorIdAndStatus(doctorId,
                    Arrays.asList(RecipeStatusConstant.CHECK_NOT_PASS, RecipeStatusConstant.UNSIGN), ConditionOperator.IN, false);
            //查询未签名的处方数据
            if (unsignCount > start) {
                hasUnsignRecipe = true;
                List<Recipe> unsignRecipes = recipeDAO.findByDoctorIdAndStatus(doctorId,
                        Arrays.asList(RecipeStatusConstant.CHECK_NOT_PASS, RecipeStatusConstant.UNSIGN), ConditionOperator.IN, false, start, limit, mark);
                if (null != unsignRecipes && !unsignRecipes.isEmpty()) {
                    recipes.addAll(unsignRecipes);
                }

                //当前页的数据未签名的数据无法充满则需要查询未审核的数据
                if (unsignCount < endIndex) {
                    List<Recipe> uncheckRecipes = recipeDAO.findByDoctorIdAndStatus(doctorId,
                            Collections.singletonList(RecipeStatusConstant.UNCHECK), ConditionOperator.EQUAL, false, 0, limit - recipes.size(), mark);
                    if (null != uncheckRecipes && !uncheckRecipes.isEmpty()) {
                        recipes.addAll(uncheckRecipes);
                    }
                }
            } else {
                //未签名的数据已经全部显示
                int startIndex = start - unsignCount;
                List<Recipe> uncheckRecipes = recipeDAO.findByDoctorIdAndStatus(doctorId,
                        Collections.singletonList(RecipeStatusConstant.UNCHECK), ConditionOperator.EQUAL, false, startIndex, limit, mark);
                if (null != uncheckRecipes && !uncheckRecipes.isEmpty()) {
                    recipes.addAll(uncheckRecipes);
                }
            }
        } else {
            //历史处方数据
            recipes = recipeDAO.findByDoctorIdAndStatus(doctorId,
                    Collections.singletonList(RecipeStatusConstant.CHECK_PASS), ConditionOperator.GREAT_EQUAL, false, start, limit, mark);
        }

        List<String> patientIds = new ArrayList<>(0);
        Map<Integer, Recipe> recipeMap = Maps.newHashMap();
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        for (Recipe recipe : recipes) {
            if (StringUtils.isNotEmpty(recipe.getMpiid())) {
                patientIds.add(recipe.getMpiid());
            }
            //设置处方具体药品名称
            recipe.setRecipeDrugName(recipeDetailDAO.getDrugNamesByRecipeId(recipe.getRecipeId()));
            //前台页面展示的时间源不同
            if (0 == mark) {
                if (null != recipe.getLastModify()) {
                    recipe.setRecipeShowTime(recipe.getLastModify());
                }
            } else {
                if (null != recipe.getSignDate()) {
                    recipe.setRecipeShowTime(recipe.getSignDate());
                }
            }
            boolean effective = false;
            //只有审核未通过的情况需要看订单状态
            if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                effective = orderDAO.isEffectiveOrder(recipe.getOrderCode(), recipe.getPayMode());
            }
            Map<String, String> tipMap = getTipsByStatus(recipe.getStatus(), recipe, effective);
            recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
            recipeMap.put(recipe.getRecipeId(), convertRecipeForRAP(recipe));
        }

        List<PatientBean> patientList = null;
        if (!patientIds.isEmpty()) {
            patientList = iPatientService.findByMpiIdIn(patientIds);
        }
        Map<String, PatientBean> patientMap = Maps.newHashMap();
        if (null != patientList && !patientList.isEmpty()) {
            for (PatientBean patient : patientList) {
                //设置患者数据
                setPatientMoreInfo(patient, doctorId);
                patientMap.put(patient.getMpiId(), convertPatientForRAP(patient));
            }
        }

        List<HashMap<String, Object>> list = new ArrayList<>(0);
        List<HashMap<String, Object>> unsignMapList = new ArrayList<>(0);
        List<HashMap<String, Object>> uncheckMapList = new ArrayList<>(0);
        for (Recipe recipe : recipes) {
            //对处方数据进行分类
            String mpiid = recipe.getMpiid();
            HashMap<String, Object> map = Maps.newHashMap();
            map.put("recipe", recipeMap.get(recipe.getRecipeId()));
            map.put("patient", patientMap.get(mpiid));

            //新开处方与历史处方JSON结构不同
            if (0 == mark) {
                if (hasUnsignRecipe) {
                    if (recipe.getStatus() <= RecipeStatusConstant.UNSIGN) {
                        //未签名处方
                        unsignMapList.add(map);
                    } else if (RecipeStatusConstant.UNCHECK == recipe.getStatus()) {
                        //未审核处方
                        uncheckMapList.add(map);
                    }
                } else {
                    uncheckMapList.add(map);
                }
            } else {
                list.add(map);
            }
        }

        if (!unsignMapList.isEmpty()) {
            HashMap<String, Object> map = Maps.newHashMap();
            map.put(UNSIGN, unsignMapList);
            list.add(map);
        }

        if (!uncheckMapList.isEmpty()) {
            HashMap<String, Object> map = Maps.newHashMap();
            map.put(UNCHECK, uncheckMapList);
            list.add(map);
        }

        return list;
    }

    /**
     * 状态文字提示（医生端）
     *
     * @param status
     * @param recipe
     * @param effective
     * @return
     */
    public static Map<String, String> getTipsByStatus(int status, Recipe recipe, boolean effective) {
        String cancelReason = "";
        String tips = "";
        String listTips = "";
        switch (status) {
            case RecipeStatusConstant.CHECK_NOT_PASS:
                tips = "审核未通过";
                break;
            case RecipeStatusConstant.UNSIGN:
                tips = "未签名";
                break;
            case RecipeStatusConstant.UNCHECK:
                tips = "待审核";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                tips = "待处理";
                break;
            case RecipeStatusConstant.REVOKE:
                tips = "已取消";
                cancelReason = "由于您已撤销，该处方单已失效";
                break;
            case RecipeStatusConstant.HAVE_PAY:
                tips = "待取药";
                break;
            case RecipeStatusConstant.IN_SEND:
                tips = "配送中";
                break;
            case RecipeStatusConstant.WAIT_SEND:
                tips = "待配送";
                break;
            case RecipeStatusConstant.FINISH:
                tips = "已完成";
                break;
            case RecipeStatusConstant.CHECK_PASS_YS:
                if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                    tips = "医生再次确认处方";
                } else {
                    tips = "审核通过";
                }
                listTips = "审核通过";
                break;
            case RecipeStatusConstant.READY_CHECK_YS:
                tips = "待审核";
                break;
            case RecipeStatusConstant.HIS_FAIL:
                tips = "已取消";
                cancelReason = "可能由于医院接口异常，处方单已取消，请稍后重试！";
                break;
            case RecipeStatusConstant.NO_DRUG:
                tips = "已取消";
                cancelReason = "由于患者未及时取药，该处方单已失效";
                break;
            case RecipeStatusConstant.NO_PAY:
            case RecipeStatusConstant.NO_OPERATOR:
                tips = "已取消";
                cancelReason = "由于患者未及时支付，该处方单已取消。";
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                if (recipe.canMedicalPay()) {
                    tips = "审核未通过";
                } else {
                    if (effective) {
                        tips = "审核未通过";
                    } else {
                        tips = "已取消";
                    }
                }
                break;
            case RecipeStatusConstant.CHECKING_HOS:
                tips = "医院确认中";
                break;
            default:
                tips = "未知状态" + status;
        }
        if (StringUtils.isEmpty(listTips)) {
            listTips = tips;
        }
        Map<String, String> map = Maps.newHashMap();
        map.put("tips", tips);
        map.put("listTips", listTips);
        map.put("cancelReason", cancelReason);
        return map;
    }

    public static void setPatientMoreInfo(PatientBean patient, int doctorId) {
        RelationDoctorBean relationDoctor = iDoctorService.getByMpiidAndDoctorId(patient.getMpiId(), doctorId);
        //是否关注
        Boolean relationFlag = false;
        //是否签约
        Boolean signFlag = false;
        List<String> labelNames = Lists.newArrayList();
        if (relationDoctor != null) {
            relationFlag = true;
            if (relationDoctor.getFamilyDoctorFlag()) {
                signFlag = true;
            }

            labelNames = iPatientService.findLabelNamesByRPId(relationDoctor.getRelationDoctorId());

        }
        patient.setRelationFlag(relationFlag);
        patient.setSignFlag(signFlag);
        patient.setLabelNames(labelNames);
    }

    public static PatientBean convertPatientForRAP(PatientBean patient) {
        PatientBean p = new PatientBean();
        p.setPatientName(patient.getPatientName());
        p.setPatientSex(patient.getPatientSex());
        p.setBirthday(patient.getBirthday());
        p.setPatientType(patient.getPatientType());
        p.setIdcard(patient.getCertificate());
        p.setMobile(patient.getMobile());
        p.setMpiId(patient.getMpiId());
        p.setPhoto(patient.getPhoto());
        p.setSignFlag(patient.getSignFlag());
        p.setRelationFlag(patient.getRelationFlag());
        p.setLabelNames(patient.getLabelNames());
        return p;
    }

    public static Recipe convertRecipeForRAP(Recipe recipe) {
        Recipe r = new Recipe();
        r.setRecipeId(recipe.getRecipeId());
        r.setCreateDate(recipe.getCreateDate());
        r.setRecipeType(recipe.getRecipeType());
        r.setStatus(recipe.getStatus());
        r.setOrganDiseaseName(recipe.getOrganDiseaseName());
        r.setRecipeDrugName(recipe.getRecipeDrugName());
        r.setRecipeShowTime(recipe.getRecipeShowTime());
        r.setShowTip(recipe.getShowTip());
        return r;
    }

    /**
     * 获取处方详情
     *
     * @param recipeId
     * @param isDoctor true:医生端  false:健康端
     * @return
     */
    public static HashMap<String, Object> getRecipeAndDetailByIdImpl(int recipeId, boolean isDoctor) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        HashMap<String, Object> map = Maps.newHashMap();
        if (recipe == null) {
            return map;
        }
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        PatientBean patientBean = iPatientService.get(recipe.getMpiid());
        PatientBean patient = null;
        if (patientBean != null) {
            //添加患者标签和关注这些字段
            RecipeServiceSub.setPatientMoreInfo(patientBean, recipe.getDoctor());
            patient = RecipeServiceSub.convertPatientForRAP(patientBean);
        }
        List<Recipedetail> recipedetails = detailDAO.findByRecipeId(recipeId);


        //中药处方处理
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
            if (CollectionUtils.isNotEmpty(recipedetails)) {
                Recipedetail recipedetail = recipedetails.get(0);
                recipe.setTcmUsePathways(recipedetail.getUsePathways());
                recipe.setTcmUsingRate(recipedetail.getUsingRate());
            }
        }
        map.put("patient", patient);
        map.put("recipedetails", recipedetails);
        if (isDoctor) {
            IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
            IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);

            // 获取处方单药品总价
            RecipeUtil.getRecipeTotalPriceRange(recipe, recipedetails);
            boolean effective = orderDAO.isEffectiveOrder(recipe.getOrderCode(), recipe.getPayMode());
            Map<String, String> tipMap = RecipeServiceSub.getTipsByStatus(recipe.getStatus(), recipe, effective);
            map.put("tips", MapValueUtil.getString(tipMap, "tips"));
            map.put("cancelReason", MapValueUtil.getString(tipMap, "cancelReason"));
            RecipeCheckService service = ApplicationUtils.getRecipeService(RecipeCheckService.class);
            //获取审核不通过详情
            List<Map<String, Object>> mapList = service.getCheckNotPassDetail(recipeId);
            map.put("reasonAndDetails", mapList);

            //设置处方撤销标识 true:可以撤销, false:不可撤销
            Boolean cancelFlag = false;
            if (RecipeStatusConstant.REVOKE != recipe.getStatus()) {
                if ((recipe.getChecker() == null) && !Integer.valueOf(1).equals(recipe.getPayFlag())
                        && recipe.getStatus() != RecipeStatusConstant.UNSIGN
                        && recipe.getStatus() != RecipeStatusConstant.HIS_FAIL
                        && recipe.getStatus() != RecipeStatusConstant.NO_DRUG
                        && recipe.getStatus() != RecipeStatusConstant.NO_PAY
                        && recipe.getStatus() != RecipeStatusConstant.NO_OPERATOR
                        && !Integer.valueOf(1).equals(recipe.getChooseFlag())) {
                    cancelFlag = true;
                }
            }
            map.put("cancelFlag", cancelFlag);
            //能否开医保处方
            boolean medicalFlag = false;
            ConsultSetBean set = iConsultService.getSetByDoctorId(recipe.getDoctor());
            if (null != set && null != set.getMedicarePrescription()) {
                medicalFlag = (true == set.getMedicarePrescription()) ? true : false;
            }
            map.put("medicalFlag", medicalFlag);
            if (null != recipe.getChecker() && recipe.getChecker() > 0) {
                String ysTel = iDoctorService.getMobileByDoctorId(recipe.getChecker());
                if (StringUtils.isNotEmpty(ysTel)) {
                    recipe.setCheckerTel(ysTel);
                }
            }

            //审核不通过处方单详情增加二次签名标记
            boolean b = RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus() && (recipe.canMedicalPay() || effective);
            if (b) {
                map.put("secondSignFlag", iOrganConfigService.getEnableSecondsignByOrganId(recipe.getClinicOrgan()));
            }
        } else {
            RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
            map.put("tips", getTipsByStatusForPatient(recipe, order));
            boolean b = null != recipe.getEnterpriseId() && RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())
                    && (recipe.getStatus() == RecipeStatusConstant.WAIT_SEND || recipe.getStatus() == RecipeStatusConstant.IN_SEND
                    || recipe.getStatus() == RecipeStatusConstant.FINISH);
            if (b) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                map.put("depTel", drugsEnterpriseDAO.getTelById(recipe.getEnterpriseId()));
            }

            recipe.setOrderAmount(recipe.getTotalMoney());
            BigDecimal actualPrice = null;
            if (null != order) {
                actualPrice = order.getRecipeFee();
                recipe.setDiscountAmount(order.getCouponName());
            } else {
                // couponId = -1有优惠券  不使用 显示“不使用优惠券”
                actualPrice = recipe.getActualPrice();
                recipe.setDiscountAmount("0.0");

                //如果获取不到有效订单，则不返回订单编号（场景：医保消息发送成功后，处方单关联了一张无效订单，此时处方单点击自费结算，应跳转到订单确认页面）
                recipe.setOrderCode(null);
            }
            if (null == actualPrice) {
                actualPrice = recipe.getTotalMoney();
            }
            recipe.setActualPrice(actualPrice);

            //无法配送时间文案提示
            map.put("unSendTitle", getUnSendTitleForPatient(recipe));
            //患者处方取药方式提示
            map.put("recipeGetModeTip", getRecipeGetModeTip(recipe));

            if (null != order && 1 == order.getEffective() && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                //如果创建过自费订单，则不显示医保支付
                recipe.setMedicalPayFlag(0);
            }

            //药品价格显示处理
            boolean b1 = RecipeStatusConstant.FINISH == recipe.getStatus() ||
                    (1 == recipe.getChooseFlag() && !RecipeUtil.isCanncelRecipe(recipe.getStatus()) &&
                            (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(recipe.getPayMode())
                                    || RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getPayMode())
                                    || RecipeBussConstant.PAYMODE_TO_HOS.equals(recipe.getPayMode())));
            if (!b1) {
                recipe.setTotalMoney(null);
            }
        }

        if (StringUtils.isEmpty(recipe.getMemo())) {
            recipe.setMemo("无");
        }

        //设置失效时间
        if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus()) {
            recipe.setRecipeSurplusHours(getRecipeSurplusHours(recipe.getSignDate()));
        }

        map.put("recipe", recipe);

        return map;
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
        String orderCode = recipe.getOrderCode();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.FINISH:
                tips = "处方单已完结.";
                break;
            case RecipeStatusConstant.HAVE_PAY:
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                    //配送到家
                    tips = "您已支付，药品将尽快为您配送.";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                    //医院取药
                    tips = "您已支付，请尽快到院取药.";
                }
                break;
            case RecipeStatusConstant.NO_OPERATOR:
            case RecipeStatusConstant.NO_PAY:
                tips = "由于您未及时缴费，该处方单已失效，请联系医生.";
                break;
            case RecipeStatusConstant.NO_DRUG:
                tips = "由于您未及时取药，该处方单已失效.";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "";
                } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode) && 0 == payFlag) {
                    tips = "您已选择到院支付，请及时缴费并取药.";
                }

                if (StringUtils.isNotEmpty(orderCode) && null != order && 1 == order.getEffective()) {
                    tips = "您已选择配送到家，请及时支付并取药.";
                }

                break;
            case RecipeStatusConstant.READY_CHECK_YS:
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "您已支付，药品将尽快为您配送.";
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                    tips = "处方正在审核中.";
                }
                break;
            case RecipeStatusConstant.WAIT_SEND:
            case RecipeStatusConstant.CHECK_PASS_YS:
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "您已支付，药品将尽快为您配送.";
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    //货到付款
                    tips = "药品将尽快为您配送.";
                } else if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                    tips = "请尽快前往药店取药.";
                }
                break;
            case RecipeStatusConstant.IN_SEND:
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "您已支付，药品正在配送中，请保持手机畅通.";
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    //货到付款
                    tips = "药品正在配送中，请保持手机畅通.";
                }
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                tips = "由于未通过审核，该处方单已失效，请联系医生.";
                if (StringUtils.isNotEmpty(orderCode) && null != order && 1 == order.getEffective()) {
                    if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                        //在线支付
                        tips = "您已支付，药品将尽快为您配送.";
                    } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                        tips = "处方正在审核中.";
                    }
                }

                break;
            case RecipeStatusConstant.REVOKE:
                tips = "由于医生已撤销，该处方单已失效，请联系医生.";
                break;
            default:
                tips = "未知状态" + status;

        }
        return tips;
    }

    /**
     * 无法配送时间段文案提示
     * 处方单详情（待处理，待支付,药师未审核，状态为待配送,药师已审核，状态为待配送）
     */
    public static String getUnSendTitleForPatient(Recipe recipe) {
        String unSendTitle = "";
        switch (recipe.getStatus()) {
            case RecipeStatusConstant.CHECK_PASS:
            case RecipeStatusConstant.WAIT_SEND:
            case RecipeStatusConstant.CHECK_PASS_YS:
            case RecipeStatusConstant.READY_CHECK_YS:
                if (!RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())
                        && !RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode())) {
                    unSendTitle = iSysParamterService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP, null);
                }
                //患者选择药店取药但是未点击下一步而返回处方单详情，此时payMode会变成4，增加判断条件
                if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode()) && 0 == recipe.getChooseFlag()) {
                    unSendTitle = iSysParamterService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP, null);
                }
                break;
            default:
                unSendTitle = "";
        }
        return unSendTitle;
    }

    /**
     * 患者处方取药方式提示
     */
    public static String getRecipeGetModeTip(Recipe recipe) {
        String recipeGetModeTip = "";
        // 该处方不是只能配送处方，可以显示 到院取药 的文案
        if (1 != recipe.getChooseFlag() && !Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
            String organName = StringUtils.isEmpty(recipe.getOrganName())?"医院":recipe.getOrganName();
            // 邵逸夫特殊处理院区
            if(1 == recipe.getClinicOrgan()){
                organName = "浙大附属邵逸夫医院庆春院区";
            }
            recipeGetModeTip = iSysParamterService.getParam(ParameterConstant.KEY_RECIPE_GETMODE_TIP,null);
            recipeGetModeTip = LocalStringUtil.processTemplate(recipeGetModeTip, ImmutableMap.of("orgName", organName));
        }
        return recipeGetModeTip;

    }

    /**
     * 获取处方失效剩余时间
     *
     * @param signDate
     * @return
     */
    public static String getRecipeSurplusHours(Date signDate) {
        String recipeSurplusHours = "0.1";
        if (null != signDate) {
            long startTime = Calendar.getInstance().getTimeInMillis();
            long endTime = DateConversion.getDateAftXDays(signDate, 3).getTime();
            if (endTime > startTime) {
                DecimalFormat df = new DecimalFormat("0.00");
                recipeSurplusHours = df.format((endTime - startTime) / (float) (1000 * 60 * 60));
            }
        }

        return recipeSurplusHours;
    }

    /**
     * 配送模式选择
     *
     * @param payMode
     * @return
     */
    public static List<Integer> getDepSupportMode(Integer payMode) {
        //具体见DrugsEnterprise的payModeSupport字段
        //配送模式支持 0:不支持 1:线上付款 2:货到付款 3:药店取药 8:货到付款和药店取药 9:都支持
        List<Integer> supportMode = new ArrayList<>();
        if (null == payMode) {
            return supportMode;
        }

        if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ONLINE);
        } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_COD);
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_COD_TFDS);
        } else if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_TFDS);
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_COD_TFDS);
        } else if (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            //医保选用线上支付配送方式
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ONLINE);
        }

        if (CollectionUtils.isNotEmpty(supportMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ALL);
        }

        return supportMode;
    }

    /**
     * 往咨询界面发送处方卡片
     * @param recipe
     * @param details
     * @param rMap
     * @param send true:发送卡片
     */
    public static void sendRecipeTagToPatient(Recipe recipe, List<Recipedetail> details,
                                              Map<String, Object> rMap, boolean send) {
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);

        RecipeTagMsgBean recipeTagMsg = getRecipeMsgTag(recipe, details);
        //由于就诊人改造，已经可以知道申请人的信息，所以可以直接往当前咨询发消息
        if (StringUtils.isNotEmpty(recipe.getRequestMpiId()) && null != recipe.getDoctor()) {
            sendRecipeMsgTag(recipe.getRequestMpiId(), recipe.getDoctor(), recipeTagMsg, rMap, send);
        }else if (StringUtils.isNotEmpty(recipe.getMpiid()) && null != recipe.getDoctor()) {
            //处方的患者编号在咨询单里其实是就诊人编号，不是申请人编号
            List<String> requestMpiIds = iConsultService.findPendingConsultByMpiIdAndDoctor(recipe.getMpiid(),
                    recipe.getDoctor());
            if (CollectionUtils.isNotEmpty(requestMpiIds)) {
                for (String requestMpiId : requestMpiIds) {
                    sendRecipeMsgTag(requestMpiId, recipe.getDoctor(), recipeTagMsg, rMap, send);
                }
            }
        }
    }

    private static void sendRecipeMsgTag(String requestMpiId, int doctorId, RecipeTagMsgBean recipeTagMsg,
                                         Map<String, Object> rMap, boolean send){
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);

        //根据申请人mpiid，requestMode 获取当前咨询单consultId
        Integer consultId = null;
        List<Integer> consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(requestMpiId,
                                        doctorId, RecipeSystemConstant.CONSULT_TYPE_RECIPE);
        if (CollectionUtils.isNotEmpty(consultIds)) {
            consultId = consultIds.get(0);
        }
        if (consultId != null) {
            if (null != rMap && null == rMap.get("consultId")) {
                rMap.put("consultId", consultId);
            }

            if(send) {
                ConsultBean consultBean = iConsultService.get(consultId);
                if (consultBean != null) {
                    //判断咨询单状态是否为处理中
                    if (consultBean.getConsultStatus() == RecipeSystemConstant.CONSULT_STATUS_HANDLING) {
                        if (StringUtils.isEmpty(consultBean.getSessionID())) {
                            recipeTagMsg.setSessionID(null);
                        } else {
                            recipeTagMsg.setSessionID(consultBean.getSessionID());
                        }
                        LOGGER.info("sendRecipeMsgTag recipeTagMsg={}", JSONUtils.toString(recipeTagMsg));
                        //将消息存入数据库consult_msg，并发送环信消息
                        iConsultService.handleRecipeMsg(consultBean, recipeTagMsg);
                    }
                }
            }
        }
    }

    /**
     * 获取处方卡片信息
     * @param recipe
     * @param details
     * @return
     */
    private static RecipeTagMsgBean getRecipeMsgTag(Recipe recipe, List<Recipedetail> details){
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

        //获取诊断疾病名称
        String diseaseName = recipe.getOrganDiseaseName();
        List<String> drugNames = Lists.newArrayList();
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            for (Recipedetail r : details) {
                drugNames.add(r.getDrugName() + " * " + BigDecimal.valueOf(r.getUseDose()).toBigInteger().toString() + r.getUseDoseUnit());
            }
        } else {
            //组装药品名称   药品名+商品名+规格
            List<Integer> drugIds = Lists.newArrayList();
            for (Recipedetail r : details) {
                drugIds.add(r.getDrugId());
            }
            List<DrugList> drugLists = drugListDAO.findByDrugIds(drugIds);
            for (DrugList drugList : drugLists) {
                //判断非空
                String drugName = StringUtils.isEmpty(drugList.getDrugName()) ? "" : drugList.getDrugName();
                String saleName = StringUtils.isEmpty(drugList.getSaleName()) ? "" : drugList.getSaleName();
                String drugSpec = StringUtils.isEmpty(drugList.getDrugSpec()) ? "" : drugList.getDrugSpec();

                //数据库中saleName字段可能包含与drugName相同的字符串,增加判断条件，将这些相同的名字过滤掉
                StringBuilder drugAndSale = new StringBuilder("");
                if (StringUtils.isNotEmpty(saleName)) {
                    String[] strArray = saleName.split("\\s+");
                    for (String saleName1 : strArray) {
                        if (!saleName1.equals(drugName)) {
                            drugAndSale.append(saleName1 + " ");
                        }
                    }
                }
                drugAndSale.append(drugName + " ");
                //拼装
                drugNames.add(drugAndSale + drugSpec);
            }
        }

        RecipeTagMsgBean recipeTagMsg = new RecipeTagMsgBean();
        recipeTagMsg.setDiseaseName(diseaseName);
        recipeTagMsg.setDrugNames(drugNames);
        if (null != recipe.getRecipeId()) {
            recipeTagMsg.setRecipeId(recipe.getRecipeId());
        }

        return recipeTagMsg;
    }

    /**
     * 处方撤销接口区分医生端和运营平台
     *
     * @param recipeId
     * @param flag
     * @return
     */
    public static Map<String, Object> cancelRecipeImpl(Integer recipeId, Integer flag, String name, String message) {
        LOGGER.info("cancelRecipe [recipeId：" + recipeId + "]");
        //获取订单
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        //获取处方单
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Map<String, Object> rMap = Maps.newHashMap();
        Boolean result = false;
        //医生撤销提醒msg，供医生app端使用
        String msg = "";
        if (null == recipe) {
            msg = "该处方单不存在";
            rMap.put("result", result);
            rMap.put("msg", msg);
            return rMap;
        }
        //获取撤销前处方单状态
        Integer beforeStatus = recipe.getStatus();
        //不能撤销的情况:1 患者已支付 2 药师已审核(不管是否通过)
        if (Integer.valueOf(RecipeStatusConstant.REVOKE).equals(recipe.getStatus())) {
            msg = "该处方单已撤销，不能进行撤销操作";
        }
        if (!(recipe.getChecker() == null)) {
            msg = "该处方单已经过审核，不能进行撤销操作";
        }
        if (recipe.getStatus() == RecipeStatusConstant.UNSIGN) {
            msg = "暂存的处方单不能进行撤销";
        }
        if (Integer.valueOf(1).equals(recipe.getPayFlag())) {
            msg = "该处方单用户已支付，不能进行撤销操作";
        }
        if (recipe.getStatus() == RecipeStatusConstant.HIS_FAIL
                || recipe.getStatus() == RecipeStatusConstant.NO_DRUG
                || recipe.getStatus() == RecipeStatusConstant.NO_PAY
                || recipe.getStatus() == RecipeStatusConstant.NO_OPERATOR) {
            msg = "该处方单已取消，不能进行撤销操作";
        }
        if (Integer.valueOf(1).equals(recipe.getChooseFlag())) {
            msg = "患者已选择购药方式，不能进行撤销操作";
        }
        if (1 == flag) {
            if (StringUtils.isEmpty(name)) {
                msg = "姓名不能为空";
            }
            if (StringUtils.isEmpty(message)) {
                msg = "撤销原因不能为空";
            }
        }
        //处方撤销信息，供记录日志使用
        StringBuilder memo = new StringBuilder(msg);
        if (StringUtils.isEmpty(msg)) {
            Map<String, Integer> changeAttr = Maps.newHashMap();
            if (!recipe.canMedicalPay()) {
                changeAttr.put("chooseFlag", 1);
            }
            result = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.REVOKE, changeAttr);
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
            if (result) {
                msg = "处方撤销成功";
                //向患者推送处方撤销消息
                if (!(RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus() && recipe.canMedicalPay())) {
                    //医保的处方待审核时患者无法看到处方，不发送撤销消息提示
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.REVOKE);
                }
                memo.append(msg);
                //HIS消息发送
                boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                if (succFlag) {
                    memo.append(",HIS推送成功");
                } else {
                    memo.append(",HIS推送失败");
                }
                //处方撤销后将状态设为已撤销，供记录日志使用
                recipe.setStatus(RecipeStatusConstant.REVOKE);
            } else {
                msg = "未知原因，处方撤销失败";
                memo.append("," + msg);
            }
        }

        if (1 == flag) {
            memo.append("。" + "撤销人：" + name + ",撤销原因：" + message);
        }
        //记录日志
        RecipeLogService.saveRecipeLog(recipeId, beforeStatus, recipe.getStatus(), memo.toString());
        rMap.put("result", result);
        rMap.put("msg", msg);
        LOGGER.info("cancelRecipe execute ok! rMap:" + JSONUtils.toString(rMap));
        return rMap;
    }
}
