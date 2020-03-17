package recipe.drugTool.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.ProvinceDrugListBean;
import com.ngari.recipe.drugTool.service.IDrugToolService;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.UpdateMatchStatusFormBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.spring.AppDomainContext;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.OrganToolBean;
import recipe.constant.DrugMatchConstant;
import recipe.dao.*;
import recipe.service.OrganDrugListService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.DrugMatchUtil;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * created by shiyuping on 2019/2/1
 */
@RpcBean(value = "drugToolService", mvc_authentication = false)
public class DrugToolService implements IDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugToolService.class);

    private double progress;

    private RedisClient redisClient = RedisClient.instance();

    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";
    //全局map
    private ConcurrentHashMap<String, Double> progressMap = new ConcurrentHashMap<>();
    /**
     * 用于药品小工具搜索历史记录缓存
     */
    private ConcurrentHashMap<String, ArrayBlockingQueue> cmap = new ConcurrentHashMap<>();

    /**
     * 修改匹配中的原状态（已提交，已匹配）
     */
    public static final Integer[] Change_Matching_StatusList = {DrugMatchConstant.ALREADY_MATCH, DrugMatchConstant.SUBMITED};

    /**
     * 修改匹配中的原状态（已提交，已匹配）
     */
    public static final Integer[] Ready_Match_StatusList = {DrugMatchConstant.ALREADY_MATCH, DrugMatchConstant.SUBMITED};

    /*平台类型*/
    private static final int Platform_Type = 0;

    /*省平台类型*/
    private static final int Province_Platform_Type = 1;

    @Resource
    private DrugListMatchDAO drugListMatchDAO;

    @Resource
    private DrugListDAO drugListDAO;

    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Resource
    private DrugToolUserDAO drugToolUserDAO;

    @Resource
    private OrganService organService;
    @Resource
    private OrganDrugListService organDrugListService;

    @Resource
    private ProvinceDrugListDAO provinceDrugListDAO;

    private LoadingCache<String, List<DrugList>> drugListCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, List<DrugList>>() {
        @Override
        public List<DrugList> load(String str) throws Exception {
            return drugListDAO.findBySaleNameLike(str);
        }
    });


    @RpcService
    public void resetMatchCache() {
        drugListCache.cleanUp();
    }


    @RpcService
    public DrugToolUser loginOrRegist(String name, String mobile, String pwd) {
        if (StringUtils.isEmpty(name)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "name is required");
        }
        if (StringUtils.isEmpty(mobile)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "mobile is required");
        }
        if (StringUtils.isEmpty(pwd)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pwd is required");
        }
        DrugToolUser dbUser = drugToolUserDAO.getByMobile(mobile);
        if (dbUser == null) {
            DrugToolUser user = new DrugToolUser();
            user.setName(name);
            user.setMobile(mobile);
            user.setPassword(pwd);
            user.setStatus(1);
            dbUser = drugToolUserDAO.save(user);
        } else {
            if (!(pwd.equals(dbUser.getPassword()) && name.equals(dbUser.getName()))) {
                throw new DAOException(609, "姓名或密码不正确");
            }
        }
        return dbUser;
    }

    @RpcService
    public boolean isLogin(String mobile) {
        if (StringUtils.isEmpty(mobile)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "mobile is required");
        }
        boolean result = false;
        DrugToolUser dbUser = drugToolUserDAO.getByMobile(mobile);
        if (dbUser != null) {
            result = true;
        }
        return result;
    }

    //获取进度条
    @RpcService
    public double getProgress(int organId, String operator) throws InterruptedException {
        String key = organId + operator;
//      Double data = progressMap.get(key);
        Double data = redisClient.get(key);
        if (data != null) {
            progress = data;
            if (progress == 100 && redisClient.exists(key)) {
//                 progressMap.remove(key);
                redisClient.del(key);
            }
        }
        LOGGER.info("进度条加载={}=", progress);
        return progress;
    }

    @Override
    public synchronized Map<String, Object> readDrugExcel(byte[] buf, String originalFilename, int organId, String operator) {
        LOGGER.info(operator + "开始 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        progress = 0;
        String key = organId + operator;
        if (redisClient.exists(key)) {
            redisClient.del(key);
        }
        Map<String, Object> result = Maps.newHashMap();
        if (StringUtils.isEmpty(operator)) {
            result.put("code", 609);
            result.put("msg", "operator is required");
            return result;
        }
        int length = buf.length;
        LOGGER.info("readDrugExcel byte[] length=" + length);
        int max = 1343518;
        //控制导入数据量
        if (max <= length) {
            result.put("code", 609);
            result.put("msg", "超过7000条数据,请分批导入");
            return result;
        }
        InputStream is = new ByteArrayInputStream(buf);
        //获得用户上传工作簿
        Workbook workbook = null;
        try {
            if (originalFilename.endsWith(SUFFIX_2003)) {
                workbook = new HSSFWorkbook(is);
            } else if (originalFilename.endsWith(SUFFIX_2007)) {
                //使用InputStream需要将所有内容缓冲到内存中，这会占用空间并占用时间
                //当数据量过大时，这里会非常耗时
                workbook = new XSSFWorkbook(is);
            } else {
                result.put("code", 609);
                result.put("msg", "上传文件格式有问题");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("readDrugExcel error ," + e.getMessage());
            result.put("code", 609);
            result.put("msg", "上传文件格式有问题");
            return result;
        }
        Sheet sheet = workbook.getSheetAt(0);
        Integer total = sheet.getLastRowNum();
        if (total == null || total <= 0) {
            result.put("code", 609);
            result.put("msg", "data is required");
            return result;
        }

        DrugListMatch drug;
        Row row;
        List<String> errDrugListMatchList = Lists.newArrayList();
        for (int rowIndex = 0; rowIndex <= total; rowIndex++) {
            //循环获得每个行
            row = sheet.getRow(rowIndex);
            //判断是否是模板
            if (rowIndex == 0) {
                String drugCode = getStrFromCell(row.getCell(0));
                String drugName = getStrFromCell(row.getCell(1));
                String retrievalCode = getStrFromCell(row.getCell(19));
                if ("药品编号".equals(drugCode) && "药品通用名".equals(drugName) && "院内检索码".equals(retrievalCode)) {
                    continue;
                } else {
                    result.put("code", 609);
                    result.put("msg", "模板有误，请确认！");
                    return result;
                }

            } drug = new DrugListMatch();
            StringBuilder errMsg = new StringBuilder();
            /*try{*/
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                    errMsg.append("药品编号不能为空").append(";");
                }
                drug.setOrganDrugCode(getStrFromCell(row.getCell(0)));
            } catch (Exception e) {
                errMsg.append("药品编号有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(1)))) {
                    errMsg.append("药品通用名不能为空").append(";");
                }
                drug.setDrugName(getStrFromCell(row.getCell(1)));
            } catch (Exception e) {
                errMsg.append("药品通用名有误").append(";");
            }

            try {
                drug.setSaleName(getStrFromCell(row.getCell(2)));
            } catch (Exception e) {
                errMsg.append("药品商品名有误").append(";");
            }

            try {
                drug.setDrugSpec(getStrFromCell(row.getCell(3)));
            } catch (Exception e) {
                errMsg.append("药品规格有误").append(";");
            }
            try {
                if (("中药").equals(getStrFromCell(row.getCell(4)))) {
                    drug.setDrugType(3);
                } else if (("中成药").equals(getStrFromCell(row.getCell(4)))) {
                    drug.setDrugType(2);
                } else if (("西药").equals(getStrFromCell(row.getCell(4)))) {
                    drug.setDrugType(1);
                } else {
                    errMsg.append("药品类型格式错误").append(";");
                }
            } catch (Exception e) {
                errMsg.append("药品类型有误").append(";");
            }

            //中药不需要设置
            if(3 != drug.getDrugType()){

                try {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(5)))) {
                        errMsg.append("单次剂量不能为空").append(";");
                    } else {
                        drug.setUseDose(Double.parseDouble(getStrFromCell(row.getCell(5))));
                    }
                } catch (Exception e) {
                    errMsg.append("单次剂量有误").append(";");
                }

                try {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(6)))) {
                        drug.setDefaultUseDose(null);
                    } else {
                        drug.setDefaultUseDose(Double.parseDouble(getStrFromCell(row.getCell(6))));
                    }
                } catch (Exception e) {
                    errMsg.append("默认单次剂量有误").append(";");
                }



                try {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(8)))) {
                        errMsg.append("转换系数不能为空").append(";");
                    } else {
                        drug.setPack(Integer.parseInt(getStrFromCell(row.getCell(8))));
                    }
                } catch (Exception e) {
                    errMsg.append("转换系数有误").append(";");
                }

                try {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(9)))) {
                        errMsg.append("药品单位不能为空").append(";");
                    }
                    drug.setUnit(getStrFromCell(row.getCell(9)));
                } catch (Exception e) {
                    errMsg.append("药品单位有误").append(";");
                }

                try {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(11)))) {
                        errMsg.append("生产厂家不能为空").append(";");
                    }
                    drug.setProducer(getStrFromCell(row.getCell(11)));
                } catch (Exception e) {
                    errMsg.append("生产厂家有误").append(";");
                }

                try {
                    if (("是").equals(getStrFromCell(row.getCell(18)))) {
                        drug.setBaseDrug(1);
                    } else if (("否").equals(getStrFromCell(row.getCell(18)))) {
                        drug.setBaseDrug(0);
                    }else {
                        errMsg.append("是否基药格式不正确").append(";");
                    }

                } catch (Exception e) {
                    errMsg.append("是否基药有误").append(";");
                }

            }

            try {
                drug.setUseDoseUnit(getStrFromCell(row.getCell(7)));
            } catch (Exception e) {
                errMsg.append("剂量单位有误").append(";");
            }

            try {
                drug.setRetrievalCode(getStrFromCell(row.getCell(19)));
            } catch (Exception e) {
                errMsg.append("院内检索码有误").append(";");
            }

            try {
                String priceCell = getStrFromCell(row.getCell(12));
                if (StringUtils.isEmpty(priceCell)) {
                    drug.setPrice(new BigDecimal(0));
                } else {
                    drug.setPrice(new BigDecimal(priceCell));
                }
            } catch (Exception e) {
                errMsg.append("药品单价有误").append(";");
            }
            //设置无需判断的数据
            drug.setDrugManfCode(getStrFromCell(row.getCell(10)));
            drug.setLicenseNumber(getStrFromCell(row.getCell(13)));
            drug.setStandardCode(getStrFromCell(row.getCell(14)));
            drug.setIndications(getStrFromCell(row.getCell(15)));
            drug.setDrugForm(getStrFromCell(row.getCell(16)));
            drug.setPackingMaterials(getStrFromCell(row.getCell(17)));
            //drug.setRegulationDrugCode(getStrFromCell(row.getCell(20)));
            drug.setMedicalDrugCode(getStrFromCell(row.getCell(20)));
            drug.setMedicalDrugFormCode(getStrFromCell(row.getCell(21)));
            drug.setHisFormCode(getStrFromCell(row.getCell(22)));
            drug.setSourceOrgan(organId);
            drug.setStatus(DrugMatchConstant.UNMATCH);
            drug.setOperator(operator);
            drug.setRegulationDrugCode(getStrFromCell(row.getCell(24)));
            drug.setPlatformDrugId(Integer.parseInt(getStrFromCell(row.getCell(23))));
            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1));
                errDrugListMatchList.add(error);
            }else {
                try {
                    boolean isSuccess = drugListMatchDAO.updateData(drug);
                    if (!isSuccess) {
                        //自动匹配功能暂无法提供
                         AutoMatch(drug);
                        drugListMatchDAO.save(drug);
                    }
                } catch (Exception e) {
                    LOGGER.error("save or update drugListMatch error " + e.getMessage());
                }
            }
            drug=null;
            System.gc();
            progress = new BigDecimal((float) rowIndex / total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            redisClient.set(organId + operator, progress * 100);
//                    progressMap.put(organId+operator,progress*100);
        }
        if (errDrugListMatchList.size()>0){
            result.put("code", 609);
            result.put("msg", errDrugListMatchList);
            return result;
        }

        LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code", 200);
        return result;
    }


    private void AutoMatch(DrugListMatch drug) {
        LOGGER.info("the new Drug=[{}]", JSONUtils.toString(drug));
        DrugList drugList = null;
        String addrArea = null;
        ProvinceDrugList provinceDrugList = null;
        if (StringUtils.isNotEmpty(drug.getRegulationDrugCode()) || drug.getPlatformDrugId() != null) {
            drugList = drugListDAO.get(drug.getPlatformDrugId());
            // 如果该机构有省平台关联的话
            if (checkOrganRegulation(drug.getSourceOrgan())) {
                addrArea = checkOrganAddrArea(drug.getSourceOrgan());
                provinceDrugList = provinceDrugListDAO.getByProvinceIdAndDrugId(addrArea, drug.getRegulationDrugCode(), 1);
                if (drugList != null && provinceDrugList != null) {
                    // 以匹配
                    drug.setStatus(DrugMatchConstant.ALREADY_MATCH);
                    drug.setMatchDrugId(drugList.getDrugId());
                } else if (drugList == null && provinceDrugList == null) {
                    //未匹配
                    drug.setStatus(DrugMatchConstant.UNMATCH);
                    drug.setRegulationDrugCode(null);
                    drug.setPlatformDrugId(null);
                } else if (drugList != null && provinceDrugList == null) {
                    //匹配中
                    drug.setStatus(DrugMatchConstant.MATCHING);
                    drug.setRegulationDrugCode(null);
                } else if (drugList == null && provinceDrugList != null) {
                    // 匹配中
                    drug.setStatus(DrugMatchConstant.MATCHING);
                    drug.setPlatformDrugId(null);
                }
            } else {
                drug.setRegulationDrugCode(null);
                if (drugList != null) {
                    drug.setStatus(DrugMatchConstant.ALREADY_MATCH);
                    drug.setMatchDrugId(drugList.getDrugId());
                } else {
                    drug.setPlatformDrugId(null);
                }
            }
        }
    }

    /**
     * 获取单元格值（字符串）
     *
     * @param cell
     * @return
     */
    private String getStrFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        //读取数据前设置单元格类型
        cell.setCellType(CellType.STRING);
        String strCell = cell.getStringCellValue();
        if (strCell != null) {
            strCell = strCell.trim();
            if (StringUtils.isEmpty(strCell)) {
                strCell = null;
            }
        }
        return strCell;
    }

    /**
     * 判断该机构是否已导入过
     */
    @RpcService
    public boolean isOrganImported(int organId) {
        boolean isImported = true;
        List<DrugListMatch> drugLists = drugListMatchDAO.findMatchDataByOrgan(organId);
        if (CollectionUtils.isEmpty(drugLists)) {
            isImported = false;
        }
        return isImported;
    }

    /**
     * 获取或刷新临时药品数据
     */
    @RpcService
    public QueryResult<DrugListMatch> findData(int organId, int start, int limit) {
        return drugListMatchDAO.findMatchDataByOrgan(organId, start, limit);
    }

    /**
     * 更新无匹配数据
     */
    @RpcService
    public Integer updateNoMatchData(int drugId, String operator) {
        if (StringUtils.isEmpty(operator)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        //如果是已匹配的取消匹配
        if (drugListMatch.getStatus().equals(DrugMatchConstant.ALREADY_MATCH)) {
            drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status", DrugMatchConstant.UNMATCH, "operator", operator));
        }
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("isNew", 1, "status", DrugMatchConstant.MARKED, "operator", operator));
        LOGGER.info("updateNoMatchData 操作人->{}更新无匹配数据,drugId={};status ->before={},after=3", operator, drugId, drugListMatch.getStatus());
        //updata by maoly on 2020/03/16 自动同步至平台药品库
        DrugList drugList = new DrugList();
        //药品名
        drugList.setDrugName(drugListMatch.getDrugName());
        //商品名
        drugList.setSaleName(drugListMatch.getSaleName());
        //一次剂量
        drugList.setUseDose(drugListMatch.getUseDose());
        //剂量单位
        drugList.setUseDoseUnit(drugListMatch.getUseDoseUnit());
        //规格
        drugList.setDrugSpec(drugListMatch.getDrugSpec());
        //药品包装数量
        drugList.setPack(drugListMatch.getPack());
        //药品单位
        drugList.setUnit(drugListMatch.getUnit());
        //药品类型
        drugList.setDrugType(drugListMatch.getDrugType());
        //剂型
        drugList.setDrugForm(drugListMatch.getDrugForm());
        drugList.setPrice1(drugListMatch.getPrice().doubleValue());
        drugList.setPrice2(drugListMatch.getPrice().doubleValue());
        //厂家
        drugList.setProducer(drugListMatch.getProducer());
        //其他
        drugList.setDrugClass("1901");
        drugList.setAllPyCode("");
        drugList.setStatus(1);
        drugList.setCreateDt(new Date());
        drugList.setLastModify(new Date());
        //来源机构
        drugList.setSourceOrgan(drugListMatch.getSourceOrgan());
        Integer status = drugListMatch.getStatus();
        try{
            DrugList save = drugListDAO.save(drugList);
            if (save != null){
                //更新为已匹配，将已标记上传的药品自动关联上
                //判断更新成已匹配还是匹配中
                if (isHaveReulationId(drugListMatch.getSourceOrgan())&&StringUtils.isEmpty(drugListMatch.getRegulationDrugCode())){
                    //匹配中
                    status = DrugMatchConstant.MATCHING;
                }else {
                    //已匹配
                    status = DrugMatchConstant.ALREADY_MATCH;
                }
                drugListMatchDAO.updateDrugListMatchInfoById(drugListMatch.getDrugId(),ImmutableMap.of("status", status,"matchDrugId",save.getDrugId()));
            }
        }catch(Exception e){
            LOGGER.error("DrugToolService.updateNoMatchData fail,e=[{}]",e);
            throw new DAOException(609, "数据自动导入平台药品库失败!");
        }

        return status;
    }

    /**
     * 取消已匹配状态和已提交状态
     */
    @RpcService
    public void cancelMatchStatus(int drugId, String operator) {
        if (StringUtils.isEmpty(operator)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status", DrugMatchConstant.UNMATCH, "operator", operator));
        LOGGER.info("cancelMatchStatus 操作人->{}更新为未匹配状态,drugId={};status ->before={},after=0", operator, drugId, drugListMatch.getStatus());
    }

    /**
     * 更新已匹配状态(未匹配0，已匹配1，已提交2,已标记3)
     */
    @RpcService
    public void updateMatchStatus(int drugId, int matchDrugId, String operator) {
        if (StringUtils.isEmpty(operator)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status", DrugMatchConstant.ALREADY_MATCH, "matchDrugId", matchDrugId, "operator", operator));
        LOGGER.info("updateMatchStatus 操作人->{}更新已匹配状态,drugId={};status ->before={},after=1", operator, drugId, drugListMatch.getStatus());
    }

    /**
     * 查找能匹配的机构
     */
    @RpcService
    public List<OrganDTO> findOrgan() {
        return organService.findOrgans();
    }

    /**
     * 关键字模糊匹配机构
     */
    @RpcService
    public List<OrganDTO> findOrganLikeShortName(String shortName) {
        return organService.findOrganLikeShortName(shortName);
    }

    /**
     * 查询所有机构并封装返回参数给前端，存入前端缓存使用
     */
    @RpcService
    public List<OrganToolBean> findOrganByRecipeTools() {
        LOGGER.info("findOrganByRecipeTools start");
        List<OrganToolBean> toollist = new ArrayList<>();
        try {
            List<OrganDTO> organDTOList = organService.findOrganLikeShortName("");
            for (OrganDTO o : organDTOList) {
                OrganToolBean toolBean = new OrganToolBean();
                toolBean.setName(o.getName());
                toolBean.setOrganId(o.getOrganId());
                toolBean.setPyCode(o.getPyCode());
                toolBean.setShortName(o.getShortName());
                toolBean.setWxAccount(o.getWxAccount());
                toollist.add(toolBean);
            }
        } catch (Exception e) {
            LOGGER.error("findOrganByRecipeTools 药品小工具查询所有机构接口异常");
            e.printStackTrace();
        }
        return toollist;
    }

    /**
     * 搜索当前用户的历史搜索记录
     *
     * @param userkey 搜索人的唯一标识
     * @return
     * @throws InterruptedException
     */
    @RpcService
    public List<?> findOrganSearchHistoryRecord(String userkey) {
        LOGGER.info("findOrganSearchHistoryRecord =userkey={}==", userkey);
        //创建一个存储容量为10的ArrayBlockingQueue对列
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
        List<Object> listCmap = new ArrayList<>();
        //存在历史搜索记录
        if (cmap.get(userkey) != null && cmap.get(userkey).size() > 0) {
            queue = cmap.get(userkey);
            Object[] arrayQueue = queue.toArray();
            for (Object s : arrayQueue) {
                listCmap.add(s);
            }
        }
        LOGGER.info("findOrganSearchHistoryRecord HistoryRecord  queue{}==", queue.size());
        return listCmap;
    }

    /**
     * 保存搜索人的历史记录，在导入药品库确定时调用
     *
     * @param shortName 搜索内容
     * @param userkey   搜索人的唯一标识
     * @return
     * @throws InterruptedException
     */
    @RpcService
    public void saveShortNameRecord(String shortName, String organId, String userkey) throws InterruptedException {
        LOGGER.info("saveShortNameRecord shortName=={}==organId=={}==userkey={}==", shortName, organId, userkey);
        //创建一个存储容量为10的ArrayBlockingQueue对列
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
        OrganToolBean ort = new OrganToolBean();
        //当搜索框为空的情况，直接返回缓存中的历史记录数据
        if (!StringUtils.isEmpty(shortName)) {
            if (cmap.get(userkey) != null && cmap.get(userkey).size() > 0) {
                queue = cmap.get(userkey);
            }

            ort.setOrganId(Integer.parseInt(organId));
            ort.setName(shortName);

            Object[] arrayQueue = queue.toArray();
            for (Object s : arrayQueue) {
                OrganToolBean t = (OrganToolBean) s;
                //通过organId过滤
                if (t.getOrganId() == Integer.parseInt(organId)) {
                    queue.remove(s);
                    break;
                }
            }
            //当容量超过10个时，取出第一个元素并删除
            if (10 == queue.size()) {
                queue.poll();
            }
            queue.put(ort);

            cmap.put(userkey, queue);
            LOGGER.info("saveShortNameRecord HistoryRecord  cmap{}==", cmap);
        }

    }


    /**
     * 药品匹配
     */
    @RpcService
    public List<DrugListBean> drugMatch(int drugId) {
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);

        String str = DrugMatchUtil.match(drugListMatch.getDrugName());
        //根据药品名取标准药品库查询相关药品
        List<DrugList> drugLists = null;
        List<DrugListBean> drugListBeans = null;
        try {
            drugLists = drugListCache.get(str);
        } catch (ExecutionException e) {
            LOGGER.error("drugMatch:" + e.getMessage());
        }

        //已匹配状态返回匹配药品id
        if (CollectionUtils.isNotEmpty(drugLists)) {
            drugListBeans = ObjectCopyUtils.convert(drugLists, DrugListBean.class);
            if (drugListMatch.getStatus().equals(DrugMatchConstant.ALREADY_MATCH)
                    || drugListMatch.getStatus().equals(DrugMatchConstant.SUBMITED)
                    || drugListMatch.getStatus().equals(DrugMatchConstant.MATCHING)) {
                for (DrugListBean drugListBean : drugListBeans) {
                    if (drugListBean.getDrugId().equals(drugListMatch.getMatchDrugId())) {
                        drugListBean.setIsMatched(true);
                    }
                }
            }
        }
        return drugListBeans;

    }

    /**
     * 药品提交至organDrugList(将匹配完成的数据提交更新)人工提交
     */
    @RpcService
    public Map<String, Integer> drugManualCommit(int organId,int status) {
        List<DrugListMatch> matchDataByOrgan = drugListMatchDAO.findMatchDataByOrgan(organId);
        final HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws Exception {
                List<DrugListMatch> lists = drugListMatchDAO.findDataByOrganAndStatus(organId, status);
                int num = 0;
                //更新数据到organDrugList并更新状态已提交
                for (DrugListMatch drugListMatch : lists) {
                    if (drugListMatch.getMatchDrugId() != null) {
                        OrganDrugList organDrugList = new OrganDrugList();
                        organDrugList.setDrugId(drugListMatch.getMatchDrugId());
                        organDrugList.setOrganDrugCode(drugListMatch.getOrganDrugCode());
                        organDrugList.setOrganId(drugListMatch.getSourceOrgan());
                        if (drugListMatch.getPrice() == null) {
                            organDrugList.setSalePrice(new BigDecimal(0));
                        } else {
                            organDrugList.setSalePrice(drugListMatch.getPrice());
                        }
                        organDrugList.setDrugName(drugListMatch.getDrugName());
                        if (StringUtils.isEmpty(drugListMatch.getSaleName())) {
                            organDrugList.setSaleName(drugListMatch.getDrugName());
                        } else {
                            if (drugListMatch.getSaleName().equals(drugListMatch.getDrugName())) {
                                organDrugList.setSaleName(drugListMatch.getSaleName());
                            } else {
                                organDrugList.setSaleName(drugListMatch.getSaleName() + " " + drugListMatch.getDrugName());
                            }

                        }

                        organDrugList.setUsingRate(drugListMatch.getUsingRate());
                        organDrugList.setUsePathways(drugListMatch.getUsePathways());
                        organDrugList.setProducer(drugListMatch.getProducer());
                        organDrugList.setUseDose(drugListMatch.getUseDose());
                        organDrugList.setRecommendedUseDose(drugListMatch.getDefaultUseDose());
                        organDrugList.setPack(drugListMatch.getPack());
                        organDrugList.setUnit(drugListMatch.getUnit());
                        organDrugList.setUseDoseUnit(drugListMatch.getUseDoseUnit());
                        organDrugList.setDrugSpec(drugListMatch.getDrugSpec());
                        organDrugList.setRetrievalCode(drugListMatch.getRetrievalCode());
                        organDrugList.setDrugForm(drugListMatch.getDrugForm());
                        organDrugList.setBaseDrug(drugListMatch.getBaseDrug());
                        organDrugList.setRegulationDrugCode(drugListMatch.getRegulationDrugCode());
                        organDrugList.setLicenseNumber(drugListMatch.getLicenseNumber());
                        organDrugList.setTakeMedicine(0);
                        organDrugList.setStatus(1);
                        organDrugList.setProducerCode("");
                        organDrugList.setLastModify(new Date());
                        if(StringUtils.isNotEmpty(drugListMatch.getDrugManfCode())) {
                            organDrugList.setProducerCode(drugListMatch.getDrugManfCode());
                        }
                        organDrugList.setMedicalDrugCode(drugListMatch.getMedicalDrugCode());
                        organDrugList.setMedicalDrugFormCode(drugListMatch.getMedicalDrugFormCode());
                        organDrugList.setDrugFormCode(drugListMatch.getHisFormCode());

                        Boolean isSuccess = organDrugListDAO.updateData(organDrugList);
                        if (!isSuccess) {
                            organDrugListDAO.save(organDrugList);
                            //同步药品到监管备案
                            RecipeBusiThreadPool.submit(()->{
                                organDrugListService.uploadDrugToRegulation(organDrugList);
                                return null;
                            });
                            num = num + 1;
                        }
                    }
                }
                setResult(num);
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);
        Map<String, Integer> result = Maps.newHashMap();
        result.put("before", matchDataByOrgan.size());
        result.put("saveSuccess", action.getResult());
        LOGGER.info("drugManualCommit success  beforeNum= " + matchDataByOrgan.size() + "saveSuccessNum=" + action.getResult());
        return result;
    }

    /**
     * 药品提交(将匹配完成的数据提交更新)----互联网六期改为人工提交
     */
    @RpcService
    public Map<String, Integer> drugCommit(List<DrugListMatch> lists,Integer organ) {
        List<DrugListMatch> lists1 = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        Integer result = 0;
        try{
            if(lists.size() > 0){
                for (DrugListMatch drugListMatch : lists) {
                    DrugListMatch db = drugListMatchDAO.get(drugListMatch.getDrugId());
                    if (1 == db.getStatus()) {
                        db.setUsingRate(drugListMatch.getUsingRate());
                        db.setUsePathways(drugListMatch.getUsePathways());
                        db.setDefaultUseDose(drugListMatch.getDefaultUseDose());
                        db.setStatus(DrugMatchConstant.SUBMITED);
                        lists1.add(db);
                        drugListMatchDAO.update(db);
                    }
                }
                if(lists1.size() > 0){
                    result = this.drugManualCommitNew(lists1);
                }
                map.put("successCount",result);
            }

        }catch(Exception e){
            LOGGER.error("DrugToolService.drugCommit fail,e=[{}]",e);
            throw new DAOException(609,"药品数据自动导入机构药品库失败！");
        }
        return map;

    }
    private Integer drugManualCommitNew(List<DrugListMatch> lists) {
        final HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws Exception {
                int num = 0;
                //更新数据到organDrugList并更新状态已提交
                for (DrugListMatch drugListMatch : lists) {
                    if (drugListMatch.getMatchDrugId() != null) {
                        OrganDrugList organDrugList = new OrganDrugList();
                        organDrugList.setDrugId(drugListMatch.getMatchDrugId());
                        organDrugList.setOrganDrugCode(drugListMatch.getOrganDrugCode());
                        organDrugList.setOrganId(drugListMatch.getSourceOrgan());
                        if (drugListMatch.getPrice() == null) {
                            organDrugList.setSalePrice(new BigDecimal(0));
                        } else {
                            organDrugList.setSalePrice(drugListMatch.getPrice());
                        }
                        organDrugList.setDrugName(drugListMatch.getDrugName());
                        if (StringUtils.isEmpty(drugListMatch.getSaleName())) {
                            organDrugList.setSaleName(drugListMatch.getDrugName());
                        } else {
                            if (drugListMatch.getSaleName().equals(drugListMatch.getDrugName())) {
                                organDrugList.setSaleName(drugListMatch.getSaleName());
                            } else {
                                organDrugList.setSaleName(drugListMatch.getSaleName() + " " + drugListMatch.getDrugName());
                            }

                        }

                        organDrugList.setUsingRate(drugListMatch.getUsingRate());
                        organDrugList.setUsePathways(drugListMatch.getUsePathways());
                        organDrugList.setProducer(drugListMatch.getProducer());
                        organDrugList.setUseDose(drugListMatch.getUseDose());
                        organDrugList.setRecommendedUseDose(drugListMatch.getDefaultUseDose());
                        organDrugList.setPack(drugListMatch.getPack());
                        organDrugList.setUnit(drugListMatch.getUnit());
                        organDrugList.setUseDoseUnit(drugListMatch.getUseDoseUnit());
                        organDrugList.setDrugSpec(drugListMatch.getDrugSpec());
                        organDrugList.setRetrievalCode(drugListMatch.getRetrievalCode());
                        organDrugList.setDrugForm(drugListMatch.getDrugForm());
                        organDrugList.setBaseDrug(drugListMatch.getBaseDrug());
                        organDrugList.setRegulationDrugCode(drugListMatch.getRegulationDrugCode());
                        organDrugList.setLicenseNumber(drugListMatch.getLicenseNumber());
                        organDrugList.setTakeMedicine(0);
                        organDrugList.setStatus(1);
                        organDrugList.setProducerCode("");
                        organDrugList.setLastModify(new Date());
                        if(StringUtils.isNotEmpty(drugListMatch.getDrugManfCode())) {
                            organDrugList.setProducerCode(drugListMatch.getDrugManfCode());
                        }
                        organDrugList.setMedicalDrugCode(drugListMatch.getMedicalDrugCode());
                        organDrugList.setMedicalDrugFormCode(drugListMatch.getMedicalDrugFormCode());
                        organDrugList.setDrugFormCode(drugListMatch.getHisFormCode());

                        Boolean isSuccess = organDrugListDAO.updateData(organDrugList);
                        if (!isSuccess) {
                            organDrugListDAO.save(organDrugList);
                            //同步药品到监管备案
                            RecipeBusiThreadPool.submit(()->{
                                organDrugListService.uploadDrugToRegulation(organDrugList);
                                return null;
                            });
                            num = num + 1;
                        }
                    }
                }
                setResult(num);
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);
        return action.getResult();
    }

    /**
     * 药品搜索(可根据药品名称，厂家等进行搜索)
     */
    @RpcService
    public QueryResult<DrugListMatch> drugSearch(int organId, String keyWord, Integer status, int start, int limit) {
        return drugListMatchDAO.queryDrugListsByDrugNameAndStartAndLimit(organId, keyWord, status, start, limit);
    }

    /**
     * 获取用药频率和用药途径
     */
    @RpcService
    public Map<String, Object> getUsingRateAndUsePathway() {
        Map<String, Object> result = Maps.newHashMap();
        List<DictionaryItem> usingRateList = new ArrayList<DictionaryItem>();
        List<DictionaryItem> usePathwayList = new ArrayList<DictionaryItem>();
        try {
            usingRateList = DictionaryController.instance().get("eh.cdr.dictionary.UsingRateWithKey").getSlice(null, 0, "");
            usePathwayList = DictionaryController.instance().get("eh.cdr.dictionary.UsePathwaysWithKey").getSlice(null, 0, "");
        } catch (ControllerException e) {
            LOGGER.error("getUsingRateAndUsePathway() error : " + e);
        }
        result.put("usingRate", usingRateList);
        result.put("usePathway", usePathwayList);
        return result;
    }

    /**
     * 获取用药频率和用药途径
     */
    @RpcService
    public Map<String, Object> findUsingRateAndUsePathwayWithOutKey() {
        Map<String, Object> result = Maps.newHashMap();
        List<DictionaryItem> usingRateList = new ArrayList<DictionaryItem>();
        List<DictionaryItem> usePathwayList = new ArrayList<DictionaryItem>();
        try {
            usingRateList = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getSlice(null, 0, "");
            usePathwayList = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getSlice(null, 0, "");
        } catch (ControllerException e) {
            LOGGER.error("getUsingRateAndUsePathway() error : " + e);
        }
        result.put("usingRate", usingRateList);
        result.put("usePathway", usePathwayList);
        return result;
    }

    @RpcService
    public void deleteDrugMatchData(Integer id, Boolean isOrganId) {
        if (isOrganId) {
            drugListMatchDAO.deleteByOrganId(id);
        } else {
            drugListMatchDAO.deleteById(id);
        }
    }

    @RpcService
    public void deleteOrganDrugData(Integer id, Boolean isOrganId) {
        if (isOrganId) {
            organDrugListDAO.deleteByOrganId(id);
        } else {
            organDrugListDAO.deleteById(id);
        }
    }

    @RpcService
    public void deleteProvinceDrugData(Integer id, Boolean isProvinceId) {
        if (isProvinceId) {
            if(null == id){
               LOGGER.warn("当前清除省平台的数据，没有需要的省区域信息！");
            }
            provinceDrugListDAO.deleteByProvinceId(id.toString());
        } else {
            provinceDrugListDAO.deleteByProvinceDrugId(id);
        }
    }

    /**
     * 根据药品id更新匹配表药品机构编码
     *
     * @param map
     */
    @RpcService
    public void updateMatchCodeById(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            drugListMatchDAO.updateDrugListMatchInfoById(Integer.valueOf(entry.getKey()), ImmutableMap.of("organDrugCode", entry.getValue()));
        }
    }
    /**
     * 根据id更新机构药品表药品机构编码
     *
     * @param map
     */
    @RpcService
    public void updateOrganDrugCodeById(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            organDrugListDAO.updateOrganDrugById(Integer.valueOf(entry.getKey()), ImmutableMap.of("organDrugCode", entry.getValue()));
        }
    }

    /**
     *
     * @param organId 机构id
     * @param depId 药企id
     * @param flag 是否用机构药品的编码作为药企编码，否就用平台的id作为药企编码
     */
    @RpcService
    public void addOrganDrugDataToSaleDrugList(Integer organId, Integer depId,Boolean flag) {
        List<OrganDrugList> drugs = organDrugListDAO.findOrganDrugByOrganId(organId);
        SaleDrugList saleDrugList;
        for (OrganDrugList organDrugList : drugs){
            saleDrugList = new SaleDrugList();
            SaleDrugList sales = saleDrugListDAO.getByDrugIdAndOrganId(organDrugList.getDrugId(),depId);
            if (sales == null){
                saleDrugList.setDrugId(organDrugList.getDrugId());
                saleDrugList.setDrugName(organDrugList.getDrugName());
                saleDrugList.setDrugSpec(organDrugList.getDrugSpec());
                saleDrugList.setOrganId(depId);
                saleDrugList.setStatus(1);
                saleDrugList.setPrice(organDrugList.getSalePrice());
                if (flag){
                    saleDrugList.setOrganDrugCode(organDrugList.getOrganDrugCode());
                }else {
                    saleDrugList.setOrganDrugCode(String.valueOf(organDrugList.getDrugId()));
                }
                saleDrugList.setInventory(new BigDecimal(100));
                saleDrugList.setCreateDt(new Date());
                saleDrugList.setLastModify(new Date());
                saleDrugListDAO.save(saleDrugList);
            }

        }


    }

    /**
     * 上传未匹配数据到通用药品目录
     *
     * @param organId       机构id
     * @param isHaveOrganId 通用药品目录是否包含机构来源
     * @return
     */
    @RpcService
    public Integer uploadNoMatchData(Integer organId, Boolean isHaveOrganId) {
        List<DrugListMatch> data = drugListMatchDAO.findDataByOrganAndStatus(organId, DrugMatchConstant.MARKED);
        if (CollectionUtils.isNotEmpty(data)) {
            for (DrugListMatch drugListMatch : data) {
                DrugList drugList = new DrugList();
                //药品名
                drugList.setDrugName(drugListMatch.getDrugName());
                //商品名
                drugList.setSaleName(drugListMatch.getSaleName());
                //一次剂量
                drugList.setUseDose(drugListMatch.getUseDose());
                //剂量单位
                drugList.setUseDoseUnit(drugListMatch.getUseDoseUnit());
                //规格
                drugList.setDrugSpec(drugListMatch.getDrugSpec());
                //药品包装数量
                drugList.setPack(drugListMatch.getPack());
                //药品单位
                drugList.setUnit(drugListMatch.getUnit());
                //药品类型
                drugList.setDrugType(drugListMatch.getDrugType());
                //剂型
                drugList.setDrugForm(drugListMatch.getDrugForm());
                drugList.setPrice1(drugListMatch.getPrice().doubleValue());
                drugList.setPrice2(drugListMatch.getPrice().doubleValue());
                //厂家
                drugList.setProducer(drugListMatch.getProducer());
                //其他
                drugList.setDrugClass("1901");
                drugList.setAllPyCode("");
                drugList.setStatus(1);
                //来源机构
                if (isHaveOrganId) {
                    drugList.setSourceOrgan(organId);
                }
                DrugList save = drugListDAO.save(drugList);
                if (save != null){
                    //更新为已匹配，将已标记上传的药品自动关联上
                    //判断更新成已匹配还是匹配中
                    Integer status;
                    if (isHaveReulationId(organId)&&StringUtils.isEmpty(drugListMatch.getRegulationDrugCode())){
                        //匹配中
                        status = DrugMatchConstant.MATCHING;
                    }else {
                        //已匹配
                        status = DrugMatchConstant.ALREADY_MATCH;
                    }
                    drugListMatchDAO.updateDrugListMatchInfoById(drugListMatch.getDrugId(),ImmutableMap.of("status", status,"matchDrugId",save.getDrugId()));
                }
            }
            return data.size();
        }
        return 0;
    }

    private boolean isHaveReulationId(Integer organId) {
        String addrArea = checkOrganAddrArea(organId);
        Long provinceDrugNum = provinceDrugListDAO.getCountByProvinceIdAndStatus(addrArea, 1);
        //更新药品状态成匹配中
        if(0L < provinceDrugNum){
            return true;
        }
        return false;
    }

    /**
     * 上传机构药品数据到监管平台备案
     *
     * @param organId
     */
    @RpcService
    public void uploadDrugToRegulation(Integer organId) {
        List<OrganDrugList> organDrug = organDrugListDAO.findOrganDrugByOrganId(organId);
        for (OrganDrugList organDrugList : organDrug) {
            organDrugListService.uploadDrugToRegulation(organDrugList);
        }

    }

    /**
     * 查询省平台药品列表
     *
     * @param organId
     */
    @RpcService
    public List<ProvinceDrugList> findProvinceDrugList(Integer organId) {
        String addrArea = checkOrganAddrArea(organId);
        return provinceDrugListDAO.findByProvinceIdAndStatus(addrArea, 1);
    }

    /**
     * 根据机构，判断机构关联的省平台药品有没有药品录入
     * 如果已匹配的和已提交设置成匹配中的
     */
    @RpcService
    public void updateOrganDrugMatchByProvinceDrug(int organId) {
        //首先判断his配置中机构有没有监管平台。没有就不返回省平台列表
        if (!checkOrganRegulation(organId)) return;

        String addrArea = checkOrganAddrArea(organId);
        Long provinceDrugNum = provinceDrugListDAO.getCountByProvinceIdAndStatus(addrArea, 1);
        //更新药品状态成匹配中
        if(0L < provinceDrugNum){
            //批量将匹配药品状态设置成匹配中
            drugListMatchDAO.updateStatusListToStatus(Arrays.asList(Change_Matching_StatusList), organId, DrugMatchConstant.MATCHING);
        }
    }

    private String checkOrganAddrArea(int organId) {
        //查询是否有意需要更新状态的药品（将匹配药品中已匹配的和已提交）
        //这里直接判断机构对应省药品有无药品
        OrganDTO organDTO = organService.get(organId);
        if(null == organDTO){
            LOGGER.warn("updateOrganDrugMatchByProvinceDrug 当期机构[{}]不存在", organId);
            return null;
        }
        String addrArea = organDTO.getAddrArea();
        //校验省平台的地址信息合理性
        if(null == addrArea || 2 > addrArea.length()){
            LOGGER.error("updateOrganDrugMatchByProvinceDrug() error : 医院[{}],对应的省信息不全", organId);
            throw new DAOException(DAOException.VALUE_NEEDED, "医院对应的省信息不全");
        }
        return addrArea.substring(0, 2);
    }

    /**
     * @method  updateMatchStatusCurrent
     * @description 选中的平台药品/省平台药品匹配机构药品
     * @date: 2019/10/25
     * @author: JRK
     * @param updateMatchStatusFormBean 属性值
     * drugId 更新的药品id
     * matchDrugId 匹配上的平台药品id
     * matchDrugInfo 匹配上省平台的药品code
     * operator 操作人
     * haveProvinceDrug 是否有当前机构对应省的药品（由前端查询list是否为空判断）
     * @return void
     */
    @RpcService
    public Integer updateMatchStatusCurrent(UpdateMatchStatusFormBean updateMatchStatusFormBean) {
        String operator = updateMatchStatusFormBean.getOperator();
        if (StringUtils.isEmpty(operator)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }

        //更新状态数据准备
        Integer drugId = updateMatchStatusFormBean.getDrugId();
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        Integer drugListMatchStatus = drugListMatch.getStatus();
        if(null == drugListMatch){
            LOGGER.warn("updateMatchStatusCurrent 当期对照药品[{}]不存在", drugId);
            return null;
        }
        int makeType = updateMatchStatusFormBean.getMakeType();
        Boolean haveProvinceDrug = updateMatchStatusFormBean.getHaveProvinceDrug();
        Integer matchDrugId = updateMatchStatusFormBean.getMatchDrugId();
        String matchDrugInfo = updateMatchStatusFormBean.getMatchDrugInfo();


        Map<String, Object> updateMap = new HashMap<>();
        //判断是否有省平台药品对应
        Integer status = 0;
        if(Platform_Type == makeType){
            //平台匹配操作
            if(haveProvinceDrug){
                //匹配省平台的时候
                status = geUpdateStatus(drugListMatch, Platform_Type);
                if (status == null) return null;
            }else{
                //无匹省台的时候
                status = DrugMatchConstant.ALREADY_MATCH;
            }
            updateMap.put("matchDrugId", matchDrugId);
        }else if (Province_Platform_Type == makeType){
            //省平台匹配操作
            status = geUpdateStatus(drugListMatch, Province_Platform_Type);
            if (status == null) return null;
            updateMap.put("regulationDrugCode", matchDrugInfo);
        }else{
            LOGGER.info("updateMatchStatusCurrent 传入操作状态非平台和省平台", makeType);
            return null;
        }
        updateMap.put("operator", operator);
        updateMap.put("status", status);
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, updateMap);
        LOGGER.info("updateMatchStatusCurrent 操作人->{}更新已匹配状态,drugId={};status ->before={},after={}", operator, drugId, drugListMatch.getStatus(), status);
        return status;
    }

    /*获取更新后的对照状态状态*/
    private Integer geUpdateStatus(DrugListMatch drugListMatch, int updateType) {
        Integer drugId = drugListMatch.getDrugId();
        Integer drugListMatchStatus = drugListMatch.getStatus();
        boolean haveMatchDrug = null != drugListMatch.getMatchDrugId();
        boolean haveRegulationDrugCode = null != drugListMatch.getRegulationDrugCode();

        Integer status;
        if(Platform_Type == updateType){
            //平台的更新
            //说明已经匹配，只需要修改匹配的药品代码就行了
            if(haveMatchDrug){
                //已提交的也可以更新
                if(DrugMatchConstant.SUBMITED == drugListMatchStatus){
                    if(haveRegulationDrugCode){
                        status = DrugMatchConstant.ALREADY_MATCH;
                    }else{
                        status = DrugMatchConstant.MATCHING;
                    }
                }else{
                    status = drugListMatchStatus;
                }
            }else{
                //没有匹配的话，判断有没有省药品字段
                if(haveRegulationDrugCode){
                    status = DrugMatchConstant.ALREADY_MATCH;
                }else{
                    status = DrugMatchConstant.MATCHING;
                }
            }
        }else{
            //省平台的更新
            //说明已经匹配，只需要修改匹配的省药品代码就行了
            if(haveRegulationDrugCode){
                //已提交的也可以更新
                if(DrugMatchConstant.SUBMITED == drugListMatchStatus){
                    if(haveMatchDrug){
                        status = DrugMatchConstant.ALREADY_MATCH;
                    }else{
                        status = DrugMatchConstant.MATCHING;
                    }
                }else{
                    status = drugListMatchStatus;
                }
            }else{
                //没有匹配的话，判断有没有药品字段
                if(haveMatchDrug){
                    status = DrugMatchConstant.ALREADY_MATCH;
                }else{
                    status = DrugMatchConstant.MATCHING;
                }
            }
        }


        return status;
    }

    /**
     * 省药品匹配
     */
    @RpcService
    public List<ProvinceDrugListBean> provinceDrugMatch(int drugId, int organId, int start, int limit, String seacrhString) {
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);

        if(null == drugListMatch){
            LOGGER.warn("provinceDrugMatch 当期药品[{}]不在机构对照列表中", drugId);
            return null;
        }
        List<ProvinceDrugList> provinceDrugLists = getProvinceDrugLists(organId, drugListMatch, start, limit, seacrhString);
        if(null == provinceDrugLists){
            //如果没有省平台药品数据则为null
            return null;
        }
        List<ProvinceDrugListBean> provinceDrugListBeans = getProvinceDrugListBean(drugListMatch, provinceDrugLists);

        return provinceDrugListBeans;

    }

    /*根据匹配的药品销售名，获取相似名称的省平台药品*/
    private List<ProvinceDrugList> getProvinceDrugLists(int organId, DrugListMatch drugListMatch, int start, int limit, String seacrhString) {
        List<ProvinceDrugList> provinceDrugLists = new ArrayList<>();
        if (!checkOrganRegulation(organId)) return null;

        //判断机构对应省平台下有没有药品，没有省平台返回null
        String addrArea = checkOrganAddrArea(organId);
        Long countByProvinceIdAndStatus = provinceDrugListDAO.getCountByProvinceIdAndStatus(addrArea, 1);
        if(null == countByProvinceIdAndStatus || 0 >= countByProvinceIdAndStatus){
            return null;
        }

        //根据药品名取标准药品库查询相关药品
        String likeDrugName = DrugMatchUtil.match(drugListMatch.getDrugName());

        List<ProvinceDrugList> searchDrugs = provinceDrugListDAO.findByProvinceSaleNameLike(likeDrugName, addrArea, start, limit, seacrhString);
        if(CollectionUtils.isNotEmpty(searchDrugs)){
            provinceDrugLists = searchDrugs;
        }

        return provinceDrugLists;
    }

    /*判断当前机构下的建挂壁平台有没有配置，没有配置判断为没有省平台药品*/
    private boolean checkOrganRegulation(int organId) {
        IRegulationService regulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        try {
            Boolean haveList = regulationService.checkRegulationList();
            if(!haveList){
                //没有这个配置，则说明是互联网，默认关联互联网平台的，不需要在查有没有关联互联网平台
                return true;
            }
        }catch (Exception e) {
            LOGGER.warn("查询互联网列表失败{}.",e);
        }

        //首先判断his配置中机构有没有监管平台。没有就不返回省平台列表
        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        HisResponseTO<ServiceConfigResponseTO> configResponse = configService.queryHisServiceConfigByOrganid(organId);
        if(null != configResponse){
            ServiceConfigResponseTO config = configResponse.getData();
            if(null == config || null == config.getRegulation()){
                LOGGER.info("当前机构[{}]没有配置监管平台", organId);
                return false;
            }
        }else{
            LOGGER.warn("没有对应当前机构[{}]的配置", organId);
            return false;
        }
        return true;
    }

    /*渲染页面上的勾选展示的项*/
    private List<ProvinceDrugListBean> getProvinceDrugListBean(DrugListMatch drugListMatch, List<ProvinceDrugList> provinceDrugLists) {
        List<ProvinceDrugListBean> provinceDrugListBeans = new ArrayList<>();
        //已匹配状态返回匹配药品id
        if (CollectionUtils.isNotEmpty(provinceDrugLists)) {
            provinceDrugListBeans = ObjectCopyUtils.convert(provinceDrugLists, ProvinceDrugListBean.class);
            if (drugListMatch.getStatus().equals(DrugMatchConstant.ALREADY_MATCH) || drugListMatch.getStatus().equals(DrugMatchConstant.SUBMITED) || drugListMatch.getStatus().equals(DrugMatchConstant.MATCHING)) {
                for (ProvinceDrugListBean provinceDrugListBean : provinceDrugListBeans) {
                    //判断当前关联省平台药品code和关联code一致
                    if (null != provinceDrugListBean.getProvinceDrugCode() && provinceDrugListBean.getProvinceDrugCode().equals(drugListMatch.getRegulationDrugCode())) {
                        provinceDrugListBean.setMatched(true);
                    }
                }
            }
        }

        return provinceDrugListBeans;
    }

    /**
     * 取消已匹配状态和已提交状态
     */
    @RpcService
    public Integer cancelMatchStatusByOrgan(Integer drugId, String operator, Integer makeType, Boolean haveProvinceDrug) {
        if (StringUtils.isEmpty(operator)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        Map<String, Object> updateMap = new HashMap<>();
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        Integer status = 0;
        if(Platform_Type == makeType){
            if(haveProvinceDrug){
                status = getCancelStatus(drugListMatch, "cancelMatchStatusByOrgan 当前匹配药品[{}]状态[{}]不能取消平台匹配");
                if (status == null) return status;
            }else{
                status = DrugMatchConstant.UNMATCH;
            }
            updateMap.put("matchDrugId", null);
        }else if(Province_Platform_Type == makeType){
            status = getCancelStatus(drugListMatch, "cancelMatchStatusByOrgan 当前匹配药品[{}]状态[{}]不能取消省平台匹配");
            if (status == null) return status;
            updateMap.put("regulationDrugCode", null);
        }else{
            LOGGER.info("cancelMatchStatusByOrgan 传入操作状态非平台和省平台", makeType);
            return status;
        }
        updateMap.put("status", status);
        updateMap.put("operator", operator);
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, updateMap);
        LOGGER.info("cancelMatchStatusByOrgan 操作人取消关联->{}更新状态,drugId={};status ->before={},after={}", operator, drugId, drugListMatch.getStatus(), status);
        return status;
    }

    /*获取取消匹配后的对照状态状态*/
    private Integer getCancelStatus(DrugListMatch drugListMatch, String message) {
        Integer drugId = drugListMatch.getDrugId();
        Integer drugListMatchStatus = drugListMatch.getStatus();
        boolean haveMatchDrug = null != drugListMatch.getMatchDrugId();
        boolean haveRegulationDrugCode = null != drugListMatch.getRegulationDrugCode();

        Integer status;
        if(DrugMatchConstant.ALREADY_MATCH == drugListMatchStatus){
            status = DrugMatchConstant.MATCHING;
        }else if(DrugMatchConstant.MATCHING == drugListMatchStatus) {
            status = DrugMatchConstant.UNMATCH;
        }else if(DrugMatchConstant.SUBMITED == drugListMatchStatus){
            if((!haveMatchDrug && haveRegulationDrugCode) && (haveMatchDrug && !haveRegulationDrugCode)){
                status = DrugMatchConstant.UNMATCH;
            }else if(haveMatchDrug && haveRegulationDrugCode){
                status = DrugMatchConstant.MATCHING;
            }else{
                LOGGER.info(message, drugId, drugListMatchStatus);
                return null;
            }
        }else{
            LOGGER.info(message, drugId, drugListMatchStatus);
            return null;
        }
        return status;
    }


}
