package engine.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The AI keys of the engine: one Anthropic key and one key for an OpenAI-compatible provider (with
 * its base URL and model). With a keys file (ENGINE_KEYS_FILE, D-109) they are saved there, readable
 * by the owner of the engine process only, and read again at the next start; without one they live
 * in memory only. No table, log or project file ever holds them.
 */
@Component
public class SessionKey {

    static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    static final String GEMINI_HOST = "generativelanguage.googleapis.com";
    static final String DEFAULT_MODEL = "gpt-5.4-mini";
    private static final tools.jackson.databind.json.JsonMapper JSON =
            tools.jackson.databind.json.JsonMapper.builder().build();

    /** The OpenAI-compatible provider of the session. */
    public record Compatible(String apiKey, String baseUrl, String model) {
        @Override
        public String toString() {
            return "Compatible[baseUrl=" + baseUrl + ", model=" + model + ", key=***]";
        }
    }

    private final String defaultBaseUrl;
    private final String defaultModel;
    private volatile String anthropic;
    private volatile Compatible compatible;
    private volatile AiProvider preferred;
    private final Path file;
    /** Planning without a key: Claude Code with the sign-in of this machine, or null when off. */
    private final AiTarget signIn;

    /** What the keys file holds; a field is null when that key is not set. */
    record Saved(String anthropic, Compatible compatible, AiProvider preferred) {
    }

    @Autowired
    public SessionKey(
            @Value("${engine.openai.base-url:" + DEFAULT_BASE_URL + "}") String defaultBaseUrl,
            @Value("${engine.openai.model:" + DEFAULT_MODEL + "}") String defaultModel,
            @Value("${engine.keys.file:}") String keysFile,
            @Value("${engine.planning.sign-in:false}") boolean planningSignIn,
            @Value("${engine.execution.claude-bin:claude}") String claudeCommand,
            @Value("${engine.execution.model:fable}") String claudeModel) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.file = keysFile == null || keysFile.isBlank() ? null : Path.of(keysFile).toAbsolutePath();
        this.signIn = planningSignIn ? AiTarget.claudeSignIn(claudeCommand, claudeModel) : null;
        load();
    }

    /** Keys in the file (or in memory only when null), planning without a key as given. */
    public SessionKey(Path keysFile, boolean planningSignIn) {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL, keysFile == null ? "" : keysFile.toString(), planningSignIn,
                "claude", "fable");
    }

    /** Keys in memory only. */
    public SessionKey() {
        this(null, false);
    }

    /** Keys saved in the file and read from it. */
    public SessionKey(Path keysFile) {
        this(keysFile, false);
    }

    /** True when the keys are saved on this machine and outlive a restart of the engine. */
    public boolean saved() {
        return file != null;
    }

    private void load() {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            Saved saved = JSON.readValue(Files.readString(file, StandardCharsets.UTF_8), Saved.class);
            anthropic = saved.anthropic();
            compatible = saved.compatible();
            preferred = saved.preferred();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("The keys file " + file + " cannot be read as the engine wrote"
                    + " it. Delete the file and enter the keys again in the key panel.", e);
        }
    }

    /** Writes the keys to the file, which only the owner may read; without keys the file is removed. */
    private synchronized void save() {
        if (file == null) {
            return;
        }
        try {
            if (anthropic == null && compatible == null) {
                Files.deleteIfExists(file);
                return;
            }
            Files.createDirectories(file.getParent());
            Path next = file.resolveSibling(file.getFileName() + ".new");
            Files.deleteIfExists(next);
            Files.createFile(next, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")));
            Files.writeString(next, JSON.writeValueAsString(new Saved(anthropic, compatible, preferred)),
                    StandardCharsets.UTF_8);
            Files.move(next, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("The keys could not be saved in " + file
                    + ". Check that the folder can be written, or start the engine without"
                    + " ENGINE_KEYS_FILE to keep keys in memory only.", e);
        }
    }

    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public void setAnthropic(String apiKey) {
        anthropic = apiKey;
        save();
    }

    /** Sets the key of the compatible provider; a blank base URL or model is the default. */
    public void setCompatible(String apiKey, String baseUrl, String model) {
        String url = AiEndpoint.checked(baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl);
        String chosen = model == null || model.isBlank() ? defaultModel : model.strip();
        compatible = new Compatible(apiKey, url, chosen);
        save();
    }

    public void clear(AiProvider provider) {
        if (provider == AiProvider.ANTHROPIC) {
            anthropic = null;
        } else {
            compatible = null;
        }
        save();
    }

    public void clearAll() {
        anthropic = null;
        compatible = null;
        preferred = null;
        save();
    }

    /** The Anthropic key: used by the planning calls and by the builder. */
    public Optional<String> anthropicKey() {
        return Optional.ofNullable(anthropic);
    }

    /** The key of the compatible slot when it points at Google Gemini: the Gemini CLI builder uses it. */
    public Optional<String> geminiKey() {
        return Optional.ofNullable(compatible)
                .filter(c -> c.baseUrl().contains(GEMINI_HOST)).map(Compatible::apiKey);
    }

    public Optional<Compatible> compatible() {
        return Optional.ofNullable(compatible);
    }

    /** Chooses which provider plans when both keys are set. */
    public void choose(AiProvider provider) {
        preferred = provider;
        save();
    }

    /** True when planning can go through Claude Code with the sign-in of this machine (D-110). */
    public boolean signInOffered() {
        return signIn != null;
    }

    /**
     * The provider the planning calls use. Claude counts as available with its key or with the
     * sign-in of this machine; when Claude and the compatible provider are both available the
     * chosen one plans, Claude when nothing was chosen.
     */
    public Optional<AiProvider> active() {
        boolean hasClaude = anthropic != null || signIn != null;
        boolean hasCompatible = compatible != null;
        if (hasClaude && hasCompatible) {
            return Optional.of(preferred == null ? AiProvider.ANTHROPIC : preferred);
        }
        if (hasClaude) {
            return Optional.of(AiProvider.ANTHROPIC);
        }
        return hasCompatible ? Optional.of(AiProvider.OPENAI) : Optional.empty();
    }

    /**
     * What the next planning call talks to: the active provider; Claude without a key means Claude
     * Code with the sign-in of this machine (D-110). Empty when nothing can plan.
     */
    public Optional<AiTarget> planning() {
        String claude = anthropic;
        Compatible other = compatible;
        return active().map(provider -> {
            if (provider == AiProvider.OPENAI) {
                return new AiTarget(provider, other.apiKey(), other.baseUrl(), other.model());
            }
            return claude == null ? signIn : new AiTarget(provider, claude, null, AnthropicCall.MODEL);
        });
    }
}
