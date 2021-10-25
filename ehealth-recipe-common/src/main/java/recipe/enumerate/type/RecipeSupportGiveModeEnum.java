package recipe.enumerate.type;

import com.ngari.recipe.dto.GiveModeButtonDTO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description： 处方支持的购药方式
 * @author： whf
 * @date： 2021-04-26 13:51
 */
public enum RecipeSupportGiveModeEnum {
    /**
     * 到店取药
     */
    SUPPORT_TFDS("supportTFDS", 1, "到店取药"),

    /**
     * showSendToHos 医院配送
     */
    SHOW_SEND_TO_HOS("showSendToHos", 2, "医院配送"),

    /**
     * showSendToEnterprises 药企配送
     */
    SHOW_SEND_TO_ENTERPRISES("showSendToEnterprises", 3, "药企配送"),

    /**
     * supportToHos 到院取药
     */
    SUPPORT_TO_HOS("supportToHos", 4, "到院取药"),

    /**
     * downloadRecipe 下载处方笺
     */
    DOWNLOAD_RECIPE("supportDownload", 5, "下载处方笺"),


    /**
     * supportMedicalPayment 例外支付
     */
    SUPPORT_MEDICAL_PAYMENT("supportMedicalPayment", 6, "例外支付");


    /**
     * 配送文案
     */
    private String text;
    /**
     * 配送状态
     */
    private Integer type;

    private String name;


    RecipeSupportGiveModeEnum(String text, Integer type, String name) {
        this.text = text;
        this.type = type;
        this.name = name;
    }


    public String getText() {
        return text;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static List<String> enterpriseList = Arrays.asList(SHOW_SEND_TO_HOS.text, SHOW_SEND_TO_ENTERPRISES.text, SUPPORT_TFDS.text);

    /**
     * 校验何种类型库存
     *
     * @param configurations 按钮
     * @return
     */
    public static Integer checkFlag(List<String> configurations) {
        int hospital = DrugStockCheckEnum.NO_CHECK_STOCK.getType();
        int enterprise = DrugStockCheckEnum.NO_CHECK_STOCK.getType();
        for (String a : configurations) {
            if (SUPPORT_TO_HOS.getText().equals(a)) {
                hospital = DrugStockCheckEnum.HOS_CHECK_STOCK.getType();
            }
            if (enterpriseList.contains(a)) {
                enterprise = DrugStockCheckEnum.ENT_CHECK_STOCK.getType();
            }
        }
        return hospital + enterprise;
    }

    /**
     * 判断是否校验药企库存
     *
     * @param giveModeButtonBeans
     * @return false 无配置 不校验
     */
    public static boolean checkEnterprise(List<GiveModeButtonDTO> giveModeButtonBeans) {
        if (CollectionUtils.isEmpty(giveModeButtonBeans)) {
            return false;
        }
        List<String> configurations = giveModeButtonBeans.stream().map(GiveModeButtonDTO::getShowButtonKey).collect(Collectors.toList());
        boolean enterprise = configurations.stream().anyMatch(a -> RecipeSupportGiveModeEnum.enterpriseList.contains(a));
        if (!enterprise) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否校验机构库存
     *
     * @param giveModeButtonBeans
     * @return 到院取药按钮名称
     */
    public static String checkOrgan(List<GiveModeButtonDTO> giveModeButtonBeans) {
        if (CollectionUtils.isEmpty(giveModeButtonBeans)) {
            return null;
        }
        Map<String, String> configurations = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
        String showButtonName = configurations.get(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText());
        //无到院取药
        if (StringUtils.isEmpty(showButtonName)) {
            return null;
        }
        return showButtonName;
    }

}