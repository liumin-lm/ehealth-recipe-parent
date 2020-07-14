package com.ngari.recipe.hisprescription.model;


/**
 * 诊断信息
 */
public class DiseaseInfo implements java.io.Serializable {

	private static final long serialVersionUID = 7755871065899035804L;
	//诊断编号
	private String diseaseId;
	//诊断代码 必填
	private String diseaseCode;
	//诊断名称 必填
	private String diseaseName;
	//诊断说明
	private String diseaseMemo;

	public String getDiseaseMemo() {
		return diseaseMemo;
	}

	public void setDiseaseMemo(String diseaseMemo) {
		this.diseaseMemo = diseaseMemo;
	}

	public String getDiseaseCode() {
		return diseaseCode;
	}

	public void setDiseaseCode(String diseaseCode) {
		this.diseaseCode = diseaseCode;
	}

	public String getDiseaseName() {
		return diseaseName;
	}

	public void setDiseaseName(String diseaseName) {
		this.diseaseName = diseaseName;
	}

	public String getDiseaseId() {
		return diseaseId;
	}

	public void setDiseaseId(String diseaseId) {
		this.diseaseId = diseaseId;
	}
}
