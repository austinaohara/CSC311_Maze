package edu.farmingdale.maze;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.util.Objects;

public final class RobotOverlay {
    private RobotOverlay() {}

    private static final String KEY_RUNTIME = "robotOverlayRuntime";

    //intalls arrow key movement, wires dropdown to show or hide, if already done return existing robot view
    public static ImageView attach(AnchorPane imagePane, ComboBox<String> vehicleDropdown)
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
    private static ImageView findMazeImageView(AnchorPane pane)
    {
        for (Node n : pane.getChildren()) {if (n instanceof ImageView iv) return iv;}
        return null;
    }

    private static void findSpawnRegion(ImageView actor, Image mazeImage, double actorSize) {
        PixelReader reader = mazeImage.getPixelReader();
        int width = (int) mazeImage.getWidth();
        int height = (int) mazeImage.getHeight();

        // scan left to right
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean spaceAvailable = true;

                // check if actorSize * 5 area is fully white
                for (int dx = 0; dx < actorSize * 5; dx++) {
                    for (int dy = 0; dy < actorSize * 5; dy++) {
                        int checkX = x + dx;
                        int checkY = y + dy;
                        if (checkX >= width || checkY >= height) {
                            spaceAvailable = false;
                            break;
                        }
                        Color color = reader.getColor(checkX, checkY);
                        if (!color.equals(Color.WHITE)) {
                            spaceAvailable = false;
                            break;
                        }
                    }
                    if (!spaceAvailable) break;
                }

                if (spaceAvailable) {
                    // set actor to position
                    actor.setTranslateX(x);
                    actor.setTranslateY(y);
                    return;
                }
            }
        }
    }

    private static final class Runtime
    {
        private final AnchorPane imagePane;
        private final ImageView mazeView;
        private final ComboBox<String> dropdown;

        private final ImageView robotView;
        private Group carGroup;

        private final int step = 10; //pixels per key press (screen pixels)

        //stores refernces to anchorpane + image + dropdown, loads image, configures robot size
        Runtime(AnchorPane imagePane, ImageView mazeView, ComboBox<String> dropdown)
        {
            this.imagePane = imagePane;
            this.mazeView = mazeView;
            this.dropdown = dropdown;

            Image robotImg = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/edu/farmingdale/maze/images/robot.png"),
                    "Missing robot.png at /edu/farmingdale/maze/images/robot.png"
            ));

            robotView = new ImageView(robotImg);
            robotView.setFitWidth(25);
            robotView.setFitHeight(robotView.getFitWidth());
            robotView.setPreserveRatio(true);
            robotView.setMouseTransparent(true);
        }

        //adds robot to anchor, sets initial translation and listens to dropdown changes
        void install()
        {
            //Put robot on top of maze
            imagePane.getChildren().add(robotView);

            //Set model spawn / spawnpoint
//            robotView.setTranslateX(0);
//            robotView.setTranslateY(0);
            findSpawnRegion(robotView, mazeView.getImage(), robotView.getFitHeight()); //Assumes model is a square(l=h)

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

                // prevent arrow keys from changing ComboBox selection
                e.consume();

                Node active = getActiveNode();
                if (active == null) return;

                double dx = 0, dy = 0;
                if (c == KeyCode.UP) dy = -step;
                if (c == KeyCode.DOWN) dy = step;
                if (c == KeyCode.LEFT) dx = -step;
                if (c == KeyCode.RIGHT) dx = step;

                //Check bounds of actor
                PixelReader reader = mazeView.getImage().getPixelReader();
                if (reader != null) {
                    Bounds mazeB = mazeView.getBoundsInParent();
                    Bounds actorB = active.getBoundsInParent();

                    double scaleX = mazeView.getImage().getWidth() / mazeB.getWidth();
                    double scaleY = mazeView.getImage().getHeight() / mazeB.getHeight();

                    // Robot next bounding box relative to image
                    double nextMinX = actorB.getMinX() + dx - mazeB.getMinX();
                    double nextMinY = actorB.getMinY() + dy - mazeB.getMinY();
                    double nextMaxX = actorB.getMaxX() + dx - mazeB.getMinX();
                    double nextMaxY = actorB.getMaxY() + dy - mazeB.getMinY();

                    int imgMinX = (int)(nextMinX * scaleX);
                    int imgMinY = (int)(nextMinY * scaleY);
                    int imgMaxX = (int)(nextMaxX * scaleX);
                    int imgMaxY = (int)(nextMaxY * scaleY);

                    imgMinX = Math.max(0, Math.min(imgMinX, (int) mazeView.getImage().getWidth() - 1));
                    imgMinY = Math.max(0, Math.min(imgMinY, (int) mazeView.getImage().getHeight() - 1));
                    imgMaxX = Math.max(0, Math.min(imgMaxX, (int) mazeView.getImage().getWidth() - 1));
                    imgMaxY = Math.max(0, Math.min(imgMaxY, (int) mazeView.getImage().getHeight() - 1));

                    // Only move if all corners are white
                    if (!reader.getColor(imgMinX, imgMinY).equals(Color.WHITE)
                            || !reader.getColor(imgMaxX, imgMinY).equals(Color.WHITE)
                            || !reader.getColor(imgMinX, imgMaxY).equals(Color.WHITE)
                            || !reader.getColor(imgMaxX, imgMaxY).equals(Color.WHITE)) {
                        return;
                    }
                }

                active.setTranslateX(active.getTranslateX() + dx);
                active.setTranslateY(active.getTranslateY() + dy);

                clampToMaze(active);
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
