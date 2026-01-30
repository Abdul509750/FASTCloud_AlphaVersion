import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.collections.FXCollections;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;

public class AppLogin extends Application {

    @Override
    public void start(Stage stage) {

        // ===== ROOT =====
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right,#0a1a2e,#162a4a);");

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        // ===== LOGO =====
        Image logoImage = new Image("file:/home/rafay/FASTCloud_AplhaVersion/FASTCLOUD/Resources/FinalLogo.png");
        ImageView logo = new ImageView(logoImage);
        //logo.setFitWidth(260);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setCache(true);
        logo.setEffect(new DropShadow(45, Color.rgb(0, 215, 255, 0.55)));
         logo.setFitWidth(260);
logo.setPreserveRatio(true);
logo.setCache(true);
logo.setCacheHint(CacheHint.SPEED);

        // ===== TITLE =====
        Label title = new Label("Welcome to FASTcloud");
        title.setFont(new Font("Arial Bold", 28));
        title.setTextFill(Color.web("#00D4FF"));

        Label production = new Label("FASTcloud System • Production Build");
        production.setFont(new Font("Arial", 13));
        production.setTextFill(Color.web("#00BFFF"));

        Label subtitle = new Label("Secure cloud access platform");
        subtitle.setFont(new Font("Arial", 14));
        subtitle.setTextFill(Color.web("#1AFFFF"));

        // ===== INPUTS =====
        TextField username = new TextField();
        username.setPromptText("Username");
        username.setMaxWidth(320);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setMaxWidth(320);

        String normal =
                "-fx-background-radius: 16; -fx-padding: 12; -fx-border-radius: 16;" +
                "-fx-border-color: rgba(0,212,255,0.5);" +
                "-fx-background-color: rgba(10,26,46,0.88);" +
                "-fx-text-fill: #00D4FF;";

        String focus =
                "-fx-background-radius: 16; -fx-padding: 12; -fx-border-radius: 16;" +
                "-fx-border-color: #00D4FF; -fx-border-width:3;" +
                "-fx-background-color: rgba(15,40,70,0.98);" +
                "-fx-text-fill: #00FFFF;";

        username.setStyle(normal);
        password.setStyle(normal);

        username.focusedProperty().addListener((a,b,c)-> username.setStyle(c?focus:normal));
        password.focusedProperty().addListener((a,b,c)-> password.setStyle(c?focus:normal));

        // ===== BUTTON =====
        Button loginBtn = new Button("Login");
        loginBtn.setStyle(
                "-fx-background-radius: 26;" +
                "-fx-background-color: linear-gradient(#00D4FF,#0099FF);" +
                "-fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-padding: 12 36;"
        );

        loginBtn.setOnMouseEntered(e ->loginBtn.setStyle("-fx-background-radius:26;-fx-background-color:linear-gradient(#00FFFF,#00B0FF);-fx-text-fill:#0a1a2e;-fx-font-weight:bold;-fx-padding:12 36;"));

        loginBtn.setOnMouseExited(e ->loginBtn.setStyle("-fx-background-radius:26;-fx-background-color:linear-gradient(#00D4FF,#0099FF);-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:12 36;") );

        loginBtn.setOnMousePressed(e -> {
            loginBtn.setScaleX(.95);
            loginBtn.setScaleY(.95);
        });

        loginBtn.setOnMouseReleased(e -> {
            loginBtn.setScaleX(1);
            loginBtn.setScaleY(1);
        });

        // subtle glow + scale on hover for login button
        DropShadow loginHoverShadow = new DropShadow(20, Color.web("#00D4FF"));
        Glow loginGlow = new Glow(0.25);
        loginBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            loginBtn.setEffect(loginHoverShadow);
            ScaleTransition st = new ScaleTransition(Duration.seconds(0.14), loginBtn);
            st.setToX(1.03); st.setToY(1.03); st.play();
            loginBtn.setBlendMode(null);
        });
        loginBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            loginBtn.setEffect(null);
            ScaleTransition st2 = new ScaleTransition(Duration.seconds(0.12), loginBtn);
            st2.setToX(1); st2.setToY(1); st2.play();
        });

        // ===== LOGIN HANDLER =====
        loginBtn.setOnAction(e -> handleLogin(stage, root, username.getText(), password.getText()));

        // ===== GLASS PANEL =====
        VBox loginBox = new VBox(16, username, password, loginBtn);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(36));

        loginBox.setStyle(
                "-fx-background-radius: 30;" +
                "-fx-background-color: linear-gradient(to bottom right, rgba(10,26,46,0.85), rgba(15,40,70,0.65));" +
                "-fx-border-color: rgba(0,212,255,0.7);" +
                "-fx-border-radius: 30;"
        );

        loginBox.setEffect(new DropShadow(50, Color.rgb(0, 212, 255, 0.5)));
        loginBox.setCache(true);
        loginBox.setCacheHint(CacheHint.SPEED);

        // ===== GLOSS OVERLAY =====
        Rectangle gloss = new Rectangle(420, 160);
        gloss.setArcWidth(40);
        gloss.setArcHeight(40);
        gloss.setFill(new LinearGradient(0,0,1,1,true,null,
                new Stop(0, Color.web("rgba(0,255,255,0.65)")),
                new Stop(0.35, Color.web("rgba(0,212,255,0.35)")),
                new Stop(1, Color.web("rgba(0,180,255,0.08)"))
        ));
        gloss.setMouseTransparent(true);

        StackPane glassCard = new StackPane(loginBox, gloss);
        glassCard.setCache(true);
        glassCard.setCacheHint(CacheHint.SPEED);

        // ===== FOOTER =====
        Label footer = new Label("© 2026 FASTcloud Technologies — All Rights Reserved ");
        footer.setFont(new Font("Arial", 12));
        footer.setTextFill(Color.web("#888"));
         
        Label footer2  = new Label("Version 1.0.0 Alpha - Developed by Rafay");
        footer2.setFont(new Font("Arial", 12));
        footer2.setTextFill(Color.web("#888"));
        
        // ===== ASSEMBLE CONTENT =====
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content.getChildren().addAll(logo, title, production, subtitle, glassCard, spacer, footer , footer2);
        root.getChildren().add(content);

        // ===== ANIMATIONS =====
        FadeTransition fade = new FadeTransition(Duration.seconds(1.3), logo);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(6), logo);
        floatAnim.setFromY(0);
        floatAnim.setToY(-6);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.play();

        TranslateTransition enter = new TranslateTransition(Duration.seconds(1), glassCard);
        enter.setFromY(90);
        enter.setToY(0);
        enter.play();

        // ===== STAGE =====
        Scene scene = new Scene(root, 760, 650);
        stage.setTitle("FASTcloud Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void handleLogin(Stage stage, StackPane root, String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        // Create success circle animation
        Circle successCircle = new Circle(0);
        successCircle.setFill(Color.web("#00D4FF"));
        successCircle.setEffect(new DropShadow(60, Color.web("#00FFFF")));
        
        StackPane overlayPane = new StackPane(root, successCircle);
        Scene currentScene = stage.getScene();
        
        // Scale animation for circle
        ScaleTransition scaleCircle = new ScaleTransition(Duration.seconds(0.8), successCircle);
        scaleCircle.setFromX(0);
        scaleCircle.setFromY(0);
        scaleCircle.setToX(150);
        scaleCircle.setToY(150);
        scaleCircle.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        
        // Fade out overlay
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.6), successCircle);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(0.3));
        
        // Sequential animation
        SequentialTransition sequence = new SequentialTransition(
            new ParallelTransition(scaleCircle, fadeOut),
            new PauseTransition(Duration.seconds(0.2))
        );
        
        sequence.setOnFinished(event -> showDashboard(stage));
        
        overlayPane.setStyle("-fx-background-color: linear-gradient(to bottom right,#0a1a2e,#162a4a);");
        Scene overlayScene = new Scene(overlayPane, currentScene.getWidth(), currentScene.getHeight());
        stage.setScene(overlayScene);
        
        sequence.play();
    }

private void showDashboard(Stage stage) {
    DashboardView dashboard = new DashboardView(stage);
    Scene dashScene = dashboard.createScene();
    stage.setScene(dashScene);
}
}