package edu.farmingdale.maze;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MazeApp extends Application {

    @Override
    public void start(Stage stage) {
        // Scene 1 (main menu)
        Label label = new Label("No maze loaded!");
        Button openButton = new Button("Open Maze");

        VBox firstRoot = new VBox(15, label, openButton);
        firstRoot.setAlignment(Pos.CENTER);

        Scene firstScene = new Scene(firstRoot, 500, 300);

        openButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            // Start in resources folder
            File resourcesFolder = new File("src/main/resources/edu/farmingdale/maze/images");
            if (resourcesFolder.exists() && resourcesFolder.isDirectory()) {
                fileChooser.setInitialDirectory(resourcesFolder);
            }

            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                Image image = new Image(selectedFile.toURI().toString());

                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);

                // Back button
                Button mainMenuButton = new Button("Main Menu");
                mainMenuButton.setOnAction(ev -> {
                    stage.setScene(firstScene);
                    stage.setTitle("Image Picker");
                });

                // Put image in center, button at top
                BorderPane secondRoot = new BorderPane();

                HBox topBar = new HBox(mainMenuButton);
                topBar.setPadding(new Insets(10));
                topBar.setAlignment(Pos.TOP_LEFT);

                StackPane imagePane = new StackPane(imageView);
                imagePane.setPadding(new Insets(10));

                secondRoot.setTop(topBar);
                secondRoot.setCenter(imagePane);

                Scene secondScene = new Scene(secondRoot, 800, 600);

                // Resize image with window (leave some room for button bar)
                imageView.fitWidthProperty().bind(secondScene.widthProperty().subtract(20));
                imageView.fitHeightProperty().bind(secondScene.heightProperty().subtract(70));

                stage.setScene(secondScene);
                stage.setTitle(selectedFile.getName());
            }
        });

        stage.setTitle("Maze App");
        stage.setScene(firstScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}