package com.lishicloud.lretry.constant;

/**
 * @author ztq
 */
public class RetryConstant {

    public static Byte RETRY_INIT = 0;
    public static Byte RETRY_SUCCESSFUL = 1;
    public static Byte RETRY_FAIL = 2;


    public static Byte AUTO_COMPENSATE_DISABLE = 0;
    public static Byte AUTO_COMPENSATE_ENABLE = 1;
    public static Byte AUTO_COMPENSATE_ED = 2;


    public static String RETRY_TYPE_AUTO = "auto";
    public static String RETRY_TYPE_LABOUR = "labour";

    //db
    public static final String INIT_SQL = "sql/init_tables.sql";
    public static final String TABLE_NAME = "retry_event_compensation";


    public static final Integer MAX_MESSAGE_LENGTH = 500;
 }
