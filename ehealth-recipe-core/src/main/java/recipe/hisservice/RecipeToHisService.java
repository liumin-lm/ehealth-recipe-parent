package recipe.hisservice;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.hisservice.model.HisResTO;
import com.ngari.base.hisservice.service.IRecipeHisService;
import com.ngari.his.recipe.mode.*;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.service.HisCallBackService;
import recipe.service.RecipeLogService;
import recipe.util.ApplicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/12.
 */
public class RecipeToHisService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeToHisService.class);

    public void recipeSend(RecipeSendRequestTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Integer recipeId = Integer.valueOf(request.getRecipeID());
        LOGGER.info("recipeSend recipeId={}, request={}", recipeId, JSONUtils.toString(request));
        try {

            recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECKING_HOS, null);
            HisResTO resTO = hisService.recipeSend(request);
            LOGGER.info("recipeSend recipeId={}, response={}", recipeId, JSONUtils.toString(resTO));
            if (resTO.isSuccess()) {
                LOGGER.info("recipeSend recipeId={}, 调用BASE 处方写入服务成功!", recipeId);
            } else {
                //失败发送系统消息
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.HIS_FAIL, null);
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECKING_HOS,
                        RecipeStatusConstant.HIS_FAIL, "his写入失败，调用前置机处方写入服务失败");
                LOGGER.error("recipeSend recipeId={}, 调用BASE 处方写入服务错误!", recipeId);
            }
        } catch (Exception e) {
            LOGGER.error("recipeSend recipeId={}, error ", request.getRecipeID(), e);
        }
    }

    public Integer listSingleQuery(RecipeListQueryReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("listSingleQuery request={}", JSONUtils.toString(request));

        try {
            HisResTO resTO = hisService.listQuery(request);
            LOGGER.info("listSingleQuery response={}", JSONUtils.toString(resTO));
            RecipeListQueryResTO response = getResponseObj(resTO.getResponse(), RecipeListQueryResTO.class);
            Integer busStatus = null;
            if (null == response || null == response.getMsgCode()) {
                return busStatus;
            }

            List<QueryRepTO> list = response.getData();
            if (StringUtils.isNotEmpty(response.getOrganID()) && CollectionUtils.isNotEmpty(list)) {
                List<String> payList = new ArrayList<>();
                List<String> finishList = new ArrayList<>();

                QueryRepTO rep = list.get(0);
                if (null != rep) {
                    Integer organId = Integer.valueOf(response.getOrganID());
                    Integer isPay = StringUtils.isEmpty(rep.getIsPay()) ? Integer.valueOf(0) : Integer.valueOf(rep.getIsPay());
                    Integer recipeStatus = StringUtils.isEmpty(rep.getRecipeStatus())  ? Integer.valueOf(0) : Integer.valueOf(rep.getRecipeStatus());
                    Integer phStatus = StringUtils.isEmpty(rep.getPhStatus()) ? Integer.valueOf(0) : Integer.valueOf(rep.getPhStatus());
                    if (recipeStatus == 1) {
                        busStatus = RecipeStatusConstant.CHECK_PASS;
                        //有效的处方单已支付 未发药 为已支付状态
                        if (isPay == 1 && phStatus == 0) {
                            busStatus = RecipeStatusConstant.HAVE_PAY;
                            payList.add(rep.getRecipeNo());
                            HisCallBackService.havePayRecipesFromHis(payList, organId);
                        }
                        //有效的处方单已支付 已发药 为已完成状态
                        if (isPay == 1 && phStatus == 1) {
                            busStatus = RecipeStatusConstant.FINISH;
                            finishList.add(rep.getRecipeNo());
                            HisCallBackService.finishRecipesFromHis(finishList, organId);
                        }
                    }
                }
            }
            return busStatus;
        } catch (Exception e) {
            LOGGER.error("listSingleQuery error ", e);
        }
        return null;
    }


    public void listQuery(RecipeListQueryReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("listQuery request={}", JSONUtils.toString(request));

        try {
            HisResTO resTO = hisService.listQuery(request);
            LOGGER.info("listQuery response={}", JSONUtils.toString(resTO));
            RecipeListQueryResTO response = getResponseObj(resTO.getResponse(), RecipeListQueryResTO.class);
            if (null == response || null == response.getMsgCode()) {
                return;
            }
            List<QueryRepTO> list = response.getData();
            List<String> payList = new ArrayList<>();
            List<String> finishList = new ArrayList<>();
            Integer organId = Integer.valueOf(response.getOrganID());

            for (QueryRepTO rep : list) {
                Integer isPay = Integer.valueOf(rep.getIsPay());
                Integer recipeStatus = Integer.valueOf(rep.getRecipeStatus());
                Integer phStatus = Integer.valueOf(rep.getPhStatus());
                if (recipeStatus == 1) {
                    //有效的处方单已支付 未发药 为已支付状态
                    if (isPay == 1 && phStatus == 0) {
                        payList.add(rep.getRecipeNo());
                    }
                    //有效的处方单已支付 已发药 为已完成状态
                    if (isPay == 1 && phStatus == 1) {
                        finishList.add(rep.getRecipeNo());
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(payList)) {
                HisCallBackService.havePayRecipesFromHis(payList, organId);
            }

            if (CollectionUtils.isNotEmpty(finishList)) {
                HisCallBackService.finishRecipesFromHis(finishList, organId);
            }

        } catch (Exception e) {
            LOGGER.error("listQuery error ", e);
        }
    }


    public RecipeRefundResTO recipeRefund(RecipeRefundReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("recipeRefund request={}", JSONUtils.toString(request));
        RecipeRefundResTO response = null;
        try {
            HisResTO resTO = hisService.recipeRefund(request);
            LOGGER.info("recipeRefund response={}", JSONUtils.toString(resTO));
            response = getResponseObj(resTO.getResponse(), RecipeRefundResTO.class);
        } catch (Exception e) {
            LOGGER.error("recipeRefund error ", e);
        }
        return response;
    }

    public Recipedetail payNotify(PayNotifyReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("payNotify request={}", JSONUtils.toString(request));
        try {
            HisResTO resTO = hisService.payNotify(request);
            LOGGER.info("payNotify response={}", JSONUtils.toString(resTO));
            PayNotifyResTO response = getResponseObj(resTO.getResponse(), PayNotifyResTO.class);
            if (null == response || null == response.getMsgCode()) {
                return null;
            }
            Recipedetail detail = new Recipedetail();
            detail.setPatientInvoiceNo(response.getData().getInvoiceNo());
            detail.setPharmNo(response.getData().getWindows());
            return detail;
        } catch (Exception e) {
            LOGGER.error("payNotify error ", e);
        }
        return null;
    }

    /**
     * 查询药品在医院里的信息
     *
     * @param
     * @return
     */
    public List<DrugInfoTO> queryDrugInfo(List<DrugInfoTO> drugInfoList, int organId) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);

        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        if (CollectionUtils.isEmpty(drugInfoList)) {
            //查询全部药品信息，返回的是医院所有有效的药品信息
            request.setData(Lists.<DrugInfoTO>newArrayList());
        } else {
            //查询限定范围内容的药品数据，返回的是该医院 无效的药品信息
            request.setData(drugInfoList);
        }
        LOGGER.info("queryDrugInfo request={}", JSONUtils.toString(request));

        try {
            HisResTO resTO = hisService.queryDrugInfo(request);
            LOGGER.info("queryDrugInfo response={}", JSONUtils.toString(resTO));
            DrugInfoResponseTO response = getResponseObj(resTO.getResponse(), DrugInfoResponseTO.class);
            if (null != response && Integer.valueOf(0).equals(response.getMsgCode())) {
                return (null != response.getData()) ? response.getData() : new ArrayList<DrugInfoTO>();
            }
        } catch (Exception e) {
            LOGGER.error("queryDrugInfo error ", e);
        }
        return null;
    }


    public Boolean drugTakeChange(DrugTakeChangeReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("drugTakeChange request={}", JSONUtils.toString(request));
        Boolean response = false;
        try {
            HisResTO resTO = hisService.drugTakeChange(request);
            LOGGER.info("drugTakeChange response={}", JSONUtils.toString(resTO));
            response = resTO.isSuccess();
        } catch (Exception e) {
            LOGGER.error("drugTakeChange error ", e);
        }
        return response;
    }

    public Boolean recipeUpdate(RecipeStatusUpdateReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("recipeUpdate request={}", JSONUtils.toString(request));
        Boolean response = false;
        try {
            HisResTO resTO = hisService.recipeUpdate(request);
            LOGGER.info("recipeUpdate response={}", JSONUtils.toString(resTO));
            response = resTO.isSuccess();
        } catch (Exception e) {
            LOGGER.error("recipeUpdate error ", e);
        }
        return response;
    }


    public DrugInfoResponseTO scanDrugStock(List<Recipedetail> detailList, int organId) {
        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        OrganDrugListDAO drugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);

        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        List<Integer> drugIdList = FluentIterable.from(detailList).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();

        List<OrganDrugList> organDrugList = drugDao.findByOrganIdAndDrugIds(organId, drugIdList);
        Map<Integer, OrganDrugList> drugIdAndProduce = Maps.uniqueIndex(organDrugList, new Function<OrganDrugList, Integer>() {
            @Override
            public Integer apply(OrganDrugList input) {
                return input.getDrugId();
            }
        });

        List<DrugInfoTO> data = new ArrayList<>(detailList.size());
        DrugInfoTO drugInfo;
        OrganDrugList organDrug;
        for (Recipedetail detail : detailList) {
            drugInfo = new DrugInfoTO(detail.getOrganDrugCode());
            drugInfo.setPack(detail.getPack().toString());
            drugInfo.setPackUnit(detail.getDrugUnit());
            organDrug = drugIdAndProduce.get(detail.getDrugId());
            if (null != organDrug) {
                drugInfo.setManfcode(organDrug.getProducerCode());
            }
            data.add(drugInfo);
        }
        request.setData(data);

        DrugInfoResponseTO response = null;
        LOGGER.info("scanDrugStock request={}", JSONUtils.toString(request));
        try {
            HisResTO resTO = hisService.scanDrugStock(request);
            LOGGER.info("scanDrugStock response={}", JSONUtils.toString(resTO));
            response = getResponseObj(resTO.getResponse(), DrugInfoResponseTO.class);
        } catch (Exception e) {
            LOGGER.error("scanDrugStock error ", e);
        }
        return response;
    }

    public RecipeQueryResTO recipeQuery(RecipeQueryReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("recipeQuery request={}", JSONUtils.toString(request));
        RecipeQueryResTO response = null;
        try {
            HisResTO resTO = hisService.recipeQuery(request);
            LOGGER.info("recipeQuery response={}", JSONUtils.toString(resTO));
            response = getResponseObj(resTO.getResponse(), RecipeQueryResTO.class);
        } catch (Exception e) {
            LOGGER.error("recipeQuery error ", e);
        }
        return response;
    }

    public DetailQueryResTO detailQuery(DetailQueryReqTO request) {
        IRecipeHisService hisService = ApplicationUtils.getBaseService(IRecipeHisService.class);
        LOGGER.info("detailQuery request={}", JSONUtils.toString(request));
        DetailQueryResTO response = null;
        try {
            HisResTO resTO = hisService.detailQuery(request);
            LOGGER.info("detailQuery response={}", JSONUtils.toString(resTO));
            response = getResponseObj(resTO.getResponse(), DetailQueryResTO.class);
        } catch (Exception e) {
            LOGGER.error("detailQuery error ", e);
        }
        return response;
    }


    private <T> T getResponseObj(Object obj, Class<T> clazz) {
        if (null == obj) {
            return null;
        }

        return (T) obj;
    }

}
