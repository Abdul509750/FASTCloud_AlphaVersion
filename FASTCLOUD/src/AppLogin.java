import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AppLogin extends Application {

    private static final String DEEP_SPACE  = "#060A12";
    private static final String BLUE_CORE   = "#3B82F6";
    private static final String BLUE_BRIGHT = "#60A5FA";
    private static final String BLUE_DEEP   = "#1D4ED8";
    private static final String CYAN_GLOW   = "#06B6D4";
    private static final String ERROR_COL   = "#F87171";

    private final FirebaseAuthService authService      = new FirebaseAuthService();
    private final FirestoreService    firestoreService = new FirestoreService();

    private Button            loginBtn;
    private Label             errorLabel;
    private ProgressIndicator spinner;
    private TextField         emailField;
    private PasswordField     passField;
    private Stage             primaryStage; // ← stored cleanly, no casting

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + DEEP_SPACE + ";");

        // Animated background
        Pane bgCanvas = new Pane();
        bgCanvas.setMouseTransparent(true);
        createParticles(bgCanvas, 900, 820);
        createGridLines(bgCanvas, 900, 820);

        // Radial glow
        Circle bgGlow = new Circle(280);
        bgGlow.setFill(new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLUE_CORE, 0.12)),
            new Stop(1, Color.web(BLUE_CORE, 0.0))
        ));
        bgGlow.setMouseTransparent(true);

        VBox logoEl   = createLogo();
        VBox loginCard = createCard();

        VBox mainLayout = new VBox(32, logoEl, loginCard);
        mainLayout.setAlignment(Pos.CENTER);

        root.getChildren().addAll(bgCanvas, bgGlow, mainLayout);

        Scene scene = new Scene(root, 900, 820);
        scene.setFill(Color.web(DEEP_SPACE));
        stage.setTitle("Aqalnama AI — Secure Access");
        stage.setScene(scene);
        stage.show();

        applyEntryAnimation(mainLayout);
    }

    // =========================================================================
    //  LOGO
    // =========================================================================
    private VBox createLogo() {
        VBox container = new VBox(4);
        container.setAlignment(Pos.CENTER);

        HBox lineRow = new HBox(8);
        lineRow.setAlignment(Pos.CENTER);
        Line l1 = thinLine(80, true);
        Line l2 = thinLine(80, false);
        Circle dot = new Circle(3, Color.web(CYAN_GLOW));
        dot.setEffect(new Glow(1.0));
        lineRow.getChildren().addAll(l1, dot, l2);

        Text logo = new Text("AQALNAMA");
        logo.setFont(Font.font("System", FontWeight.BLACK, 58));
        logo.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLUE_BRIGHT)),
            new Stop(0.5, Color.WHITE),
            new Stop(1, Color.web(CYAN_GLOW))
        ));
        logo.setEffect(new Glow(0.35));

        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
            "-fx-background-color: rgba(59,130,246,0.15);" +
            "-fx-border-color: rgba(59,130,246,0.4);" +
            "-fx-border-width: 1; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-padding: 4 14;"
        );
        Circle statusDot = new Circle(4, Color.web("#22C55E"));
        statusDot.setEffect(new Glow(1.0));
        Text badgeText = new Text("NEURAL ENCYCLOPEDIA  •  v1.0  •  ONLINE");
        badgeText.setFont(Font.font("System", FontWeight.BOLD, 10));
        badgeText.setFill(Color.web(BLUE_BRIGHT));
        badge.getChildren().addAll(statusDot, badgeText);

        HBox stats = new HBox(24);
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(4, 0, 0, 0));
        stats.getChildren().addAll(
            statChip("∞", "Articles"), statSep(),
            statChip("AI", "Authored"), statSep(),
            statChip("24/7", "Updated"), statSep(),
            statChip("3", "Roles")
        );

        container.getChildren().addAll(lineRow, logo, badge, stats);

        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(4), container);
        floatAnim.setByY(-10); floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        floatAnim.play();

        return container;
    }

    private Line thinLine(double w, boolean reversed) {
        Line l = new Line(0, 0, w, 0);
        l.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(CYAN_GLOW, reversed ? 0.6 : 0.0)),
            new Stop(1, Color.web(CYAN_GLOW, reversed ? 0.0 : 0.6))
        ));
        l.setStrokeWidth(1);
        return l;
    }

    private VBox statChip(String val, String lbl) {
        VBox v = new VBox(0);
        v.setAlignment(Pos.CENTER);
        Text valT = new Text(val);
        valT.setFont(Font.font("System", FontWeight.BOLD, 13));
        valT.setFill(Color.web(BLUE_BRIGHT));
        Text lblT = new Text(lbl);
        lblT.setFont(Font.font("System", 9));
        lblT.setFill(Color.web("#6B7280"));
        v.getChildren().addAll(valT, lblT);
        return v;
    }

    private Text statSep() {
        Text t = new Text("•");
        t.setFill(Color.web("#1F2937"));
        t.setFont(Font.font(14));
        return t;
    }

    // =========================================================================
    //  CARD
    // =========================================================================
    private VBox createCard() {
        VBox card = new VBox(22);
        card.setMaxWidth(420);
        card.setPadding(new Insets(40, 44, 36, 44));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: rgba(10,16,28,0.88);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(59,130,246,0.25);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 18;"
        );
        card.setEffect(new DropShadow(BlurType.GAUSSIAN,
            Color.rgb(59, 130, 246, 0.3), 50, 0.1, 0, 8));

        // Header
        VBox header = new VBox(3);
        Text cardTitle = new Text("Secure Access Portal");
        cardTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        cardTitle.setFill(Color.WHITE);
        Text cardSub = new Text("Authenticate to access the knowledge network");
        cardSub.setFont(Font.font("System", 13));
        cardSub.setFill(Color.web("#6B7280"));
        header.getChildren().addAll(cardTitle, cardSub);

        Line divider = new Line(0, 0, 332, 0);
        divider.setStroke(Color.web(BLUE_CORE, 0.2));

        // Fields
        emailField = new TextField();
        styledField(emailField, "✉   Email address");
        passField = new PasswordField();
        styledField(passField, "⬡   Security cipher");

        // Forgot
        HBox forgotRow = new HBox();
        forgotRow.setAlignment(Pos.CENTER_RIGHT);
        Label forgot = new Label("Forgot password?");
        forgot.setFont(Font.font("System", 12));
        forgot.setTextFill(Color.web(BLUE_BRIGHT));
        forgot.setCursor(javafx.scene.Cursor.HAND);
        forgot.setOnMouseEntered(e -> forgot.setTextFill(Color.web(CYAN_GLOW)));
        forgot.setOnMouseExited(e  -> forgot.setTextFill(Color.web(BLUE_BRIGHT)));
        forgotRow.getChildren().add(forgot);

        // Error
        errorLabel = new Label();
        errorLabel.setFont(Font.font("System", 12));
        errorLabel.setTextFill(Color.web(ERROR_COL));
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(332);
        errorLabel.setStyle(
            "-fx-background-color: rgba(239,68,68,0.1);" +
            "-fx-border-color: rgba(239,68,68,0.4);" +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 10 14;"
        );

        // Spinner
        spinner = new ProgressIndicator(-1);
        spinner.setMaxSize(24, 24);
        spinner.setStyle("-fx-progress-color: " + BLUE_BRIGHT + ";");
        spinner.setVisible(false);

        // Button
        loginBtn = new Button("INITIALIZE SESSION");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(48);
        loginBtn.setFont(Font.font("System", FontWeight.BOLD, 13));
        loginBtn.setStyle(btnStyle(false));
        loginBtn.setCursor(javafx.scene.Cursor.HAND);
        loginBtn.setOnMouseEntered(e -> { loginBtn.setStyle(btnStyle(true));  loginBtn.setEffect(new Glow(0.4)); });
        loginBtn.setOnMouseExited(e  -> { loginBtn.setStyle(btnStyle(false)); loginBtn.setEffect(null); });
        loginBtn.setOnMousePressed(e  -> { loginBtn.setScaleX(0.97); loginBtn.setScaleY(0.97); });
        loginBtn.setOnMouseReleased(e -> { loginBtn.setScaleX(1.0);  loginBtn.setScaleY(1.0);  });

        // ── Clean wiring — no casting ─────────────────────────────────────────
        loginBtn.setOnAction(e -> handleLogin(emailField.getText().trim(), passField.getText()));
        passField.setOnAction(e -> handleLogin(emailField.getText().trim(), passField.getText()));
        // ─────────────────────────────────────────────────────────────────────

        HBox btnRow = new HBox(12, spinner, loginBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(loginBtn, Priority.ALWAYS);

        // Join row
        HBox joinRow = new HBox(6);
        joinRow.setAlignment(Pos.CENTER);
        joinRow.setPadding(new Insets(4, 0, 0, 0));
        Label joinTxt = new Label("New to Aqalnama?");
        joinTxt.setFont(Font.font("System", 13));
        joinTxt.setTextFill(Color.web("#6B7280"));
        Label joinLink = new Label("Request access →");
        joinLink.setFont(Font.font("System", FontWeight.BOLD, 13));
        joinLink.setTextFill(Color.web(BLUE_BRIGHT));
        joinLink.setCursor(javafx.scene.Cursor.HAND);
        joinLink.setOnMouseEntered(e -> joinLink.setTextFill(Color.web(CYAN_GLOW)));
        joinLink.setOnMouseExited(e  -> joinLink.setTextFill(Color.web(BLUE_BRIGHT)));
        joinRow.getChildren().addAll(joinTxt, joinLink);

        // Role chips
        HBox roles = new HBox(8);
        roles.setAlignment(Pos.CENTER);
        roles.setPadding(new Insets(6, 0, 0, 0));
        roles.getChildren().addAll(
            roleChip("⬡  ADMIN",  "#F59E0B"),
            roleChip("◎  READER", BLUE_BRIGHT),
            roleChip("⬢  BOT",    "#A78BFA")
        );

        card.getChildren().addAll(
            header, divider,
            labeledField("EMAIL ADDRESS", emailField),
            labeledField("PASSWORD", passField),
            forgotRow, errorLabel, btnRow, joinRow, roles
        );

        return card;
    }

    private VBox labeledField(String labelText, Control field) {
        VBox group = new VBox(6);
        Text lbl = new Text(labelText);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 10));
        lbl.setFill(Color.web("#4B5563"));
        group.getChildren().addAll(lbl, field);
        return group;
    }

    private void styledField(TextInputControl f, String prompt) {
        f.setPromptText(prompt);
        f.setPrefHeight(46);
        f.setMaxWidth(Double.MAX_VALUE);
        String base =
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #374151;" +
            "-fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-padding: 0 15; -fx-font-size: 14px;";
        String focused =
            "-fx-background-color: rgba(59,130,246,0.07);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #374151;" +
            "-fx-background-radius: 10; -fx-border-color: " + BLUE_CORE + ";" +
            "-fx-border-radius: 10; -fx-border-width: 1.5;" +
            "-fx-padding: 0 15; -fx-font-size: 14px;";
        f.setStyle(base);
        f.focusedProperty().addListener((o, ov, nv) -> f.setStyle(nv ? focused : base));
    }

    private String btnStyle(boolean hover) {
        return hover
            ? "-fx-background-color: linear-gradient(to right," + BLUE_DEEP + "," + CYAN_GLOW + ");" +
              "-fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;"
            : "-fx-background-color: linear-gradient(to right," + BLUE_DEEP + "," + BLUE_CORE + ");" +
              "-fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;";
    }

    private HBox roleChip(String label, String color) {
        HBox chip = new HBox();
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(3, 10, 3, 10));
        chip.setStyle(
            "-fx-background-color: " + color + "22;" +
            "-fx-border-color: " + color + "66;" +
            "-fx-border-width: 1; -fx-border-radius: 20; -fx-background-radius: 20;"
        );
        Text t = new Text(label);
        t.setFont(Font.font("System", FontWeight.BOLD, 9));
        t.setFill(Color.web(color));
        chip.getChildren().add(t);
        return chip;
    }

    // =========================================================================
    //  BACKGROUND
    // =========================================================================
    private void createParticles(Pane canvas, double w, double h) {
        for (int i = 0; i < 28; i++) {
            double r = 1 + Math.random() * 2.5;
            Circle c = new Circle(r);
            c.setFill(i % 2 == 0
                ? Color.web(BLUE_CORE, 0.12 + Math.random() * 0.12)
                : Color.web(CYAN_GLOW, 0.08 + Math.random() * 0.1));
            c.setCenterX(Math.random() * w);
            c.setCenterY(Math.random() * h);
            c.setEffect(new Glow(0.5));
            canvas.getChildren().add(c);

            TranslateTransition tt = new TranslateTransition(
                Duration.seconds(14 + Math.random() * 20), c);
            tt.setByX(Math.random() * 160 - 80);
            tt.setByY(Math.random() * 160 - 80);
            tt.setAutoReverse(true);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
    }

    private void createGridLines(Pane canvas, double w, double h) {
        for (int x = 0; x < w; x += 60) {
            Line l = new Line(x, 0, x, h);
            l.setStroke(Color.web(BLUE_CORE, 0.04));
            canvas.getChildren().add(l);
        }
        for (int y = 0; y < h; y += 60) {
            Line l = new Line(0, y, w, y);
            l.setStroke(Color.web(BLUE_CORE, 0.04));
            canvas.getChildren().add(l);
        }
    }

    private void applyEntryAnimation(Node node) {
        node.setOpacity(0);
        node.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), node);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), node);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    // =========================================================================
    //  FIREBASE AUTH — clean,
    // =========================================================================
    private void handleLogin(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email address and password.");
            return;
        }
        setLoading(true);
        errorLabel.setVisible(false);

        Thread t = new Thread(() -> {
            FirebaseAuthService.AuthResult auth = authService.signIn(email, password);
            if (!auth.success) {
                Platform.runLater(() -> { showError(auth.errorMessage); setLoading(false); });
                return;
            }
            User user = firestoreService.getUserProfile(auth.uid, auth.idToken);
            if (user == null) {
                Platform.runLater(() -> {
                    showError("Account found but profile missing. Contact admin.");
                    setLoading(false);
                });
                return;
            }
            if (!user.getStatus().equals("ACTIVE")) {
                Platform.runLater(() -> {
                    showError("Account is " + user.getStatus().toLowerCase() + ". Contact admin.");
                    setLoading(false);
                });
                return;
            }
            Platform.runLater(() -> { setLoading(false); playSuccess(user); });
        });
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), errorLabel);
        shake.setFromX(0); shake.setToX(8);
        shake.setAutoReverse(true); shake.setCycleCount(6);
        shake.play();
    }

    private void setLoading(boolean on) {
        loginBtn.setDisable(on);
        spinner.setVisible(on);
        loginBtn.setText(on ? "AUTHENTICATING..." : "INITIALIZE SESSION");
    }

    private void playSuccess(User user) {
        StackPane root = (StackPane) primaryStage.getScene().getRoot();
        Rectangle flash = new Rectangle(
            primaryStage.getScene().getWidth(),
            primaryStage.getScene().getHeight(),
            Color.web(BLUE_CORE, 0.0)
        );
        root.getChildren().add(flash);
        FadeTransition ft = new FadeTransition(Duration.millis(280), flash);
        ft.setToValue(0.55);
        ft.setAutoReverse(true); ft.setCycleCount(2);
        ft.setOnFinished(e -> redirectByRole(user));
        ft.play();
    }

    private void redirectByRole(User user) {
    switch (user.getRole().toUpperCase().trim()) {
    case "ADMIN" -> {
        AdminDashboard ad = new AdminDashboard();
        ad.show(primaryStage, user);
    }
    case "BOT" -> showPlaceholder(user, "Bot Interface", "#A78BFA");
    default -> {
        ReaderDashboard rd = new ReaderDashboard();
        rd.show(primaryStage, user);
    }
}
}

    private void showPlaceholder(User user, String name, String color) {
        StackPane p = new StackPane();
        p.setStyle("-fx-background-color: " + DEEP_SPACE + ";");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        Text title = new Text("AQALNAMA");
        title.setFont(Font.font("System", FontWeight.BLACK, 42));
        title.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLUE_BRIGHT)),
            new Stop(1, Color.web(CYAN_GLOW))
        ));
        title.setEffect(new Glow(0.4));
        Text welcome = new Text("✓  Welcome, " + user.getUsername());
        welcome.setFont(Font.font("System", FontWeight.BOLD, 18));
        welcome.setFill(Color.WHITE);
        Text next = new Text(name + " — coming soon");
        next.setFont(Font.font("System", 14));
        next.setFill(Color.web(color));
        box.getChildren().addAll(title, welcome, next);
        p.getChildren().add(box);
        FadeTransition fi = new FadeTransition(Duration.millis(400), p);
        fi.setFromValue(0); fi.setToValue(1);
        primaryStage.setScene(new Scene(p, 900, 820));
        fi.play();
    }

    public static void main(String[] args) { launch(args); }
}