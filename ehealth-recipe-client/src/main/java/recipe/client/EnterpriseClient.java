package recipe.client;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.currentuserinfo.model.SimpleThirdBean;
import com.ngari.base.currentuserinfo.model.SimpleWxAccountBean;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.RecipePDFToHisTO;
import com.ngari.his.recipe.mode.RecipeThirdUrlReqTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.mvc.upload.FileMetaRecord;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.third.IFileDownloadService;
import recipe.util.ByteUtils;

import java.util.List;

/**
 * 药企对接处理类
 *
 * @author fuzi
 */
@Service
public class EnterpriseClient extends BaseClient {
    @Autowired
    private ICurrentUserInfoService userInfoService;
    @Autowired
    private IFileDownloadService fileDownloadService;
    @Autowired
    private IRecipeEnterpriseService recipeEnterpriseService;

    /**
     * 获取跳转第三方地址
     *
     * @param req
     * @return
     */
    public SkipThirdDTO skipThird(RecipeThirdUrlReqTO req) {
        logger.info("getRecipeThirdUrl request={}", JSONUtils.toString(req));
        try {
            HisResponseTO<String> response = recipeEnterpriseService.getRecipeThirdUrl(req);
            String thirdUrl = getResponse(response);
            SkipThirdDTO skipThirdDTO = new SkipThirdDTO();
            try {
                skipThirdDTO = JSONObject.parseObject(thirdUrl, SkipThirdDTO.class);
            } catch (Exception e) {
                //前置机传过来的可能是json字符串也可能是非json ,说明不是标准的JSON格式
                skipThirdDTO.setUrl(thirdUrl);
            }
            return skipThirdDTO;
        } catch (Exception e) {
            logger.error("EnterpriseClient skipThird error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "获取第三方跳转链接异常");
        }
    }

    /**
     * 黄河医院获取药企患者id
     * @return
     */
    public SimpleThirdBean getSimpleWxAccount(){
        try {
            SimpleWxAccountBean account = userInfoService.getSimpleWxAccount();
            logger.info("EnterpriseClient getSimpleWxAccount account={}", JSONObject.toJSONString(account));
            if (null == account) {
                return new SimpleThirdBean();
            }
            if (account instanceof SimpleThirdBean) {
                return (SimpleThirdBean) account;
            }
        } catch (Exception e) {
            logger.error("EnterpriseClient getSimpleWxAccount error", e);
        }
        return new SimpleThirdBean();
    }


    /**
     * 上传处方pdf给第三方
     *
     * @param recipe
     */
    public void uploadRecipePdfToHis(Recipe recipe) {
        if (recipe == null || StringUtils.isEmpty(recipe.getSignFile())) {
            return;
        }
        RecipePDFToHisTO req = new RecipePDFToHisTO();
        req.setOrganId(recipe.getClinicOrgan());
        req.setRecipeId(recipe.getRecipeId());
        req.setRecipeCode(recipe.getRecipeCode());
        FileMetaRecord fileMetaRecord = fileDownloadService.downloadAsRecord(recipe.getSignFile());
        if (fileMetaRecord != null) {
            req.setRecipePdfName(fileMetaRecord.getFileName());
        }
        req.setRecipePdfData(fileDownloadService.downloadAsByte(recipe.getSignFile()));
        recipeHisService.sendRecipePDFToHis(req);
    }

    /**
     * 前置机推送药企
     *
     * @param pushRecipeAndOrder
     * @param node
     * @return
     */
    public SkipThirdDTO pushRecipeInfoForThird(PushRecipeAndOrder pushRecipeAndOrder, Integer node) {
        pushRecipeAndOrder.setNode(node);
        HisResponseTO responseTO = recipeEnterpriseService.pushSingleRecipeInfo(pushRecipeAndOrder);
        logger.info("EnterpriseClient pushRecipeInfoForThird recipeId:{} responseTO:{}.",
                pushRecipeAndOrder.getRecipeBean().getRecipeId(), JSONUtils.toString(responseTO));
        SkipThirdDTO result = new SkipThirdDTO();
        //推送药企失败
        result.setType(ByteUtils.strValueOf(ByteUtils.objValueOf(responseTO.getExtend().get("type"))));
        if (null == responseTO || !responseTO.isSuccess()) {
            result.setCode(0);
            result.setMsg(responseTO.getMsg());
            return result;
        }
        //推送药企处方成功,判断是否为扁鹊平台
        result.setCode(1);
        result.setPrescId(ByteUtils.objValueOf(responseTO.getExtend().get("prescId")));
        result.setUrl(ByteUtils.objValueOf(responseTO.getExtend().get("urlCode")));
        return result;
    }

    /**
     * 获取取药站点
     * @param medicineStationDTO 站点信息
     * @param organBean 机构信息
     * @param enterpriseBean 药企信息
     * @return 取药站点列表
     */
    public List<MedicineStationDTO> getMedicineStationList(MedicineStationDTO medicineStationDTO, OrganBean organBean, DrugsEnterpriseBean enterpriseBean){
        logger.info("EnterpriseClient getMedicineStationList medicineStationDTO:{},organBean:{},enterpriseBean:{}.", JSONUtils.toString(medicineStationDTO)
        , JSONUtils.toString(organBean), JSONUtils.toString(enterpriseBean));
        try {
            MedicineStationReqDTO medicineStationReqDTO = new MedicineStationReqDTO();
            medicineStationReqDTO.setMedicineStationDTO(medicineStationDTO);
            medicineStationReqDTO.setOrganBean(organBean);
            medicineStationReqDTO.setDrugsEnterpriseBean(enterpriseBean);
            HisResponseTO<List<MedicineStationDTO>> response = recipeEnterpriseService.getMedicineStationList(medicineStationReqDTO);
            List<MedicineStationDTO> medicineStationDTOList = getResponse(response);
            return medicineStationDTOList;
        } catch (Exception e) {
            logger.error("EnterpriseClient getMedicineStationList medicineStationDTOList", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 推送数据组织地址
     *
     * @param recipeOrder        处方订单信息
     * @param pushRecipeAndOrder 推送处方信息
     */
    public void addressBean(RecipeOrder recipeOrder, PushRecipeAndOrder pushRecipeAndOrder) {
        pushRecipeAndOrder.setRecipeOrderBean(ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class));
        AddressBean addressBean = new AddressBean();
        addressBean.setProvince(getAddress(recipeOrder.getAddress1()));
        addressBean.setCity(getAddress(recipeOrder.getAddress2()));
        addressBean.setDistrict(getAddress(recipeOrder.getAddress3()));
        addressBean.setStreetAddress(getAddress(recipeOrder.getStreetAddress()));
        addressBean.setAddress(getAddress(recipeOrder.getAddress4()));
        pushRecipeAndOrder.setAddressBean(addressBean);
    }

}
