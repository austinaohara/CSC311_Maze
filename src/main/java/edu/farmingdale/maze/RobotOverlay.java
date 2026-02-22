package edu.farmingdale.maze;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import java.util.Objects;

public final class RobotOverlay {
    private RobotOverlay() {}

    private static final String KEY_RUNTIME = "robotOverlayRuntime";

    //intalls arrow key movement, wires dropdown to show or hide, if already done return existing robot view
    public static ImageView attach(StackPane imagePane, ComboBox<String> vehicleDropdown)
    {
        Object existing = imagePane.getProperties().get(KEY_RUNTIME);
        if (existing instanceof Runtime rt) return rt.robotView;

        ImageView mazeView = findMazeImageView(imagePane);
        if (mazeView == null) {throw new IllegalStateException("RobotOverlay.attach: No maze ImageView found in imagePane.");}

        Runtime rt = new Runtime(imagePane, mazeView, vehicleDropdown);
        imagePane.getProperties().put(KEY_RUNTIME, rt);
        rt.install();
        return rt.robotView;
    }

    //finds the maze selected
    private static ImageView findMazeImageView(StackPane pane)
    {
        for (Node n : pane.getChildren()) {if (n instanceof ImageView iv) return iv;}
        return null;
    }

    private static final class Runtime
    {
        private final StackPane imagePane;
        private final ImageView mazeView;
        private final ComboBox<String> dropdown;

        private final ImageView robotView;
        private Group carGroup;

        private final int step = 10; //pixels per key press (screen pixels)

        //stores refernces to stackpane + image + dropdown, loads image, configures robot size
        Runtime(StackPane imagePane, ImageView mazeView, ComboBox<String> dropdown)
        {
            this.imagePane = imagePane;
            this.mazeView = mazeView;
            this.dropdown = dropdown;

            Image robotImg = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/edu/farmingdale/maze/images/robot.png"),
                    "Missing robot.png at /edu/farmingdale/maze/images/robot.png"
            ));

            robotView = new ImageView(robotImg);
            robotView.setFitWidth(40);
            robotView.setFitHeight(30);
            robotView.setPreserveRatio(true);
            robotView.setMouseTransparent(true);
        }

        //adds robot to stackpane, sets initial translation and listens to dropdown changes
        void install()
        {
            //Put robot on top of maze
            imagePane.getChildren().add(robotView);

            //Start centered for now, translate will moves it around
            robotView.setTranslateX(0);
            robotView.setTranslateY(0);

            //displays robot based on the dropdown option
            dropdown.valueProperty().addListener((obs, a, b) -> syncVisibility());
            syncVisibility();

            //make sure  we can capture keys
            imagePane.setFocusTraversable(true);
            imagePane.setOnMouseClicked(e -> imagePane.requestFocus());

            //install key handler when scene becomes available
            imagePane.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) installKeys(newS);
            });
            if (imagePane.getScene() != null) installKeys(imagePane.getScene());

            Platform.runLater(this::clampActiveToMaze);
        }

        //add key listener to the Scene so arrowkeys work
        private void installKeys(Scene scene)
        {
            String key = "robotOverlayKeysInstalled";
            if (Boolean.TRUE.equals(scene.getProperties().get(key))) return;
            scene.getProperties().put(key, true);

            // event filter so it works even if ComboBox is focused
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                KeyCode c = e.getCode();
                if (c != KeyCode.UP && c != KeyCode.DOWN && c != KeyCode.LEFT && c != KeyCode.RIGHT) return;

                Node active = getActiveNode();
                if (active == null) return;

                double dx = 0, dy = 0;
                if (c == KeyCode.UP) dy = -step;
                if (c == KeyCode.DOWN) dy = step;
                if (c == KeyCode.LEFT) dx = -step;
                if (c == KeyCode.RIGHT) dx = step;

                active.setTranslateX(active.getTranslateX() + dx);
                active.setTranslateY(active.getTranslateY() + dy);

                clampToMaze(active);

                // prevent arrow keys from changing ComboBox selection
                e.consume();
            });
        }

        private Node getActiveNode()
        {
            String v = dropdown.getValue();
            if ("Car".equals(v))
            {
                return carGroup; // might be null
            }
            return robotView; // Robot default
        }

        //decides if Robot or Car should move
        private void syncVisibility()
        {
            boolean robotSelected = "Robot".equals(dropdown.getValue());

            robotView.setVisible(robotSelected);

            if (carGroup != null) {carGroup.setVisible(!robotSelected);}
        }

        //show robot when dropdown is robot and hides it if not
        private void clampActiveToMaze()
        {
            Node active = getActiveNode();
            if (active != null) clampToMaze(active);
        }

        //logic to assure robot can't leave the bounds of the maze
        private void clampToMaze(Node actor)
        {
            Bounds mazeB = mazeView.getBoundsInParent();
            Bounds actorB = actor.getBoundsInParent();

            double adjustX = 0;
            double adjustY = 0;

            if (actorB.getMinX() < mazeB.getMinX()) adjustX = mazeB.getMinX() - actorB.getMinX();
            else if (actorB.getMaxX() > mazeB.getMaxX()) adjustX = mazeB.getMaxX() - actorB.getMaxX();

            if (actorB.getMinY() < mazeB.getMinY()) adjustY = mazeB.getMinY() - actorB.getMinY();
            else if (actorB.getMaxY() > mazeB.getMaxY()) adjustY = mazeB.getMaxY() - actorB.getMaxY();

            actor.setTranslateX(actor.getTranslateX() + adjustX);
            actor.setTranslateY(actor.getTranslateY() + adjustY);
        }
    }
}
