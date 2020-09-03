package recipe.caNew;

public enum CARecipeTypeEnum {

    Before_CAType("before", new CaBeforeProcessType()),
    After_CAType("after",new CaAfterProcessType());

    private String exceptionName;

    private AbstractCaProcessType caProcess;

    CARecipeTypeEnum(String exceptionName, AbstractCaProcessType caProcess) {
        this.exceptionName = exceptionName;
        this.caProcess = caProcess;
    }

    public static AbstractCaProcessType getCaProcessType(String exceptionName){
        for(CARecipeTypeEnum c:CARecipeTypeEnum.values()){
            if(c.getExceptionName().equals(exceptionName)){
                return c.getCaProcess();
            }
        }
        return null;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }

    public AbstractCaProcessType getCaProcess() {
        return caProcess;
    }

    public void setCaProcess(AbstractCaProcessType caProcess) {
        this.caProcess = caProcess;
    }
}