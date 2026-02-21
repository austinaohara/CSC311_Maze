module edu.farmingdale.maze {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.farmingdale.maze to javafx.fxml;
    exports edu.farmingdale.maze;
}