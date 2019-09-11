package recipe.drugTool.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drugTool.service.IDrugToolService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugListMatch;
import com.ngari.recipe.entity.DrugToolUser;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.OrganToolBean;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugListMatchDAO;
import recipe.dao.DrugToolUserDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.DrugMatchUtil;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * created by shiyuping on 2019/2/1
 */
@RpcBean(value = "drugToolService",mvc_authentication = false)
public class DrugToolService implements IDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugToolService.class);

    private double progress;

    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";
    //全局map
    private ConcurrentHashMap<String,Double> progressMap = new ConcurrentHashMap<>();
    /**
     * 用于药品小工具搜索历史记录缓存
     */
    private ConcurrentHashMap<String,ArrayBlockingQueue> cmap = new ConcurrentHashMap<>();

    @Resource
    private DrugListMatchDAO drugListMatchDAO;

    @Resource
    private DrugListDAO drugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Resource
    private DrugToolUserDAO drugToolUserDAO;

    @Resource
    private OrganService organService;

    private LoadingCache<String, List<DrugList>> drugListCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, List<DrugList>>() {
        @Override
        public List<DrugList> load(String str) throws Exception {
            return drugListDAO.findBySaleNameLike(str);
        }
    });

    @RpcService
    public void resetMatchCache(){
        drugListCache.cleanUp();
    }


    @RpcService
    public DrugToolUser loginOrRegist(String name, String mobile, String pwd){
        if (StringUtils.isEmpty(name)){
            throw new DAOException(DAOException.VALUE_NEEDED, "name is required");
        }
        if (StringUtils.isEmpty(mobile)){
            throw new DAOException(DAOException.VALUE_NEEDED, "mobile is required");
        }
        if (StringUtils.isEmpty(pwd)){
            throw new DAOException(DAOException.VALUE_NEEDED, "pwd is required");
        }
        DrugToolUser dbUser = drugToolUserDAO.getByMobile(mobile);
        if (dbUser == null){
            DrugToolUser user = new DrugToolUser();
            user.setName(name);
            user.setMobile(mobile);
            user.setPassword(pwd);
            user.setStatus(1);
            dbUser = drugToolUserDAO.save(user);
        }else {
            if (!(pwd.equals(dbUser.getPassword())&&name.equals(dbUser.getName()))){
                throw new DAOException(609, "姓名或密码不正确");
            }
        }
        return dbUser;
    }

    @RpcService
    public boolean isLogin(String mobile){
        if (StringUtils.isEmpty(mobile)){
            throw new DAOException(DAOException.VALUE_NEEDED, "mobile is required");
        }
        boolean result = false;
        DrugToolUser dbUser = drugToolUserDAO.getByMobile(mobile);
        if (dbUser != null){
            result = true;
        }
        return result;
    }

    //获取进度条
    @RpcService
    public synchronized double getProgress(int organId,String operator) throws InterruptedException {
        String key = organId +operator;
        Double data = progressMap.get(key);
        if (data != null){
            progress = data;
            if (progress >= 100){
                progressMap.remove(key);
            }
        }
        LOGGER.info("进度条加载={}=", progress);
        return progress;
    }

    @Override
    public  Map<String,Object> readDrugExcel(byte[] buf, String originalFilename, int organId, String operator) {
        LOGGER.info(operator + "开始 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        progress = 0;
        Map<String,Object> result = Maps.newHashMap();
        if (StringUtils.isEmpty(operator)){
            result.put("code",609);
            result.put("msg","operator is required");
            return result;
        }
        int length = buf.length;
        LOGGER.info("readDrugExcel byte[] length="+length);
        int max = 1343518;
        //控制导入数据量
        if (max <= length){
            result.put("code",609);
            result.put("msg","超过7000条数据,请分批导入");
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
            }else {
                result.put("code",609);
                result.put("msg","上传文件格式有问题");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("readDrugExcel error ,"+e.getMessage());
            result.put("code",609);
            result.put("msg","上传文件格式有问题");
            return result;
        }
        Sheet sheet = workbook.getSheetAt(0);
        Integer total = sheet.getLastRowNum();
        if (total == null || total <= 0) {
            result.put("code",609);
            result.put("msg","data is required");
            return result;
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                double progress;
                DrugListMatch drug;
                Row row;
                for (int rowIndex = 1; rowIndex <= total; rowIndex++) {
                    //循环获得每个行
                    row = sheet.getRow(rowIndex);
                    drug = new DrugListMatch();
                    boolean flag = true;
                    try{
//                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(0)))){
//                            result.put("code",609);
//                            result.put("msg","【第"+rowIndex+"行】存在药品编号为空，请重新导入");
//                            return result;
//                        }
//                        drug.setOrganDrugCode(getStrFromCell(row.getCell(0)));
//                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(1)))){
//                            result.put("code",609);
//                            result.put("msg","【第"+rowIndex+"行】药品名为空，请重新导入");
//                            return result;
//                        }
                        drug.setDrugName(getStrFromCell(row.getCell(1)));
                        drug.setSaleName(getStrFromCell(row.getCell(2)));
                        drug.setDrugSpec(getStrFromCell(row.getCell(3)));
                        if (("中药").equals(getStrFromCell(row.getCell(4)))){
                            drug.setDrugType(3);
                        }else if (("中成药").equals(getStrFromCell(row.getCell(4)))){
                            drug.setDrugType(2);
                        }else if (("西药").equals(getStrFromCell(row.getCell(4)))){
                            drug.setDrugType(1);
                        }
                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(5)))){
                            drug.setUseDose(null);
                        }else{
                            drug.setUseDose(Double.parseDouble(getStrFromCell(row.getCell(5))));
                        }
                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(6)))){
                            drug.setDefaultUseDose(null);
                        }else{
                            drug.setDefaultUseDose(Double.parseDouble(getStrFromCell(row.getCell(6))));
                        }
                        drug.setUseDoseUnit(getStrFromCell(row.getCell(7)));
                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(8)))){
                            drug.setPack(null);
                        }else{
                            drug.setPack(Integer.parseInt(getStrFromCell(row.getCell(8))));
                        }

                        drug.setUnit(getStrFromCell(row.getCell(9)));
                        drug.setProducer(getStrFromCell(row.getCell(10)));
                        String priceCell = getStrFromCell(row.getCell(11));
                        if (StringUtils.isEmpty(priceCell)){
                            drug.setPrice(null);
                        }else{
                            drug.setPrice(new BigDecimal(priceCell));
                        }
                        drug.setLicenseNumber(getStrFromCell(row.getCell(12)));
                        drug.setStandardCode(getStrFromCell(row.getCell(13)));
                        drug.setIndications(getStrFromCell(row.getCell(14)));
                        drug.setDrugForm(getStrFromCell(row.getCell(15)));
                        drug.setPackingMaterials(getStrFromCell(row.getCell(16)));
                        if (("是").equals(getStrFromCell(row.getCell(17)))){
                            drug.setBaseDrug(1);
                        }else if (("否").equals(getStrFromCell(row.getCell(17)))){
                            drug.setBaseDrug(0);
                        }
                        drug.setRetrievalCode(getStrFromCell(row.getCell(18)));
                        drug.setSourceOrgan(organId);
                        drug.setStatus(0);
                        drug.setOperator(operator);
                    }catch (Exception e){
                        LOGGER.error("药品小工具【第"+rowIndex+1+"行】导入字段有异常"+e.getMessage());
                        flag = false;
                    }
                    if (flag){
                        try{
                            boolean isSuccess = drugListMatchDAO.updateData(drug);
                            if (!isSuccess){
                                //自动匹配功能暂无法提供
                                //*AutoMatch(drug);*//*
                                drugListMatchDAO.save(drug);}
                        }catch(Exception e){
                            LOGGER.error("save or update drugListMatch error "+e.getMessage());
                        }
                    }
                    progress = new BigDecimal((float)rowIndex / total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    progressMap.put(organId+operator,progress*100);
                }
            }
        });
        t.run();

        LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code",200);
        return result;
    }


    /*private void AutoMatch(DrugListMatch drug) {
        List<DrugList> drugLists = drugListDAO.findByDrugName(drug.getDrugName());
        if (CollectionUtils.isNotEmpty(drugLists)){
            for (DrugList drugList : drugLists){
                if (drugList.getPack().equals(drug.getPack())
                        &&(drugList.getProducer().equals(drug.getProducer()))
                        &&(drugList.getUnit().equals(drug.getUnit()))
                        &&(drugList.getUseDose().equals(drug.getUseDose()))
                        &&(drugList.getDrugType().equals(drug.getDrugType()))){
                    drug.setStatus(1);
                    drug.setMatchDrugId(drugList.getDrugId());
                }
            }
        }
    }*/

    /**
     * 获取单元格值（字符串）
     * @param cell
     * @return
     */
    private String getStrFromCell(Cell cell){
        if(cell==null){
            return null;
        }
        //读取数据前设置单元格类型
        cell.setCellType(CellType.STRING);
        String strCell =cell.getStringCellValue();
        if(strCell!=null){
            strCell = strCell.trim();
            if(StringUtils.isEmpty(strCell)){
                strCell=null;
            }
        }
        return strCell ;
    }

    /**
     * 判断该机构是否已导入过
     */
    @RpcService
    public boolean isOrganImported(int organId){
        boolean isImported = true;
        List<DrugListMatch> drugLists = drugListMatchDAO.findMatchDataByOrgan(organId);
        if (CollectionUtils.isEmpty(drugLists)){
            isImported = false;
        }
        return isImported;
    }

    /**
     * 获取或刷新临时药品数据
     */
    @RpcService
    public QueryResult<DrugListMatch> findData(int organId, int start, int limit){
        return drugListMatchDAO.findMatchDataByOrgan(organId, start, limit);
    }

    /**
     * 更新无匹配数据
     */
    @RpcService
    public void updateNoMatchData(int drugId,String operator){
        if (StringUtils.isEmpty(operator)){
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        //如果是已匹配的取消匹配
        if (drugListMatch.getStatus().equals(1)){
            drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status",0,"operator",operator));
        }
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("isNew",1,"status",3,"operator",operator));
        LOGGER.info("updateNoMatchData 操作人->{}更新无匹配数据,drugId={};status ->before={},after=3",operator,drugId,drugListMatch.getStatus());
    }

    /**
     * 取消已匹配状态和已提交状态
     */
    @RpcService
    public void cancelMatchStatus(int drugId,String operator){
        if (StringUtils.isEmpty(operator)){
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status",0,"operator",operator));
        LOGGER.info("cancelMatchStatus 操作人->{}更新为未匹配状态,drugId={};status ->before={},after=0",operator,drugId,drugListMatch.getStatus());
    }

    /**
     * 更新已匹配状态(未匹配0，已匹配1，已提交2,已标记3)
     */
    @RpcService
    public void updateMatchStatus(int drugId,int matchDrugId,String operator){
        if (StringUtils.isEmpty(operator)){
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status",1,"matchDrugId",matchDrugId,"operator",operator));
        LOGGER.info("updateMatchStatus 操作人->{}更新已匹配状态,drugId={};status ->before={},after=1",operator,drugId,drugListMatch.getStatus());
    }

    /**
     * 查找能匹配的机构
     */
    @RpcService
    public List<OrganDTO> findOrgan(){
        return organService.findOrgans();
    }

    /**
     * 关键字模糊匹配机构
     */
    @RpcService
    public List<OrganDTO> findOrganLikeShortName(String shortName){
        return organService.findOrganLikeShortName(shortName);
    }

    /**
     *  查询所有机构并封装返回参数给前端，存入前端缓存使用
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
     * @param userkey 搜索人的唯一标识
     * @return
     * @throws InterruptedException
     */
    @RpcService
    public List<?> findOrganSearchHistoryRecord(String userkey) {
        LOGGER.info("findOrganSearchHistoryRecord =userkey={}==",userkey);
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
        LOGGER.info("findOrganSearchHistoryRecord HistoryRecord  queue{}==",queue.toString());
        return listCmap;
    }

    /**
     * 保存搜索人的历史记录，在导入药品库确定时调用
     * @param shortName 搜索内容
     * @param userkey 搜索人的唯一标识
     * @return
     * @throws InterruptedException
     */
    @RpcService
    public void saveShortNameRecord(String shortName,String organId,String userkey) throws InterruptedException {
        LOGGER.info("saveShortNameRecord shortName=={}==organId=={}==userkey={}==",shortName,organId,userkey);
        //创建一个存储容量为10的ArrayBlockingQueue对列
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
        OrganToolBean ort = new OrganToolBean();
        //当搜索框为空的情况，直接返回缓存中的历史记录数据
        if (!StringUtils.isEmpty(shortName)) {
            if (cmap.get(userkey) != null && cmap.get(userkey).size() > 0) {
                queue =  cmap.get(userkey);
            }

            ort.setOrganId(Integer.parseInt(organId));
            ort.setName(shortName);

            Object[] arrayQueue = queue.toArray();
            for (Object s : arrayQueue) {
                OrganToolBean t = (OrganToolBean)s;
                //通过organId过滤
                if(t.getOrganId() == Integer.parseInt(organId)){
                    queue.remove(s);
                    break;
                }
            }
            //当容量超过10个时，取出第一个元素并删除
            if (10 == queue.size()){
                queue.poll();
            }
            queue.put(ort);

            cmap.put(userkey,queue);
            LOGGER.info("saveShortNameRecord HistoryRecord  cmap{}==",cmap);
        }

    }



    /**
     * 药品匹配
     */
    @RpcService
    public List<DrugListBean> drugMatch(int drugId){
        DrugListMatch drugListMatch = drugListMatchDAO.get(drugId);

        String str = DrugMatchUtil.match(drugListMatch.getDrugName());
        //根据药品名取标准药品库查询相关药品
        List<DrugList> drugLists = null;
        List<DrugListBean> drugListBeans = null;
        try {
            drugLists = drugListCache.get(str);
        } catch (ExecutionException e) {
            LOGGER.error("drugMatch:"+e.getMessage());
        }

        //已匹配状态返回匹配药品id
        if (CollectionUtils.isNotEmpty(drugLists)){
            drugListBeans = ObjectCopyUtils.convert(drugLists, DrugListBean.class);
            if (drugListMatch.getStatus().equals(1) || drugListMatch.getStatus().equals(2)){
                for (DrugListBean drugListBean : drugListBeans){
                    if (drugListBean.getDrugId().equals(drugListMatch.getMatchDrugId())){
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
    public Map<String,Integer> drugManualCommit(final int organId){
        List<DrugListMatch> matchDataByOrgan = drugListMatchDAO.findMatchDataByOrgan(organId);
        final HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws Exception {
                List<DrugListMatch> lists = drugListMatchDAO.findReadyComimitDataByOrgan(organId);
                int num = 0;
                //更新数据到organDrugList并更新状态已提交
                for (DrugListMatch drugListMatch : lists){
                    if (drugListMatch.getStatus().equals(2) && drugListMatch.getMatchDrugId()!=null){
                        OrganDrugList organDrugList = new OrganDrugList();
                        organDrugList.setDrugId(drugListMatch.getMatchDrugId());
                        organDrugList.setOrganDrugCode(drugListMatch.getOrganDrugCode());
                        organDrugList.setOrganId(drugListMatch.getSourceOrgan());
                        if (drugListMatch.getPrice()==null){
                            organDrugList.setSalePrice(new BigDecimal(0));
                        }else {
                            organDrugList.setSalePrice(drugListMatch.getPrice());
                        }
                        organDrugList.setDrugName(drugListMatch.getDrugName());
                        if (StringUtils.isEmpty(drugListMatch.getSaleName())){
                            organDrugList.setSaleName(drugListMatch.getDrugName());
                        }else {
                            if (drugListMatch.getSaleName().equals(drugListMatch.getDrugName())){
                                organDrugList.setSaleName(drugListMatch.getSaleName());
                            }else {
                                organDrugList.setSaleName(drugListMatch.getSaleName()+" "+drugListMatch.getDrugName());
                            }

                        }

                        organDrugList.setUsingRate(drugListMatch.getUsingRate());
                        organDrugList.setUsePathways(drugListMatch.getUsePathways());
                        organDrugList.setProducer(drugListMatch.getProducer());
                        organDrugList.setUseDose(drugListMatch.getDefaultUseDose());
                        organDrugList.setRecommendedUseDose(drugListMatch.getUseDose());
                        organDrugList.setPack(drugListMatch.getPack());
                        organDrugList.setUnit(drugListMatch.getUnit());
                        organDrugList.setUseDoseUnit(drugListMatch.getUseDoseUnit());
                        organDrugList.setDrugSpec(drugListMatch.getDrugSpec());
                        organDrugList.setRetrievalCode(drugListMatch.getRetrievalCode());
                        organDrugList.setTakeMedicine(0);
                        organDrugList.setStatus(1);
                        organDrugList.setProducerCode("");
                        organDrugList.setLastModify(new Date());
                        Boolean isSuccess = organDrugListDAO.updateOrganDrugListByOrganIdAndOrganDrugCode(organDrugList.getOrganId(), organDrugList.getOrganDrugCode(), ImmutableMap.of("salePrice", organDrugList.getSalePrice()));
                        if (!isSuccess){
                            organDrugListDAO.save(organDrugList);
                            num = num + 1;
                        }
                    }
                }
                setResult(num);
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);
        Map<String,Integer> result = Maps.newHashMap();
        result.put("before",matchDataByOrgan.size());
        result.put("saveSuccess",action.getResult());
        LOGGER.info("drugManualCommit success  beforeNum= "+matchDataByOrgan.size()+"saveSuccessNum="+action.getResult());
        return result;
    }

    /**
     * 药品提交(将匹配完成的数据提交更新)----互联网六期改为人工提交
     */
    @RpcService
    public void drugCommit(final List<DrugListMatch> lists){
        for (DrugListMatch drugListMatch : lists){
            if (drugListMatch.getStatus().equals(1) && drugListMatch.getMatchDrugId()!=null){
                drugListMatch.setStatus(2);
                drugListMatchDAO.update(drugListMatch);
            }
        }
    }

    /**
     * 药品搜索(可根据药品名称，厂家等进行搜索)
     */
    @RpcService
    public QueryResult<DrugListMatch> drugSearch(int organId, String keyWord, Integer status , int start, int limit){
        return drugListMatchDAO.queryDrugListsByDrugNameAndStartAndLimit(organId,keyWord, status, start, limit);
    }

    /**
     * 获取用药频率和用药途径
     *
     */
    @RpcService
    public Map<String,Object> getUsingRateAndUsePathway() {
        Map<String,Object> result = Maps.newHashMap();
        List<DictionaryItem> usingRateList = new ArrayList<DictionaryItem>();
        List<DictionaryItem> usePathwayList = new ArrayList<DictionaryItem>();
        try {
            usingRateList = DictionaryController.instance().get("eh.cdr.dictionary.UsingRateWithKey")
                    .getSlice(null, 0, "");
            usePathwayList = DictionaryController.instance().get("eh.cdr.dictionary.UsePathwaysWithKey")
                    .getSlice(null, 0, "");
        } catch (ControllerException e) {
            LOGGER.error("getUsingRateAndUsePathway() error : " + e);
        }
        result.put("usingRate",usingRateList);
        result.put("usePathway",usePathwayList);
        return result;
    }

    @RpcService
    public void deleteDrugMatchData(Integer id,Boolean isOrganId){
        if (isOrganId){
            drugListMatchDAO.deleteByOrganId(id);
        }else {
            drugListMatchDAO.deleteById(id);
        }
    }

    @RpcService
    public void deleteOrganDrugData(Integer id,Boolean isOrganId){
        if (isOrganId){
            organDrugListDAO.deleteByOrganId(id);
        }else {
            organDrugListDAO.deleteById(id);
        }
    }

}
