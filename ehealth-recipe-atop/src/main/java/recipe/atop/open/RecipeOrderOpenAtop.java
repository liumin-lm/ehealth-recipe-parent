package recipe.atop.open;

import com.alibaba.fastjson.JSONArray;
import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeOrderDto;
import com.ngari.recipe.dto.ReimbursementDTO;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.*;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.IRecipeOrderAtopService;
import recipe.atop.BaseAtop;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.ImperfectInfoVO;
import recipe.vo.greenroom.RecipeRefundInfoReqVO;
import recipe.vo.second.CabinetVO;
import recipe.vo.second.OrderPharmacyVO;
import recipe.vo.second.RecipeOrderVO;
import recipe.vo.second.RecipeVo;
import recipe.vo.second.enterpriseOrder.DownOrderRequestVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDownDataVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description： 处方订单 open 实现类
 * @author： whf
 * @date： 2021-11-08 15:46
 */
@RpcBean("recipeOrderOpenAtop")
public class RecipeOrderOpenAtop extends BaseAtop implements IRecipeOrderAtopService {
    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;

    @Override
    public RecipeOrderVO getRecipeOrderByBusId(Integer orderId) {
        logger.info("RecipeOrderOpenAtop getRecipeOrderByBusId req orderId={}", orderId);
        validateAtop(orderId);
        RecipeOrderDto recipeOrderDto = recipeOrderService.getRecipeOrderByBusId(orderId);
        if(Objects.isNull(recipeOrderDto)){
            return null;
        }
        RecipeOrderVO recipeOrderVO = new RecipeOrderVO();
        BeanUtils.copyProperties(recipeOrderDto,recipeOrderVO);
        List<RecipeVo> collect = recipeOrderDto.getRecipeList().stream().map(recipeBeanDTO -> {
            RecipeVo recipeVo = new RecipeVo();
            BeanCopyUtils.copy(recipeBeanDTO,recipeVo);
            return recipeVo;
        }).collect(Collectors.toList());
        recipeOrderVO.setRecipeVos(collect);
        logger.info("RecipeOrderOpenAtop getRecipeOrderByBusId res  recipeOrderVO={}", JSONArray.toJSONString(recipeOrderVO));
        return recipeOrderVO;
    }

