package recipe.bean.cqjgptbussdata;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.Data;

/**
 * @program: regulation-front-cqs
 * @description: 诊断列表
 * @author: liumin
 * @create: 2020-05-20 09:24
 **/
@Data
@XStreamAlias("additionaldiagnosis")
public class AdditionalDiagnosis {
    //各个接口使用的诊断字段不同，所有字段均在该实体类中
    private String diagnosisCode;//	VARCHAR2(10)	诊断编码
    private String diagnosisName;//	VARCHAR2(20)	诊断名称
    @XStreamOmitField
    private String clinicalDiagnosis;//	VARCHAR2(200)	临床诊断
    @XStreamOmitField
    private String diagnosisType;//	VARCHAR2(2)	诊断类型
    @XStreamOmitField
    private String diagSort;//	VARCHAR2(2)	诊断排序
    // --------------- Q310使用 -------------//
    @XStreamOmitField
    private String symptomCode;//	症候编码
    @XStreamOmitField
    private String symptomCodeName;//	症候名称

    private String DiagnosisClassify;//	诊断分类  1中医诊断，2 西医诊断

}
