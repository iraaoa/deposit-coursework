module com.sabat.deposit {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.apache.logging.log4j;
    requires java.sql;
    requires jbcrypt;
    requires reload4j;

    requires java.mail;

    opens com.sabat.deposit.controller to javafx.fxml;
    exports com.sabat.deposit.controller;
    exports com.sabat.deposit;
    opens com.sabat.deposit.model to javafx.base;

    exports com.sabat.deposit.service;
    exports com.sabat.deposit.model;
    opens com.sabat.deposit.service to org.junit.platform.commons, org.mockito;
    // Додай інші opens/exports за потреби
}
