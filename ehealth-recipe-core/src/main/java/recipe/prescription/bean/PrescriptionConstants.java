package recipe.prescription.bean;

public class PrescriptionConstants {

    public static String CRMSWEBSRV = "CRMS_WEBSRV";
    public static String GetAdminAccounts = "GetAdminAccounts";
    public static String GetClientVersion = "GetClientVersion";
    public static String GetPAResults  = "GetPAResults";
    public static String GetPAResultsEx  = "GetPAResultsEx";
    public static String IMDSWEBSRV = "IMDS_WEBSRV";

    public static String TargetNamespace = "http://121.43.189.212:820/PAWebService.asmx";
    public static String NAMESPACE = "WinningPAWebservice";


    public String getTargetNamespace(){ return TargetNamespace; }
    public void setTargetNamespace(String targetNamespace){ PrescriptionConstants.TargetNamespace = targetNamespace;}

    public String getNAMESPACE(){ return NAMESPACE; }
    public void setNAMESPACE(String NAMESPACE){ PrescriptionConstants.NAMESPACE = NAMESPACE;}

}