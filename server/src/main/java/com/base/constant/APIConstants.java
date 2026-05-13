package com.base.constant;

import org.springframework.beans.factory.annotation.Value;

public final class APIConstants {

    private APIConstants() {
    }

    // Domain api use for all lc import
    public static final String API_LC_BPMN = "http://localhost:8081";

    public static final String START_TASK = "/process/start";
    public static final String GET_TASK ="/tasks/getTasks";
    public static final String COMPLETE_TASK = "/tasks/complete";
    public static final String ENDPOINT_UPDATE = "/update";
    public static final String ENDPOINT_GET_DETAIL = "/get-detail";
    public static final String ENDPOINT_SEARCH = "/search";
    public static final String ENDPOINT_GET_ALL = "/get-all";
    public static final String ENDPOINT_CREATE_QF_AREA = "/create-qf-area";
    public static final String ENDPOINT_CREATE_INPUT_AREA = "/create-input-area";
    public static final String ENDPOINT_GET_LIST_OWED_FEE = "/get-list-owed-fee";
    public static final String ENDPOINT_CLEAR_EDIT_MODE = "/clear-edit-mode";

}
