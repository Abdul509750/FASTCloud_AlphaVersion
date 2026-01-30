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
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
//import javafx.beans.property.DoubleProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.collections.FXCollections;
import javafx.scene.control.ProgressBar;

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
        Image logoImage = new Image("file:/home/rafay/FASTCloud_AplhaVersion/FASTCLOUD/src/FinalLogo.png");
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
        // ===== DASHBOARD =====
        BorderPane dashRoot = new BorderPane();
        dashRoot.setStyle("-fx-background-color: linear-gradient(to bottom right,#0a1a2e,#162a4a);");

        // --- LEFT NAV ---
        VBox leftNav = new VBox(16);
        leftNav.setPadding(new Insets(18));
        leftNav.setPrefWidth(220);
        leftNav.setStyle("-fx-background-color: rgba(10,20,35,0.12); -fx-border-color: rgba(0,200,255,0.06);");

        Label brand = new Label("SKYNET");
        brand.setFont(Font.font("Arial Bold", 20));
        brand.setTextFill(Color.web("#00D4FF"));

        Button navDash = new Button("Dashboard");
        Button navStorage = new Button("Data Storage");
        Button navCompute = new Button("Compute");
        Button navAnalytics = new Button("Analytics");

        for (Button b : new Button[]{navDash, navStorage, navCompute, navAnalytics}) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-background-radius:12; -fx-background-color: transparent; -fx-text-fill:#BFEFFF; -fx-font-weight:600; -fx-padding:10 12;");
            // interactive hover
            DropShadow navShadow = new DropShadow(18, Color.web("#00D4FF"));
            b.addEventHandler(MouseEvent.MOUSE_ENTERED, ev -> {
                ScaleTransition s = new ScaleTransition(Duration.seconds(0.12), b);
                s.setToX(1.03); s.setToY(1.03); s.play();
                b.setEffect(navShadow);
                b.setStyle("-fx-background-radius:12; -fx-background-color: rgba(0,212,255,0.06); -fx-text-fill:#001a2e; -fx-font-weight:700; -fx-padding:10 12;");
            });
            b.addEventHandler(MouseEvent.MOUSE_EXITED, ev -> {
                ScaleTransition s2 = new ScaleTransition(Duration.seconds(0.1), b);
                s2.setToX(1); s2.setToY(1); s2.play();
                b.setEffect(null);
                b.setStyle("-fx-background-radius:12; -fx-background-color: transparent; -fx-text-fill:#BFEFFF; -fx-font-weight:600; -fx-padding:10 12;");
            });
        }

        leftNav.getChildren().addAll(brand, navDash, navStorage, navCompute, navAnalytics);

        // --- CENTER: Charts and main content ---
        VBox center = new VBox(18);
        center.setPadding(new Insets(20, 28, 20, 28));

        HBox headerRow = new HBox(12);
        VBox titleBox = new VBox(4);
        Label dashTitle = new Label("FASTcloud Dashboard");
        dashTitle.setFont(Font.font("Arial Bold", FontWeight.EXTRA_BOLD, 28));
        dashTitle.setTextFill(Color.web("#00D4FF"));
        Label dashSubtitle = new Label("Overview of system usage and health");
        dashSubtitle.setTextFill(Color.web("#9FDFFF"));
        titleBox.getChildren().addAll(dashTitle, dashSubtitle);
        Region hdrSpacer = new Region();
        HBox.setHgrow(hdrSpacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleBox, hdrSpacer);

        // CPU Line Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("% CPU");
        LineChart<String, Number> cpuChart = new LineChart<>(xAxis, yAxis);
        cpuChart.setTitle("CPU Memory Usage");
        cpuChart.setLegendVisible(false);
        cpuChart.setCreateSymbols(false);
        cpuChart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("00:00", 20));
        series.getData().add(new XYChart.Data<>("04:00", 35));
        series.getData().add(new XYChart.Data<>("08:00", 28));
        series.getData().add(new XYChart.Data<>("12:00", 48));
        series.getData().add(new XYChart.Data<>("16:00", 30));
        series.getData().add(new XYChart.Data<>("20:00", 42));
        series.getData().add(new XYChart.Data<>("24:00", 33));
        cpuChart.getData().add(series);

        // animate cpu chart fade-in
        FadeTransition cpuFade = new FadeTransition(Duration.seconds(0.9), cpuChart);
        cpuFade.setFromValue(0);
        cpuFade.setToValue(1);
        cpuFade.play();

        // Mid row: pie + active deployments
        HBox midRow = new HBox(18);

        // Storage pie
        PieChart storagePie = new PieChart(FXCollections.observableArrayList(
            new PieChart.Data("Used", 75),
            new PieChart.Data("Free", 25)
        ));
        storagePie.setTitle("Storage Utilization");
        storagePie.setLabelsVisible(false);
        storagePie.setPrefSize(320, 220);

        // Active deployments & progress
        VBox rightCards = new VBox(12);
        rightCards.setPrefWidth(360);
        Label deploymentsTitle = new Label("Active Deployments");
        deploymentsTitle.setFont(Font.font(16));
        deploymentsTitle.setTextFill(Color.web("#BFEFFF"));

        HBox deploy1 = new HBox(8);
        Label d1 = new Label("E-commerce API");
        d1.setTextFill(Color.web("#CFF8FF"));
        ProgressBar p1 = new ProgressBar(0.65);
        p1.setPrefWidth(180);
        Label p1lbl = new Label("65%");
        p1lbl.setTextFill(Color.web("#9FDFFF"));
        deploy1.getChildren().addAll(d1, p1, p1lbl);

        HBox deploy2 = new HBox(8);
        Label d2 = new Label("Mobile Backend");
        d2.setTextFill(Color.web("#CFF8FF"));
        ProgressBar p2 = new ProgressBar(0.30);
        p2.setPrefWidth(180);
        Label p2lbl = new Label("30%");
        p2lbl.setTextFill(Color.web("#9FDFFF"));
        deploy2.getChildren().addAll(d2, p2, p2lbl);

        rightCards.getChildren().addAll(deploymentsTitle, deploy1, deploy2);

        midRow.getChildren().addAll(storagePie, rightCards);

        // animate pie and right cards
        FadeTransition pieFade = new FadeTransition(Duration.seconds(0.9), storagePie);
        pieFade.setFromValue(0); pieFade.setToValue(1); pieFade.play();

        FadeTransition rcFade = new FadeTransition(Duration.seconds(0.9), rightCards);
        rcFade.setFromValue(0); rcFade.setToValue(1); rcFade.play();

        // animate progress bars from 0 to target
        Timeline p1Anim = new Timeline(new KeyFrame(Duration.seconds(0), new KeyValue(p1.progressProperty(), 0)),
            new KeyFrame(Duration.seconds(1.2), new KeyValue(p1.progressProperty(), 0.65)));
        p1Anim.play();

        Timeline p2Anim = new Timeline(new KeyFrame(Duration.seconds(0), new KeyValue(p2.progressProperty(), 0)),
            new KeyFrame(Duration.seconds(1.2), new KeyValue(p2.progressProperty(), 0.30)));
        p2Anim.play();

        // Activity feed
        Label feedTitle = new Label("Recent Activity Feed");
        feedTitle.setTextFill(Color.web("#BFEFFF"));
        ListView<String> feed = new ListView<>(FXCollections.observableArrayList(
            "16:08:22 - VM 'Tinderphp-Alpha' scaled up",
            "14:05:23 - New bucket 'financial_Q4' completed",
            "12:02:10 - User 'rafay' deployed API"
        ));
        feed.setPrefHeight(140);

        // slide-in animation for feed
        TranslateTransition feedIn = new TranslateTransition(Duration.seconds(0.7), feed);
        feedIn.setFromX(80); feedIn.setToX(0); feedIn.play();

        center.getChildren().addAll(headerRow, cpuChart, midRow, feedTitle, feed);

        // --- RIGHT: summary panels ---
        VBox right = new VBox(14);
        right.setPadding(new Insets(18));
        right.setPrefWidth(300);

        Label costTitle = new Label("Cost Overview");
        costTitle.setFont(Font.font(16));
        costTitle.setTextFill(Color.web("#BFEFFF"));
        Label costVal = new Label("$1,250.75");
        costVal.setFont(Font.font("Arial Bold", 20));
        costVal.setTextFill(Color.web("#7CFFC4"));

        Label healthTitle = new Label("System Health");
        healthTitle.setTextFill(Color.web("#BFEFFF"));
        Label healthVal = new Label("All Systems Operational");
        healthVal.setTextFill(Color.web("#9FDFFF"));

        right.getChildren().addAll(costTitle, costVal, new Separator(), healthTitle, healthVal);

        // Assemble
        dashRoot.setLeft(leftNav);
        dashRoot.setCenter(center);
        dashRoot.setRight(right);

        Scene dashScene = new Scene(dashRoot, 1200, 720);
        stage.setScene(dashScene);

        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), dashRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // subtle periodic pulse on brand
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.8), brand);
        pulse.setFromX(1); pulse.setFromY(1); pulse.setToX(1.02); pulse.setToY(1.02);
        pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE); pulse.play();

        // make cards and nav more interactive: card hover
        for (javafx.scene.Node n : new javafx.scene.Node[]{storagePie, cpuChart}) {
            n.addEventHandler(MouseEvent.MOUSE_ENTERED, ev -> {
                ScaleTransition s = new ScaleTransition(Duration.seconds(0.12), n);
                s.setToX(1.02); s.setToY(1.02); s.play();
            });
            n.addEventHandler(MouseEvent.MOUSE_EXITED, ev -> {
                ScaleTransition s2 = new ScaleTransition(Duration.seconds(0.1), n);
                s2.setToX(1); s2.setToY(1); s2.play();
            });
        }
    }

    private VBox createDashCard(String title, String description) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-radius: 20;" +
                "-fx-background-color: linear-gradient(to bottom right, rgba(0,212,255,0.2), rgba(0,150,255,0.1));" +
                "-fx-border-color: rgba(0,212,255,0.5);" +
                "-fx-border-radius: 20;" +
                "-fx-padding: 30; -fx-min-width: 180; -fx-pref-height: 150;"
        );
        card.setEffect(new DropShadow(30, Color.rgb(0, 212, 255, 0.4)));
        card.setAlignment(Pos.CENTER);

        Label cardTitle = new Label(title);
        cardTitle.setFont(new Font("Arial Bold", 20));
        cardTitle.setTextFill(Color.web("#00D4FF"));

        Label cardDesc = new Label(description);
        cardDesc.setFont(new Font("Arial", 12));
        cardDesc.setTextFill(Color.web("#00BFFF"));
        cardDesc.setWrapText(true);

        card.getChildren().addAll(cardTitle, cardDesc);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

