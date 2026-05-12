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

public class ReaderDashboard {

 // pallete of the color combinition 
    private static final String BG     = "#060A12";
    private static final String CYAN   = "#06B6D4";
    private static final String TEAL   = "#2DD4BF";
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
    private static final String[] NC   = {CYAN, BLT, PURP};

    //  Article data 
    private final ArticleService        articleService        = new ArticleService();
    private final BotService            botService            = new BotService();
    private final ArticleRequestService articleRequestService = new ArticleRequestService();
    private final BotEngine             botEngine             = new BotEngine();
    private final List<Article>         publishedArticles     = new ArrayList<>();
    private final List<BotPersona>      allBots               = new ArrayList<>();
    private final Map<String, BotPersona> authorsById = new HashMap<>();
    private final List<Map<String, String>> myRequests = new ArrayList<>();

    //Current state
    private User       user;
    private ScrollPane homeSection, searchSection, profileSection;
    private int        activeSec = 0;
    private GridPane   articleGrid;
    private VBox       searchResults;
    private VBox       requestsBox;
    private Label      resultCountLabel;
    private TextField  searchField;
    private Text       feedSubtitle;
    private Text       publishedStatText;

    private final List<Rectangle> nBgs  = new ArrayList<>();
    private final List<Rectangle> nBars = new ArrayList<>();
    private final List<Circle>    nIcs  = new ArrayList<>();
    private final List<Text>      nIts  = new ArrayList<>();
    private final List<Text>      nMts  = new ArrayList<>();

    //--------- Particle state  
    private static final int P = 25;
    private final double[] pX = new double[P], pY = new double[P];
    private final double[] pVX= new double[P], pVY= new double[P];
    private final double[] pR = new double[P], pA = new double[P];

    //  Globe state 
    private double   globeRotY = 0, globeOrb = 0;
    private double[] sinSeg, cosSeg;
    private static final int SEGS = 34;

    //  Pre-cached colors for faster access
    private static final int     CS = 32;
    private static final Color[] C_CYAN = new Color[CS];
    private static final Color[] C_TEAL = new Color[CS];
    private static final Color   C_TRAIL;
    static {
        for (int i = 0; i < CS; i++) {
            double a = i / (double)(CS - 1);
            C_CYAN[i] = Color.web(CYAN, a);
            C_TEAL[i] = Color.web(TEAL, a);
        }
        C_TRAIL = Color.web(BG, 0.28);
    }
    private static Color cC(double a) { return C_CYAN[Math.max(0,Math.min(CS-1,(int)(a*(CS-1))))]; }
    private static Color cT(double a) { return C_TEAL[Math.max(0,Math.min(CS-1,(int)(a*(CS-1))))]; }

    //  Master timer (throttled to ~20fps for canvas) 
    private AnimationTimer masterTimer;
    private long           lastNs = 0;
    private static final long FRAME_NS = 50_000_000L; // 20fps

    private Canvas globeCanvas;
    private Canvas partCanvas;

    // =========================================================================
    //  ENTRY
    // =========================================================================
    public void show(Stage stage, User user) {
        this.user = user;
        initTrigTables();
        initParticles(1280, 820);

        Canvas bgGrid = new Canvas(1280, 820);
        drawStaticGrid(bgGrid.getGraphicsContext2D(), 1280, 820);
        bgGrid.setMouseTransparent(true);

        BorderPane main = new BorderPane();
        main.setStyle("-fx-background-color: transparent;");
        main.setLeft(buildSidebar(stage));

        homeSection    = buildHomeSection();
        searchSection  = buildSearchSection();
        profileSection = buildProfileSection();
        searchSection.setVisible(false);
        profileSection.setVisible(false);

        StackPane center = new StackPane(homeSection, searchSection, profileSection);
        center.setStyle("-fx-background-color: transparent;");
        main.setCenter(center);

        partCanvas = new Canvas(1280, 820);
        partCanvas.setMouseTransparent(true);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.getChildren().addAll(bgGrid, main, partCanvas);

        Scene scene = new Scene(root, 1280, 820);
        scene.setFill(Color.web(BG));
        stage.setTitle("Aqalnama — Knowledge Hub");
        stage.setScene(scene);
        stage.show();

        startMasterTimer();

        main.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(800), main);
        ft.setToValue(1.0);
        ft.play();

