package com.ngari.recipe.recipe.service;

import com.ngari.recipe.recipe.model.TcmTreatmentDTO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public interface ITcmTreatmentService  {

    @RpcService
    QueryResult<TcmTreatmentDTO> querTcmTreatmentByOrganIdAndName(Integer organId, String input, Boolean isRegulationSymptom, final int start, final int limit);

    @RpcService
    List<TcmTreatmentDTO> querTcmTreatmentByOrganId(Integer organId);

    @RpcService(mvcDisabled = false)
    Map<String, Object> readTcmTreatmentExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId, String manageUnit);

}
