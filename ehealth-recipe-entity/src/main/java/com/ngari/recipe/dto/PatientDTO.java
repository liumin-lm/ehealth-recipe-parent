package com.ngari.recipe.dto;

import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.schema.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 医生端脱敏对象
 *
 * @author fuzi
 * @create 2020-07-06 15:00
 * @return
 */
@Setter
@Getter
public class PatientDTO implements Serializable {
    private static final long serialVersionUID = -6772977197600216684L;

    @ItemProperty(alias = "登陆id")
    private String loginId;

    @ItemProperty(alias = "主索引")
    private String mpiId;

    @ItemProperty(alias = "病人姓名")
    private String patientName;

    @ItemProperty(alias = "病人性别")
    @Dictionary(id = "eh.base.dictionary.Gender")
    private String patientSex;

    @ItemProperty(alias = "出生日期")
    @Temporal(TemporalType.DATE)
    private Date birthday;

    @ItemProperty(alias = "病人类型")
    @Dictionary(id = "eh.mpi.dictionary.PatientType")
    private String patientType;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "18位身份证号-用于业务处理")
    private String idcard;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "18位身份证号，备选")
    private String idcard2;

    @Desensitizations(type = DesensitizationsType.MOBILE)
    @ItemProperty(alias = "手机号")
    private String mobile;

    @Desensitizations(type = DesensitizationsType.ADDRESS)
    @ItemProperty(alias = "家庭地址")
    private String address;

    @ItemProperty(alias = "家庭地址区域")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String homeArea;

    @ItemProperty(alias = "个人照片")
    @FileToken(expires = 3600)
    private String photo;

    @ItemProperty(alias = "建档时间")
    private Date createDate;

    @ItemProperty(alias = "最后更新时间")
    private Date lastModify;

    @ItemProperty(alias = "状态")
    @Dictionary(id = "eh.mpi.dictionary.Status")
    private Integer status;

    @ItemProperty(alias = "监护人姓名")
    private String guardianName;

    @ItemProperty(alias = "监护人标记")
    private Boolean guardianFlag;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "证件号")
    private String certificate;

    @ItemProperty(alias = "证件类型")
    @Dictionary(id = "eh.mpi.dictionary.CertificateType")
    private Integer certificateType;

    @ItemProperty(alias = "证件类型枚举值")
    private String certificateTypeText;

    @ItemProperty(alias = "地址(省市区)")
    private String fullHomeArea;

    @ItemProperty(alias = "就诊人类型 0:成人 1:有身份证儿童 2:无身份证儿童")
    @Dictionary(id = "eh.mpi.dictionary.PatientUserType")
    private Integer patientUserType;

    @ItemProperty(alias = "认证状态")
    @Dictionary(id = "eh.mpi.dictionary.AuthStatus")
    private Integer authStatus;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "陪诊人（监护人）证件号")
    private String guardianCertificate;

    @ItemProperty(alias = "是否用户自己")
    private Boolean isOwn;

    @ItemProperty(alias = "是否默认就诊人")
    private Boolean isDefaultPatient;

    @Dictionary(id = "eh.mpi.dictionary.Educations")
    private String education;

    @Dictionary(id = "eh.mpi.dictionary.Country")
    private String country;

    @Dictionary(id = "eh.base.dictionary.Marry")
    private String marry;

    @Dictionary(id = "eh.mpi.dictionary.Resident")
    private String resident;

    @Dictionary(id = "eh.mpi.dictionary.Household")
    private String houseHold;

    @Dictionary(id = "eh.mpi.dictionary.Job")
    private String job;

    @Dictionary(id = "eh.mpi.dictionary.PayType", multiple = true)
    private String docPayType;

    @Dictionary(id = "eh.mpi.dictionary.Nation")
    private String nation;

    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String state;

    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String birthPlace;

    private String authMsg;

    private Boolean haveUnfinishedFollow;

    private Boolean ashFlag;

    /**
     * 年龄
     */
    private Integer age;
    @ItemProperty(alias = "新版年龄（带单位）")
    private String ageString;
    private Boolean signFlag;

    /**
     * 医生是否关注病人标记
     */
    private Boolean relationFlag;

    /**
     * 标签列表
     */
    private List<String> labelNames;

    /**
     * 账号
     */
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String loginName;

    /**
     * 用户姓名
     */
    private String userName;
    private Integer urt;

    private Integer relationPatientId;

    private String cardId;

    private String userIcon;

    @Dictionary(id = "eh.base.dictionary.ExpectClinicPeriodType")
    private Integer expectClinicPeriodType;

    @ItemProperty(alias = "是否为无身份证患者标识(0:否 1:是)")
    private Integer notIdCardFlag;

    @Desensitizations(type = DesensitizationsType.NAME)
    private String realName;

    private String vaccineCardId;

    private String weight;

    public String getCertificateTypeText() {
        if (this.certificateType != null) {
            try {
                return DictionaryController.instance().get("eh.mpi.dictionary.CertificateType").getText(this.certificateType);
            } catch (ControllerException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
