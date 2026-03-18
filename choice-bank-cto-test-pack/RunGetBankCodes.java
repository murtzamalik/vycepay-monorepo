import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/**
 * Choice Bank getBankCodes – generate signed request and optionally call API.
 *
 * Usage: java RunGetBankCodes [--no-call]
 *   Default: prints curl + body and calls API. Use --no-call to only print.
 * Env: CHOICE_BANK_SENDER_ID (default VYCEIN), CHOICE_BANK_PRIVATE_KEY (required for real call)
 */
public class RunGetBankCodes {

    public static void main(String[] args) throws Exception {
        boolean noCall = Arrays.asList(args).contains("--no-call");

        // -------------------------------------------------------------------
        // Load .env from script directory or parent
        // -------------------------------------------------------------------
        Map<String, String> env = new HashMap<>(System.getenv());
        Path scriptDir = Paths.get(System.getProperty("user.dir"));

        for (Path envPath : new Path[]{scriptDir.resolve(".env"), scriptDir.getParent().resolve(".env")}) {
            if (Files.isRegularFile(envPath)) {
                for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        env.putIfAbsent(key, value);
                    }
                }
                break;
            }
        }

        // -------------------------------------------------------------------
        // Configuration
        // -------------------------------------------------------------------
        String baseUrl = env.getOrDefault("CHOICE_BANK_BASE_URL", "https://baas-pilot.choicebankapi.com");
        String senderId = env.getOrDefault("CHOICE_BANK_SENDER_ID", "VYCEIN");
        String privateKey = env.getOrDefault("CHOICE_BANK_PRIVATE_KEY", "");
        String endpoint = "staticData/getBankCodes";

        if (privateKey.isEmpty()) {
            System.out.println("WARNING: CHOICE_BANK_PRIVATE_KEY not set. Using a placeholder; API call will fail.");
            privateKey = "YOUR_PRIVATE_KEY_HERE";
        }

        // -------------------------------------------------------------------
        // Build request
        // -------------------------------------------------------------------
        String requestId = senderId + UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        String salt = generateHexSalt(6); // 12 hex chars

        String stringToSign = "locale=en_KE"
                + "&params={}"
                + "&requestId=" + requestId
                + "&salt=" + salt
                + "&sender=" + senderId
                + "&senderKey=" + privateKey
                + "&timestamp=" + timestamp;

        String signature = sha256Hex(stringToSign);

        String bodyJson = "{"
                + "\"requestId\":\"" + requestId + "\","
                + "\"sender\":\"" + senderId + "\","
                + "\"locale\":\"en_KE\","
                + "\"timestamp\":" + timestamp + ","
                + "\"salt\":\"" + salt + "\","
                + "\"signature\":\"" + signature + "\","
                + "\"params\":{}"
                + "}";

        String bodyPretty = "{\n"
                + "    \"requestId\": \"" + requestId + "\",\n"
                + "    \"sender\": \"" + senderId + "\",\n"
                + "    \"locale\": \"en_KE\",\n"
                + "    \"timestamp\": " + timestamp + ",\n"
                + "    \"salt\": \"" + salt + "\",\n"
                + "    \"signature\": \"" + signature + "\",\n"
                + "    \"params\": {}\n"
                + "}";

        String url = baseUrl + "/" + endpoint;
        String flatDisplay = stringToSign.replace("senderKey=" + privateKey, "senderKey=***REDACTED***");

        // -------------------------------------------------------------------
        // Output
        // -------------------------------------------------------------------
        System.out.println("==============================================");
        System.out.println("Choice Bank getBankCodes – CTO Test Pack");
        System.out.println("==============================================");
        System.out.println();
        System.out.println("Flattened string used for signature (senderKey redacted):");
        System.out.println(flatDisplay);
        System.out.println();
        System.out.println("URL: POST " + url);
        System.out.println();
        System.out.println("Request Body (raw JSON):");
        System.out.println(bodyPretty);
        System.out.println();
        System.out.println("==============================================");
        System.out.println("cURL (copy to Postman: Import → Raw text)");
        System.out.println("==============================================");
        System.out.println("curl -X POST \"" + url + "\" \\");
        System.out.println("  -H \"Content-Type: application/json\" \\");
        System.out.println("  -d '" + bodyJson + "'");
        System.out.println();

        // -------------------------------------------------------------------
        // API call
        // -------------------------------------------------------------------
        if (!noCall) {
            System.out.println("==============================================");
            System.out.println("API Response");
            System.out.println("==============================================");

            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(30_000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
                }

                int httpCode = conn.getResponseCode();
                String respBody;
                try (InputStream is = (httpCode >= 400) ? conn.getErrorStream() : conn.getInputStream()) {
                    respBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                System.out.println("HTTP Status: " + httpCode);
                System.out.println(prettyPrintJson(respBody));
                System.out.println();

                if (respBody.contains("\"code\":\"00000\"") || respBody.contains("\"code\": \"00000\"")) {
                    System.out.println("Result: SUCCESS (code 00000)");
                } else if (respBody.contains("\"code\":\"12004\"") || respBody.contains("\"code\": \"12004\"")) {
                    System.out.println("Result: INVALID SIGNATURE (code 12004)");
                } else {
                    System.out.println("Result: See response body above");
                }
            } catch (Exception e) {
                System.out.println("Request failed: " + e.getMessage());
                System.exit(1);
            }
        } else {
            System.out.println("Skipping API call (--no-call). Run without --no-call to hit the API.");
        }
    }

    private static String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String generateHexSalt(int numBytes) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String prettyPrintJson(String json) {
        // Simple indent-based pretty printer (no external dependencies)
        try {
            StringBuilder sb = new StringBuilder();
            int indent = 0;
            boolean inString = false;
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = !inString;
                    sb.append(c);
                } else if (!inString) {
                    switch (c) {
                        case '{': case '[':
                            sb.append(c).append('\n');
                            indent++;
                            sb.append("    ".repeat(indent));
                            break;
                        case '}': case ']':
                            sb.append('\n');
                            indent--;
                            sb.append("    ".repeat(indent)).append(c);
                            break;
                        case ',':
                            sb.append(c).append('\n').append("    ".repeat(indent));
                            break;
                        case ':':
                            sb.append(": ");
                            break;
                        default:
                            if (!Character.isWhitespace(c)) sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return json; // fallback to raw
        }
    }
}
