import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Handles Firebase Authentication via REST API.
 * Uses email/password sign-in and sign-up endpoints from Firebase Identity Toolkit.
 */
public class FirebaseAuthService {

    // ── Replace with your Firebase Web API Key ──────────────────────────────
    // Firebase Console → Project Settings → General → Web API Key
    private static final String API_KEY = "AIzaSyB2MsjsrGqaWMcfpB2xUo_Zjdzmuap-DGE";
    // ────────────────────────────────────────────────────────────────────────

    private static final String SIGN_IN_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;

    private static final String SIGN_UP_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // =========================================================================
    //  SIGN IN  (existing — unchanged)
    // =========================================================================
    public AuthResult signIn(String email, String password) {
        String body = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
            email, password
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SIGN_IN_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            String responseBody = response.body();
            System.out.println("[FirebaseAuth] SignIn Response: " + responseBody);

            if (response.statusCode() == 200) {
                String uid     = extractJsonValue(responseBody, "localId");
                String idToken = extractJsonValue(responseBody, "idToken");
                return new AuthResult(true, uid, idToken, null);
            } else {
                String errorMsg = extractJsonValue(responseBody, "message");
                return new AuthResult(false, null, null, friendlySignInError(errorMsg));
            }

        } catch (Exception e) {
            return new AuthResult(false, null, null, "Network error: " + e.getMessage());
        }
    }

    // =========================================================================
    //  SIGN UP  (new)
    // =========================================================================
    public AuthResult signUp(String email, String password) {
        String body = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
            email, password
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SIGN_UP_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            String responseBody = response.body();
            System.out.println("[FirebaseAuth] SignUp Response: " + responseBody);

            if (response.statusCode() == 200) {
                String uid     = extractJsonValue(responseBody, "localId");
                String idToken = extractJsonValue(responseBody, "idToken");
                return new AuthResult(true, uid, idToken, null);
            } else {
                String errorMsg = extractJsonValue(responseBody, "message");
                return new AuthResult(false, null, null, friendlySignUpError(errorMsg));
            }

        } catch (Exception e) {
            return new AuthResult(false, null, null, "Network error: " + e.getMessage());
        }
    }

    // =========================================================================
    //  ERROR MESSAGES
    // =========================================================================
    private String friendlySignInError(String code) {
        if (code == null) return "Login failed. Please try again.";
        return switch (code) {
            case "EMAIL_NOT_FOUND"             -> "No account found with this email.";
            case "INVALID_PASSWORD"            -> "Incorrect password.";
            case "USER_DISABLED"               -> "This account has been disabled.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Too many attempts. Please try later.";
            case "INVALID_LOGIN_CREDENTIALS"   -> "Invalid email or password.";
            default -> "Login failed: " + code;
        };
    }

    private String friendlySignUpError(String code) {
        if (code == null) return "Registration failed. Please try again.";
        return switch (code) {
            case "EMAIL_EXISTS"                -> "An account with this email already exists.";
            case "WEAK_PASSWORD : Password should be at least 6 characters",
                 "WEAK_PASSWORD"              -> "Password is too weak. Use at least 6 characters.";
            case "INVALID_EMAIL"              -> "Invalid email address format.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER"-> "Too many attempts. Please try later.";
            case "OPERATION_NOT_ALLOWED"      -> "Email/password accounts are not enabled. Contact admin.";
            default -> "Registration failed: " + code;
        };
    }

    // =========================================================================
    //  JSON HELPER  (existing — unchanged)
    // =========================================================================
    public static String extractJsonValue(String json, String key) {
        String[] patterns = {
            "\"" + key + "\":\"",
            "\"" + key + "\": \""
        };
        for (String search : patterns) {
            int start = json.indexOf(search);
            if (start != -1) {
                start += search.length();
                int end = json.indexOf("\"", start);
                if (end != -1) return json.substring(start, end);
            }
        }
        return null;
    }

    // =========================================================================
    //  AuthResult record  (existing — unchanged)
    // =========================================================================
    public static class AuthResult {
        public final boolean success;
        public final String  uid;
        public final String  idToken;
        public final String  errorMessage;

        public AuthResult(boolean success, String uid, String idToken, String errorMessage) {
            this.success      = success;
            this.uid          = uid;
            this.idToken      = idToken;
            this.errorMessage = errorMessage;
        }
    }
}