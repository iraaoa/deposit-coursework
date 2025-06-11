package com.sabat.deposit.util;

public class Logger {

    public static final void error(String mess, String p) {
        LoggerConfig.logger.error(mess, p);
    }

    public static final void info(String mess) {
        LoggerConfig.logger.info(mess);
    }
}