package recipe.service.recipeexception;

import ctd.util.exception.CodedBaseRuntimeException;

/**
 * 复诊开处方规则优化.在线复诊处方优先抛出异常处理
 */
public class RevisitException extends CodedBaseRuntimeException {

    public RevisitException(){
    }

    public RevisitException(String message){
        super(message);
    }

    public RevisitException(Integer code,String message){
        super(code,message);
    }
}
