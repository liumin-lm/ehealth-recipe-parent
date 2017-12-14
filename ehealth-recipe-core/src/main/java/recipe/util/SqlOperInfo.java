package recipe.util;

import recipe.constant.ConditionOperator;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/5/5.
 */
public class SqlOperInfo
{
    public static final String BETWEEN_START = "_startDate";

    public static final String BETWEEN_END = "_endDate";

    private String key;

    private String oper;

    private Object value;

    private Object extValue;

    public SqlOperInfo(String key, Object value){
         this(key, ConditionOperator.EQUAL,value);
    }

    public SqlOperInfo(String key, String oper, Object value){
        this(key, oper,value,null);
    }

    public SqlOperInfo(String key, String oper, Object value, Object extValue){
        this.key = key;
        this.oper = oper;
        this.value = value;
        this.extValue = extValue;
    }

    public String getHqlCondition(){
        StringBuilder sql = new StringBuilder();
        sql.append(" "+this.key+" "+this.oper);
        if(ConditionOperator.BETWEEN.equals(this.oper)){
            sql.append(" :"+this.key+BETWEEN_START+" and :"+this.key+BETWEEN_END);
        }else{
            sql.append(" :"+this.key);
        }
        sql.append(" ");
        return sql.toString();
    }

    public String getKey() {
        return key;
    }


    public String getOper() {
        return oper;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getExtValue() {
        return extValue;
    }

    public void setExtValue(Object extValue) {
        this.extValue = extValue;
    }
}
