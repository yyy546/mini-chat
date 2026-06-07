package com.minichat.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private static final Integer SUCCESS = 1;
    private static final Integer ERROR = 0;
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success(){
        Result<T> result = new Result<>();
        result.code = SUCCESS;
        return result;
    }

    public static <T> Result<T> success(String msg){
        Result<T> result = new Result<>();
        result.code = SUCCESS;
        result.msg = msg;
        return result;
    }

    public static <T> Result<T> success(T data){
        Result<T> result = new Result<>();
        result.code = SUCCESS;
        result.data = data;
        return result;
    }

     public static <T> Result<T> success(String msg, T data){
        Result<T> result = new Result<>();
        result.code = SUCCESS;
        result.msg = msg;
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(String msg){
        Result<T> result = new Result<>();
        result.code = ERROR;
        result.msg = msg;
        return result;
    }

    public static <T> Result<T> error(int code, String msg){
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        return result;
    }
}
