import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Handles Firestore REST API calls.
 * Used to fetch user profile (role, status, username) after Firebase Auth.
 *
 * Firestore structure:
 *   users/
 *     {uid}/
 *       username: "rafay"
 *       role:     "ADMIN" | "READER" | "BOT"
 *       status:   "ACTIVE" | "PENDING" | "DISABLED"
 */
public class FirestoreService {

    // ── Replace with your Firebase Project ID ───────────────────────────────
    // Firebase Console → Project Settings → General → Project ID
    private static final String PROJECT_ID = "aqalnama-9d5f2";
    // ────────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
        "/databases/(default)/documents/";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Fetches a user document from Firestore by UID.
     * Requires a valid Firebase idToken for authorization.
     *
     * @param uid     Firebase user UID
     * @param idToken Firebase ID token from authentication
     * @return User object or null if not found
     */
    public User getUserProfile(String uid, String idToken) {
        String url = BASE_URL + "users/" + uid;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("[FirestoreService] Raw response: " + body); // DEBUG
                String username = extractStringField(body, "username");
                String role     = extractStringField(body, "role");
                String status   = extractStringField(body, "status");

               if (role == null || role.isEmpty())   role   = "READER";
                if (status == null || status.isEmpty()) status = "ACTIVE";
                        System.out.println("[FirestoreService] Parsed → username=" + username + " role=" + role + " status=" + status);


                return new User(uid, username != null ? username : "User", role, status);
            } else {
                System.err.println("[FirestoreService] Failed to fetch user: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("[FirestoreService] Network error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new user document in Firestore after registration.
     * Call this once when a user first registers.
     */
    public boolean createUserProfile(String uid, String username, String role, String idToken) {
        String url = BASE_URL + "users/" + uid;

        String body = String.format("""
            {
              "fields": {
                "username": {"stringValue": "%s"},
                "role":     {"stringValue": "%s"},
                "status":   {"stringValue": "ACTIVE"}
              }
            }
            """, username, role);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + idToken)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200;

        } catch (Exception e) {
            System.err.println("[FirestoreService] Create user error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts a Firestore stringValue field from response JSON.
     * Handles both compact and spaced formats:
     *   "fieldName":{"stringValue":"value"}
     *   "fieldName": { "stringValue": "value" }
     */
    private String extractStringField(String json, String fieldName) {
    // Strip ALL whitespace (spaces, newlines, tabs) before parsing
    String normalized = json.replaceAll("\\s+", "");

    String search = "\"" + fieldName + "\":{\"stringValue\":\"";
    int start = normalized.indexOf(search);
    if (start == -1) return null;
    start += search.length();
    int end = normalized.indexOf("\"", start);
    if (end == -1) return null;
    return normalized.substring(start, end);
}
}