package recipe.enumerate.type;

/**
 * 购药方式
 * @author yinsheng
 * @date 2021\6\20 0020 18:13
 */
public enum GiveModeTextEnum {

    SENDTOENTER("showSendToEnterprises", 1, "药企配送"),
    SENDTOHOS("showSendToHos", 1, "医院配送"),
    SUPPORTTOHIS("supportToHos", 2, "到院取药"),
    SUPPORTTFDS("supportTFDS", 3, "药店取药"),
    SUPPORTTHORDORDER("supportThirdOrder", 5, "订单跳转");


    /**
     * 购药方式文本
     */
    private String giveModeText;
    /**
     * 购药方式
     */
    private Integer giveMode;
    /**
     * 描述
     */
    private String desc;

    GiveModeTextEnum(String giveModeText,Integer giveMode, String desc) {
        this.giveModeText = giveModeText;
        this.giveMode = giveMode;
        this.desc = desc;
    }

    /**
     * 根据文本获取对应的购药方式
     * @param giveModeText
     * @return
     */
    public static Integer getGiveMode(String giveModeText) {
        for (GiveModeTextEnum e : GiveModeTextEnum.values()) {
            if (e.giveModeText.equals(giveModeText)) {
                return e.giveMode;
            }
        }
        return null;
    }

    /**
     * 根据购药方式获取文本
     * @param giveMode
     * @return
     */
    public static String getGiveModeText(Integer giveMode){
        for (GiveModeTextEnum e : GiveModeTextEnum.values()) {
            if (new Integer(giveMode).equals(e.giveMode)) {
                return e.giveModeText;
            }
        }
        return null;
    }
}
