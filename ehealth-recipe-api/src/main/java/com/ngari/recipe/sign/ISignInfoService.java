package com.ngari.recipe.sign;

import ctd.util.annotation.RpcService;

import java.util.Date;

public interface ISignInfoService {
    @RpcService
    void setSerCodeAndEndDateByDoctorId(Integer doctorId, String type, String serCode, Date caEndTime);
}
