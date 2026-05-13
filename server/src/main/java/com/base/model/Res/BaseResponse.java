package com.base.model.Res;

public class BaseResponse {

    private Integer statusCode;

    private Object data;

    private String description;

    public BaseResponse() {
    }

    public BaseResponse(
            Integer statusCode,
            Object data,
            String description) {

        this.statusCode = statusCode;
        this.data = data;
        this.description = description;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}