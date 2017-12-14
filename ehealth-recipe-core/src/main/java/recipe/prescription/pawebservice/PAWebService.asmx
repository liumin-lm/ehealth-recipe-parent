<?xml version="1.0" encoding="utf-8"?>
<wsdl:definitions xmlns:tm="http://microsoft.com/wsdl/mime/textMatching/" xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/" xmlns:mime="http://schemas.xmlsoap.org/wsdl/mime/" xmlns:tns="WinningPAWebservice" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:s="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/" xmlns:http="http://schemas.xmlsoap.org/wsdl/http/" targetNamespace="WinningPAWebservice" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">
  <wsdl:types>
    <s:schema elementFormDefault="qualified" targetNamespace="WinningPAWebservice">
      <s:element name="CRMS_WEBSRV">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="funId" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="baseData" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="detailsData" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="CRMS_WEBSRVResponse">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="CRMS_WEBSRVResult" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="uiResults" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="hisResults" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="IMDS_WEBSRV">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="funId" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="baseData" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="detailsData" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="IMDS_WEBSRVResponse">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="IMDS_WEBSRVResult" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="uiResults" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="hisResults" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="GetPAResults">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="funId" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="baseData" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="detailsData" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="GetPAResultsResponse">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="GetPAResultsResult" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="uiResults" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="hisResults" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="GetPAResultsEx">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="funId" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="baseData" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="detailsData" type="s:string" />
            <s:element minOccurs="1" maxOccurs="1" name="timeOut" type="s:unsignedInt" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="GetPAResultsExResponse">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="1" maxOccurs="1" name="GetPAResultsExResult" type="s:int" />
            <s:element minOccurs="0" maxOccurs="1" name="uiResults" type="s:string" />
            <s:element minOccurs="0" maxOccurs="1" name="hisResults" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="GetAdminAccounts">
        <s:complexType />
      </s:element>
      <s:element name="GetAdminAccountsResponse">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="0" maxOccurs="1" name="GetAdminAccountsResult" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
      <s:element name="GetClientVersion">
        <s:complexType />
      </s:element>
      <s:element name="GetClientVersionResponse">
        <s:complexType>
          <s:sequence>
            <s:element minOccurs="0" maxOccurs="1" name="GetClientVersionResult" type="s:string" />
          </s:sequence>
        </s:complexType>
      </s:element>
    </s:schema>
  </wsdl:types>
  <wsdl:message name="CRMS_WEBSRVSoapIn">
    <wsdl:part name="parameters" element="tns:CRMS_WEBSRV" />
  </wsdl:message>
  <wsdl:message name="CRMS_WEBSRVSoapOut">
    <wsdl:part name="parameters" element="tns:CRMS_WEBSRVResponse" />
  </wsdl:message>
  <wsdl:message name="IMDS_WEBSRVSoapIn">
    <wsdl:part name="parameters" element="tns:IMDS_WEBSRV" />
  </wsdl:message>
  <wsdl:message name="IMDS_WEBSRVSoapOut">
    <wsdl:part name="parameters" element="tns:IMDS_WEBSRVResponse" />
  </wsdl:message>
  <wsdl:message name="GetPAResultsSoapIn">
    <wsdl:part name="parameters" element="tns:GetPAResults" />
  </wsdl:message>
  <wsdl:message name="GetPAResultsSoapOut">
    <wsdl:part name="parameters" element="tns:GetPAResultsResponse" />
  </wsdl:message>
  <wsdl:message name="GetPAResultsExSoapIn">
    <wsdl:part name="parameters" element="tns:GetPAResultsEx" />
  </wsdl:message>
  <wsdl:message name="GetPAResultsExSoapOut">
    <wsdl:part name="parameters" element="tns:GetPAResultsExResponse" />
  </wsdl:message>
  <wsdl:message name="GetAdminAccountsSoapIn">
    <wsdl:part name="parameters" element="tns:GetAdminAccounts" />
  </wsdl:message>
  <wsdl:message name="GetAdminAccountsSoapOut">
    <wsdl:part name="parameters" element="tns:GetAdminAccountsResponse" />
  </wsdl:message>
  <wsdl:message name="GetClientVersionSoapIn">
    <wsdl:part name="parameters" element="tns:GetClientVersion" />
  </wsdl:message>
  <wsdl:message name="GetClientVersionSoapOut">
    <wsdl:part name="parameters" element="tns:GetClientVersionResponse" />
  </wsdl:message>
  <wsdl:portType name="PAWebServiceSoap">
    <wsdl:operation name="CRMS_WEBSRV">
      <wsdl:documentation xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">兼容接口函数</wsdl:documentation>
      <wsdl:input message="tns:CRMS_WEBSRVSoapIn" />
      <wsdl:output message="tns:CRMS_WEBSRVSoapOut" />
    </wsdl:operation>
    <wsdl:operation name="IMDS_WEBSRV">
      <wsdl:documentation xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">兼容接口函数</wsdl:documentation>
      <wsdl:input message="tns:IMDS_WEBSRVSoapIn" />
      <wsdl:output message="tns:IMDS_WEBSRVSoapOut" />
    </wsdl:operation>
    <wsdl:operation name="GetPAResults">
      <wsdl:documentation xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">接口函数</wsdl:documentation>
      <wsdl:input message="tns:GetPAResultsSoapIn" />
      <wsdl:output message="tns:GetPAResultsSoapOut" />
    </wsdl:operation>
    <wsdl:operation name="GetPAResultsEx">
      <wsdl:documentation xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">接口函数(扩展) - 有读超时限制</wsdl:documentation>
      <wsdl:input message="tns:GetPAResultsExSoapIn" />
      <wsdl:output message="tns:GetPAResultsExSoapOut" />
    </wsdl:operation>
    <wsdl:operation name="GetAdminAccounts">
      <wsdl:documentation xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">客户端内部调用函数</wsdl:documentation>
      <wsdl:input message="tns:GetAdminAccountsSoapIn" />
      <wsdl:output message="tns:GetAdminAccountsSoapOut" />
    </wsdl:operation>
    <wsdl:operation name="GetClientVersion">
      <wsdl:documentation xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">客户端内部调用函数</wsdl:documentation>
      <wsdl:input message="tns:GetClientVersionSoapIn" />
      <wsdl:output message="tns:GetClientVersionSoapOut" />
    </wsdl:operation>
  </wsdl:portType>
  <wsdl:binding name="PAWebServiceSoap" type="tns:PAWebServiceSoap">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http" />
    <wsdl:operation name="CRMS_WEBSRV">
      <soap:operation soapAction="WinningPAWebservice/CRMS_WEBSRV" style="document" />
      <wsdl:input>
        <soap:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="IMDS_WEBSRV">
      <soap:operation soapAction="WinningPAWebservice/IMDS_WEBSRV" style="document" />
      <wsdl:input>
        <soap:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetPAResults">
      <soap:operation soapAction="WinningPAWebservice/GetPAResults" style="document" />
      <wsdl:input>
        <soap:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetPAResultsEx">
      <soap:operation soapAction="WinningPAWebservice/GetPAResultsEx" style="document" />
      <wsdl:input>
        <soap:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetAdminAccounts">
      <soap:operation soapAction="WinningPAWebservice/GetAdminAccounts" style="document" />
      <wsdl:input>
        <soap:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetClientVersion">
      <soap:operation soapAction="WinningPAWebservice/GetClientVersion" style="document" />
      <wsdl:input>
        <soap:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
  </wsdl:binding>
  <wsdl:binding name="PAWebServiceSoap12" type="tns:PAWebServiceSoap">
    <soap12:binding transport="http://schemas.xmlsoap.org/soap/http" />
    <wsdl:operation name="CRMS_WEBSRV">
      <soap12:operation soapAction="WinningPAWebservice/CRMS_WEBSRV" style="document" />
      <wsdl:input>
        <soap12:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap12:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="IMDS_WEBSRV">
      <soap12:operation soapAction="WinningPAWebservice/IMDS_WEBSRV" style="document" />
      <wsdl:input>
        <soap12:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap12:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetPAResults">
      <soap12:operation soapAction="WinningPAWebservice/GetPAResults" style="document" />
      <wsdl:input>
        <soap12:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap12:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetPAResultsEx">
      <soap12:operation soapAction="WinningPAWebservice/GetPAResultsEx" style="document" />
      <wsdl:input>
        <soap12:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap12:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetAdminAccounts">
      <soap12:operation soapAction="WinningPAWebservice/GetAdminAccounts" style="document" />
      <wsdl:input>
        <soap12:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap12:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetClientVersion">
      <soap12:operation soapAction="WinningPAWebservice/GetClientVersion" style="document" />
      <wsdl:input>
        <soap12:body use="literal" />
      </wsdl:input>
      <wsdl:output>
        <soap12:body use="literal" />
      </wsdl:output>
    </wsdl:operation>
  </wsdl:binding>
  <wsdl:service name="PAWebService">
    <wsdl:port name="PAWebServiceSoap" binding="tns:PAWebServiceSoap">
      <soap:address location="http://121.43.189.212:820/PAWebService.asmx" />
    </wsdl:port>
    <wsdl:port name="PAWebServiceSoap12" binding="tns:PAWebServiceSoap12">
      <soap12:address location="http://121.43.189.212:820/PAWebService.asmx" />
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>