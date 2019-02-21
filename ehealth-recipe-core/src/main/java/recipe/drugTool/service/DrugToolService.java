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
import recipe.dao.DrugListDAO;
import recipe.dao.DrugListMatchDAO;
import recipe.dao.DrugToolUserDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.DrugMatchUtil;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * created by shiyuping on 2019/2/1
 */
@RpcBean(value = "drugToolService",mvc_authentication = false)
public class DrugToolService implements IDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugToolService.class);

    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";
    //全局map
    private Map<String,Double> progressMap = Maps.newHashMap();

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

    private LoadingCache<String, List<DrugList>> drugListCache = CacheBuilder.newBuilder().build(new CacheLoader<String, List<DrugList>>() {
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
    public double getProgress(int organId,String operator) {
        double progress = 0;
        /*if (progressNum != null&&total != null){
            progress = new BigDecimal((float)progressNum / total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }*/
        Double data = progressMap.get(organId + operator);
        if (data != null){
            progress = data;
        }
        return progress;
    }

    public void readDrugExcel(byte[] buf, String originalFilename, int organId, String operator) {
        if (StringUtils.isEmpty(operator)){
            throw new DAOException(DAOException.VALUE_NEEDED, "operator is required");
        }
        InputStream is = new ByteArrayInputStream(buf);
        //获得用户上传工作簿
        Workbook workbook = null;
        try {
            if (originalFilename.endsWith(SUFFIX_2003)) {
                workbook = new HSSFWorkbook(is);
            } else if (originalFilename.endsWith(SUFFIX_2007)) {
                workbook = new XSSFWorkbook(is);
            }else {
                throw new DAOException("上传文件格式有问题");
            }
        } catch (Exception e) {
            throw new DAOException("上传文件格式有问题");
        }
        Sheet sheet = workbook.getSheetAt(0);
        Integer total = sheet.getLastRowNum();
        if (total == null || total <= 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "data is required");
        }

        double progress;
        for (int rowIndex = 1; rowIndex <= total; rowIndex++) {
            Row row = sheet.getRow(rowIndex);//循环获得每个行
            DrugListMatch drug = new DrugListMatch();
            if (StringUtils.isEmpty(getStrFromCell(row.getCell(0)))){
                throw new DAOException(DAOException.VALUE_NEEDED, "存在药品编号为空，请重新导入");
            }
            drug.setOrganDrugCode(getStrFromCell(row.getCell(0)));
            if (StringUtils.isEmpty(getStrFromCell(row.getCell(1)))){
                throw new DAOException(DAOException.VALUE_NEEDED, "存在药品名为空，请重新导入");
            }
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
            drug.setUseDoseUnit(getStrFromCell(row.getCell(7)));
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(5)))){
                    drug.setPack(null);
                }else{
                    drug.setPack(Integer.parseInt(getStrFromCell(row.getCell(8))));
                }
            }catch (Exception e){
                LOGGER.error("pack字段错误"+e.getMessage());
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
            drug.setSourceOrgan(organId);
            drug.setStatus(0);
            drug.setOperator(operator);

            boolean isSuccess = drugListMatchDAO.updateData(drug);
            if (!isSuccess){
                //自动匹配功能暂无法提供
                /*AutoMatch(drug);*/
                drugListMatchDAO.save(drug);
            }
            progress = new BigDecimal((float)rowIndex / total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            progressMap.put(organId+operator,progress*100);
        }
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
    public List<DrugListMatch> findData(int organId, int start, int limit){
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
        if (drugListMatch.getStatus().equals(2)){
            //删除organDrugList记录
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(drugListMatch.getSourceOrgan(),drugListMatch.getOrganDrugCode());
            organDrugListDAO.remove(organDrugList.getOrganDrugId());
        }
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status",0,"operator",operator));
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
        //如果是已提交状态再次修改，先删除原来的
        if (drugListMatch.getStatus().equals(2)){
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(drugListMatch.getSourceOrgan(),drugListMatch.getOrganDrugCode());
            organDrugListDAO.remove(organDrugList.getOrganDrugId());
        }
        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status",1,"matchDrugId",matchDrugId,"operator",operator));
    }

    /**
     * 查找能匹配的机构
     */
    @RpcService
    public List<OrganDTO> findOrgan(){
        return organService.findOrgans();
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
     * 药品提交(将匹配完成的数据提交更新)
     */
    @RpcService
    public void drugCommit(final List<DrugListMatch> lists){
        HibernateStatelessResultAction<String> action = new AbstractHibernateStatelessResultAction<String>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws Exception {
                //更新数据到organDrugList并更新状态已提交
                for (DrugListMatch drugListMatch : lists){
                    Integer drugId = drugListMatch.getDrugId();
                    if (drugListMatch.getStatus().equals(1) && drugListMatch.getMatchDrugId()!=null){
                        OrganDrugList organDrugList = new OrganDrugList();
                        organDrugList.setDrugId(drugListMatch.getMatchDrugId());
                        organDrugList.setOrganDrugCode(drugListMatch.getOrganDrugCode());
                        organDrugList.setOrganId(drugListMatch.getSourceOrgan());
                        organDrugList.setSalePrice(drugListMatch.getPrice());
                        organDrugList.setTakeMedicine(0);
                        organDrugList.setStatus(1);
                        organDrugList.setProducerCode("");
                        organDrugListDAO.save(organDrugList);
                        drugListMatchDAO.updateDrugListMatchInfoById(drugId, ImmutableMap.of("status",2));
                    }
                }
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);

    }

    /**
     * 药品搜索(可根据药品名称，厂家等进行搜索)
     */
    @RpcService
    public QueryResult<DrugListMatch> drugSearch(int organId, String keyWord, int status , int start, int limit){
        return drugListMatchDAO.queryDrugListsByDrugNameAndStartAndLimit(organId,keyWord, status, start, limit);
    }
}
