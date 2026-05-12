import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class AdminDashboard {
 
    // palette color combinitions
    private static final String BG     = "#060A12";
    private static final String CYAN   = "#06B6D4";
    private static final String BLUE   = "#3B82F6";
    private static final String BLT    = "#60A5FA";
    private static final String PURP   = "#A78BFA";
    private static final String GREEN  = "#22C55E";
    private static final String AMBER  = "#F59E0B";
    private static final String RED    = "#F87171";
    private static final String DIM    = "#374151";
    private static final String MID    = "#6B7280";
    private static final String LIGHT  = "#9CA3AF";
    private static final String BRIGHT = "#F3F4F6";
    private static final String[] NC   = {CYAN, AMBER, BLT, PURP};

    // services by app
    private final BotService     botService     = new BotService();
    private final ArticleService articleService = new ArticleService();
    private final BotEngine      botEngine      = new BotEngine();

    // current active interface
    private User   user;
    private Stage  stage;
    private int    activeSec = 0;

    // Live data
    private final List<BotPersona> bots     = new ArrayList<>();
    private final List<Article>    articles = new ArrayList<>();

    // UI refs
    private VBox botsListPane;
    private VBox articlesPane;
    private Label draftCountLabel, publishedCountLabel, botCountLabel;

    private final List<Rectangle> nBgs  = new ArrayList<>();
    private final List<Rectangle> nBars = new ArrayList<>();
    private final List<Text>      nMts  = new ArrayList<>();

    private ScrollPane botsSection, articlesSection, profileSection;

    // =========================================================================
    //  ENTRY
    // =========================================================================
    public void show(Stage stage, User user) {
        this.stage = stage;
        this.user  = user;

        // Wire bot engine callback → refresh articles panel live
        botEngine.setOnArticleWritten((bot, article) -> {
            articles.add(0, article);
            refreshArticlesPane();
            refreshStats();
            showToast(" New article by " + bot.getName() + ": " + article.getTitle());
        });

        // Load data in background
        new Thread(() -> {
            List<BotPersona> loadedBots     = botService.getAllBots();
            List<Article>    loadedArticles = articleService.getAllArticles();
            Platform.runLater(() -> {
                bots.addAll(loadedBots);
                articles.addAll(loadedArticles);
                // Auto-start ACTIVE bots
                for (BotPersona b : bots)
                    if ("ACTIVE".equals(b.getStatus())) botEngine.startBot(b);
                refreshBotsPane();
                refreshArticlesPane();
                refreshStats();
            });
        }).start();

        // Build UI
        Canvas bgGrid = new Canvas(1280, 820);
        drawGrid(bgGrid);

        BorderPane main = new BorderPane();
        main.setStyle("-fx-background-color: transparent;");
        main.setLeft(buildSidebar());

        botsSection     = buildBotsSection();
        articlesSection = buildArticlesSection();
        profileSection  = buildProfileSection();
        articlesSection.setVisible(false);
        profileSection.setVisible(false);

        StackPane center = new StackPane(botsSection, articlesSection, profileSection);
        center.setStyle("-fx-background-color: transparent;");
        main.setCenter(center);

        StackPane root = new StackPane(bgGrid, main);
        root.setStyle("-fx-background-color: " + BG + ";");

        Scene scene = new Scene(root, 1280, 820);
        scene.setFill(Color.web(BG));

        // Stop bot engine on window close
        stage.setOnCloseRequest(e -> botEngine.stopAll());

        stage.setTitle("Aqalnama — Admin Dashboard");
        stage.setScene(scene);
        stage.show();

        main.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(700), main);
        ft.setToValue(1.0); ft.play();
    }

    // =========================================================================
    //  SIDEBAR
    // =========================================================================
    private VBox buildSidebar() {
        VBox sb = new VBox(0);
        sb.setPrefWidth(232); sb.setMinWidth(232);
        sb.setStyle(
            "-fx-background-color: rgba(3,6,13,0.97);" +
            "-fx-border-color: rgba(245,158,11,0.15);" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Logo
        VBox logoBlock = new VBox(5);
        logoBlock.setPadding(new Insets(26,20,18,20));
        Text logoT = new Text("AQALNAMA");
        logoT.setFont(Font.font("System", FontWeight.BLACK, 22));
        logoT.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(AMBER)), new Stop(1, Color.web(BLT))));
        logoT.setEffect(new Glow(0.4));
        Text roleT = new Text("ADMIN  /  CONTROL CENTER");
        roleT.setFont(Font.font("System", FontWeight.BOLD, 8));
        roleT.setFill(Color.web(DIM));

        // Live stats row
        HBox statsRow = new HBox(0);
        statsRow.setPadding(new Insets(10,0,0,0));
        botCountLabel       = statPill("0", "Bots",     GREEN);
        draftCountLabel     = statPill("0", "Drafts",   AMBER);
        publishedCountLabel = statPill("0", "Live",     CYAN);
        statsRow.getChildren().addAll(
            wrapPill(botCountLabel, "Bots", GREEN),
            wrapPill(draftCountLabel, "Drafts", AMBER),
            wrapPill(publishedCountLabel, "Live", CYAN)
        );
        logoBlock.getChildren().addAll(logoT, roleT, statsRow);

        // Separator
        Line sep = new Line(0,0,192,0); sep.setStroke(Color.web(AMBER, 0.10));
        HBox sepBox = new HBox(sep); sepBox.setPadding(new Insets(0,20,0,20));

        // Nav
        Text navHdr = new Text("CONTROL PANELS");
        navHdr.setFont(Font.font("System", FontWeight.BOLD, 8)); navHdr.setFill(Color.web(MID));
        HBox navHdrBox = new HBox(navHdr); navHdrBox.setPadding(new Insets(14,20,8,20));

        String[][] navDef = {
            {"⬢", "Bot Manager",      "Spawn & control bots"},
            {"◉", "Article Review",   "Authenticate & publish"},
            {"◈", "Admin Profile",    "Your account"},
        };
        VBox navBox = new VBox(3); navBox.setPadding(new Insets(0,10,0,10));
        for (int i = 0; i < navDef.length; i++) {
            final int idx = i;
            StackPane ni = buildNavItem(idx, navDef[i], i == 0);
            ni.setOnMouseClicked(e -> switchTo(idx));
            navBox.getChildren().add(ni);
        }

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        // User card
        HBox userCard = buildUserCard();

        sb.getChildren().addAll(logoBlock, sepBox, navHdrBox, navBox, spacer, userCard);
        return sb;
    }

    private Label statPill(String val, String lbl, String color) {
        Label l = new Label(val);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(color));
        return l;
    }

    private VBox wrapPill(Label valLabel, String lbl, String color) {
        VBox v = new VBox(1); v.setAlignment(Pos.CENTER); v.setPrefWidth(77);
        Text lblT = new Text(lbl); lblT.setFont(Font.font("System", 8)); lblT.setFill(Color.web(DIM));
        v.getChildren().addAll(valLabel, lblT);
        return v;
    }

    private StackPane buildNavItem(int idx, String[] def, boolean active) {
        StackPane sp = new StackPane();
        sp.setPrefWidth(212); sp.setMaxWidth(212); sp.setMinHeight(50);
        sp.setCursor(javafx.scene.Cursor.HAND);

        Rectangle bg = new Rectangle(212,50);
        bg.setArcWidth(10); bg.setArcHeight(10);
        bg.setFill(active ? Color.web(NC[idx],0.10) : Color.TRANSPARENT);
        nBgs.add(bg);

        Rectangle bar = new Rectangle(3,28);
        bar.setArcWidth(3); bar.setArcHeight(3);
        bar.setFill(active ? Color.web(NC[idx]) : Color.TRANSPARENT);
        if (active) bar.setEffect(new Glow(1.0));
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);
        nBars.add(bar);

        HBox content = new HBox(11); content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0,12,0,12));

        StackPane icSp = new StackPane();
        Circle ic = new Circle(16);
        ic.setFill(active ? Color.web(NC[idx],0.17) : Color.web(NC[idx],0.05));
        ic.setStroke(active ? Color.web(NC[idx],0.65) : Color.web(NC[idx],0.12));
        ic.setStrokeWidth(1);
        Text it = new Text(def[0]); it.setFont(Font.font("System",FontWeight.BOLD,13));
        it.setFill(active ? Color.web(NC[idx]) : Color.web(MID));
        icSp.getChildren().addAll(ic, it);

        VBox lbls = new VBox(1);
        Text mt = new Text(def[1]); mt.setFont(Font.font("System",FontWeight.BOLD,12));
        mt.setFill(active ? Color.web(BRIGHT) : Color.web(LIGHT));
        nMts.add(mt);
        Text st = new Text(def[2]); st.setFont(Font.font("System",9)); st.setFill(Color.web(DIM));
        lbls.getChildren().addAll(mt, st);
        content.getChildren().addAll(icSp, lbls);
        sp.getChildren().addAll(bg, bar, content);

        sp.setOnMouseEntered(e -> { if (idx!=activeSec) bg.setFill(Color.web(NC[idx],0.05)); });
        sp.setOnMouseExited(e  -> { if (idx!=activeSec) bg.setFill(Color.TRANSPARENT); });
        return sp;
    }

    private void switchTo(int idx) {
        if (idx == activeSec) return;
        activeSec = idx;
        for (int i = 0; i < 3; i++) {
            boolean a = (i==idx);
            nBgs.get(i).setFill(a ? Color.web(NC[i],0.10) : Color.TRANSPARENT);
            nBars.get(i).setFill(a ? Color.web(NC[i]) : Color.TRANSPARENT);
            nBars.get(i).setEffect(a ? new Glow(1.0) : null);
            nMts.get(i).setFill(a ? Color.web(BRIGHT) : Color.web(LIGHT));
        }
        List<ScrollPane> secs = List.of(botsSection, articlesSection, profileSection);
        for (int i = 0; i < secs.size(); i++) {
            ScrollPane s = secs.get(i);
            if (i == idx) {
                s.setOpacity(0); s.setVisible(true); s.setTranslateX(16);
                FadeTransition ft = new FadeTransition(Duration.millis(340),s); ft.setToValue(1.0);
                TranslateTransition tt = new TranslateTransition(Duration.millis(340),s);
                tt.setToX(0); tt.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft,tt).play();
            } else { s.setVisible(false); s.setTranslateX(0); }
        }
    }

    private HBox buildUserCard() {
        HBox card = new HBox(10); card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14,16,16,16));
        card.setStyle("-fx-background-color: rgba(245,158,11,0.04);");
        Circle avC = new Circle(14);
        avC.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web(AMBER,0.8)), new Stop(1,Color.web(BLT,0.8))));
        String init = user.getUsername().isEmpty()?"A":user.getUsername().substring(0,1).toUpperCase();
        Text avT = new Text(init); avT.setFont(Font.font("System",FontWeight.BLACK,13)); avT.setFill(Color.WHITE);
        StackPane av = new StackPane(avC, avT);
        VBox info = new VBox(2);
        Text un = new Text(user.getUsername()); un.setFont(Font.font("System",FontWeight.BOLD,12)); un.setFill(Color.web(BRIGHT));
        Text role = new Text("ADMIN  •  ACTIVE"); role.setFont(Font.font("System",FontWeight.BOLD,8)); role.setFill(Color.web(AMBER));
        info.getChildren().addAll(un, role);
        card.getChildren().addAll(av, info);
        return card;
    }

    // =========================================================================
    //  BOTS SECTION
    // =========================================================================
    private ScrollPane buildBotsSection() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(40,40,40,40));
        content.setStyle("-fx-background-color: transparent;");

        // Header
        HBox hdr = new HBox(); hdr.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Text t1 = new Text("BOT MANAGER"); t1.setFont(Font.font("System",FontWeight.BLACK,32));
        t1.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web(AMBER)), new Stop(1,Color.web(BLT))));
        t1.setEffect(new Glow(0.3));
        Text t2 = new Text("Spawn, schedule and control AI writer bots");
        t2.setFont(Font.font("System",13)); t2.setFill(Color.web(MID));
        titleBox.getChildren().addAll(t1,t2);
        Region hdrSp = new Region(); HBox.setHgrow(hdrSp, Priority.ALWAYS);

        // Spawn bot button
        Button spawnBtn = buildPrimaryBtn("⊕  SPAWN NEW BOT", AMBER);
        spawnBtn.setOnAction(e -> showSpawnDialog());
        hdr.getChildren().addAll(titleBox, hdrSp, spawnBtn);

        // Bots list
        botsListPane = new VBox(12);
        content.getChildren().addAll(hdr, botsListPane);

        ScrollPane sp = new ScrollPane(content);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToWidth(true);
        return sp;
    }

    private void refreshBotsPane() {
        if (botsListPane == null) return;
        botsListPane.getChildren().clear();
        if (bots.isEmpty()) {
            Text empty = new Text("No bots yet. Spawn your first bot →");
            empty.setFont(Font.font("System",14)); empty.setFill(Color.web(MID));
            botsListPane.getChildren().add(empty);
            return;
        }
        for (BotPersona bot : bots) botsListPane.getChildren().add(buildBotCard(bot));
    }

    private HBox buildBotCard(BotPersona bot) {
        boolean running = botEngine.isRunning(bot.getId());
        String  accent  = "ACTIVE".equals(bot.getStatus()) ? GREEN : AMBER;

        HBox card = new HBox(18); card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18,22,18,22));
        card.setStyle(
            "-fx-background-color: rgba(6,12,26,0.92);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-width: 1; -fx-border-radius: 12;"
        );
        card.setEffect(new DropShadow(BlurType.GAUSSIAN,Color.web(accent,0.12),16,0.1,0,3));

        // Status dot (pulses if running)
        Circle dot = new Circle(6, Color.web(accent));
        dot.setEffect(new Glow(running ? 1.2 : 0.2));
        if (running) {
            ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.2), dot);
            pulse.setFromX(0.8); pulse.setToX(1.4);
            pulse.setFromY(0.8); pulse.setToY(1.4);
            pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE); pulse.play();
        }

        // Bot info
        VBox info = new VBox(5); HBox.setHgrow(info, Priority.ALWAYS);
        Text nameT = new Text(bot.getName());
        nameT.setFont(Font.font("System",FontWeight.BOLD,16)); nameT.setFill(Color.web(BRIGHT));
        HBox meta = new HBox(14); meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
            chip(bot.getTopic(), CYAN),
            chip(bot.getStyle(), PURP),
            chip("Every " + bot.getIntervalMins() + " min", AMBER),
            chip(bot.getArticleCount() + " articles", BLT)
        );
        if (bot.isFlagged()) {
            meta.getChildren().add(chip("FLAGGED", RED));
        }
        Text statusT = new Text(running ? "● RUNNING" : "◯ " + bot.getStatus());
        statusT.setFont(Font.font("System",FontWeight.BOLD,9));
        statusT.setFill(Color.web(bot.isFlagged() ? RED : running ? GREEN : MID));
        info.getChildren().addAll(nameT, meta, statusT);

        // Controls
        HBox controls = new HBox(8); controls.setAlignment(Pos.CENTER_RIGHT);
        if (running) {
            Button pauseBtn = buildSmallBtn(" PAUSE", AMBER);
            pauseBtn.setOnAction(e -> {
                botEngine.pauseBot(bot);
                bot.setStatus("PAUSED");
                refreshBotsPane();
            });
            controls.getChildren().add(pauseBtn);
        } else {
            Button resumeBtn = buildSmallBtn(" START", GREEN);
            resumeBtn.setOnAction(e -> {
                botEngine.resumeBot(bot);
                bot.setStatus("ACTIVE");
                refreshBotsPane();
            });
            controls.getChildren().add(resumeBtn);
        }

        Button flagBtn = buildSmallBtn(bot.isFlagged() ? "UNFLAG" : "FLAG", bot.isFlagged() ? GREEN : RED);
        flagBtn.setOnAction(e -> {
            if (bot.isFlagged()) {
                updateBotFlag(bot, false, "");
            } else {
                showFlagBotDialog(bot);
            }
        });

        Button removeBtn = buildSmallBtn("REMOVE", RED);
        removeBtn.setOnAction(e -> confirmRemoveBot(bot));
        controls.getChildren().addAll(flagBtn, removeBtn);

        card.getChildren().addAll(dot, info, controls);
        return card;
    }

    private void showFlagBotDialog(BotPersona bot) {
        TextInputDialog dialog = new TextInputDialog("Reader reports require admin review.");
        dialog.initOwner(stage);
        dialog.setTitle("Flag Author");
        dialog.setHeaderText("Flag " + bot.getName());
        dialog.setContentText("Reason shown to readers:");
        dialog.showAndWait().ifPresent(reason -> {
            String cleanReason = reason == null || reason.trim().isEmpty()
                ? "Flagged by admin for review."
                : reason.trim();
            updateBotFlag(bot, true, cleanReason);
        });
    }

    private void updateBotFlag(BotPersona bot, boolean flagged, String reason) {
        new Thread(() -> {
            boolean ok = botService.updateBotFlag(bot.getId(), flagged, reason);
            Platform.runLater(() -> {
                if (ok) {
                    bot.setFlagged(flagged);
                    bot.setFlagReason(flagged ? reason : "");
                    refreshBotsPane();
                    showToast((flagged ? "Flagged " : "Unflagged ") + bot.getName());
                } else {
                    showToast("Could not update author flag.");
                }
            });
        }).start();
    }

    private void confirmRemoveBot(BotPersona bot) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Remove Author Bot");
        alert.setHeaderText("Remove " + bot.getName() + "?");
        long ownedArticles = articles.stream().filter(a -> bot.getId().equals(a.getBotId())).count();
        alert.setContentText("This removes the bot author and "
            + ownedArticles + " article" + (ownedArticles == 1 ? "" : "s") + " written by this author.");
        alert.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.OK) return;
            new Thread(() -> {
                botEngine.pauseBot(bot);
                boolean articlesOk = articleService.deleteArticlesByBot(bot.getId());
                boolean botOk = articlesOk && botService.deleteBot(bot.getId());
                Platform.runLater(() -> {
                    if (botOk) {
                        bots.removeIf(b -> bot.getId().equals(b.getId()));
                        articles.removeIf(a -> bot.getId().equals(a.getBotId()));
                        refreshBotsPane();
                        refreshArticlesPane();
                        refreshStats();
                        showToast("Removed author bot and articles: " + bot.getName());
                    } else if (articlesOk) {
                        articles.removeIf(a -> bot.getId().equals(a.getBotId()));
                        refreshArticlesPane();
                        refreshStats();
                        showToast("Removed author's articles, but could not remove author bot.");
                    } else {
                        showToast("Could not remove author's articles.");
                    }
                });
            }).start();
        });
    }

    // instance for Spawning the  Bot Dialog 
    private void showSpawnDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);

        VBox root = new VBox(18);
        root.setPadding(new Insets(36));
        root.setStyle("-fx-background-color: #0A1020;");
        root.setPrefWidth(420);

        Text title = new Text("SPAWN NEW BOT");
        title.setFont(Font.font("System",FontWeight.BLACK,20));
        title.setFill(Color.web(AMBER));

        TextField nameField  = dialogField("Bot name  (e.g. NeuralBot α)");
        TextField topicField = dialogField("Topic  (e.g. Physics, History, Biology)");

        ComboBox<String> styleBox = new ComboBox<>();
        styleBox.getItems().addAll("academic","technical","educational","analytical","journalistic");
        styleBox.setValue("academic");
        styleBox.setMaxWidth(Double.MAX_VALUE);
        styleBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );

        ComboBox<String> intervalBox = new ComboBox<>();
        intervalBox.getItems().addAll("1 min","2 min","5 min","10 min","30 min","60 min");
        intervalBox.setValue("5 min");
        intervalBox.setMaxWidth(Double.MAX_VALUE);
        intervalBox.setStyle(styleBox.getStyle());

        Label errLabel = new Label();
        errLabel.setTextFill(Color.web(RED)); errLabel.setVisible(false);

        Button spawnBtn = buildPrimaryBtn("⊕  REGISTER & SPAWN", AMBER);
        spawnBtn.setOnAction(e -> {
            String name  = nameField.getText().trim();
            String topic = topicField.getText().trim();
            if (name.isEmpty() || topic.isEmpty()) {
                errLabel.setText("Please fill in all fields."); errLabel.setVisible(true); return;
            }
            int mins = Integer.parseInt(intervalBox.getValue().replace(" min",""));
            spawnBtn.setText("REGISTERING...");
            spawnBtn.setDisable(true);

            new Thread(() -> {
                BotPersona bot = botService.registerBot(name, topic, styleBox.getValue(), mins);
                Platform.runLater(() -> {
                    if (bot == null) {
                        errLabel.setText("Registration failed. Check console."); errLabel.setVisible(true);
                        spawnBtn.setText("⊕  REGISTER & SPAWN"); spawnBtn.setDisable(false);
                    } else {
                        bots.add(bot);
                        botEngine.startBot(bot);
                        refreshBotsPane(); refreshStats();
                        dialog.close();
                        showToast("✓ Bot spawned: " + bot.getName());
                    }
                });
            }).start();
        });

        root.getChildren().addAll(
            title,
            fieldGroup("BOT NAME", nameField),
            fieldGroup("TOPIC AREA", topicField),
            fieldGroup("WRITING STYLE", styleBox),
            fieldGroup("WRITE EVERY", intervalBox),
            errLabel, spawnBtn
        );

        dialog.setScene(new Scene(root));
        dialog.setTitle("Spawn Bot — Aqalnama");
        dialog.show();
    }

    // =========================================================================
    //  ARTICLES SECTION
    // =========================================================================
    private ScrollPane buildArticlesSection() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: transparent;");

        HBox hdr = new HBox(); hdr.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Text t1 = new Text("ARTICLE REVIEW"); t1.setFont(Font.font("System",FontWeight.BLACK,32));
        t1.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web(BLT)), new Stop(1,Color.web(CYAN))));
        t1.setEffect(new Glow(0.3));
        Text t2 = new Text("Authenticate and publish AI-written articles");
        t2.setFont(Font.font("System",13)); t2.setFill(Color.web(MID));
        titleBox.getChildren().addAll(t1,t2);
        Region sp2 = new Region(); HBox.setHgrow(sp2,Priority.ALWAYS);
        Button refreshBtn = buildSmallBtn("↻ REFRESH", CYAN);
        refreshBtn.setOnAction(e -> {
            new Thread(() -> {
                List<Article> fresh = articleService.getAllArticles();
                Platform.runLater(() -> { articles.clear(); articles.addAll(fresh);
                    refreshArticlesPane(); refreshStats(); });
            }).start();
        });
        hdr.getChildren().addAll(titleBox, sp2, refreshBtn);

        // Tab bar
        HBox tabs = new HBox(8);
        String[] tabNames = {"DRAFTS", "AUTHENTICATED", "PUBLISHED"};
        String[] tabColors = {AMBER, BLT, GREEN};
        for (int i = 0; i < tabNames.length; i++) {
            final String s = i==0?"DRAFT":tabNames[i].equals("AUTHENTICATED")?"AUTHENTICATED":"PUBLISHED";
            Label tab = new Label(tabNames[i]);
            tab.setFont(Font.font("System",FontWeight.BOLD,10));
            tab.setTextFill(Color.web(tabColors[i]));
            tab.setPadding(new Insets(6,14,6,14)); tab.setCursor(javafx.scene.Cursor.HAND);
            tab.setStyle(
                "-fx-background-color:"+tabColors[i]+"1A;-fx-border-color:"+tabColors[i]+"50;"+
                "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;"
            );
            tabs.getChildren().add(tab);
        }

        articlesPane = new VBox(12);
        content.getChildren().addAll(hdr, tabs, articlesPane);

        ScrollPane sp = new ScrollPane(content);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToWidth(true);
        return sp;
    }

    private void refreshArticlesPane() {
        if (articlesPane == null) return;
        articlesPane.getChildren().clear();
        if (articles.isEmpty()) {
            Text empty = new Text("No articles yet. Bots will write here automatically.");
            empty.setFont(Font.font("System",14)); empty.setFill(Color.web(MID));
            articlesPane.getChildren().add(empty);
            return;
        }
        for (Article a : articles) articlesPane.getChildren().add(buildArticleRow(a));
    }

    private HBox buildArticleRow(Article article) {
        String statusColor = switch (article.getStatus()) {
            case "DRAFT"         -> AMBER;
            case "AUTHENTICATED" -> BLT;
            case "PUBLISHED"     -> GREEN;
            default              -> MID;
        };

        HBox row = new HBox(16); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16,20,16,20));
        row.setStyle(
            "-fx-background-color: rgba(6,12,26,0.90);" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(255,255,255,0.06);" +
            "-fx-border-width: 1; -fx-border-radius: 10;"
        );

        Rectangle accent = new Rectangle(4,50);
        accent.setArcWidth(4); accent.setArcHeight(4);
        accent.setFill(Color.web(statusColor)); accent.setEffect(new Glow(0.5));

        VBox info = new VBox(6); HBox.setHgrow(info, Priority.ALWAYS);
        Text titleT = new Text(article.getTitle());
        titleT.setFont(Font.font("System",FontWeight.BOLD,14)); titleT.setFill(Color.web(BRIGHT));
        HBox meta = new HBox(10); meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
            chipText(article.getBotName(), PURP),
            chipText("•", DIM),
            chipText(article.getTopic(), CYAN),
            chipText("•", DIM),
            chipText(article.getStatus(), statusColor),
            chipText("•", DIM),
            chipText(article.getRatingCount() == 0
                ? "No ratings"
                : String.format("%.1f ★ (%d)", article.getAverageRating(), article.getRatingCount()), AMBER),
            chipText("•", DIM),
            chipText(article.getReportCount() + " reports", article.getReportCount() > 0 ? RED : MID)
        );
        // Show "USER REQUESTED" badge if this article was requested by a reader
        String reqBy = article.getRequestedBy();
        if (reqBy != null && !reqBy.isBlank()) {
            meta.getChildren().add(chipText("•", DIM));
            Label reqBadge = new Label("⊕ REQUESTED BY: " + reqBy.toUpperCase());
            reqBadge.setFont(Font.font("System", FontWeight.BOLD, 8));
            reqBadge.setTextFill(Color.web(CYAN));
            reqBadge.setPadding(new Insets(2, 8, 2, 8));
            reqBadge.setStyle("-fx-background-color:" + CYAN + "1A;-fx-border-color:" + CYAN + "55;"
                + "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;");
            meta.getChildren().add(reqBadge);
        }
        info.getChildren().addAll(titleT, meta);

        // Action buttons based on status
        HBox actions = new HBox(8); actions.setAlignment(Pos.CENTER_RIGHT);
        Button flagAuthorBtn = buildSmallBtn("FLAG AUTHOR", RED);
        flagAuthorBtn.setOnAction(e -> {
            BotPersona author = findBotById(article.getBotId());
            if (author == null) {
                showToast("Author bot is not loaded or was removed.");
            } else {
                showFlagBotDialog(author);
            }
        });
        Button removeArticleBtn = buildSmallBtn("REMOVE", RED);
        removeArticleBtn.setOnAction(e -> confirmRemoveArticle(article));
        if ("DRAFT".equals(article.getStatus())) {
            Button previewBtn    = buildSmallBtn("👁 VIEW",         BLT);
            Button authenticateBtn = buildSmallBtn("✓ AUTHENTICATE", GREEN);
            previewBtn.setOnAction(e -> showArticlePreview(article));
            authenticateBtn.setOnAction(e -> {
                new Thread(() -> {
                    boolean ok = articleService.updateStatus(article.getId(), "AUTHENTICATED");
                    Platform.runLater(() -> {
                        if (ok) { article.setStatus("AUTHENTICATED"); refreshArticlesPane(); refreshStats(); }
                    });
                }).start();
            });
            actions.getChildren().addAll(previewBtn, authenticateBtn, flagAuthorBtn, removeArticleBtn);
        } else if ("AUTHENTICATED".equals(article.getStatus())) {
            Button previewBtn  = buildSmallBtn("👁 VIEW",    BLT);
            Button publishBtn  = buildSmallBtn("🚀 PUBLISH", CYAN);
            previewBtn.setOnAction(e -> showArticlePreview(article));
            publishBtn.setOnAction(e -> {
                new Thread(() -> {
                    boolean ok = articleService.updateStatus(article.getId(), "PUBLISHED");
                    Platform.runLater(() -> {
                        if (ok) { article.setStatus("PUBLISHED"); refreshArticlesPane(); refreshStats();
                            showToast("🚀 Published: " + article.getTitle()); }
                    });
                }).start();
            });
            actions.getChildren().addAll(previewBtn, publishBtn, flagAuthorBtn, removeArticleBtn);
        } else {
            Button viewBtn = buildSmallBtn("👁 VIEW", BLT);
            viewBtn.setOnAction(e -> showArticlePreview(article));
            actions.getChildren().addAll(viewBtn, flagAuthorBtn, removeArticleBtn);
        }

        row.getChildren().addAll(accent, info, actions);
        return row;
    }

    private void confirmRemoveArticle(Article article) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Remove Article");
        alert.setHeaderText("Remove " + article.getTitle() + "?");
        alert.setContentText("This permanently removes the article from Firestore.");
        alert.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.OK) return;
            BotPersona author = findBotById(article.getBotId());
            int newCount = author == null ? 0 : Math.max(0, author.getArticleCount() - 1);
            new Thread(() -> {
                boolean ok = articleService.deleteArticle(article.getId());
                if (ok && author != null) botService.updateBotArticleCount(author.getId(), newCount);
                Platform.runLater(() -> {
                    if (ok) {
                        articles.removeIf(a -> article.getId().equals(a.getId()));
                        if (author != null) author.setArticleCount(newCount);
                        refreshArticlesPane();
                        refreshBotsPane();
                        refreshStats();
                        showToast("Removed article: " + article.getTitle());
                    } else {
                        showToast("Could not remove article.");
                    }
                });
            }).start();
        });
    }

    private BotPersona findBotById(String botId) {
        if (botId == null) return null;
        for (BotPersona bot : bots) {
            if (botId.equals(bot.getId())) return bot;
        }
        return null;
    }

    // Article Preview Dialog 
    private void showArticlePreview(Article article) {
        Stage dialog = new Stage();
        dialog.initOwner(stage);

        VBox root = new VBox(16); root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color: #080E1A;");
        root.setPrefWidth(680);

        Text statusT = new Text(article.getStatus());
        statusT.setFont(Font.font("System",FontWeight.BOLD,9));
        statusT.setFill(Color.web("PUBLISHED".equals(article.getStatus()) ? GREEN
            : "AUTHENTICATED".equals(article.getStatus()) ? BLT : AMBER));

        Text titleT = new Text(article.getTitle());
        titleT.setFont(Font.font("System",FontWeight.BOLD,22)); titleT.setFill(Color.web(BRIGHT));
        titleT.setWrappingWidth(620);

        HBox meta = new HBox(12);
        meta.getChildren().addAll(chipText(article.getBotName(),PURP), chipText(article.getTopic(),CYAN));

        Line divider = new Line(0,0,620,0); divider.setStroke(Color.web(BLUE,0.2));

        TextArea contentArea = new TextArea(article.getContent());
        contentArea.setWrapText(true); contentArea.setEditable(false);
        contentArea.setPrefRowCount(18);
        contentArea.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #9CA3AF;" +
            "-fx-font-size: 13px; -fx-border-color: transparent;"
        );

        root.getChildren().addAll(statusT, titleT, meta, divider, contentArea);

        ScrollPane sp = new ScrollPane(root);
        sp.setStyle("-fx-background: #080E1A;");
        dialog.setScene(new Scene(sp, 700, 580));
        dialog.setTitle(article.getTitle());
        dialog.show();
    }

    // =========================================================================
    //  PROFILE SECTION — real username edit
    // =========================================================================
    private ScrollPane buildProfileSection() {
        VBox content = new VBox(24); content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: transparent;");

        Text heading = new Text("ADMIN PROFILE");
        heading.setFont(Font.font("System",FontWeight.BLACK,32));
        heading.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web(AMBER)), new Stop(1,Color.web(PURP))));

        VBox card = new VBox(20); card.setPadding(new Insets(32,36,32,36));
        card.setMaxWidth(520);
        card.setStyle(
            "-fx-background-color: rgba(6,10,22,0.92);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(245,158,11,0.22);" +
            "-fx-border-width: 1; -fx-border-radius: 16;"
        );

        Text subT = new Text("Change Username");
        subT.setFont(Font.font("System",FontWeight.BOLD,14)); subT.setFill(Color.web(BRIGHT));
        Text noteT = new Text("Only your display name can be changed. Email and role are fixed.");
        noteT.setFont(Font.font("System",11)); noteT.setFill(Color.web(MID));

        TextField nameField = new TextField(user.getUsername());
        nameField.setPrefHeight(44);
        nameField.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-text-fill: white; -fx-background-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8;" +
            "-fx-font-size: 14px; -fx-padding: 0 14;"
        );

        Label statusLabel = new Label(); statusLabel.setVisible(false);
        statusLabel.setFont(Font.font("System",12));

        Button saveBtn = buildPrimaryBtn("SAVE USERNAME", AMBER);
        saveBtn.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) return;
            saveBtn.setDisable(true); saveBtn.setText("SAVING...");
            new Thread(() -> {
                boolean ok = updateUsernameInFirestore(newName);
                Platform.runLater(() -> {
                    saveBtn.setDisable(false); saveBtn.setText("SAVE USERNAME");
                    if (ok) {
                        user = new User(user.getUid(), newName, user.getRole(), user.getStatus());
                        statusLabel.setText("✓ Username updated successfully");
                        statusLabel.setTextFill(Color.web(GREEN));
                    } else {
                        statusLabel.setText("✗ Failed to update. Try again.");
                        statusLabel.setTextFill(Color.web(RED));
                    }
                    statusLabel.setVisible(true);
                });
            }).start();
        });

        card.getChildren().addAll(subT, noteT, fieldGroup("NEW USERNAME", nameField), statusLabel, saveBtn);
        content.getChildren().addAll(heading, card);

        ScrollPane sp = new ScrollPane(content);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToWidth(true);
        return sp;
    }

        // updation of the user name
    private boolean updateUsernameInFirestore(String newName) {
        try {
            String url  = "https://firestore.googleapis.com/v1/projects/aqalnama-9d5f2"
                + "/databases/(default)/documents/users/" + user.getUid()
                + "?updateMask.fieldPaths=username";
            String body = "{\"fields\":{\"username\":{\"stringValue\":\"" + newName + "\"}}}";
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type","application/json")
                .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
            return client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
                .statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    // =========================================================================
    //  STATS REFRESH
    // =========================================================================
    private void refreshStats() {
        long drafts    = articles.stream().filter(a -> "DRAFT".equals(a.getStatus())).count();
        long published = articles.stream().filter(a -> "PUBLISHED".equals(a.getStatus())).count();
        if (botCountLabel != null)       botCountLabel.setText(String.valueOf(bots.size()));
        if (draftCountLabel != null)     draftCountLabel.setText(String.valueOf(drafts));
        if (publishedCountLabel != null) publishedCountLabel.setText(String.valueOf(published));
    }

    // =========================================================================
    //  TOAST NOTIFICATION
    // =========================================================================
    private void showToast(String msg) {
        Label toast = new Label(msg);
        toast.setFont(Font.font("System",FontWeight.BOLD,12));
        toast.setTextFill(Color.web(BRIGHT));
        toast.setPadding(new Insets(12,20,12,20));
        toast.setStyle(
            "-fx-background-color: rgba(6,182,212,0.18);" +
            "-fx-border-color: rgba(6,182,212,0.5);" +
            "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;"
        );
        StackPane root = (StackPane) stage.getScene().getRoot();
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0,24,24,0));
        root.getChildren().add(toast);
        FadeTransition ft = new FadeTransition(Duration.millis(300), toast); ft.setToValue(1.0);
        ft.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(400), toast);
            out.setDelay(Duration.seconds(3)); out.setToValue(0);
            out.setOnFinished(ev -> root.getChildren().remove(toast));
            out.play();
        });
        ft.play();
    }

    // =========================================================================
    //  UI HELPERS
    // =========================================================================
    private Button buildPrimaryBtn(String text, String color) {
        Button b = new Button(text);
        b.setFont(Font.font("System",FontWeight.BOLD,12));
        b.setPrefHeight(40); b.setPadding(new Insets(0,20,0,20));
        b.setCursor(javafx.scene.Cursor.HAND);
        String base = "-fx-background-color:"+color+";-fx-text-fill: #060A12;-fx-background-radius:8;-fx-font-weight:bold;";
        String hov  = "-fx-background-color:"+color+"CC;-fx-text-fill:#060A12;-fx-background-radius:8;-fx-font-weight:bold;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hov));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Button buildSmallBtn(String text, String color) {
        Button b = new Button(text);
        b.setFont(Font.font("System",FontWeight.BOLD,10));
        b.setPrefHeight(32); b.setPadding(new Insets(0,14,0,14));
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle(
            "-fx-background-color:"+color+"1A;-fx-text-fill:"+color+";"+
            "-fx-border-color:"+color+"50;-fx-border-width:1;"+
            "-fx-border-radius:8;-fx-background-radius:8;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:"+color+"30;-fx-text-fill:"+color+";"+
            "-fx-border-color:"+color+"90;-fx-border-width:1;"+
            "-fx-border-radius:8;-fx-background-radius:8;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color:"+color+"1A;-fx-text-fill:"+color+";"+
            "-fx-border-color:"+color+"50;-fx-border-width:1;"+
            "-fx-border-radius:8;-fx-background-radius:8;"
        ));
        return b;
    }

    private Label chip(String text, String color) {
        Label l = new Label(text); l.setFont(Font.font("System",FontWeight.BOLD,9));
        l.setTextFill(Color.web(color)); l.setPadding(new Insets(2,9,2,9));
        l.setStyle("-fx-background-color:"+color+"1A;-fx-border-color:"+color+"40;"+
            "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;");
        return l;
    }

    private Text chipText(String t, String color) {
        Text tx = new Text(t); tx.setFont(Font.font("System",10)); tx.setFill(Color.web(color));
        return tx;
    }

    private TextField dialogField(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt);
        f.setPrefHeight(42); f.setMaxWidth(Double.MAX_VALUE);
        f.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #374151;" +
            "-fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-radius: 8; -fx-font-size: 13px; -fx-padding: 0 12;"
        );
        return f;
    }

    private VBox fieldGroup(String label, Control field) {
        VBox g = new VBox(5);
        Text l = new Text(label); l.setFont(Font.font("System",FontWeight.BOLD,9)); l.setFill(Color.web(MID));
        g.getChildren().addAll(l, field);
        return g;
    }

    private void drawGrid(Canvas c) {
        var gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web(BLUE, 0.03)); gc.setLineWidth(1);
        for (double x=0; x<1280; x+=60) gc.strokeLine(x,0,x,820);
        for (double y=0; y<820;  y+=60) gc.strokeLine(0,y,1280,y);
    }
}
