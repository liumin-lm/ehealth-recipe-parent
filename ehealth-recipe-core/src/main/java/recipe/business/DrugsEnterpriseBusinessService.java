package recipe.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.manager.EnterpriseManager;
import recipe.util.ByteUtils;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;
import recipe.vo.patient.AddressAreaVo;
import recipe.vo.patient.CheckAddressReq;
import recipe.vo.patient.CheckAddressRes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description： 药企 业务类
 * @author： yinsheng
 * @date： 2021-12-08 18:58
 */
@Service
public class DrugsEnterpriseBusinessService extends BaseService implements IDrugsEnterpriseBusinessService {
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Autowired
    private OrganDrugsSaleConfigDAO organDrugsSaleConfigDAO;
    @Autowired
    private DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private EnterpriseDecoctionAddressDAO enterpriseDecoctionAddressDAO;
    @Autowired
    private EnterpriseAddressDAO enterpriseAddressDAO;
    @Autowired
    private OrganService organService;

    @Override
    public Boolean existEnterpriseByName(String name) {
        List<DrugsEnterprise> drugsEnterprises = enterpriseManager.findAllDrugsEnterpriseByName(name);
        if (CollectionUtils.isNotEmpty(drugsEnterprises)) {
            return true;
        }
        return false;
    }

