import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
    private StackPane mainContentStack; // The "Deck of Cards"

    public DashboardView(Stage stage) {
        this.stage = stage;
    }

    public Scene createScene() {
        BorderPane dashRoot = new BorderPane();
        dashRoot.setStyle("-fx-background-color: linear-gradient(to bottom right, #0a1a2e, #162a4a);");

        // ===== 1. LEFT NAV =====
        VBox leftNav = new VBox(15);
        leftNav.setPadding(new Insets(25));
        leftNav.setPrefWidth(280);
        leftNav.setStyle("-fx-background-color: rgba(10, 20, 35, 0.4);");

        ImageView logo = new ImageView(new Image("file:/home/rafay/FASTCloud_AplhaVersion/FASTCLOUD/Resources/FinalLogo.png"));
        logo.setFitWidth(220);
        logo.setPreserveRatio(true);
        
        Button navDash = createNavButton("Dashboard");
        // Navigation logic: Clicking "Dashboard" brings the grid back
        navDash.setOnAction(e -> showDashboardGrid());

        leftNav.getChildren().addAll(logo, navDash, createNavButton("Leader Board"), createNavButton("Compute"), createNavButton("Analytics"));
        dashRoot.setLeft(leftNav);

        // ===== 2. THE STACKPANE (The Secret Sauce) =====
        mainContentStack = new StackPane();
        
        // Initial View: The Grid of AI Logos
        VBox dashboardGrid = createDashboardGrid();
        mainContentStack.getChildren().add(dashboardGrid);

        dashRoot.setCenter(mainContentStack);

        // Fade in entire UI
        dashRoot.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), dashRoot);
        fadeIn.setToValue(1);
        fadeIn.play();

        return new Scene(dashRoot, 1100, 800);
    }

   
    private VBox createDashboardGrid() {
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Choose Your Intelligence");
        title.setTextFill(Color.web("#00D4FF"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        TilePane iconGrid = new TilePane(25, 25);
        iconGrid.setAlignment(Pos.CENTER);
        iconGrid.setPrefColumns(3);

        String path = "file:/home/rafay/FASTCloud_AplhaVersion/FASTCLOUD/Resources/";
        LogoConstituents[] aiTools = {
            new LogoConstituents(new Image(path + "ChatGpt.png"), "ChatGPT"),
            new LogoConstituents(new Image(path + "Claude-ai-logo.png"), "Claude AI"),
            new LogoConstituents(new Image(path + "Google-Gemini-Logo-PNG-Photo.png"), "Gemini"),
            new LogoConstituents(new Image(path + "Deepseek.png"), "DeepSeek"),
            new LogoConstituents(new Image(path + "GrokAI.png"), "Grok"),
            new LogoConstituents(new Image(path + "perplexity.png"), "Perplexity")
        };

        for (LogoConstituents tool : aiTools) {
            iconGrid.getChildren().add(createAICard(tool));
        }

        ScrollPane scroll = new ScrollPane(iconGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        container.getChildren().addAll(title, scroll);
        return container;
    }

    // --- HELPER: Create the AI Card ---
    private VBox createAICard(LogoConstituents tool) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(180, 200);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 20; -fx-border-color: rgba(0,212,255,0.1); -fx-border-radius: 20;");

        ImageView iv = new ImageView(tool.getImage());
        iv.setFitWidth(70);
        iv.setPreserveRatio(true);

        Label name = new Label(tool.getName());
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("System", FontWeight.BOLD, 16));

        card.getChildren().addAll(iv, name);

        // Hover & Click Logic
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(0, 212, 255, 0.1); -fx-border-color: #00D4FF; -fx-background-radius: 20; -fx-border-radius: 20;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(0,212,255,0.1); -fx-background-radius: 20; -fx-border-radius: 20;"));
        
        card.setOnMouseClicked(e -> openAIChatInterface(tool.getName()));

        return card;
    }

    // --- THE SWITCHING LOGIC ---
    private void openAIChatInterface(String aiName) {
        VBox chatView = new VBox(20);
        chatView.setPadding(new Insets(30));
        chatView.setStyle("-fx-background-color: #0a1a2e;");

        Label chatTitle = new Label("Chatting with " + aiName);
        chatTitle.setTextFill(Color.WHITE);
        chatTitle.setFont(Font.font("System", 24));

        // Mock chat area
        VBox chatArea = new VBox(10);
        chatArea.setPrefHeight(500);
        chatArea.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10;");
        
        TextField input = new TextField();
        input.setPromptText("Message " + aiName + "...");
        input.setStyle("-fx-background-color: #162a4a; -fx-text-fill: white; -fx-padding: 15;");

        chatView.getChildren().addAll(chatTitle, chatArea, input);

        // Animation: Slide in from right
        chatView.setTranslateX(1000);
        mainContentStack.getChildren().add(chatView);

        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.4), chatView);
        slideIn.setToX(0);
        slideIn.play();
    }

    private void showDashboardGrid() {
        if (mainContentStack.getChildren().size() > 1) {
            mainContentStack.getChildren().remove(1, mainContentStack.getChildren().size());
        }
    }

   private Button createNavButton(String text) {
    Button b = new Button(text);
    b.setMaxWidth(Double.MAX_VALUE);
    // Initial Style
    b.setStyle("-fx-background-color: transparent; -fx-text-fill: #BFEFFF; -fx-font-weight: 600; -fx-padding: 12; -fx-alignment: CENTER_LEFT; -fx-background-radius: 10;");

    // 1. Hover Color and Background
    b.setOnMouseEntered(e -> {
        b.setStyle("-fx-background-color: rgba(0, 212, 255, 0.1); -fx-text-fill: #00D4FF; -fx-font-weight: 600; -fx-padding: 12; -fx-alignment: CENTER_LEFT; -fx-background-radius: 10; -fx-border-color: rgba(0, 212, 255, 0.3); -fx-border-radius: 10;");
        
        // 2. Add a tiny Slide-Right Animation
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), b);
        slide.setToX(10); 
        slide.play();
    });

    // Reset Style on Exit
    b.setOnMouseExited(e -> {
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #BFEFFF; -fx-font-weight: 600; -fx-padding: 12; -fx-alignment: CENTER_LEFT; -fx-background-radius: 10;");
        
        TranslateTransition slideBack = new TranslateTransition(Duration.millis(200), b);
        slideBack.setToX(0);
        slideBack.play();
    });

    return b;
}
}
class LogoConstituents{
    Image LogoImages;
    String name;
    ImageView img;

    public LogoConstituents(Image imge , String str){
        this.LogoImages = imge;
        img = new ImageView(LogoImages);
        img.setFitHeight(200);
        img.setPreserveRatio(true); 
        this.name = str;
    }

    public Image getImage() { return LogoImages; }
    public String getName() { return name; }
}