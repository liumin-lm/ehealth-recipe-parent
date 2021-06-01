package recipe.bussutil.drugdisplay;

import ctd.persistence.exception.DAOException;

/**
 * created by shiyuping on 2021/3/11
 */
public enum DisplayNameEnum {

    WM_SALENAME("drugDisplaySplicedSaleNameByWm", new WmSaleNameDisplay()), WM_DRUGNAME("drugDisplaySplicedNameByWm", new CommonDrugNameDisplay()), TCM_DRUGNAME("drugDisplaySplicedNameByTcm", new CommonDrugNameDisplay());


    private String configKey;
    private IDrugNameDisplay drugNameDisplay;

    DisplayNameEnum(String configKey, IDrugNameDisplay drugNameDisplay) {
        this.configKey = configKey;
        this.drugNameDisplay = drugNameDisplay;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public IDrugNameDisplay getDrugNameDisplay() {
        return drugNameDisplay;
    }

    public void setDrugNameDisplay(IDrugNameDisplay drugNameDisplay) {
        this.drugNameDisplay = drugNameDisplay;
    }

    public static IDrugNameDisplay getDisplayObject(String configKey) {
        for (DisplayNameEnum value : DisplayNameEnum.values()) {
            if (value.getConfigKey().equals(configKey)) {
                return value.getDrugNameDisplay();
            }
        }
        throw new DAOException(609, "机构配置key" + configKey + "在枚举配置中不存在");
    }
}
