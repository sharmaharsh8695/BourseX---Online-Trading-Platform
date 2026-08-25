package com.harsh.finance_project.exception;

public class ErrorResponse {
    private String code;
    private String mssg;

    public ErrorResponse(String code, String mssg) {
        this.code = code;
        this.mssg = mssg;
    }

    public String getCode() {
        return code;
    }

    public String getMssg() {
        return mssg;
    }
}
