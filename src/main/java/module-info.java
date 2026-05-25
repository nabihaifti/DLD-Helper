module com.example.dldtool {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.example.dldtool to javafx.fxml;
    opens com.example.dldtool.controller to javafx.fxml;

    exports com.example.dldtool;
    exports com.example.dldtool.model;
    exports com.example.dldtool.model.kmap;
}