    @Override
    public void saveOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        logger.info("DrugsEnterpriseBusinessService saveOrganEnterpriseRelation organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        if (Objects.isNull(relation)) {
            throw new DAOException("机构药企关联关系不存在");
        }
        String giveModeTypes = StringUtils.join(organEnterpriseRelationVo.getGiveModeTypes(), ByteUtils.COMMA);
        relation.setDrugsEnterpriseSupportGiveMode(giveModeTypes);
        String recipeTypes = StringUtils.join(organEnterpriseRelationVo.getRecipeTypes(), ByteUtils.COMMA);
        relation.setEnterpriseRecipeTypes(recipeTypes);
        String decoctionIds = StringUtils.join(organEnterpriseRelationVo.getDecoctionIds(), ByteUtils.COMMA);
        relation.setEnterpriseDecoctionIds(decoctionIds);
        organAndDrugsepRelationDAO.updateNonNullFieldByPrimaryKey(relation);
    }


    @Override
    public void saveOrganDrugsSaleConfig(OrganDrugsSaleConfigVo organDrugsSaleConfigVo) {
        OrganDrugsSaleConfig organDrugsSaleConfig = new OrganDrugsSaleConfig();
        BeanUtils.copyProperties(organDrugsSaleConfigVo, organDrugsSaleConfig);
        enterpriseManager.saveOrganDrugsSaleConfig(organDrugsSaleConfig);
    }

    @Override
    public OrganEnterpriseRelationVo getOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        logger.info("DrugsEnterpriseBusinessService getOrganEnterpriseRelation req organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        if (Objects.isNull(relation)) {
            throw new DAOException("请到机构配置关联药企");
        }
        if (StringUtils.isNotEmpty(relation.getDrugsEnterpriseSupportGiveMode())) {
            List<Integer> giveModeTypes = Arrays.stream(relation.getDrugsEnterpriseSupportGiveMode().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            organEnterpriseRelationVo.setGiveModeTypes(giveModeTypes);
        }
        if (StringUtils.isNotEmpty(relation.getEnterpriseDecoctionIds())) {
            List<Integer> enterpriseDecoctionIds = Arrays.stream(relation.getEnterpriseDecoctionIds().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            organEnterpriseRelationVo.setDecoctionIds(enterpriseDecoctionIds);
        }
        if (StringUtils.isNotEmpty(relation.getEnterpriseRecipeTypes())) {
            List<Integer> enterpriseRecipeTypes = Arrays.stream(relation.getEnterpriseRecipeTypes().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            organEnterpriseRelationVo.setRecipeTypes(enterpriseRecipeTypes);
        }
        logger.info("DrugsEnterpriseBusinessService getOrganEnterpriseRelation res organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        return organEnterpriseRelationVo;
    }

    @Override
    public OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer drugsEnterpriseId) {
        OrganDrugsSaleConfig byOrganIdAndEnterpriseId = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterpriseId);

        return byOrganIdAndEnterpriseId;
    }

    @Override
    public QueryResult<DrugsEnterprise> drugsEnterpriseLimit(OrganEnterpriseRelationVo vo) {
        List<Integer> drugsEnterpriseIds = null;
        if (1 == vo.getType()) {
            List<DrugsEnterprise> drugsEnterpriseList = enterpriseManager.drugsEnterpriseByOrganId(vo.getOrganId());
            if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
                return null;
            }
            vo.setOrganId(null);
            drugsEnterpriseIds = drugsEnterpriseList.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        }
        logger.info("DrugsEnterpriseBusinessService drugsEnterpriseLimit drugsEnterpriseIds:{}", JSON.toJSONString(drugsEnterpriseIds));
        return enterpriseManager.drugsEnterpriseLimit(vo.getName(), vo.getCreateType(), vo.getOrganId(), vo.getStart(), vo.getLimit(), drugsEnterpriseIds);
    }

    @Override
    public List<PharmacyVO> pharmacy() {
        List<Pharmacy> list = enterpriseManager.pharmacy();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return ObjectCopyUtils.convert(list, PharmacyVO.class);
    }

    @Override
    public void addEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        // 先删除所有机构药企煎法关联的地址
        enterpriseManager.deleteEnterpriseDecoctionAddress(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());

        //如果没有具体的煎法关联地址传进来,默认不需要更新
        if (CollectionUtils.isEmpty(enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS())) {
            return;
        }

        // 更新机构药企煎法关联的地址
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = BeanCopyUtils.copyList(enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS(), EnterpriseDecoctionAddress::new);
        enterpriseManager.addEnterpriseDecoctionAddressList(enterpriseDecoctionAddresses);
    }

    @Override
    public List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        return enterpriseManager.findEnterpriseDecoctionAddressList(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());
    }

    @Override
    public List<OrganAndDrugsepRelation> findOrganAndDrugsepRelationBean(Integer enterpriseId) {
        UserRoleToken ur = UserRoleToken.getCurrent();
        String manageUnit = ur.getManageUnit();
        // 机构管理员获取机构信息
        if (!"eh".equals(manageUnit)) {
            List<Integer> organIds = organService.findOrganIdsByManageUnit(manageUnit + "%");
            logger.info("findOrganAndDrugsepRelationBean manageUnit={},organIds={}", JSONArray.toJSONString(organIds), JSONArray.toJSONString(manageUnit));
            return organAndDrugsepRelationDAO.findByEntIdAndOrganIds(enterpriseId, organIds);
        }
        return organAndDrugsepRelationDAO.findByEntId(enterpriseId);
    }

    @Override
    public List<EnterpriseDecoctionList> findEnterpriseDecoctionList(Integer enterpriseId, Integer organId) {
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, enterpriseId);
        if (Objects.isNull(relation)) {
            return null;
        }
        String enterpriseDecoctionIds = relation.getEnterpriseDecoctionIds();
        if (StringUtils.isEmpty(enterpriseDecoctionIds)) {
            return null;
        }
        String[] split = enterpriseDecoctionIds.split(",");
        List<Integer> list = new ArrayList<>();
        for (String s : split) {
            list.add(Integer.valueOf(s));
        }
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressListByOrganIdAndEntId(organId, enterpriseId);
        Map<Integer, List<EnterpriseDecoctionAddress>> collect = null;
        if (CollectionUtils.isNotEmpty(enterpriseDecoctionAddresses)) {
            collect = enterpriseDecoctionAddresses.stream().collect(Collectors.groupingBy(EnterpriseDecoctionAddress::getDecoctionId));
        }
        // 获取机构下的所有煎法
        List<DecoctionWay> decoctionWayList = drugDecoctionWayDao.findByOrganId(organId);
        Map<Integer, List<EnterpriseDecoctionAddress>> finalCollect = collect;
        List<EnterpriseDecoctionList> enterpriseDecoctionLists = decoctionWayList.stream().map(decoctionWay -> {
            EnterpriseDecoctionList enterpriseDecoctionList = null;
            if ("-1".equals(enterpriseDecoctionIds) || list.contains(decoctionWay.getDecoctionId())) {
                enterpriseDecoctionList = new EnterpriseDecoctionList();
                enterpriseDecoctionList.setDecoctionId(decoctionWay.getDecoctionId());
                enterpriseDecoctionList.setDecoctionName(decoctionWay.getDecoctionText());
                int status = 0;
                if (MapUtils.isNotEmpty(finalCollect) && CollectionUtils.isNotEmpty(finalCollect.get(decoctionWay.getDecoctionId()))) {
                    status = 1;
                }
                enterpriseDecoctionList.setStatus(status);
            }
            return enterpriseDecoctionList;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return enterpriseDecoctionLists;
    }

    @Override
    public CheckAddressRes checkEnterpriseDecoctionAddress(CheckAddressReq checkAddressReq) {
        CheckAddressRes checkAddressRes = new CheckAddressRes();
        Boolean sendFlag = false;
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddressList = enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressList(checkAddressReq.getOrganId(),
                checkAddressReq.getEnterpriseId(),
                checkAddressReq.getDecoctionId());
        if (CollectionUtils.isEmpty(enterpriseDecoctionAddressList)) {
            List<EnterpriseAddress> list = enterpriseAddressDAO.findByEnterPriseId(checkAddressReq.getEnterpriseId());
            if (CollectionUtils.isNotEmpty(list)) {
                List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = BeanCopyUtils.copyList(list, EnterpriseDecoctionAddress::new);
                if (addressCanSend(enterpriseDecoctionAddresses, checkAddressReq.getAddress3())) {
                    sendFlag = true;
                    checkAddressRes.setSendFlag(sendFlag);
                    return checkAddressRes;
                }
            }
        }
        List<AddressAreaVo> list = enterpriseDecoctionAddressList.stream().map(enterpriseDecoctionAddress -> {
            AddressAreaVo addressAreaVo = null;
            if (enterpriseDecoctionAddress.getAddress().length() == 2) {
                addressAreaVo = new AddressAreaVo();
                addressAreaVo.setAddress1(enterpriseDecoctionAddress.getAddress());
            }
            return addressAreaVo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        checkAddressRes.setAreaList(list);
        // 配送地址精确到区域,区域可以配送就可以配送
        if (addressCanSend(enterpriseDecoctionAddressList, checkAddressReq.getAddress3())) {
            sendFlag = true;
        }
        checkAddressRes.setSendFlag(sendFlag);
        return checkAddressRes;
    }

    @Override
    public boolean retryPushRecipeOrder(Integer recipeId) {
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugEnterpriseResult result = remoteDrugEnterpriseService.pushSingleRecipeInfo(recipeId);
        return result.getCode() != 1 ? false : true;
    }

    private boolean addressCanSend(List<EnterpriseDecoctionAddress> list, String address) {
        boolean flag = false;
        if (StringUtils.isEmpty(address)) {
            return flag;
        }
        for (EnterpriseDecoctionAddress e : list) {
            if (e.getAddress().startsWith(address)) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}
