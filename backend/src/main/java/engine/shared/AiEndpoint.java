package engine.shared;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** Rules for the base URL of an OpenAI-compatible provider. */
public final class AiEndpoint {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    private AiEndpoint() {
    }

    /** The host of a URL in lower case, or empty when there is none. */
    public static String hostOf(String url) {
        try {
            String host = URI.create(url == null ? "" : url.strip()).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Checks a base URL: https, or http for this machine only.
     *
     * @return the URL without surrounding blanks
     * @throws ApiProblem 400 when the URL is not usable
     */
    public static String checked(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.strip();
        String host = hostOf(url);
        boolean https = url.toLowerCase(Locale.ROOT).startsWith("https://");
        boolean localHttp = url.toLowerCase(Locale.ROOT).startsWith("http://") && LOCAL_HOSTS.contains(host);
        if (host.isEmpty() || !(https || localHttp)) {
            throw new ApiProblem(400, "Base URL not allowed",
                    "The base URL must start with https:// (http:// only for localhost), for example"
                            + " https://generativelanguage.googleapis.com/v1beta/openai/");
        }
        return url;
    }
}
