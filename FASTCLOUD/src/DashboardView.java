import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardView {

    private Stage stage;

    public DashboardView(Stage stage) {
        this.stage = stage;
    }

    public Scene createScene() {
        // ===== ROOT =====
        BorderPane dashRoot = new BorderPane();
        dashRoot.setStyle("-fx-background-color: linear-gradient(to bottom right, #0a1a2e, #162a4a);");

        // ===== LEFT NAV =====
        VBox leftNav = new VBox(16);
        leftNav.setPadding(new Insets(18));
        leftNav.setStyle("-fx-background-color: rgba(10, 20, 35, 0.12);");

        // Logo
        ImageView logo = new ImageView(
            new Image("file:/home/rafay/FASTCloud_AplhaVersion/FASTCLOUD/Resources/FinalLogo.png")
        );
        logo.setFitWidth(200);
        logo.setPreserveRatio(true);
        logo.setEffect(new DropShadow(45, Color.rgb(0, 215, 255, 0.55)));

        // Nav buttons
        Button navDash = new Button("Dashboard");
        Button navStorage = new Button("Leader Board");
        Button navCompute = new Button("Compute");
        Button navAnalytics = new Button("Analytics");

        Button[] navButtons = {navDash, navStorage, navCompute, navAnalytics};
        for (Button b : navButtons) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle(
                "-fx-background-radius: 12;" +
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #BFEFFF;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 10 12;"
            );

            // Hover effects
            DropShadow navShadow = new DropShadow(18, Color.web("#00D4FF"));
            b.addEventHandler(MouseEvent.MOUSE_ENTERED, ev -> {
                ScaleTransition s = new ScaleTransition(Duration.seconds(0.12), b);
                s.setToX(1.03);
                s.setToY(1.03);
                s.play();
                b.setEffect(navShadow);
                b.setStyle(
                    "-fx-background-radius: 12;" +
                    "-fx-background-color: rgba(0, 212, 255, 0.06);" +
                    "-fx-text-fill: #00FFFF;" +
                    "-fx-font-weight: 700;" +
                    "-fx-padding: 10 12;"
                );
            });

            b.addEventHandler(MouseEvent.MOUSE_EXITED, ev -> {
                ScaleTransition s2 = new ScaleTransition(Duration.seconds(0.1), b);
                s2.setToX(1);
                s2.setToY(1);
                s2.play();
                b.setEffect(null);
                b.setStyle(
                    "-fx-background-radius: 12;" +
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #BFEFFF;" +
                    "-fx-font-weight: 600;" +
                    "-fx-padding: 10 12;"
                );
            });
        }

        leftNav.getChildren().add(logo);
        leftNav.getChildren().addAll(navDash, navStorage, navCompute, navAnalytics);

        // Wrap left nav in ScrollPane
        ScrollPane leftScroll = new ScrollPane(leftNav);
        leftScroll.setFitToWidth(true);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        dashRoot.setLeft(leftScroll);

        // ===== CENTER CONTENT =====
        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(20));

        // Floating main label
        Label mainLabel = new Label("Welcome to FASTcloud Dashboard!");
        mainLabel.setTextFill(Color.web("#00D4FF"));
        mainLabel.setFont(Font.font("Arial Bold", FontWeight.EXTRA_BOLD, 28));

        TranslateTransition floatLabel = new TranslateTransition(Duration.seconds(3), mainLabel);
        floatLabel.setFromY(0);
        floatLabel.setToY(-10);
        floatLabel.setAutoReverse(true);
        floatLabel.setCycleCount(Animation.INDEFINITE);
        floatLabel.play();

        centerContent.getChildren().add(mainLabel);

        // Add dashboard items
        for (int i = 1; i <= 30; i++) {
            Label item = new Label("Dashboard Item " + i);
            item.setTextFill(Color.web("#00D4FF"));
            item.setFont(Font.font("Arial", 16));
            item.setPadding(new Insets(8));
            item.setStyle("-fx-background-color: rgba(0, 212, 255, 0.08); -fx-background-radius: 8;");
            centerContent.getChildren().add(item);
        }

        // Wrap center content in ScrollPane
        ScrollPane centerScroll = new ScrollPane(centerContent);
        centerScroll.setFitToWidth(true);
        centerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        dashRoot.setCenter(centerScroll);

        // ===== FADE IN DASHBOARD =====
        dashRoot.setOpacity(0);
        FadeTransition fadeInDash = new FadeTransition(Duration.seconds(0.8), dashRoot);
        fadeInDash.setFromValue(0);
        fadeInDash.setToValue(1);
        fadeInDash.play();

        return new Scene(dashRoot, 760, 650);
    }
}