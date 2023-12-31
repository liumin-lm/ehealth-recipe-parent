package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import com.ngari.recipe.vo.CheckItemListVo;
import com.ngari.recipe.vo.ItemListVO;
import com.ngari.revisit.RevisitBean;
import ctd.persistence.bean.QueryResult;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.client.OrganClient;
import recipe.client.PatientClient;
import recipe.client.RevisitClient;
import recipe.client.SmsClient;
import recipe.common.CommonConstant;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.enumerate.type.TherapyCancellationTypeEnum;
import recipe.manager.*;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;
import recipe.vo.doctor.RecipeInfoVO;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 诊疗处方 核心处理类
 *
 * @author fuzi
 */
@Service
public class TherapyRecipeBusinessService extends BaseService implements ITherapyRecipeBusinessService {

    public final String ITEM_NAME = "itemName";
    public final String ITEM_CODE = "itemCode";

    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private RecipeTherapyManager recipeTherapyManager;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private OrganClient organClient;
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Resource
    private RevisitClient revisitClient;
    @Resource
    private SmsClient smsClient;

    @Override
    public Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        //保存处方
        Recipe recipe = ObjectCopyUtils.convert(recipeInfoVO.getRecipeBean(), Recipe.class);
        recipe = recipeManager.saveRecipe(recipe);
        //保存处方扩展
        if (null != recipeInfoVO.getRecipeExtendBean()) {
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeInfoVO.getRecipeExtendBean(), RecipeExtend.class);
            String cardNo = recipeManager.getCardNoByRecipe(recipe);
            if (StringUtils.isNotEmpty(cardNo)) {
                recipeExtend.setCardNo(cardNo);
            }
            recipeManager.saveRecipeExtend(recipeExtend, recipe);
        }
        //保存处方明细
        if (!CollectionUtils.isEmpty(recipeInfoVO.getRecipeDetails())) {
            List<Recipedetail> details = ObjectCopyUtils.convert(recipeInfoVO.getRecipeDetails(), Recipedetail.class);
            recipeDetailManager.saveRecipeDetails(details, recipe);
        }
        //保存诊疗
        RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeInfoVO.getRecipeTherapyVO(), RecipeTherapy.class);
        if (null == recipeTherapy) {
            recipeTherapy = new RecipeTherapy();
        }
        recipeTherapy.setStatus(TherapyStatusEnum.READYSUBMIT.getType());
        recipeTherapyManager.saveRecipeTherapy(recipeTherapy, recipe);
        return recipe.getRecipeId();
    }

    @Override
    public Integer therapyRecipeTotal(RecipeTherapy recipeTherapy) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.therapyRecipeList(recipeTherapy);
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return 0;
        }
        return recipeTherapyList.size();
    }

    @Override
    public Integer therapyRecipeByMpiIdTotal(String mpiId) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.findTherapyPageByMpiIds(mpiId, null, null);
        return recipeTherapyList.size();
    }

    @Override
    public List<RecipeInfoDTO> therapyRecipeList(RecipeTherapy recipeTherapy, int start, int limit) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.therapyRecipeList(recipeTherapy, start, limit);
        return paddingRecipeInfoDTO(recipeTherapyList);
    }

    @Override
    public RecipeInfoDTO therapyRecipeInfo(Integer recipeId) {
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDTO(recipeId);
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipeId);
        recipePdfDTO.setRecipeTherapy(recipeTherapy);
        OrganDTO organDTO = organClient.organDTO(recipePdfDTO.getRecipe().getClinicOrgan());
        recipePdfDTO.setOrgan(organDTO);
        return recipePdfDTO;
    }

    @Override
    public boolean abolishTherapyRecipe(Integer recipeId) {
        return recipeTherapyManager.abolishTherapyRecipe(recipeId);
    }

    @Override
    public boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId) {
        List<RecipeTherapy> recipeTherapies = recipeTherapyManager.findTherapyByClinicId(clinicId);
        recipeTherapies.forEach(recipeTherapy -> {
            if (TherapyStatusEnum.READYSUBMIT.getType().equals(recipeTherapy.getStatus())) {
                recipeTherapy.setStatus(TherapyStatusEnum.HADECANCEL.getType());
                recipeTherapy.setTherapyCancellation("超时未提交");
                recipeTherapy.setTherapyCancellationType(TherapyCancellationTypeEnum.SYSTEM_CANCEL.getType());
                recipeTherapyManager.updateRecipeTherapy(recipeTherapy);
            }
        });
        return true;
    }

    @Override
    public void updatePushTherapyRecipe(Integer recipeId, RecipeTherapy recipeTherapy, Integer pushType) {
        if (CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            emrRecipeManager.updateDisease(recipeId);
            Recipe recipe = recipeManager.getRecipeById(recipeId);
            RecipeServiceSub.sendRecipeTagToPatient(recipe, null, null, true);
            //给患者推送微信模板消息
            smsClient.therapyRecipeApplyToPatient(recipe);
        }
        recipeTherapyManager.updatePushTherapyRecipe(recipeTherapy, pushType);
    }

    @Override
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO) {
        List<ItemList> itemLists = recipeTherapyManager.findItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
        return ObjectCopyUtils.convert(itemLists, ItemListVO.class);
    }

    @Override
    public boolean updateTherapyRecipe(Integer organId, String recipeCode, RecipeTherapyDTO recipeTherapyDTO) {
        Recipe recipe = recipeManager.getByRecipeCodeAndClinicOrgan(recipeCode, organId);
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipe.getRecipeId());
        ObjectCopyUtils.copyPropertiesIgnoreNull(recipeTherapyDTO, recipeTherapy);
        return recipeTherapyManager.updateRecipeTherapy(recipeTherapy);
    }

    @Override
    public List<RecipeInfoDTO> therapyRecipeListForPatient(String mpiId, int start, int limit) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.findTherapyPageByMpiIds(mpiId, start, limit);
        return paddingRecipeInfoDTO(recipeTherapyList);
    }

    @Override
    public List<RecipeTherapy> findTherapyByClinicId(Integer clinicId) {
        return recipeTherapyManager.findTherapyByClinicId(clinicId);
    }

    @Override
    public List<RecipeInfoDTO> therapyListByClinicId(Integer clinicId) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.findTherapyByClinicId(clinicId);
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return new LinkedList<>();
        }
        List<RecipeInfoDTO> list = new LinkedList<>();
        List<Integer> recipeIds = recipeTherapyList.stream().map(RecipeTherapy::getRecipeId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeManager.findByRecipeIds(recipeIds);
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, a -> a, (k1, k2) -> k1));
        Map<Integer, List<Recipedetail>> recipeDetailGroup = recipeDetailManager.findRecipeDetailMap(recipeIds);
        recipeTherapyList.forEach(a -> {
            RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
            recipeInfoDTO.setRecipeTherapy(a);
            recipeInfoDTO.setRecipe(recipeMap.get(a.getRecipeId()));
            recipeInfoDTO.setRecipeDetails(recipeDetailGroup.get(a.getRecipeId()));
            list.add(recipeInfoDTO);
        });
        return list;
    }

    @Override
    public QueryResult<RecipeTherapyOpDTO> findTherapyByInfo(RecipeTherapyOpQueryDTO recipeTherapyOpQueryDTO) {
        logger.info("TherapyRecipeBusinessService findTherapyByInfo recipeTherapyOpQueryDTO={}", JSONUtils.toString(recipeTherapyOpQueryDTO));
        QueryResult<RecipeTherapyOpBean> recipeTherapyRes = recipeTherapyManager.findTherapyByInfo(recipeTherapyOpQueryDTO);
        QueryResult<RecipeTherapyOpDTO> recipeTherapyReq = new QueryResult<>();
        if (null != recipeTherapyRes) {
            List<RecipeTherapyOpBean> items = recipeTherapyRes.getItems();
            recipeTherapyReq.setTotal(recipeTherapyRes.getTotal());
            if (null != recipeTherapyRes.getProperties()) {
                Map<String, Object> properties = recipeTherapyRes.getProperties();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    recipeTherapyReq.setProperty(key, entry.getValue());
                }
            }
            recipeTherapyReq.setStart(recipeTherapyRes.getStart());
            recipeTherapyReq.setLimit((int) recipeTherapyRes.getLimit());
            List<RecipeTherapyOpDTO>  recipeTherapyList = new ArrayList<>();
            for(RecipeTherapyOpBean item : items){
                PatientDTO patientDTO;
                try{
                    RecipeTherapyOpDTO recipeTherapyOpDTO = new RecipeTherapyOpDTO();
                    patientDTO = patientClient.getPatientDTO(item.getMpiId());
                    recipeTherapyOpDTO.setRecipeId(item.getRecipeId());
                    recipeTherapyOpDTO.setRecipeCode(item.getRecipeCode());
                    recipeTherapyOpDTO.setStatus(item.getStatus());
                    String createTime = item.getCreateTime().replace(".0", "");
                    recipeTherapyOpDTO.setCreateTime(createTime);
                    recipeTherapyOpDTO.setAppointDepartName(item.getAppointDepartName());
                    recipeTherapyOpDTO.setPatientName(item.getPatientName());
                    recipeTherapyOpDTO.setDoctorName(item.getDoctorName());
                    recipeTherapyOpDTO.setOrganName(item.getOrganName());
                    if(StringUtils.isNotEmpty(patientDTO.getMobile())){
                        recipeTherapyOpDTO.setPatientMobile(patientDTO.getMobile());
                    }
                    recipeTherapyList.add(recipeTherapyOpDTO);
                }catch (Exception e){
                    logger.error("TherapyRecipeBusinessService findTherapyByInfo error={}", JSONUtils.toString(e));
                }
            }
            recipeTherapyReq.setItems(recipeTherapyList);
        }
        logger.info("TherapyRecipeBusinessService findTherapyByInfo recipeTherapyReq={}", JSONUtils.toString(recipeTherapyReq));
        return recipeTherapyReq;
    }


    /**
     * 包装数据 返回填充对象
     *
     * @param recipeTherapyList 诊疗列表
     * @return 填充后的诊疗信息
     */
    private List<RecipeInfoDTO> paddingRecipeInfoDTO(List<RecipeTherapy> recipeTherapyList) {
        List<RecipeInfoDTO> list = new LinkedList<>();
        logger.info("TherapyRecipeBusinessService paddingRecipeInfoDTO recipeTherapyList:{}", JSON.toJSONString(recipeTherapyList));
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return list;
        }
        List<Integer> recipeIds = recipeTherapyList.stream().map(RecipeTherapy::getRecipeId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeManager.findByRecipeIds(recipeIds);
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, a -> a, (k1, k2) -> k1));
        Map<Integer, List<Recipedetail>> recipeDetailGroup = recipeDetailManager.findRecipeDetailMap(recipeIds);
        List<String> mpiIds = recipeTherapyList.stream().map(RecipeTherapy::getMpiId).distinct().collect(Collectors.toList());
        List<Integer> clinicIds = recipeTherapyList.stream().map(RecipeTherapy::getClinicId).collect(Collectors.toList());
        List<RevisitBean> revisitBeans = revisitClient.findByConsultIds(clinicIds);
        Map<Integer, RevisitBean> revisitBeanMap = revisitBeans.stream().collect(Collectors.toMap(RevisitBean::getConsultId, a -> a, (k1, k2) -> k1));
        Map<String, PatientDTO> patientMap = patientClient.findPatientMap(mpiIds);
        recipeTherapyList.forEach(a -> {
            RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
            recipeInfoDTO.setRecipeTherapy(a);
            recipeInfoDTO.setRevisitTime(DateConversion.getDateFormatter(revisitBeanMap.get(a.getClinicId()).getRequestTime(), DateConversion.DEFAULT_DATE_TIME));
            recipeInfoDTO.setRecipe(recipeMap.get(a.getRecipeId()));
            recipeInfoDTO.setRecipeDetails(recipeDetailGroup.get(a.getRecipeId()));
            recipeInfoDTO.setPatientBean(patientMap.get(a.getMpiId()));
            list.add(recipeInfoDTO);
        });
        return list;
    }


    @Override
    public QueryResult<ItemList> pageItemList(ItemListVO itemListVO) {
        QueryResult<ItemList> itemLists = recipeTherapyManager.pageItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
        return itemLists;
    }

    @Override
    public boolean saveItemList(ItemList itemList) {
        if (StringUtils.isNotEmpty(itemList.getItemName())) {
            List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(resByItemName)) {
                return false;
            }
        }
        if (StringUtils.isNotEmpty(itemList.getItemCode())) {
            List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(resByItemName)) {
                return false;
            }
        }
        recipeTherapyManager.saveItemList(itemList);
        return true;
    }

    @Override
    public boolean updateItemList(ItemList itemList) {
        AtomicBoolean res = new AtomicBoolean(true);
        if (StringUtils.isNotEmpty(itemList.getItemName())) {
            List<ItemList> itemListsByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
            itemListsByItemName.forEach(itemListByItemName -> {
                //存在一条id与当前传入itemList.getId不相同的就为false
                if (itemListByItemName != null && !itemList.getId().equals(itemListByItemName.getId())) {
                    res.set(false);
                }
            });
        }
        if (StringUtils.isNotEmpty(itemList.getItemCode())) {
            List<ItemList> itemListsByItemCode = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
            itemListsByItemCode.forEach(itemListByItemCode -> {
                //存在一条id与当前传入itemList.getId不相同的就为false
                if (itemListByItemCode != null && !itemList.getId().equals(itemListByItemCode.getId())) {
                    res.set(false);
                }
            });
        }
        if (!res.get()) {
            return false;
        }
        itemList.setGmtModified(new Date());
        recipeTherapyManager.updateItemList(itemList);
        return true;
    }

    @Override
    public ItemList getItemListById(ItemList itemList) {
        return recipeTherapyManager.getItemListById(itemList);
    }

    @Override
    public void batchUpdateItemList(List<ItemList> itemLists) {
        itemLists.forEach(itemList -> {
            if (itemList != null && itemList.getId() != null) {
                recipeTherapyManager.updateItemList(itemList);
            }
        });
    }

    @Override
    public List<ItemList> findItemListByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode) {
        return recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(organId, itemName, itemCode);
    }

    @Override
    public List<ItemList> findItemListByOrganIdAndItemNameOrCode2(Integer organId, String itemName, String itemCode) {
        return recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode2(organId, itemName, itemCode);
    }

    @Override
    public CheckItemListVo checkItemList(ItemList itemList) {
        CheckItemListVo checkItemListVo = new CheckItemListVo();
        List<String> cause = Arrays.asList("", "");
        AtomicBoolean result = new AtomicBoolean(true);
        //true表示可向下执行流程 false抛出错误
        AtomicBoolean res = new AtomicBoolean(true);
        if (itemList.getId() != null) {
            //修改
            if (StringUtils.isNotEmpty(itemList.getItemName())) {
                List<ItemList> itemListsByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
                itemListsByItemName.forEach(itemListByItemName -> {
                    //存在一条id与当前传入itemList.getId不相同的就为false
                    if (itemListByItemName != null && !itemList.getId().equals(itemListByItemName.getId())) {
                        result.set(false);
                        cause.set(0, ITEM_NAME);
                    }
                });
            }
            if (StringUtils.isNotEmpty(itemList.getItemCode())) {
                List<ItemList> itemListsByItemCode = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
                itemListsByItemCode.forEach(itemListByItemCode -> {
                    //存在一条id与当前传入itemList.getId不相同的就为false
                    if (itemListByItemCode != null && !itemList.getId().equals(itemListByItemCode.getId())) {
                        result.set(false);
                        cause.set(1, ITEM_CODE);
                    }
                });
            }
        } else {
            //新增
            if (StringUtils.isNotEmpty(itemList.getItemName())) {
                List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(resByItemName)) {
                    result.set(false);
                    cause.set(0, ITEM_NAME);
                }
            }
            if (StringUtils.isNotEmpty(itemList.getItemCode())) {
                List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(resByItemName)) {
                    result.set(false);
                    cause.set(1, ITEM_CODE);
                }
            }
        }
        checkItemListVo.setCause(cause);
        checkItemListVo.setResult(result.get());
        return checkItemListVo;
    }


}
