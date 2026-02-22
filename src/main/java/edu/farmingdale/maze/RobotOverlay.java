package edu.farmingdale.maze;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Objects;

public final class RobotOverlay
{
    private RobotOverlay() {}

    public static ImageView attach(StackPane imagePane, ComboBox<String> vehicleDropdown)
    {
        Image robotImg = new Image(Objects.requireNonNull(
                RobotOverlay.class.getResourceAsStream("/edu/farmingdale/maze/images/robot.png"),
                "Missing robot.png at /edu/farmingdale/maze/images/robot.png"
        ));

        ImageView robotView = new ImageView(robotImg);

        //random robot size for now
        robotView.setFitWidth(40);
        robotView.setFitHeight(40);
        robotView.setPreserveRatio(true);

        //make sure the overlay is on top of the maze
        imagePane.getChildren().add(robotView);
        StackPane.setAlignment(robotView, Pos.CENTER);
        robotView.setMouseTransparent(true);

        // show/hide based on the dropdown
        robotView.setVisible("Robot".equals(vehicleDropdown.getValue()));
        vehicleDropdown.valueProperty().addListener((obs, oldVal, newVal) ->
                robotView.setVisible("Robot".equals(newVal))
        );

        return robotView;
    }
}
