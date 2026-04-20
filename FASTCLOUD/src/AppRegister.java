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

public class AppRegister extends Application {

    // ── Palette (mirrors AppLogin exactly) ────────────────────────────────────
    private static final String DEEP_SPACE  = "#060A12";
    private static final String BLUE_CORE   = "#3B82F6";
    private static final String BLUE_BRIGHT = "#60A5FA";
    private static final String BLUE_DEEP   = "#1D4ED8";
    private static final String CYAN_GLOW   = "#06B6D4";
    private static final String ERROR_COL   = "#F87171";
    private static final String SUCCESS_COL = "#22C55E";

    // ── Firebase services ─────────────────────────────────────────────────────
    private final FirebaseAuthService authService      = new FirebaseAuthService();
    private final FirestoreService    firestoreService = new FirestoreService();

    // ── UI refs ───────────────────────────────────────────────────────────────
    private Button            registerBtn;
    private Label             statusLabel;
    private ProgressIndicator spinner;
    private TextField         usernameField;
    private TextField         emailField;
    private PasswordField     passField;
    private PasswordField     confirmPassField;
    private ComboBox<String>  roleCombo;

    // ── Called by AppLogin to reuse the same stage ────────────────────────────
    public void showOn(Stage stage) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + DEEP_SPACE + ";");

        Pane bgCanvas = new Pane();
        bgCanvas.setMouseTransparent(true);
        createParticles(bgCanvas, 900, 860);
        createGridLines(bgCanvas, 900, 860);

        Circle bgGlow = new Circle(300);
        bgGlow.setFill(new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(CYAN_GLOW, 0.10)),
            new Stop(1, Color.web(CYAN_GLOW, 0.0))
        ));
        bgGlow.setMouseTransparent(true);

        VBox logoEl  = createLogo();
        VBox regCard = createCard(stage);

        VBox mainLayout = new VBox(28, logoEl, regCard);
        mainLayout.setAlignment(Pos.CENTER);

        root.getChildren().addAll(bgCanvas, bgGlow, mainLayout);

        Scene scene = new Scene(root, 900, 860);
        scene.setFill(Color.web(DEEP_SPACE));
        stage.setTitle("Aqalnama AI — Create Account");
        stage.setScene(scene);
        stage.show();

        applyEntryAnimation(mainLayout);
    }

    @Override
    public void start(Stage stage) { showOn(stage); }

    // =========================================================================
    //  LOGO
    // =========================================================================
    private VBox createLogo() {
        VBox container = new VBox(4);
        container.setAlignment(Pos.CENTER);

        HBox lineRow = new HBox(8);
        lineRow.setAlignment(Pos.CENTER);
        Line l1 = thinLine(80); Line l2 = thinLine(80);
        Circle dot = new Circle(3, Color.web(CYAN_GLOW));
        dot.setEffect(new Glow(1.0));
        lineRow.getChildren().addAll(l1, dot, l2);

        Text logo = new Text("AQALNAMA");
        logo.setFont(Font.font("System", FontWeight.BLACK, 48));
        logo.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLUE_BRIGHT)),
            new Stop(0.5, Color.WHITE),
            new Stop(1, Color.web(CYAN_GLOW))
        ));
        logo.setEffect(new Glow(0.35));

        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
            "-fx-background-color: rgba(6,182,212,0.12);" +
            "-fx-border-color: rgba(6,182,212,0.35);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 4 14;"
        );
        Circle statusDot = new Circle(4, Color.web(CYAN_GLOW));
        statusDot.setEffect(new Glow(1.0));
        Text badgeText = new Text("CREATE YOUR ACCOUNT  •  JOIN THE NETWORK");
        badgeText.setFont(Font.font("System", FontWeight.BOLD, 10));
        badgeText.setFill(Color.web(CYAN_GLOW));
        badge.getChildren().addAll(statusDot, badgeText);

        container.getChildren().addAll(lineRow, logo, badge);

        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(4), container);
        floatAnim.setByY(-8); floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        floatAnim.play();

        return container;
    }

    private Line thinLine(double w) {
        Line l = new Line(0, 0, w, 0);
        l.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(CYAN_GLOW, 0.0)),
            new Stop(1, Color.web(CYAN_GLOW, 0.6))
        ));
        l.setStrokeWidth(1);
        return l;
    }

    // =========================================================================
    //  CARD
    // =========================================================================
    private VBox createCard(Stage stage) {
        VBox card = new VBox(20);
        card.setMaxWidth(440);
        card.setPadding(new Insets(36, 44, 32, 44));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: rgba(10,16,28,0.90);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(6,182,212,0.25),rgba(59,130,246,0.1);" +
            "-fx-border-width: 1, 1;" +
            "-fx-border-radius: 18, 20;" +
            "-fx-border-insets: 0, -2;"
        );
        card.setEffect(new DropShadow(BlurType.GAUSSIAN,
            Color.rgb(6, 182, 212, 0.25), 50, 0.1, 0, 8));

        VBox header = new VBox(3);
        Text cardTitle = new Text("Create Account");
        cardTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        cardTitle.setFill(Color.WHITE);
        Text cardSub = new Text("Fill in your details to join the knowledge network");
        cardSub.setFont(Font.font("System", 13));
        cardSub.setFill(Color.web("#6B7280"));
        header.getChildren().addAll(cardTitle, cardSub);

        Line divider = new Line(0, 0, 352, 0);
        divider.setStroke(Color.web(CYAN_GLOW, 0.2));

        usernameField    = new TextField();     styledField(usernameField,    "◈   Display name");
        emailField       = new TextField();     styledField(emailField,       "✉   Email address");
        passField        = new PasswordField(); styledField(passField,        "⬡   Password  (min 6 chars)");
        confirmPassField = new PasswordField(); styledField(confirmPassField, "⬡   Confirm password");

        // ADMIN role is assigned manually — only READER / BOT selectable
        roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("READER", "BOT");
        roleCombo.setValue("READER");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setPrefHeight(46);
        roleCombo.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-text-fill: white;" +
            "-fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-font-size: 13px;"
        );

        statusLabel = new Label();
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setVisible(false);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(352);

        spinner = new ProgressIndicator(-1);
        spinner.setMaxSize(24, 24);
        spinner.setStyle("-fx-progress-color: " + CYAN_GLOW + ";");
        spinner.setVisible(false);

        registerBtn = new Button("CREATE ACCOUNT");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(48);
        registerBtn.setFont(Font.font("System", FontWeight.BOLD, 13));
        registerBtn.setStyle(btnStyle(false));
        registerBtn.setCursor(javafx.scene.Cursor.HAND);
        registerBtn.setOnMouseEntered(e -> { registerBtn.setStyle(btnStyle(true));  registerBtn.setEffect(new Glow(0.4)); });
        registerBtn.setOnMouseExited(e  -> { registerBtn.setStyle(btnStyle(false)); registerBtn.setEffect(null); });
        registerBtn.setOnMousePressed(e  -> { registerBtn.setScaleX(0.97); registerBtn.setScaleY(0.97); });
        registerBtn.setOnMouseReleased(e -> { registerBtn.setScaleX(1.0);  registerBtn.setScaleY(1.0);  });
        registerBtn.setOnAction(e -> handleRegister(stage));
        confirmPassField.setOnAction(e -> registerBtn.fire());

        HBox btnRow = new HBox(12, spinner, registerBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(registerBtn, Priority.ALWAYS);

        HBox loginRow = new HBox(6);
        loginRow.setAlignment(Pos.CENTER);
        loginRow.setPadding(new Insets(4, 0, 0, 0));
        Label loginTxt  = new Label("Already have an account?");
        loginTxt.setFont(Font.font("System", 13));
        loginTxt.setTextFill(Color.web("#6B7280"));
        Label loginLink = new Label("← Back to login");
        loginLink.setFont(Font.font("System", FontWeight.BOLD, 13));
        loginLink.setTextFill(Color.web(BLUE_BRIGHT));
        loginLink.setCursor(javafx.scene.Cursor.HAND);
        loginLink.setOnMouseEntered(e -> loginLink.setTextFill(Color.web(CYAN_GLOW)));
        loginLink.setOnMouseExited(e  -> loginLink.setTextFill(Color.web(BLUE_BRIGHT)));
        loginLink.setOnMouseClicked(e -> navigateToLogin(stage));
        loginRow.getChildren().addAll(loginTxt, loginLink);

        card.getChildren().addAll(
            header, divider,
            labeledField("DISPLAY NAME",    usernameField),
            labeledField("EMAIL ADDRESS",   emailField),
            labeledField("PASSWORD",        passField),
            labeledField("CONFIRM PASSWORD", confirmPassField),
            labeledField("ACCOUNT ROLE",    roleCombo),
            statusLabel, btnRow, loginRow
        );

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(3), card);
        pulse.setFromX(1.000); pulse.setToX(1.002);
        pulse.setFromY(1.000); pulse.setToY(1.002);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        return card;
    }

    // =========================================================================
    //  REGISTER LOGIC
    // =========================================================================
    private void handleRegister(Stage stage) {
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String pass     = passField.getText();
        String confirm  = confirmPassField.getText();
        String role     = roleCombo.getValue();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            showStatus("All fields are required.", false); return;
        }
        if (username.length() < 3) {
            showStatus("Display name must be at least 3 characters.", false); return;
        }
        if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$")) {
            showStatus("Please enter a valid email address.", false); return;
        }
        if (pass.length() < 6) {
            showStatus("Password must be at least 6 characters.", false); return;
        }
        if (!pass.equals(confirm)) {
            showStatus("Passwords do not match.", false); shake(confirmPassField); return;
        }

        setLoading(true);
        statusLabel.setVisible(false);

        Thread t = new Thread(() -> {
            // Step 1 — Firebase Auth: create account
            FirebaseAuthService.AuthResult auth = authService.signUp(email, pass);
            if (!auth.success) {
                Platform.runLater(() -> { showStatus(auth.errorMessage, false); setLoading(false); });
                return;
            }

            // Step 2 — Firestore: write profile
            // Matches existing signature: createUserProfile(uid, username, role, idToken)
            boolean saved = firestoreService.createUserProfile(auth.uid, username, role, auth.idToken);
            if (!saved) {
                Platform.runLater(() -> {
                    showStatus("Account created but profile save failed. Contact admin.", false);
                    setLoading(false);
                });
                return;
            }

            // Step 3 — Done
            Platform.runLater(() -> { setLoading(false); playSuccessAndRedirect(stage); });
        });
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================
    private void navigateToLogin(Stage stage) {
        FadeTransition ft = new FadeTransition(Duration.millis(350), stage.getScene().getRoot());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            try { new AppLogin().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });
        ft.play();
    }

    private void playSuccessAndRedirect(Stage stage) {
        showStatus("✓  Account created! Redirecting to login...", true);

        Scene s = stage.getScene();
        StackPane root = (StackPane) s.getRoot();
        Rectangle flash = new Rectangle(s.getWidth(), s.getHeight(), Color.web(SUCCESS_COL, 0.0));
        root.getChildren().add(flash);

        FadeTransition ft = new FadeTransition(Duration.millis(250), flash);
        ft.setToValue(0.35);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.setOnFinished(e -> {
            PauseTransition wait = new PauseTransition(Duration.millis(900));
            wait.setOnFinished(ev -> navigateToLogin(stage));
            wait.play();
        });
        ft.play();
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================
    private void showStatus(String msg, boolean success) {
        statusLabel.setText(success ? msg : "⚠  " + msg);
        statusLabel.setTextFill(Color.web(success ? SUCCESS_COL : ERROR_COL));
        statusLabel.setStyle(
            "-fx-background-color: " + (success ? "rgba(34,197,94,0.10)" : "rgba(239,68,68,0.10)") + ";" +
            "-fx-border-color: "     + (success ? "rgba(34,197,94,0.40)" : "rgba(239,68,68,0.40)") + ";" +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;"
        );
        statusLabel.setVisible(true);
        if (!success) shake(statusLabel);
    }

    private void shake(Node node) {
        TranslateTransition s = new TranslateTransition(Duration.millis(55), node);
        s.setFromX(0); s.setToX(8); s.setAutoReverse(true); s.setCycleCount(6); s.play();
    }

    private void setLoading(boolean on) {
        registerBtn.setDisable(on);
        spinner.setVisible(on);
        registerBtn.setText(on ? "CREATING ACCOUNT..." : "CREATE ACCOUNT");
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
            "-fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: white;" +
            "-fx-prompt-text-fill: #374151; -fx-background-radius: 10;" +
            "-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 10;" +
            "-fx-border-width: 1; -fx-padding: 0 15 0 15; -fx-font-size: 14px;";
        String focused =
            "-fx-background-color: rgba(6,182,212,0.07); -fx-text-fill: white;" +
            "-fx-prompt-text-fill: #374151; -fx-background-radius: 10;" +
            "-fx-border-color: " + CYAN_GLOW + "; -fx-border-radius: 10;" +
            "-fx-border-width: 1.5; -fx-padding: 0 15 0 15; -fx-font-size: 14px;";
        f.setStyle(base);
        f.focusedProperty().addListener((o, ov, nv) -> f.setStyle(nv ? focused : base));
    }

    private String btnStyle(boolean hover) {
        return hover
            ? "-fx-background-color: linear-gradient(to right, #0891B2, " + CYAN_GLOW + ");" +
              "-fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;"
            : "-fx-background-color: linear-gradient(to right, " + BLUE_DEEP + ", " + BLUE_CORE + ");" +
              "-fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;";
    }

    // =========================================================================
    //  BACKGROUND
    // =========================================================================
    private void createParticles(Pane canvas, double w, double h) {
        for (int i = 0; i < 35; i++) {
            double r = 1 + Math.random() * 3;
            Circle c = new Circle(r);
            c.setFill(Math.random() > 0.5
                ? Color.web(BLUE_CORE, 0.15 + Math.random() * 0.15)
                : Color.web(CYAN_GLOW, 0.1  + Math.random() * 0.1));
            c.setCenterX(Math.random() * w);
            c.setCenterY(Math.random() * h);
            c.setEffect(new Glow(0.6));
            canvas.getChildren().add(c);
            TranslateTransition tt = new TranslateTransition(Duration.seconds(12 + Math.random() * 18), c);
            tt.setByX(Math.random() * 200 - 100);
            tt.setByY(Math.random() * 200 - 100);
            tt.setAutoReverse(true); tt.setCycleCount(Animation.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
        }
    }

    private void createGridLines(Pane canvas, double w, double h) {
        for (int x = 0; x < w; x += 60) { Line l = new Line(x,0,x,h); l.setStroke(Color.web(BLUE_CORE,0.04)); canvas.getChildren().add(l); }
        for (int y = 0; y < h; y += 60) { Line l = new Line(0,y,w,y); l.setStroke(Color.web(BLUE_CORE,0.04)); canvas.getChildren().add(l); }
    }

    private void applyEntryAnimation(Node node) {
        node.setOpacity(0); node.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), node); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), node);
        tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    public static void main(String[] args) { launch(args); }
}
