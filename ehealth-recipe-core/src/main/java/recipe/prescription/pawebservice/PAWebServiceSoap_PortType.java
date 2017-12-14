/**
 * PAWebServiceSoap_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package recipe.prescription.pawebservice;

public interface PAWebServiceSoap_PortType extends java.rmi.Remote {

    /**
     * 兼容接口函数
     */
    public void CRMS_WEBSRV(int funId, String baseData, String detailsData,
        javax.xml.rpc.holders.IntHolder CRMS_WEBSRVResult,
        javax.xml.rpc.holders.StringHolder uiResults,
        javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException;

    /**
     * 兼容接口函数
     */
    public void IMDS_WEBSRV(int funId, String baseData, String detailsData,
        javax.xml.rpc.holders.IntHolder IMDS_WEBSRVResult,
        javax.xml.rpc.holders.StringHolder uiResults,
        javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException;

    /**
     * 接口函数
     */
    public void getPAResults(int funId, String baseData, String detailsData,
        javax.xml.rpc.holders.IntHolder getPAResultsResult,
        javax.xml.rpc.holders.StringHolder uiResults,
        javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException;

    /**
     * 接口函数(扩展) - 有读超时限制
     */
    public void getPAResultsEx(int funId, String baseData, String detailsData,
        org.apache.axis.types.UnsignedInt timeOut,
        javax.xml.rpc.holders.IntHolder getPAResultsExResult,
        javax.xml.rpc.holders.StringHolder uiResults,
        javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException;

    /**
     * 客户端内部调用函数
     */
    public String getAdminAccounts() throws java.rmi.RemoteException;

    /**
     * 客户端内部调用函数
     */
    public String getClientVersion() throws java.rmi.RemoteException;
}