        loadPublishedArticles();
    }

    private void loadPublishedArticles() {
        new Thread(() -> {
            List<Article> fresh = articleService.getPublishedArticles();
            List<BotPersona> authors = botService.getAllBots();
            List<Map<String, String>> userReqs = articleRequestService.getRequestsByUser(user.getUsername());
            Platform.runLater(() -> {
                publishedArticles.clear();
                publishedArticles.addAll(fresh);
                authorsById.clear();
                allBots.clear();
                allBots.addAll(authors);
                for (BotPersona author : authors) authorsById.put(author.getId(), author);
                myRequests.clear();
                myRequests.addAll(userReqs);
                refreshArticleGrid();
                refreshSearchResults();
                refreshRequestsBox();
                updateArticleStats();
            });
        }).start();
    }

    // =========================================================================
    //  MASTER TIMER — drives globe + particles in one loop
    // =========================================================================
    private void startMasterTimer() {
        masterTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now - lastNs < FRAME_NS) return;
                lastNs = now;
                globeRotY += 0.010;
                globeOrb  += 0.019;
                if (globeCanvas != null) drawGlobe();
                drawParticles();
            }
        };
        masterTimer.start();
    }

    // =========================================================================
    //  SIDEBAR  
    // =========================================================================
    private VBox buildSidebar(Stage stage) {
        VBox sb = new VBox(0);
        sb.setPrefWidth(232);
        sb.setMinWidth(232);
        sb.setStyle(
            "-fx-background-color: rgba(3,6,13,0.97);" +
            "-fx-border-color: rgba(6,182,212,0.13);" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Logo
        VBox logoBlock = new VBox(5); 
        logoBlock.setPadding(new Insets(26, 20, 18, 20));
        HBox logoLine = new HBox(7);
        logoLine.setAlignment(Pos.CENTER_LEFT);
        Line ll = new Line(0, 0, 40, 0);
        ll.setStroke(Color.web(CYAN, 0.85)); ll.setStrokeWidth(1.5);
        Circle ld = new Circle(3.5, Color.web(CYAN));
        ld.setEffect(new Glow(1.4));
        ScaleTransition ldPulse = new ScaleTransition(Duration.seconds(1.6), ld);
        ldPulse.setFromX(0.7); ldPulse.setToX(1.5);
        ldPulse.setFromY(0.7); ldPulse.setToY(1.5);
        ldPulse.setAutoReverse(true); ldPulse.setCycleCount(Animation.INDEFINITE);
        ldPulse.play();
        logoLine.getChildren().addAll(ll, ld);
        Text logoT = new Text("AQALNAMA");
        logoT.setFont(Font.font("System", FontWeight.BLACK, 22));
        logoT.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLT)), new Stop(1, Color.web(CYAN))));
        logoT.setEffect(new Glow(0.4));
        Text roleT = new Text("READER  /  KNOWLEDGE HUB");
        roleT.setFont(Font.font("System", FontWeight.BOLD, 8));
        roleT.setFill(Color.web(DIM));
        logoBlock.getChildren().addAll(logoLine, logoT, roleT);

        Line sep1 = new Line(0,0,192,0);
        sep1.setStroke(Color.web(CYAN, 0.08));
        HBox sep1Box = new HBox(sep1);
        sep1Box.setPadding(new Insets(0,20,0,20));

        Text navHdr = new Text("N A V I G A T I O N");
        navHdr.setFont(Font.font("System", FontWeight.BOLD, 8));
        navHdr.setFill(Color.web(MID));
        HBox navHdrBox = new HBox(navHdr);
        navHdrBox.setPadding(new Insets(16,20,10,20));

        String[][] navDef = {
            {"⬡", "Knowledge Hub", "Browse Articles"},
            {"◎", "Neural Search",  "Find Topics"},
            {"◈", "Identity Core",  "Your Profile"},
        };
        VBox navBox = new VBox(3);
        navBox.setPadding(new Insets(0,10,0,10));
        for (int i = 0; i < navDef.length; i++) {
            final int idx = i;
            StackPane ni = buildNavItem(idx, navDef[i], i == 0);
            ni.setOnMouseClicked(e -> switchTo(idx));
            navBox.getChildren().add(ni);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Decorative hex grid replaces heavy mini-globe
        Canvas hexDeco = buildHexDeco();
        HBox hexBox = new HBox(hexDeco);
        hexBox.setAlignment(Pos.CENTER);
        hexBox.setPadding(new Insets(8, 0, 8, 0));

        sb.getChildren().addAll(
            logoBlock, sep1Box, navHdrBox, navBox,
            spacer, hexBox, buildStatsStrip(), buildUserCard()
        );
        return sb;
    }

    /** Lightweight static decorative hex pattern — zero runtime cost */
    private Canvas buildHexDeco() {
        Canvas c = new Canvas(192, 120);
        GraphicsContext gc = c.getGraphicsContext2D();
        double[] cx = {48, 96, 144, 72, 120, 96};
        double[] cy = {38, 24, 38,  82, 82,  60};
        double[] r2 = {22, 28, 22,  18, 18,  14};
        String[] cols = {CYAN, BLT, CYAN, PURP, TEAL, BLT};
        double[] alphas = {0.18, 0.25, 0.14, 0.18, 0.16, 0.30};
        for (int i = 0; i < cx.length; i++) {
            gc.setStroke(Color.web(cols[i], alphas[i]));
            gc.setLineWidth(1);
            gc.strokePolygon(hexX(cx[i], r2[i]), hexY(cy[i], r2[i]), 6);
        }
        // Center glow dot
        gc.setFill(Color.web(CYAN, 0.5));
        gc.fillOval(93, 57, 6, 6);
        gc.setEffect(new Glow(1.0)); // note: effect on GC doesn't work, but the dot is fine
        // Connecting lines
        gc.setStroke(Color.web(CYAN, 0.09)); gc.setLineWidth(0.8);
        gc.strokeLine(48, 38, 96, 24); gc.strokeLine(96, 24, 144, 38);
        gc.strokeLine(48, 38, 72, 82); gc.strokeLine(144, 38, 120, 82);
        gc.strokeLine(72, 82, 120, 82); gc.strokeLine(96, 60, 96, 24);
        return c;
    }
    private double[] hexX(double cx, double r) {
        double[] x = new double[6];
        for (int i = 0; i < 6; i++) x[i] = cx + r * Math.cos(Math.PI/6 + i * Math.PI/3);
        return x;
    }
    private double[] hexY(double cy, double r) {
        double[] y = new double[6];
        for (int i = 0; i < 6; i++) y[i] = cy + r * Math.sin(Math.PI/6 + i * Math.PI/3);
        return y;
    }

    private StackPane buildNavItem(int idx, String[] def, boolean active) {
        StackPane sp = new StackPane();
        sp.setPrefWidth(212); sp.setMaxWidth(212); sp.setMinHeight(50);
        sp.setCursor(javafx.scene.Cursor.HAND);

        Rectangle bg = new Rectangle(212, 50);
        bg.setArcWidth(10); bg.setArcHeight(10);
        bg.setFill(active ? Color.web(NC[idx], 0.10) : Color.TRANSPARENT);
        nBgs.add(bg);

        Rectangle bar = new Rectangle(3, 28);
        bar.setArcWidth(3); bar.setArcHeight(3);
        bar.setFill(active ? Color.web(NC[idx]) : Color.TRANSPARENT);
        if (active) bar.setEffect(new Glow(1.0));
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);
        nBars.add(bar);

        HBox content = new HBox(11);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0,12,0,12));

        StackPane icSp = new StackPane();
        Circle ic = new Circle(16);
        ic.setFill(active ? Color.web(NC[idx], 0.17) : Color.web(NC[idx], 0.05));
        ic.setStroke(active ? Color.web(NC[idx], 0.65) : Color.web(NC[idx], 0.12));
        ic.setStrokeWidth(1);
        nIcs.add(ic);
        Text it = new Text(def[0]);
        it.setFont(Font.font("System", FontWeight.BOLD, 13));
        it.setFill(active ? Color.web(NC[idx]) : Color.web(MID));
        nIts.add(it);
        icSp.getChildren().addAll(ic, it);

        VBox lbls = new VBox(1);
        Text mt = new Text(def[1]);
        mt.setFont(Font.font("System", FontWeight.BOLD, 12));
        mt.setFill(active ? Color.web(BRIGHT) : Color.web(LIGHT));
        nMts.add(mt);
        Text st = new Text(def[2]);
        st.setFont(Font.font("System", 9));
        st.setFill(Color.web(DIM));
        lbls.getChildren().addAll(mt, st);
        content.getChildren().addAll(icSp, lbls);
        sp.getChildren().addAll(bg, bar, content);

        sp.setOnMouseEntered(e -> { if (idx != activeSec) bg.setFill(Color.web(NC[idx], 0.05)); });
        sp.setOnMouseExited(e  -> { if (idx != activeSec) bg.setFill(Color.TRANSPARENT); });
        return sp;
    }

    private void switchTo(int idx) {
        if (idx == activeSec) return;
        activeSec = idx;
        for (int i = 0; i < 3; i++) {
            boolean a = (i == idx);
            nBgs.get(i).setFill(a ? Color.web(NC[i], 0.10) : Color.TRANSPARENT);
            nBars.get(i).setFill(a ? Color.web(NC[i]) : Color.TRANSPARENT);
            nBars.get(i).setEffect(a ? new Glow(1.0) : null);
            nIcs.get(i).setFill(a ? Color.web(NC[i], 0.17) : Color.web(NC[i], 0.05));
            nIcs.get(i).setStroke(a ? Color.web(NC[i], 0.65) : Color.web(NC[i], 0.12));
            nIts.get(i).setFill(a ? Color.web(NC[i]) : Color.web(MID));
            nMts.get(i).setFill(a ? Color.web(BRIGHT) : Color.web(LIGHT));
        }
        List<ScrollPane> secs = List.of(homeSection, searchSection, profileSection);
        for (int i = 0; i < secs.size(); i++) {
            ScrollPane s = secs.get(i);
            if (i == idx) {
                s.setOpacity(0); s.setVisible(true); s.setTranslateX(18);
                FadeTransition ft = new FadeTransition(Duration.millis(360), s); ft.setToValue(1.0);
                TranslateTransition tt = new TranslateTransition(Duration.millis(360), s);
                tt.setToX(0); tt.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft, tt).play();
            } else { s.setVisible(false); s.setTranslateX(0); }
        }
    }

    private HBox buildStatsStrip() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(9,0,9,0));
        row.setStyle(
            "-fx-background-color: rgba(6,182,212,0.04);" +
            "-fx-border-color: rgba(6,182,212,0.09); -fx-border-width: 1 0;"
        );
        String[][] sd = {{"∞","Articles"},{"AI","Powered"},{"24/7","Live"}};
        for (int i = 0; i < sd.length; i++) {
            VBox c = new VBox(0); c.setAlignment(Pos.CENTER); c.setPrefWidth(77);
            Text v = new Text(sd[i][0]); v.setFont(Font.font("System",FontWeight.BOLD,12)); v.setFill(Color.web(CYAN));
            Text l = new Text(sd[i][1]); l.setFont(Font.font("System",8)); l.setFill(Color.web(DIM));
            c.getChildren().addAll(v, l); row.getChildren().add(c);
            if (i < 2) { Line div = new Line(0,0,0,20); div.setStroke(Color.web(DIM,0.3)); row.getChildren().add(div); }
        }
        return row;
    }

    private HBox buildUserCard() {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14,16,16,16));
        card.setStyle("-fx-background-color: rgba(6,182,212,0.04);");

        StackPane av = new StackPane();
        Circle avRing = new Circle(20); avRing.setFill(Color.TRANSPARENT);
        avRing.setStroke(Color.web(CYAN,0.4)); avRing.setStrokeWidth(1);
        avRing.getStrokeDashArray().addAll(4.0, 3.0);
        RotateTransition avSpin = new RotateTransition(Duration.seconds(10), avRing);
        avSpin.setByAngle(360); avSpin.setCycleCount(Animation.INDEFINITE);
        avSpin.setInterpolator(Interpolator.LINEAR); avSpin.play();

        Circle avC = new Circle(14);
        avC.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLUE,0.8)), new Stop(1, Color.web(CYAN,0.8))));
        avC.setStroke(Color.web(CYAN,0.6)); avC.setStrokeWidth(1.5);

        String init = user.getUsername().isEmpty() ? "U" : user.getUsername().substring(0,1).toUpperCase();
        Text avT = new Text(init);
        avT.setFont(Font.font("System", FontWeight.BLACK, 14)); avT.setFill(Color.WHITE);
        av.getChildren().addAll(avRing, avC, avT);

        VBox info = new VBox(3);
        Text un = new Text(user.getUsername());
        un.setFont(Font.font("System", FontWeight.BOLD, 12)); un.setFill(Color.web(BRIGHT));
        HBox st = new HBox(5); st.setAlignment(Pos.CENTER_LEFT);
        Circle stDot = new Circle(3, Color.web(GREEN)); stDot.setEffect(new Glow(0.9));
        Text stT = new Text("ACTIVE  •  " + user.getRole());
        stT.setFont(Font.font("System", FontWeight.BOLD, 8)); stT.setFill(Color.web(GREEN));
        st.getChildren().addAll(stDot, stT);
        info.getChildren().addAll(un, st);
        card.getChildren().addAll(av, info);
        return card;
    }

    // =========================================================================
    //  HOME SECTION
    // =========================================================================
    private ScrollPane buildHomeSection() {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: transparent;");
        content.getChildren().add(buildHero());

        HBox feedHdr = new HBox();
        feedHdr.setPadding(new Insets(28,36,14,36));
        feedHdr.setAlignment(Pos.CENTER_LEFT);
        VBox feedTitle = new VBox(3);
        Text ft1 = new Text("KNOWLEDGE FEED");
        ft1.setFont(Font.font("System", FontWeight.BLACK, 17)); ft1.setFill(Color.web(BRIGHT));
        feedSubtitle = new Text("Loading published articles...");
        feedSubtitle.setFont(Font.font("System", 12)); feedSubtitle.setFill(Color.web(MID));
        feedTitle.getChildren().addAll(ft1, feedSubtitle);
        Region fhSp = new Region(); HBox.setHgrow(fhSp, Priority.ALWAYS);
        Button refreshBtn = new Button("REFRESH");
        refreshBtn.setFont(Font.font("System", FontWeight.BOLD, 10));
        refreshBtn.setPadding(new Insets(6,14,6,14));
        refreshBtn.setCursor(javafx.scene.Cursor.HAND);
        refreshBtn.setTextFill(Color.web(CYAN));
        refreshBtn.setStyle(
            "-fx-background-color:rgba(6,182,212,0.12);" +
            "-fx-border-color:rgba(6,182,212,0.42);" +
            "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;"
        );
        refreshBtn.setOnAction(e -> loadPublishedArticles());
        feedHdr.getChildren().addAll(feedTitle, fhSp, refreshBtn);
        content.getChildren().add(feedHdr);

        articleGrid = new GridPane();
        articleGrid.setHgap(20); articleGrid.setVgap(20);
        articleGrid.setPadding(new Insets(0,36,40,36));
        content.getChildren().add(articleGrid);
        refreshArticleGrid();

        // === YOUR REQUESTS SECTION ===
        HBox reqHdr = new HBox();
        reqHdr.setPadding(new Insets(10,36,10,36));
        reqHdr.setAlignment(Pos.CENTER_LEFT);
        VBox reqTitle = new VBox(3);
        Text rqt1 = new Text("YOUR ARTICLE REQUESTS");
        rqt1.setFont(Font.font("System", FontWeight.BLACK, 15)); rqt1.setFill(Color.web(BRIGHT));
        Text rqt2 = new Text("Track AI-generated articles you requested");
        rqt2.setFont(Font.font("System", 11)); rqt2.setFill(Color.web(MID));
        reqTitle.getChildren().addAll(rqt1, rqt2);
        reqHdr.getChildren().add(reqTitle);
        content.getChildren().add(reqHdr);

        requestsBox = new VBox(10);
        requestsBox.setPadding(new Insets(0,36,40,36));
        content.getChildren().add(requestsBox);
        refreshRequestsBox();

        ScrollPane sp = new ScrollPane(content);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToWidth(true);
        return sp;
    }

    private HBox buildHero() {
        HBox hero = new HBox(0);
        hero.setPrefHeight(300); hero.setMinHeight(300); hero.setMaxHeight(300);
        hero.setStyle(
            "-fx-background-color: rgba(4,8,18,0.75);" +
            "-fx-border-color: rgba(6,182,212,0.08); -fx-border-width: 0 0 1 0;"
        );

        // Globe canvas (single instance)
        globeCanvas = new Canvas(300, 300);

        VBox heroText = new VBox(0);
        heroText.setAlignment(Pos.CENTER_LEFT);
        heroText.setPadding(new Insets(36,44,36,16));
        HBox.setHgrow(heroText, Priority.ALWAYS);

        HBox chip = new HBox(7); chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(4,12,4,12));
        chip.setStyle(
            "-fx-background-color:rgba(34,197,94,0.1);-fx-border-color:rgba(34,197,94,0.35);" +
            "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;"
        );
        Circle sdot = new Circle(3.5, Color.web(GREEN)); sdot.setEffect(new Glow(0.9));
        Text stext = new Text("NEURAL NETWORK  •  ONLINE  •  " + user.getUsername().toUpperCase());
        stext.setFont(Font.font("System", FontWeight.BOLD, 8)); stext.setFill(Color.web(GREEN));
        chip.getChildren().addAll(sdot, stext);

        Text titleLine1 = new Text("KNOWLEDGE");
        titleLine1.setFont(Font.font("System", FontWeight.BLACK, 50));
        titleLine1.setFill(Color.web(BRIGHT));

        Text titleLine2 = new Text("NETWORK");
        titleLine2.setFont(Font.font("System", FontWeight.BLACK, 50));
        titleLine2.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(CYAN)), new Stop(1, Color.web(TEAL))));
        titleLine2.setEffect(new Glow(0.45));

        Text subtitle = new Text("AI-Authored  •  Fact-Verified  •  Neural Encyclopedia");
        subtitle.setFont(Font.font("System", 12)); subtitle.setFill(Color.web(MID));

        Line accentLine = new Line(0,0,0,0);
        accentLine.setStroke(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(CYAN,0.9)), new Stop(1, Color.web(CYAN,0.0))));
        accentLine.setStrokeWidth(2); accentLine.setEffect(new Glow(0.6));
        Timeline lineGrow = new Timeline(
            new KeyFrame(Duration.ZERO,           new KeyValue(accentLine.endXProperty(), 0.0)),
            new KeyFrame(Duration.millis(1100),   new KeyValue(accentLine.endXProperty(), 280.0, Interpolator.EASE_OUT))
        );
        lineGrow.setDelay(Duration.millis(500)); lineGrow.play();

        HBox heroStats = new HBox(32); heroStats.setAlignment(Pos.CENTER_LEFT);
        String[][] hsd = {{"0","Published\nArticles"},{"AI","Authored\nKnowledge"},{"3","Access\nRoles"}};
        for (int i = 0; i < hsd.length; i++) {
            String[] d = hsd[i];
            VBox sc = new VBox(2); sc.setAlignment(Pos.CENTER_LEFT);
            Text sv = new Text(d[0]); sv.setFont(Font.font("System",FontWeight.BLACK,32));
            sv.setFill(Color.web(CYAN)); sv.setEffect(new Glow(0.5));
            if (i == 0) publishedStatText = sv;
            Text sl = new Text(d[1]); sl.setFont(Font.font("System",10)); sl.setFill(Color.web(MID));
            sc.getChildren().addAll(sv, sl); heroStats.getChildren().add(sc);
        }

        // REQUEST ARTICLE CTA Button
        Button requestBtn = new Button("⊕  REQUEST ARTICLE");
        requestBtn.setFont(Font.font("System", FontWeight.BOLD, 11));
        requestBtn.setPrefHeight(36); requestBtn.setPadding(new Insets(0, 18, 0, 18));
        requestBtn.setCursor(javafx.scene.Cursor.HAND);
        String reqBase = "-fx-background-color:" + CYAN + ";-fx-text-fill:#060A12;-fx-background-radius:8;-fx-font-weight:bold;";
        String reqHov  = "-fx-background-color:" + CYAN + "CC;-fx-text-fill:#060A12;-fx-background-radius:8;-fx-font-weight:bold;";
        requestBtn.setStyle(reqBase);
        requestBtn.setOnMouseEntered(e -> requestBtn.setStyle(reqHov));
        requestBtn.setOnMouseExited(e  -> requestBtn.setStyle(reqBase));
        requestBtn.setOnAction(e -> showRequestArticleDialog());

        HBox heroActions = new HBox(12);
        heroActions.setAlignment(Pos.CENTER_LEFT);
        heroActions.getChildren().add(requestBtn);

        Region r1 = new Region(); r1.setPrefHeight(8);
        Region r2 = new Region(); r2.setPrefHeight(10);
        Region r3 = new Region(); r3.setPrefHeight(8);
        Region r4 = new Region(); r4.setPrefHeight(6);
        heroText.getChildren().addAll(chip, r1, titleLine1, titleLine2, subtitle, r2, accentLine, r3, heroStats, r4, heroActions);

        heroText.setOpacity(0); heroText.setTranslateX(24);
        FadeTransition hft = new FadeTransition(Duration.millis(750), heroText);
        hft.setDelay(Duration.millis(300)); hft.setToValue(1.0);
        TranslateTransition htt = new TranslateTransition(Duration.millis(750), heroText);
        htt.setDelay(Duration.millis(300)); htt.setToX(0); htt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(hft, htt).play();

        hero.getChildren().addAll(globeCanvas, heroText);
        return hero;
    }

    private void refreshArticleGrid() {
        if (articleGrid == null) return;
        articleGrid.getChildren().clear();

        if (publishedArticles.isEmpty()) {
            Text empty = new Text("No published articles yet. Once admin publishes an authenticated draft, it will appear here.");
            empty.setFont(Font.font("System", 14));
            empty.setFill(Color.web(MID));
            empty.setWrappingWidth(720);
            articleGrid.add(empty, 0, 0);
            return;
        }

        for (int i = 0; i < publishedArticles.size(); i++) {
            StackPane card = buildArticleCard(publishedArticles.get(i));
            card.setOpacity(0); card.setTranslateY(16);
            FadeTransition cft = new FadeTransition(Duration.millis(480), card);
            cft.setDelay(Duration.millis(80 + i * 45)); cft.setToValue(1.0);
            TranslateTransition ctt = new TranslateTransition(Duration.millis(480), card);
            ctt.setDelay(Duration.millis(80 + i * 45)); ctt.setToY(0);
            ctt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(cft, ctt).play();
            articleGrid.add(card, i % 3, i / 3);
        }
    }

    private StackPane buildArticleCard(Article article) {
        String title  = emptyTo(article.getTitle(), "Untitled Article");
        String author = emptyTo(article.getBotName(), "Aqalnama Bot");
        String cat    = emptyTo(article.getTopic(), "General");
        String time   = estimateReadTime(article);
        String color  = topicColor(cat);
        BotPersona authorBot = authorsById.get(article.getBotId());
        boolean flaggedAuthor = authorBot != null && authorBot.isFlagged();

        VBox inner = new VBox(0);
        inner.setPrefWidth(298); inner.setMinWidth(280);
        inner.setStyle("-fx-background-color:rgba(6,12,26,0.94);-fx-background-radius:14;");

        Rectangle topBar = new Rectangle(298, 3);
        topBar.setArcWidth(14); topBar.setArcHeight(14);
        topBar.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(color,0.95)), new Stop(1, Color.web(color,0.05))));
        topBar.setEffect(new Glow(0.7));

        VBox cardContent = new VBox(10);
        cardContent.setPadding(new Insets(14,18,16,18));

        HBox metaRow = new HBox(8); metaRow.setAlignment(Pos.CENTER_LEFT);
        Label catL = new Label(cat.toUpperCase());
        catL.setFont(Font.font("System",FontWeight.BOLD,8)); catL.setTextFill(Color.web(color));
        catL.setPadding(new Insets(3,9,3,9));
        catL.setStyle("-fx-background-color:"+color+"1A;-fx-border-color:"+color+"50;-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;");
        Region msp = new Region(); HBox.setHgrow(msp, Priority.ALWAYS);
        Text timeT = new Text("⏱  " + time); timeT.setFont(Font.font("System",10)); timeT.setFill(Color.web(MID));
        metaRow.getChildren().addAll(catL, msp, timeT);

        Text titleT = new Text(title);
        titleT.setFont(Font.font("System",FontWeight.BOLD,13)); titleT.setFill(Color.web(BRIGHT));
        titleT.setWrappingWidth(262);

        HBox authRow = new HBox(8); authRow.setAlignment(Pos.CENTER_LEFT);
        Circle authDot = new Circle(4, Color.web(color)); authDot.setEffect(new Glow(0.6));
        VBox authInfo = new VBox(0);
        Text authT = new Text(author); authT.setFont(Font.font("System",FontWeight.BOLD,10)); authT.setFill(Color.web(LIGHT));
        Text authSub = new Text(flaggedAuthor ? "AI Author  •  Flagged for review" : "AI Author  •  Verified ✓");
        authSub.setFont(Font.font("System",8)); authSub.setFill(Color.web(flaggedAuthor ? RED : DIM));
        authInfo.getChildren().addAll(authT, authSub);
        Region asp = new Region(); HBox.setHgrow(asp, Priority.ALWAYS);
        Text readArr = new Text("READ →"); readArr.setFont(Font.font("System",FontWeight.BOLD,10)); readArr.setFill(Color.web(color));
        authRow.getChildren().addAll(authDot, authInfo, asp, readArr);
        cardContent.getChildren().addAll(metaRow, titleT, authRow);
        if (flaggedAuthor) {
            Label flagL = new Label("AUTHOR FLAGGED");
            flagL.setFont(Font.font("System", FontWeight.BOLD, 8));
            flagL.setTextFill(Color.web(RED));
            flagL.setPadding(new Insets(3, 8, 3, 8));
            flagL.setStyle("-fx-background-color:rgba(248,113,113,0.12);-fx-border-color:rgba(248,113,113,0.45);-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;");
            cardContent.getChildren().add(flagL);
        }
        inner.getChildren().addAll(topBar, cardContent);

        Rectangle borderRect = new Rectangle(298, flaggedAuthor ? 144 : 116);
        borderRect.setArcWidth(14); borderRect.setArcHeight(14);
        borderRect.setFill(Color.TRANSPARENT);
        borderRect.setStroke(Color.web(color, 0.18)); borderRect.setStrokeWidth(1);
        borderRect.setMouseTransparent(true);

        DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.web(color,0.18), 20, 0.1, 0, 4);
        StackPane wrapper = new StackPane(inner, borderRect);
        wrapper.setCursor(javafx.scene.Cursor.HAND); wrapper.setPrefWidth(298);
        wrapper.setEffect(ds);

        // Hover: scale only (no 3D tilt — too expensive on 6 cards)
        wrapper.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), wrapper);
            st.setToX(1.03); st.setToY(1.03); st.play();
            wrapper.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web(color,0.48), 32, 0.2, 0, 6));
            borderRect.setStroke(Color.web(color, 0.6));
        });
        wrapper.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), wrapper);
            st.setToX(1.0); st.setToY(1.0); st.play();
            wrapper.setEffect(ds);
            borderRect.setStroke(Color.web(color, 0.18));
        });
        wrapper.setOnMouseClicked(e -> showArticleReaderView(article));

        return wrapper;
    }

    // =========================================================================
    //  SEARCH SECTION
    // =========================================================================
    private ScrollPane buildSearchSection() {
        VBox content = new VBox(26);
        content.setPadding(new Insets(44));
        content.setStyle("-fx-background-color:transparent;");

        Text h1 = new Text("NEURAL SEARCH");
        h1.setFont(Font.font("System",FontWeight.BLACK,34));
        h1.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLT)), new Stop(1, Color.web(CYAN))));
        h1.setEffect(new Glow(0.3));
        Text h2 = new Text("Query the Aqalnama knowledge network");
        h2.setFont(Font.font("System",14)); h2.setFill(Color.web(MID));
        VBox heading = new VBox(5, h1, h2);

        StackPane searchRow = new StackPane();
        searchRow.setMaxWidth(780); searchRow.setAlignment(Pos.CENTER_LEFT);
        Rectangle sBg = new Rectangle(780,56);
        sBg.setArcWidth(12); sBg.setArcHeight(12);
        sBg.setFill(Color.web("rgba(4,10,22,0.92)"));
        sBg.setStroke(Color.web(BLT,0.35)); sBg.setStrokeWidth(1.5);
        sBg.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web(BLT,0.2), 20, 0.1, 0, 0));
        searchField = new TextField();
        searchField.setPromptText("  ◉  Search topics, authors, concepts...");
        searchField.setPrefWidth(780); searchField.setPrefHeight(56);
        searchField.setStyle("-fx-background-color:transparent;-fx-text-fill:white;" +
            "-fx-prompt-text-fill:#374151;-fx-font-size:14px;-fx-padding:0 20;");
        searchField.focusedProperty().addListener((o,ov,nv) -> {
            if (nv) { sBg.setStroke(Color.web(CYAN,0.8)); sBg.setEffect(new DropShadow(BlurType.GAUSSIAN,Color.web(CYAN,0.4),28,0.2,0,0)); }
            else    { sBg.setStroke(Color.web(BLT,0.35));  sBg.setEffect(new DropShadow(BlurType.GAUSSIAN,Color.web(BLT,0.2),20,0.1,0,0)); }
        });
        searchField.textProperty().addListener((obs, oldText, newText) -> refreshSearchResults());
        searchRow.getChildren().addAll(sBg, searchField);

        HBox chips = new HBox(10);
        String[] sugg = {"Quantum Physics","Neuroscience","Black Holes","CRISPR","Ancient History","Consciousness"};
        for (String s : sugg) {
            Label chip = new Label(s);
            chip.setFont(Font.font("System",FontWeight.BOLD,10)); chip.setTextFill(Color.web(BLT));
            chip.setPadding(new Insets(6,14,6,14)); chip.setCursor(javafx.scene.Cursor.HAND);
            String base2 = "-fx-background-color:rgba(96,165,250,0.09);-fx-border-color:rgba(96,165,250,0.28);-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;";
            String hov2  = "-fx-background-color:rgba(96,165,250,0.18);-fx-border-color:rgba(96,165,250,0.6);-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;";
            chip.setStyle(base2);
            chip.setOnMouseEntered(ev -> chip.setStyle(hov2));
            chip.setOnMouseExited(ev  -> chip.setStyle(base2));
            chip.setOnMouseClicked(ev -> searchField.setText(s));
            chips.getChildren().add(chip);
        }

        HBox resHdr = new HBox(10); resHdr.setAlignment(Pos.CENTER_LEFT);
        Text rhT = new Text("ALL ARTICLES"); rhT.setFont(Font.font("System",FontWeight.BOLD,10)); rhT.setFill(Color.web(MID));
        resultCountLabel = new Label("0 RESULTS");
        resultCountLabel.setFont(Font.font("System",FontWeight.BOLD,9)); resultCountLabel.setTextFill(Color.web(CYAN));
        resultCountLabel.setPadding(new Insets(2,8,2,8));
        resultCountLabel.setStyle("-fx-background-color:rgba(6,182,212,0.12);-fx-border-color:rgba(6,182,212,0.35);-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;");
        resHdr.getChildren().addAll(rhT, resultCountLabel);

        searchResults = new VBox(10); searchResults.setMaxWidth(780);
        refreshSearchResults();

        content.getChildren().addAll(heading, searchRow, chips, resHdr, searchResults);
        ScrollPane sp = new ScrollPane(content);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToWidth(true);
        return sp;
    }

    private void refreshSearchResults() {
        if (searchResults == null) return;
        searchResults.getChildren().clear();

        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        int matches = 0;
        for (Article article : publishedArticles) {
            String haystack = (emptyTo(article.getTitle(), "") + " "
                + emptyTo(article.getBotName(), "") + " "
                + emptyTo(article.getTopic(), "") + " "
                + emptyTo(article.getContent(), "")).toLowerCase();
            if (!query.isEmpty() && !haystack.contains(query)) continue;
            searchResults.getChildren().add(buildSearchRow(article));
            matches++;
        }

        if (matches == 0) {
            Text empty = new Text(query.isEmpty()
                ? "No published articles available yet."
                : "No published articles match your search.");
            empty.setFont(Font.font("System", 13));
            empty.setFill(Color.web(MID));
            searchResults.getChildren().add(empty);
        }
        if (resultCountLabel != null) resultCountLabel.setText(matches + " RESULTS");
    }

    private HBox buildSearchRow(Article article) {
        String title = emptyTo(article.getTitle(), "Untitled Article");
        String author = emptyTo(article.getBotName(), "Aqalnama Bot");
        String cat = emptyTo(article.getTopic(), "General");
        String time = estimateReadTime(article);
        String color = topicColor(cat);
        BotPersona authorBot = authorsById.get(article.getBotId());
        boolean flaggedAuthor = authorBot != null && authorBot.isFlagged();
        HBox row = new HBox(16); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14,18,14,18)); row.setCursor(javafx.scene.Cursor.HAND);
        String base3 = "-fx-background-color:rgba(6,12,26,0.85);-fx-background-radius:10;-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:1;-fx-border-radius:10;";
        String hov3  = "-fx-background-color:rgba(6,182,212,0.07);-fx-background-radius:10;-fx-border-color:rgba(6,182,212,0.28);-fx-border-width:1;-fx-border-radius:10;";
        row.setStyle(base3);
        Rectangle accent = new Rectangle(4,46); accent.setArcWidth(4); accent.setArcHeight(4);
        accent.setFill(Color.web(color)); accent.setEffect(new Glow(0.55));
        VBox info = new VBox(5); HBox.setHgrow(info, Priority.ALWAYS);
        Text t = new Text(title); t.setFont(Font.font("System",FontWeight.BOLD,13)); t.setFill(Color.web(BRIGHT));
        HBox meta = new HBox(10); meta.setAlignment(Pos.CENTER_LEFT);
        Text at=new Text(author); at.setFont(Font.font("System",10)); at.setFill(Color.web(MID));
        Text sep=new Text("•"); sep.setFill(Color.web(DIM));
        Text ct=new Text(cat); ct.setFont(Font.font("System",FontWeight.BOLD,10)); ct.setFill(Color.web(color));
        Text sep2=new Text("•"); sep2.setFill(Color.web(DIM));
        Text tm=new Text(flaggedAuthor ? time + " • AUTHOR FLAGGED" : time); tm.setFont(Font.font("System",10)); tm.setFill(Color.web(flaggedAuthor ? RED : DIM));
        meta.getChildren().addAll(at,sep,ct,sep2,tm);
        info.getChildren().addAll(t, meta);
        Text arrow = new Text("→"); arrow.setFont(Font.font("System",FontWeight.BOLD,16)); arrow.setFill(Color.web(color,0.7));
        row.getChildren().addAll(accent, info, arrow);
        row.setOnMouseEntered(e -> { row.setStyle(hov3);  arrow.setFill(Color.web(color)); });
        row.setOnMouseExited(e  -> { row.setStyle(base3); arrow.setFill(Color.web(color,0.7)); });
        row.setOnMouseClicked(e -> showArticleReaderView(article));
        return row;
    }

    private void showArticleReaderView(Article article) {
        Stage dialog = new Stage();
        VBox root = new VBox(16);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color:#080E1A;");

        String topic = emptyTo(article.getTopic(), "General");
        String color = topicColor(topic);
        BotPersona authorBot = authorsById.get(article.getBotId());
        boolean flaggedAuthor = authorBot != null && authorBot.isFlagged();

        Label topicBadge = new Label(topic.toUpperCase());
        topicBadge.setFont(Font.font("System", FontWeight.BOLD, 9));
        topicBadge.setTextFill(Color.web(color));
        topicBadge.setPadding(new Insets(4, 10, 4, 10));
        topicBadge.setStyle("-fx-background-color:" + color + "1A;-fx-border-color:" + color + "50;-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;");

        Text title = new Text(emptyTo(article.getTitle(), "Untitled Article"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setFill(Color.web(BRIGHT));
        title.setWrappingWidth(680);

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        Text author = new Text(emptyTo(article.getBotName(), "Aqalnama Bot"));
        author.setFont(Font.font("System", FontWeight.BOLD, 11));
        author.setFill(Color.web(LIGHT));
        Text sep = new Text("•"); sep.setFill(Color.web(DIM));
        Text readTime = new Text(estimateReadTime(article));
        readTime.setFont(Font.font("System", 11));
        readTime.setFill(Color.web(MID));
        meta.getChildren().addAll(author, sep, readTime);

        VBox moderationBox = new VBox(8);
        if (flaggedAuthor) {
            Text warning = new Text("Author flagged by admin: "
                + emptyTo(authorBot.getFlagReason(), "This author is under review."));
            warning.setFont(Font.font("System", FontWeight.BOLD, 12));
            warning.setFill(Color.web(RED));
            warning.setWrappingWidth(680);
            moderationBox.getChildren().add(warning);
        }

        Text stats = new Text(ratingText(article) + "  •  " + article.getReportCount() + " reports");
        stats.setFont(Font.font("System", FontWeight.BOLD, 11));
        stats.setFill(Color.web(LIGHT));

        HBox ratingRow = new HBox(8);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        Text rateLabel = new Text("Rate");
        rateLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        rateLabel.setFill(Color.web(MID));
        ratingRow.getChildren().add(rateLabel);
        for (int r = 1; r <= 5; r++) {
            final int rating = r;
            Button rateBtn = readerActionButton(String.valueOf(r), AMBER);
            rateBtn.setOnAction(e -> {
                rateBtn.setDisable(true);
                new Thread(() -> {
                    boolean ok = articleService.rateArticle(article, rating);
                    Platform.runLater(() -> {
                        rateBtn.setDisable(false);
                        stats.setText(ratingText(article) + "  •  " + article.getReportCount() + " reports");
                    });
                }).start();
            });
            ratingRow.getChildren().add(rateBtn);
        }

        HBox reportRow = new HBox(8);
        reportRow.setAlignment(Pos.CENTER_LEFT);
        TextField reportField = new TextField();
        reportField.setPromptText("Report reason");
        reportField.setPrefWidth(420);
        reportField.setStyle("-fx-background-color:rgba(255,255,255,0.05);-fx-text-fill:white;-fx-prompt-text-fill:#6B7280;-fx-border-color:rgba(255,255,255,0.12);-fx-border-radius:8;-fx-background-radius:8;");
        Button reportBtn = readerActionButton("REPORT", RED);
        reportBtn.setOnAction(e -> {
            String reason = reportField.getText().trim();
            if (reason.isEmpty()) reason = "Reader reported this article for admin review.";
            reportBtn.setDisable(true);
            final String cleanReason = reason;
            new Thread(() -> {
                boolean ok = articleService.reportArticle(article, user.getUsername(), cleanReason);
                Platform.runLater(() -> {
                    reportBtn.setDisable(false);
                    if (ok) {
                        reportField.clear();
                        stats.setText(ratingText(article) + "  •  " + article.getReportCount() + " reports");
                    }
                });
            }).start();
        });
        reportRow.getChildren().addAll(reportField, reportBtn);
        moderationBox.getChildren().addAll(stats, ratingRow, reportRow);

        TextArea body = new TextArea(emptyTo(article.getContent(), ""));
        body.setWrapText(true);
        body.setEditable(false);
        body.setPrefRowCount(22);
        body.setStyle(
            "-fx-control-inner-background:#080E1A;" +
            "-fx-background-color:#080E1A;" +
            "-fx-text-fill:#D1D5DB;" +
            "-fx-font-size:14px;" +
            "-fx-border-color:rgba(255,255,255,0.08);"
        );

        root.getChildren().addAll(topicBadge, title, meta, moderationBox, body);
        dialog.setScene(new Scene(root, 760, 640));
        dialog.setTitle(emptyTo(article.getTitle(), "Article"));
        dialog.show();
    }

    private Button readerActionButton(String text, String color) {
        Button b = new Button(text);
        b.setFont(Font.font("System", FontWeight.BOLD, 10));
        b.setTextFill(Color.web(color));
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setPadding(new Insets(6, 11, 6, 11));
        b.setStyle("-fx-background-color:" + color + "1A;-fx-border-color:" + color + "55;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;");
        return b;
    }

    private String ratingText(Article article) {
        if (article.getRatingCount() == 0) return "No ratings yet";
        return String.format("%.1f stars from %d ratings", article.getAverageRating(), article.getRatingCount());
    }

    private void updateArticleStats() {
        int count = publishedArticles.size();
        if (publishedStatText != null) publishedStatText.setText(String.valueOf(count));
        if (feedSubtitle != null) {
            feedSubtitle.setText(count == 1
                ? "1 AI-authored article verified by system administrators"
                : count + " AI-authored articles verified by system administrators");
        }
    }

    private String estimateReadTime(Article article) {
        String content = emptyTo(article.getContent(), "");
        int words = content.isBlank() ? 1 : content.trim().split("\\s+").length;
        int mins = Math.max(1, (int)Math.ceil(words / 220.0));
        return mins + " min";
    }

    private String topicColor(String topic) {
        String[] colors = {CYAN, TEAL, BLT, PURP, GREEN, AMBER, RED};
        return colors[Math.abs(emptyTo(topic, "General").toLowerCase().hashCode()) % colors.length];
    }

    private String emptyTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // =========================================================================
    //  REQUEST ARTICLE — Dialog + Tracker
    // =========================================================================
    private void showRequestArticleDialog() {
        Stage dialog = new Stage();
        VBox root = new VBox(18);
        root.setPadding(new Insets(36));
        root.setStyle("-fx-background-color: #0A1020;");
        root.setPrefWidth(460);

        Text title = new Text("REQUEST AN ARTICLE");
        title.setFont(Font.font("System", FontWeight.BLACK, 20));
        title.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(CYAN)), new Stop(1, Color.web(TEAL))));
        title.setEffect(new Glow(0.3));

        Text desc = new Text("Describe the topic you want and pick an AI author.\n"
            + "The article will be generated by the LLM and sent to admins for approval.");
        desc.setFont(Font.font("System", 12));
        desc.setFill(Color.web(MID));
        desc.setWrappingWidth(390);

        // Topic input
        Text topicLabel = new Text("ARTICLE TOPIC");
        topicLabel.setFont(Font.font("System", FontWeight.BOLD, 9));
        topicLabel.setFill(Color.web(MID));
        TextField topicField = new TextField();
        topicField.setPromptText("e.g. Quantum Entanglement, Black Holes, CRISPR Gene Editing...");
        topicField.setPrefHeight(44);
        topicField.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: white; -fx-prompt-text-fill: #374151;" +
            "-fx-background-radius: 8; -fx-border-color: rgba(6,182,212,0.25);" +
            "-fx-border-radius: 8; -fx-font-size: 13px; -fx-padding: 0 14;"
        );
        VBox topicGroup = new VBox(5, topicLabel, topicField);

        // Author selector
        Text authorLabel = new Text("PREFERRED AI AUTHOR");
        authorLabel.setFont(Font.font("System", FontWeight.BOLD, 9));
        authorLabel.setFill(Color.web(MID));
        ComboBox<String> authorBox = new ComboBox<>();
        // Populate with available bots
        Map<String, BotPersona> nameToBot = new HashMap<>();
        for (BotPersona bot : allBots) {
            if (!"ACTIVE".equals(bot.getStatus()) && !"PAUSED".equals(bot.getStatus())) continue;
            String display = bot.getName() + "  •  " + bot.getTopic() + "  •  " + bot.getStyle();
            authorBox.getItems().add(display);
            nameToBot.put(display, bot);
        }
        if (authorBox.getItems().isEmpty()) {
            authorBox.getItems().add("No authors available");
        }
        authorBox.setValue(authorBox.getItems().get(0));
        authorBox.setMaxWidth(Double.MAX_VALUE);
        authorBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: white; -fx-border-color: rgba(6,182,212,0.25);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        VBox authorGroup = new VBox(5, authorLabel, authorBox);

        // Status label
        Label statusLabel = new Label();
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        statusLabel.setVisible(false);
        statusLabel.setWrapText(true);

        // Submit button
        Button submitBtn = new Button("⊕  GENERATE ARTICLE");
        submitBtn.setFont(Font.font("System", FontWeight.BOLD, 12));
        submitBtn.setPrefHeight(42);
        submitBtn.setPadding(new Insets(0, 22, 0, 22));
        submitBtn.setCursor(javafx.scene.Cursor.HAND);
        String sBase = "-fx-background-color:" + CYAN + ";-fx-text-fill:#060A12;-fx-background-radius:8;-fx-font-weight:bold;";
        String sHov  = "-fx-background-color:" + CYAN + "CC;-fx-text-fill:#060A12;-fx-background-radius:8;-fx-font-weight:bold;";
        submitBtn.setStyle(sBase);
        submitBtn.setOnMouseEntered(e -> submitBtn.setStyle(sHov));
        submitBtn.setOnMouseExited(e  -> submitBtn.setStyle(sBase));

        submitBtn.setOnAction(e -> {
            String topic2 = topicField.getText().trim();
            if (topic2.isEmpty()) {
                statusLabel.setText("Please enter a topic.");
                statusLabel.setTextFill(Color.web(RED));
                statusLabel.setVisible(true);
                return;
            }
            BotPersona selectedBot = nameToBot.get(authorBox.getValue());
            if (selectedBot == null) {
                statusLabel.setText("No valid author selected. Ask admin to spawn bots first.");
                statusLabel.setTextFill(Color.web(RED));
                statusLabel.setVisible(true);
                return;
            }

            submitBtn.setText("⟳  GENERATING...");
            submitBtn.setDisable(true);
            statusLabel.setText("Sending request to AI... Please wait.");
            statusLabel.setTextFill(Color.web(AMBER));
            statusLabel.setVisible(true);

            // 1. Save request to Firestore as PENDING
            new Thread(() -> {
                String reqId = articleRequestService.saveRequest(
                    topic2, selectedBot.getId(), selectedBot.getName(), user.getUsername());

                if (reqId == null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed to save request. Try again.");
                        statusLabel.setTextFill(Color.web(RED));
                        submitBtn.setText("⊕  GENERATE ARTICLE");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                // 2. Update status to GENERATING
                articleRequestService.updateRequestStatus(reqId, "GENERATING", "");

                Platform.runLater(() -> {
                    statusLabel.setText("Request saved. AI is writing your article...");
                    statusLabel.setTextFill(Color.web(CYAN));
                });

                // 3. Call BotEngine to generate article on-demand
                botEngine.generateArticleOnDemand(topic2, selectedBot, user.getUsername(),
                    (article, success, errorMsg) -> {
                        if (success && article != null) {
                            // 4. Update request as COMPLETED
                            new Thread(() -> {
                                articleRequestService.updateRequestStatus(reqId, "COMPLETED", article.getId());
                                // Refresh requests
                                List<Map<String, String>> freshReqs =
                                    articleRequestService.getRequestsByUser(user.getUsername());
                                Platform.runLater(() -> {
                                    myRequests.clear();
                                    myRequests.addAll(freshReqs);
                                    refreshRequestsBox();
                                });
                            }).start();

                            statusLabel.setText("✓ Article generated: \"" + article.getTitle()
                                + "\"\nIt's now awaiting admin approval before publishing.");
                            statusLabel.setTextFill(Color.web(GREEN));
                            submitBtn.setText("✓  DONE");
                            topicField.clear();

                            // Auto-close after short delay
                            Timeline autoClose = new Timeline(
                                new KeyFrame(Duration.seconds(4), ev -> dialog.close()));
                            autoClose.play();
                        } else {
                            // 5. Mark request as FAILED
                            new Thread(() ->
                                articleRequestService.updateRequestStatus(reqId, "FAILED", "")
                            ).start();

                            statusLabel.setText("✗ Generation failed: "
                                + (errorMsg != null ? errorMsg : "Unknown error"));
                            statusLabel.setTextFill(Color.web(RED));
                            submitBtn.setText("⊕  GENERATE ARTICLE");
                            submitBtn.setDisable(false);
                        }
                    });
            }).start();
        });

        // Info note
        HBox infoRow = new HBox(8);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        Circle infoDot = new Circle(3.5, Color.web(CYAN, 0.6));
        Text infoText = new Text("Generated articles are saved as drafts and require admin approval before publishing.");
        infoText.setFont(Font.font("System", 10));
        infoText.setFill(Color.web(DIM));
        infoText.setWrappingWidth(380);
        infoRow.getChildren().addAll(infoDot, infoText);

        root.getChildren().addAll(title, desc, topicGroup, authorGroup, statusLabel, submitBtn, infoRow);

        dialog.setScene(new Scene(root));
        dialog.setTitle("Request Article — Aqalnama");
        dialog.show();
    }

    private void refreshRequestsBox() {
        if (requestsBox == null) return;
        requestsBox.getChildren().clear();

        if (myRequests.isEmpty()) {
            Text empty = new Text("You haven't requested any articles yet. Click \"REQUEST ARTICLE\" above to get started.");
            empty.setFont(Font.font("System", 12));
            empty.setFill(Color.web(MID));
            empty.setWrappingWidth(700);
            requestsBox.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < myRequests.size(); i++) {
            Map<String, String> req = myRequests.get(i);
            HBox row = buildRequestRow(req);
            // Animate in
            row.setOpacity(0); row.setTranslateX(-12);
            FadeTransition ft = new FadeTransition(Duration.millis(350), row);
            ft.setDelay(Duration.millis(50 + i * 40)); ft.setToValue(1.0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), row);
            tt.setDelay(Duration.millis(50 + i * 40)); tt.setToX(0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
            requestsBox.getChildren().add(row);
        }
    }

    private HBox buildRequestRow(Map<String, String> req) {
        String topic  = emptyTo(req.get("topic"), "Unknown Topic");
        String author = emptyTo(req.get("preferredBotName"), "AI Author");
        String status = emptyTo(req.get("status"), "PENDING");

        String statusColor = switch (status) {
            case "PENDING"    -> AMBER;
            case "GENERATING" -> CYAN;
            case "COMPLETED"  -> GREEN;
            case "FAILED"     -> RED;
            default           -> MID;
        };

        String statusIcon = switch (status) {
            case "PENDING"    -> "◌";
            case "GENERATING" -> "⟳";
            case "COMPLETED"  -> "✓";
            case "FAILED"     -> "✗";
            default           -> "•";
        };

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        String rowBase = "-fx-background-color:rgba(6,12,26,0.85);-fx-background-radius:10;" +
            "-fx-border-color:" + statusColor + "28;-fx-border-width:1;-fx-border-radius:10;";
        row.setStyle(rowBase);

        // Status accent bar
        Rectangle accent = new Rectangle(4, 42);
        accent.setArcWidth(4); accent.setArcHeight(4);
        accent.setFill(Color.web(statusColor));
        accent.setEffect(new Glow(0.5));

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Text topicT = new Text(topic);
        topicT.setFont(Font.font("System", FontWeight.BOLD, 13));
        topicT.setFill(Color.web(BRIGHT));

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Text authorT = new Text("Author: " + author);
        authorT.setFont(Font.font("System", 10));
        authorT.setFill(Color.web(LIGHT));
        Text sepT = new Text("•");
        sepT.setFill(Color.web(DIM));

        // Format time
        String timeStr = "Unknown";
        try {
            long createdAt = Long.parseLong(req.getOrDefault("createdAt", "0"));
            if (createdAt > 0) {
                long ageMins = (System.currentTimeMillis() - createdAt) / 60000;
                if (ageMins < 1) timeStr = "Just now";
                else if (ageMins < 60) timeStr = ageMins + " min ago";
                else if (ageMins < 1440) timeStr = (ageMins / 60) + " hr ago";
                else timeStr = (ageMins / 1440) + " days ago";
            }
        } catch (Exception ignored) {}
        Text timeT = new Text(timeStr);
        timeT.setFont(Font.font("System", 10));
        timeT.setFill(Color.web(DIM));

        meta.getChildren().addAll(authorT, sepT, timeT);
        info.getChildren().addAll(topicT, meta);

        // Status badge
        Label statusBadge = new Label(statusIcon + "  " + status);
        statusBadge.setFont(Font.font("System", FontWeight.BOLD, 9));
        statusBadge.setTextFill(Color.web(statusColor));
        statusBadge.setPadding(new Insets(4, 12, 4, 12));
        statusBadge.setStyle("-fx-background-color:" + statusColor + "1A;" +
            "-fx-border-color:" + statusColor + "50;-fx-border-width:1;" +
            "-fx-border-radius:20;-fx-background-radius:20;");

        // Pulse animation for GENERATING status
        if ("GENERATING".equals(status)) {
            FadeTransition pulse = new FadeTransition(Duration.seconds(1), statusBadge);
            pulse.setFromValue(0.5); pulse.setToValue(1.0);
            pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        }

        row.getChildren().addAll(accent, info, statusBadge);

        // Hover effect
        String rowHov = "-fx-background-color:rgba(6,182,212,0.06);-fx-background-radius:10;" +
            "-fx-border-color:" + statusColor + "55;-fx-border-width:1;-fx-border-radius:10;";
        row.setOnMouseEntered(e -> row.setStyle(rowHov));
        row.setOnMouseExited(e  -> row.setStyle(rowBase));

        return row;
    }

    // =========================================================================
    //  PROFILE SECTION
    // =========================================================================
    private ScrollPane buildProfileSection() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(44));
        content.setStyle("-fx-background-color:transparent;");

        Text heading = new Text("IDENTITY CORE");
        heading.setFont(Font.font("System",FontWeight.BLACK,34));
        heading.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLT)), new Stop(1, Color.web(PURP))));
        heading.setEffect(new Glow(0.3));

        HBox profileCard = new HBox(44);
        profileCard.setPadding(new Insets(34,40,34,40)); profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setMaxWidth(820);
        profileCard.setStyle(
            "-fx-background-color:rgba(6,10,22,0.92);-fx-background-radius:18;" +
            "-fx-border-color:rgba(167,139,250,0.22);-fx-border-width:1;-fx-border-radius:18;"
        );
        profileCard.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web(PURP,0.2), 28, 0.1, 0, 5));

        StackPane avatarSp = new StackPane(); avatarSp.setPrefSize(120,120);
        Circle outerRing = new Circle(55); outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web(PURP,0.22)); outerRing.setStrokeWidth(1);
        outerRing.getStrokeDashArray().addAll(6.0,5.0);
        RotateTransition or = new RotateTransition(Duration.seconds(14), outerRing);
        or.setByAngle(360); or.setCycleCount(Animation.INDEFINITE); or.setInterpolator(Interpolator.LINEAR); or.play();
        Circle midRing = new Circle(44); midRing.setFill(Color.TRANSPARENT);
        midRing.setStroke(Color.web(BLT,0.35)); midRing.setStrokeWidth(1.5);
        midRing.getStrokeDashArray().addAll(10.0,7.0);
        RotateTransition mr = new RotateTransition(Duration.seconds(9), midRing);
        mr.setByAngle(-360); mr.setCycleCount(Animation.INDEFINITE); mr.setInterpolator(Interpolator.LINEAR); mr.play();
        Circle innerRing = new Circle(35); innerRing.setFill(Color.TRANSPARENT);
        innerRing.setStroke(Color.web(CYAN,0.5)); innerRing.setStrokeWidth(1);
        innerRing.getStrokeDashArray().addAll(3.0,4.0);
        RotateTransition ir = new RotateTransition(Duration.seconds(5), innerRing);
        ir.setByAngle(360); ir.setCycleCount(Animation.INDEFINITE); ir.setInterpolator(Interpolator.LINEAR); ir.play();
        Circle avC = new Circle(26);
        avC.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(BLUE,0.85)), new Stop(1, Color.web(PURP,0.85))));
        avC.setStroke(Color.web(PURP,0.7)); avC.setStrokeWidth(2.5); avC.setEffect(new Glow(0.35));
        String init = user.getUsername().isEmpty() ? "U" : user.getUsername().substring(0,1).toUpperCase();
        Text avT = new Text(init); avT.setFont(Font.font("System",FontWeight.BLACK,24)); avT.setFill(Color.WHITE);
        avatarSp.getChildren().addAll(outerRing, midRing, innerRing, avC, avT);

        VBox userInfoBox = new VBox(14); HBox.setHgrow(userInfoBox, Priority.ALWAYS);
        Text uName = new Text(user.getUsername());
        uName.setFont(Font.font("System",FontWeight.BLACK,28)); uName.setFill(Color.web(BRIGHT));
        HBox badges = new HBox(8); badges.setAlignment(Pos.CENTER_LEFT);
        String[][] bdData = {{user.getRole(),PURP},{user.getStatus(),GREEN},{"VERIFIED",CYAN}};
        for (String[] bd : bdData) {
            Label bl = new Label(bd[0]); bl.setFont(Font.font("System",FontWeight.BOLD,9)); bl.setTextFill(Color.web(bd[1]));
            bl.setPadding(new Insets(4,11,4,11));
            bl.setStyle("-fx-background-color:"+bd[1]+"1A;-fx-border-color:"+bd[1]+"50;-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;");
            badges.getChildren().add(bl);
        }
        HBox pStats = new HBox(32); pStats.setAlignment(Pos.CENTER_LEFT);
        String[][] ps = {{"0","Articles\nRead"},{"0","Flags\nRaised"},{"0","Reports\nFiled"}};
        for (String[] p : ps) {
            VBox pc = new VBox(2);
            Text pv = new Text(p[0]); pv.setFont(Font.font("System",FontWeight.BOLD,26));
            pv.setFill(Color.web(BLT)); pv.setEffect(new Glow(0.3));
            Text pl = new Text(p[1]); pl.setFont(Font.font("System",10)); pl.setFill(Color.web(MID));
            pc.getChildren().addAll(pv, pl); pStats.getChildren().add(pc);
        }
        userInfoBox.getChildren().addAll(uName, badges, pStats);
        profileCard.getChildren().addAll(avatarSp, userInfoBox);

        Text actHdr = new Text("NEURAL ACTIVITY LOG");
        actHdr.setFont(Font.font("System",FontWeight.BOLD,10)); actHdr.setFill(Color.web(MID));
        VBox actLog = new VBox(8); actLog.setMaxWidth(820);
        String[][] acts = {
            {"Joined Aqalnama knowledge network","Just now",CYAN},
            {"Account status set to ACTIVE",     "Today",   GREEN},
            {"Reader role assigned by admin",    "Today",   PURP},
        };
        for (int i = 0; i < acts.length; i++) {
            HBox row = new HBox(14); row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(13,18,13,18));
            row.setStyle("-fx-background-color:rgba(6,12,26,0.75);-fx-background-radius:9;-fx-border-color:rgba(255,255,255,0.05);-fx-border-width:1;-fx-border-radius:9;");
            row.setOpacity(0); row.setTranslateX(-16);
            FadeTransition rft = new FadeTransition(Duration.millis(420), row); rft.setDelay(Duration.millis(200+i*90)); rft.setToValue(1.0);
            TranslateTransition rtt = new TranslateTransition(Duration.millis(420), row); rtt.setDelay(Duration.millis(200+i*90)); rtt.setToX(0); rtt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(rft, rtt).play();
            Circle aDot = new Circle(4, Color.web(acts[i][2])); aDot.setEffect(new Glow(0.7));
            Text aText = new Text(acts[i][0]); aText.setFont(Font.font("System",12)); aText.setFill(Color.web(LIGHT));
            Region ar = new Region(); HBox.setHgrow(ar, Priority.ALWAYS);
            Text aTime = new Text(acts[i][1]); aTime.setFont(Font.font("System",10)); aTime.setFill(Color.web(DIM));
            row.getChildren().addAll(aDot, aText, ar, aTime); actLog.getChildren().add(row);
        }
        content.getChildren().addAll(heading, profileCard, actHdr, actLog);
        ScrollPane sp = new ScrollPane(content);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToWidth(true);
        return sp;
    }

    // =========================================================================
    //  GLOBE (single instance, optimized)
    // =========================================================================
    private void initTrigTables() {
        sinSeg = new double[SEGS+1];
        cosSeg = new double[SEGS+1];
        for (int j = 0; j <= SEGS; j++) {
            double t = (Math.PI * 2.0 / SEGS) * j;
            sinSeg[j] = Math.sin(t);
            cosSeg[j] = Math.cos(t);
        }
    }

    private void drawGlobe() {
        GraphicsContext gc = globeCanvas.getGraphicsContext2D();
        double cW = globeCanvas.getWidth(), cH = globeCanvas.getHeight();
        double cx = cW/2, cy = cH/2, r = 105;
        double cosRY = Math.cos(globeRotY), sinRY = Math.sin(globeRotY);
        final double TWO_PI = Math.PI * 2.0;
        final int LAT = 10, LON = 14;

        gc.setFill(C_TRAIL);
        gc.fillRect(0, 0, cW, cH);

        // Longitude lines
        gc.setLineWidth(0.65);
        for (int li = 0; li < LON; li++) {
            double phi = (TWO_PI / LON) * li;
            double cosPhi = Math.cos(phi), sinPhi = Math.sin(phi);
            double prevSX = 0, prevSY = 0;
            for (int j = 0; j <= SEGS; j++) {
                double x3 = sinSeg[j]*cosPhi, y3 = cosSeg[j], z3 = sinSeg[j]*sinPhi;
                double xr = x3*cosRY+z3*sinRY, zr = -x3*sinRY+z3*cosRY;
                double sx = cx+r*xr, sy = cy-r*y3;
                if (j > 0) { gc.setStroke(cC((zr+1.0)*0.275+0.02)); gc.strokeLine(prevSX,prevSY,sx,sy); }
                prevSX=sx; prevSY=sy;
            }
        }

        // Latitude lines
        for (int li = 1; li < LAT; li++) {
            double theta = (Math.PI/LAT)*li, y3f = Math.cos(theta), rLat = r*Math.sin(theta);
            boolean eq = (li == LAT/2);
            double prevSX=0, prevSY=0;
            for (int j = 0; j <= SEGS; j++) {
                double xr = cosSeg[j]*cosRY+sinSeg[j]*sinRY, zr = -cosSeg[j]*sinRY+sinSeg[j]*cosRY;
                double sx = cx+rLat*xr, sy = cy-r*y3f;
                if (j > 0) {
                    double a = (zr+1.0)*(eq?0.36:0.18)+0.02;
                    gc.setStroke(eq ? cT(a) : cC(a)); gc.setLineWidth(eq ? 1.2 : 0.5);
                    gc.strokeLine(prevSX,prevSY,sx,sy);
                }
                prevSX=sx; prevSY=sy;
            }
        }

        // Orbiting nodes (3 instead of 4)
        double[] incl = {Math.PI/4, -Math.PI/5, Math.PI/3};
        for (int n = 0; n < 3; n++) {
            double na=globeOrb+(TWO_PI/3)*n, cna=Math.cos(na), sna=Math.sin(na);
            double nx3=cna, ny3=sna*Math.sin(incl[n]), nz3=sna*Math.cos(incl[n]);
            double nxr=nx3*cosRY+nz3*sinRY, nzr=-nx3*sinRY+nz3*cosRY;
            double nsx=cx+(r+13)*nxr, nsy=cy-(r+13)*ny3;
            double na2=(nzr+1.0)*0.45+0.05;
            double sfX=cx+r*nxr, sfY=cy-r*ny3;
            gc.setStroke(cT(na2*0.28)); gc.setLineWidth(0.5); gc.strokeLine(sfX,sfY,nsx,nsy);
            gc.setStroke(cT(na2*0.5));  gc.setLineWidth(0.7); gc.strokeOval(nsx-5,nsy-5,10,10);
            gc.setFill(cT(na2));        gc.fillOval(nsx-2.5,nsy-2.5,5,5);
        }

        // Poles
        gc.setFill(Color.web(CYAN, 0.9));
        gc.fillOval(cx-3, cy-r-3, 6, 6);
        gc.fillOval(cx-3, cy+r-3, 6, 6);
    }

    // =========================================================================
    //  PARTICLES
    // =========================================================================
    private void initParticles(double w, double h) {
        Random rng = new Random();
        for (int i = 0; i < P; i++) {
            pX[i]=rng.nextDouble()*w; pY[i]=rng.nextDouble()*h;
            pVX[i]=(rng.nextDouble()-0.5)*0.3; pVY[i]=(rng.nextDouble()-0.5)*0.3;
            pR[i]=0.8+rng.nextDouble()*2.0; pA[i]=0.04+rng.nextDouble()*0.10;
        }
    }

    private void drawParticles() {
        double cW=partCanvas.getWidth(), cH=partCanvas.getHeight();
        GraphicsContext gc = partCanvas.getGraphicsContext2D();
        gc.clearRect(0,0,cW,cH);
        for (int i = 0; i < P; i++) {
            pX[i]+=pVX[i]; pY[i]+=pVY[i];
            if (pX[i]<0) pX[i]=cW; if (pX[i]>cW) pX[i]=0;
            if (pY[i]<0) pY[i]=cH; if (pY[i]>cH) pY[i]=0;
            gc.setFill(i%2==0 ? cC(pA[i]) : Color.web(BLT, pA[i]));
            gc.fillOval(pX[i]-pR[i], pY[i]-pR[i], pR[i]*2, pR[i]*2);
        }
    }

    private void drawStaticGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web(BLUE, 0.032)); gc.setLineWidth(1);
        for (double x=0; x<w; x+=60) gc.strokeLine(x,0,x,h);
        for (double y=0; y<h; y+=60) gc.strokeLine(0,y,w,y);
    }
}
