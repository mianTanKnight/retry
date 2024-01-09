package com.lishicloud.lretry.exception;

import com.google.common.collect.Lists;

import javax.naming.ServiceUnavailableException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientException;
import java.util.List;

/**
 * @author ztq
 */
public enum RetryExceptionEnum {
    //base 自定义
    BASE_EXCEPTION {
        @Override
        public boolean isExists(Throwable e) {
            return e instanceof RetryException;
        }
    },
    //Transient
    TRANSIENT_EXCEPTION {
        private final List<Class<? extends Throwable>> throwableList = Lists.newArrayList(
                SocketTimeoutException.class,
                ConnectException.class,
                ServiceUnavailableException.class
        );

        @Override
        public boolean isExists(Throwable e) {
            return throwableList.stream().anyMatch(clazz -> clazz.isInstance(e));
        }
    },
    //SQL
    SQL_EXCEPTION {
        private final List<Class<? extends Throwable>> throwableList = Lists.newArrayList(SQLTransientException.class,
                SQLTimeoutException.class
        );

        @Override
        public boolean isExists(Throwable e) {
            return throwableList.stream().anyMatch(clazz -> clazz.isInstance(e));
        }
    };

    public abstract boolean isExists(Throwable e);


}
