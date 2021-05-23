package recipe.bean.cqJgptBussData;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

import java.util.List;

/**
 * @program: regulation-front-cqs
 * @description: 开方医生数字签名对象数据结构XML
 * @author: liumin
 * @create: 2020-05-20 09:24
 **/
@Data
@XStreamAlias("CA")
public class RecipeDocSignatureXML {
    private String MedicalRecordNo;
    private String DoctorCode;
    private String DoctorName;
    private String TrialPharmCode;
    private String TrialPharmName;
    private String PatientName;
    private String PatientSFZH;
    private String RecipeDate;
    private List<AdditionalDiagnosis> DiagnosisList;
    private List<Drug> DrugList;
}