    @Override
    public CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request) {
        logger.info("RecipeOrderOpenAtop getRevisitPageForCommonOrder req request={}", JSONArray.toJSONString(request));
        CheckRequestCommonOrderPageDTO checkRequestCommonOrderPageDTO = new CheckRequestCommonOrderPageDTO();
        if (request.getPage() == null || request.getSize() == null) {
            return checkRequestCommonOrderPageDTO;
        }
        checkRequestCommonOrderPageDTO = recipeOrderService.getRecipePageForCommonOrder(request);
        logger.info("RecipeOrderOpenAtop getRevisitPageForCommonOrder res CheckRequestCommonOrderPageDTO={}", JSONArray.toJSONString(checkRequestCommonOrderPageDTO));
        return checkRequestCommonOrderPageDTO;
    }

    @Override
    public Boolean updateTrackingNumberByOrderCode(String orderCode, String trackingNumber) {
        validateAtop(orderCode, trackingNumber);
        return recipeOrderService.updateTrackingNumberByOrderCode(orderCode, trackingNumber);
    }

    @Override
    public EnterpriseDownDataVO findOrderAndRecipes(DownOrderRequestVO downOrderRequestVO) {
        validateAtop(downOrderRequestVO, downOrderRequestVO.getAppKey());
        validateAtop(downOrderRequestVO.getBeginTime(), downOrderRequestVO.getEndTime());
        return recipeOrderService.findOrderAndRecipes(downOrderRequestVO);
    }

    @Override
    public RecipeResultBean cancelOrderByRecipeId(Integer recipeId, Integer status) {
        return recipeOrderService.cancelOrderByRecipeId(recipeId, status);
    }

    @Override
    public com.ngari.platform.recipe.mode.RecipeOrderBean getTrackingNumber(RecipeBean recipeBean) {
        validateAtop(recipeBean, recipeBean.getClinicOrgan(), recipeBean.getRecipeCode());
        RecipeOrder recipeOrder = recipeOrderService.getTrackingNumber(recipeBean.getRecipeCode(), recipeBean.getClinicOrgan());
        if (null == recipeOrder) {
            return null;
        }
        return ObjectCopyUtils.convert(recipeOrder, com.ngari.platform.recipe.mode.RecipeOrderBean.class);
    }

    @Override
    public List<ReimbursementListResVO> findReimbursementList(ReimbursementListReqVO reimbursementListReq) {
        validateAtop(reimbursementListReq.getOrganId(),reimbursementListReq.getMpiId());
        List<ReimbursementListResVO> reimbursementListResVOList = new ArrayList<>();
        List<ReimbursementDTO> reimbursementList = recipeOrderService.findReimbursementList(reimbursementListReq);
        if(CollectionUtils.isEmpty(reimbursementList)){
            return null;
        }
        for(ReimbursementDTO reimbursementDTO : reimbursementList){
            List<RecipeDetailBean> recipeDetailBeanList = new ArrayList<>();
            ReimbursementListResVO reimbursementListResVO = new ReimbursementListResVO();
            reimbursementListResVO.setRecipeId(reimbursementDTO.getRecipe().getRecipeId());
            PatientDTO patientDTO = reimbursementDTO.getPatientDTO();
            reimbursementListResVO.setName(patientDTO.getPatientName());
            reimbursementListResVO.setSex(patientDTO.getPatientSex().equals("1") ? "男":"女");
            reimbursementListResVO.setAge(patientDTO.getAge());
            reimbursementListResVO.setPayTime(reimbursementDTO.getRecipeOrder().getPayTime());
            reimbursementListResVO.setInvoiceNumber(reimbursementDTO.getInvoiceNumber());
            reimbursementListResVO.setMedicalFlag(reimbursementDTO.getRecipeOrder().getFundAmount() == null ? "自费":"医保");
            for(Recipedetail recipedetail : reimbursementDTO.getRecipeDetailList()){
                RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
                recipeDetailBean.setOrganDrugCode(recipedetail.getOrganDrugCode());
                recipeDetailBean.setDrugName(recipedetail.getDrugName());
                recipeDetailBeanList.add(recipeDetailBean);
            }
            reimbursementListResVO.setRecipeDetail(recipeDetailBeanList);
            reimbursementListResVOList.add(reimbursementListResVO);
        }
        logger.info("findReimbursementList reimbursementListResVOList={}", JSONUtils.toString(reimbursementListResVOList));
        return reimbursementListResVOList;
    }

    @Override
    public ReimbursementDetailResVO findReimbursementDetail(Integer recipeId) {
        validateAtop(recipeId);
        ReimbursementDetailResVO reimbursementDetailVO = new ReimbursementDetailResVO();
        ReimbursementDTO reimbursementDetailDTO = recipeOrderService.findReimbursementDetail(recipeId);
        if(reimbursementDetailDTO != null){
            reimbursementDetailVO.setRecipeType(reimbursementDetailDTO.getRecipe().getRecipeType());
            reimbursementDetailVO.setInvoiceNumber(reimbursementDetailDTO.getInvoiceNumber());
            reimbursementDetailVO.setPatientId(reimbursementDetailDTO.getRecipe().getPatientID());
            RecipeOrder recipeOrder = reimbursementDetailDTO.getRecipeOrder();
            if(recipeOrder != null){
                reimbursementDetailVO.setPayTime(recipeOrder.getPayTime());
                reimbursementDetailVO.setMedicalFlag(recipeOrder.getFundAmount() == null ? "自费":"医保");
            }
            PatientDTO patientDTO = reimbursementDetailDTO.getPatientDTO();
            if(patientDTO != null){
                reimbursementDetailVO.setName(patientDTO.getPatientName());
            }
            List<Recipedetail> recipeDetailList = reimbursementDetailDTO.getRecipeDetailList();
            if(CollectionUtils.isNotEmpty(recipeDetailList)){
                reimbursementDetailVO.setRecipeDetailList(ObjectCopyUtils.convert(recipeDetailList, RecipeDetailBean.class));
            }
        }
        logger.info("findReimbursementDetail reimbursementDetailVO={}", JSONUtils.toString(reimbursementDetailVO));
        return reimbursementDetailVO;
    }

    @Override
    public Integer thirdCreateOrder(ThirdCreateOrderReqDTO thirdCreateOrderReqDTO) {
        logger.info("RecipeOrderOpenAtop thirdCreateOrder thirdCreateOrderReqDTO:{}.", JSONUtils.toString(thirdCreateOrderReqDTO));
        return recipeOrderService.thirdCreateOrder(thirdCreateOrderReqDTO);
    }

    @Override
    public ThirdOrderPreSettleRes thirdOrderPreSettle(ThirdOrderPreSettleReq thirdOrderPreSettleReq) {
        return recipeOrderService.thirdOrderPreSettle(thirdOrderPreSettleReq);
    }

    @Override
    public CabinetVO validateCabinetRecipeStatus(CabinetVO cabinetVO) {
        validateAtop(cabinetVO.getOrganId(),cabinetVO.getRecipeCode());

        return recipeOrderService.validateCabinetRecipeStatus(cabinetVO);
    }

    @Override
    public void putInCabinetNotice(CabinetVO cabinetVO) {
        validateAtop(cabinetVO);
        recipeOrderService.putInCabinetNotice(cabinetVO);
    }

    @Override
    public Integer getImperfectFlag(com.ngari.recipe.recipe.model.RecipeBean recipeBean) {
        return recipeOrderService.getImperfectFlag(recipeBean);
    }

    @Override
    public List<ImperfectInfoVO> batchGetImperfectFlag(List<com.ngari.recipe.recipe.model.RecipeBean> recipeBeans) {
        return recipeOrderService.batchGetImperfectFlag(recipeBeans);
    }

    @Override
    public ImperfectInfoVO getImperfectInfo(com.ngari.recipe.recipe.model.RecipeBean recipeBean) {
        validateAtop(recipeBean.getRecipeCode(), recipeBean.getClinicOrgan(), recipeBean.getMpiid());
        return recipeOrderService.getImperfectInfo(recipeBean);
    }

    @Override
    public Integer getRecipeRefundCount(RecipeRefundInfoReqVO recipeRefundCountVO) {
        validateAtop(recipeRefundCountVO, recipeRefundCountVO.getDoctorId(), recipeRefundCountVO.getStartTime(), recipeRefundCountVO.getEndTime());
        return recipeOrderService.getRecipeRefundCount(recipeRefundCountVO);
    }

    @Override
    public List<RecipeOrderVO> orderListByClinicId(Integer clinicId, Integer bussSource) {
        List<RecipeOrder> list = recipeOrderService.orderListByClinicId(clinicId, bussSource);
        return ObjectCopyUtils.convert(list, RecipeOrderVO.class);
    }

    @Override
    public List<OrderPharmacyVO> getPharmacyByOrderCode(String orderCode) {
        validateAtop(orderCode);
        return recipeOrderService.getPharmacyByOrderCode(orderCode);
    }

}
