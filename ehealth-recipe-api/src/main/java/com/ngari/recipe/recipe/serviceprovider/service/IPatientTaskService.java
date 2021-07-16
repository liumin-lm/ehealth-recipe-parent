package com.ngari.recipe.recipe.serviceprovider.service;

import com.ngari.bus.task.module.PatientTask;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * @author xlx
 * @Date: 2020/12/14
 * @Description:com.ngari.recipe.recipe.serviceprovider.service
 * @version:1.0
 */
public interface IPatientTaskService {

    /**
     * 首页获取任务栏，初诊，复诊，处方，检查等
     *
     * @param mpiId   患者id
     * @param organId 机构id
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    List<PatientTask> findPatientTask(String mpiId, Integer organId, Integer start, Integer limit);

    @RpcService
    List<PatientTask> findPatientTask(String mpiId, List<Integer> organIds, Integer start, Integer limit);
}
