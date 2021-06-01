package recipe.caNew;

public enum CARecipeTypeEnum {

    Before_CAType(0, new CaBeforeProcessType()),
    After_CAType(1,new CaAfterProcessType());

    private Integer exceptionName;

    private AbstractCaProcessType caProcess;

    CARecipeTypeEnum(Integer exceptionName, AbstractCaProcessType caProcess) {
        this.exceptionName = exceptionName;
        this.caProcess = caProcess;
    }

    public static AbstractCaProcessType getCaProcessType(Integer exceptionName){
        for(CARecipeTypeEnum c:CARecipeTypeEnum.values()){
            if(c.getExceptionName().equals(exceptionName)){
                return c.getCaProcess();
            }
        }
        return null;
    }

    public Integer getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(Integer exceptionName) {
        this.exceptionName = exceptionName;
    }

    public AbstractCaProcessType getCaProcess() {
        return caProcess;
    }

    public void setCaProcess(AbstractCaProcessType caProcess) {
        this.caProcess = caProcess;
    }
}