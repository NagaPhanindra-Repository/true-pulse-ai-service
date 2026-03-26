package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.BusinessImageGenerateRequest;
import com.codmer.turepulseai.model.BusinessImageGenerateResponse;
import com.codmer.turepulseai.model.BusinessImageGenerateResponse.OverlaySpec;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import com.codmer.turepulseai.service.BusinessImageService;
import com.codmer.turepulseai.service.EmbeddingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessImageServiceImpl implements BusinessImageService {

    private static final int DEFAULT_TOP_K = 6;
    private static final String TEXT_OVERLAY_HEADER = "TEXT_OVERLAYS";
    // We continue to generate overlay metadata, but the image itself must never contain readable text.
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("\"([^\"]+)\"|'([^']+)'");
    // Increase max overlay phrases so we can show brand, offer, occasion, CTA or key message
    private static final int MAX_OVERLAY_PHRASES = 4;
    private static final int MAX_OVERLAY_LENGTH = 64;
    // OpenAI image models currently enforce a maximum prompt length of 4000 characters.
    // We keep a safety margin so we never hit the hard limit even if the provider
    // counts bytes differently. This is ONLY for the image prompt string we send
    // to the ImageModel, not for the user-visible fields.
    private static final int MAX_IMAGE_PROMPT_CHARS = 3800;
    private static final List<String> KNOWN_OCCASIONS = List.of(
            "holi", "diwali", "eid", "christmas", "thanksgiving", "new year", "valentine's day", "navratri");
    private static final List<String> HERO_KEYWORDS = List.of(
            "biryani", "pizza", "coffee", "spa", "salon", "burger", "taco", "dessert", "festival", "offer");
    private static final Pattern ANY_TWO_OFFER_PATTERN = Pattern.compile("(?i)(any\\s+two\\s+[a-z\\s]+?\\s+\\$?\\d+)");
    private static final Pattern BUY_GET_PATTERN = Pattern.compile("(?i)buy\\s+\\d+\\s+get\\s+\\d+");
    private static final Pattern PERCENT_OFF_PATTERN = Pattern.compile("(?i)\\d+%\\s*(?:off|discount)");
    private static final Pattern PRICE_VALUE_PATTERN = Pattern.compile("(?i)(\\$\\s?\\d+|\\d+\\s?\\$)");
    private static final Pattern CTA_PATTERN = Pattern.compile("(?i)(order now|book now|shop now|visit today|call now)");

    private final BusinessDocumentChunkRepository businessDocumentChunkRepository;
    private final EmbeddingCacheService embeddingCacheService;
    private final ChatClient chatClient;
    private final ImageModel imageModel;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public BusinessImageGenerateResponse generate(BusinessImageGenerateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request is required");
        }
        if (request.getEntityId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId is required");
        }
        if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }

        if (imageModel == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Image provider is not configured. Please set up the OpenAI image model.");
        }

        Long entityId = request.getEntityId();
        String displayName = request.getDisplayName().trim();
        String prompt = request.getPrompt().trim();

        // Backend is now ALWAYS image-only: no readable text must be baked into the generated image.
        // We still compute rich overlay specs but the frontend is fully responsible for rendering text.
        IntentSummary intentSummary = analyzeIntent(prompt);

        List<String> overlayPhrases = buildOverlayPhrases(prompt, displayName, intentSummary);
        List<OverlaySpec> overlaySpecs = buildOverlaySpecs(displayName, intentSummary, overlayPhrases);

        List<String> contextChunks = getContextChunks(entityId, displayName, prompt);
        String contextText = contextChunks.isEmpty()
                ? "No relevant business document context found."
                : String.join("\n\n", contextChunks);

        // Always build a text-free image prompt that only describes visuals and explicitly forbids readable text.
        String rawPrompt = buildTextFreeImagePrompt(prompt, displayName, contextText, intentSummary);
        String revisedPrompt = ensurePromptWithinLimit(rawPrompt);

        try {
            String normalizedSize = normalizeSize(request.getSize());
            int width = parseDimension(normalizedSize, 0);
            int height = parseDimension(normalizedSize, 1);

            ImagePrompt imagePrompt = new ImagePrompt(
                    revisedPrompt,
                    ImageOptionsBuilder.builder()
                            .width(width)
                            .height(height)
                            .build()
            );

            ImageResponse imageResponse = imageModel.call(imagePrompt);
            List<ImageGeneration> generations = imageResponse.getResults();
            if (generations == null || generations.isEmpty()) {
                log.warn("Image provider returned empty result list for entityId={} displayName={}", request.getEntityId(), displayName);
                return BusinessImageGenerateResponse.builder()
                        .success(false)
                        .error("Image provider returned empty response")
                        .revisedPrompt(revisedPrompt)
                        .entityId(request.getEntityId())
                        .displayName(displayName)
                        .overlays(overlaySpecs)
                        .build();
            }

            ImageGeneration generation = generations.get(0);
            Object rawOutput = generation.getOutput();
            log.debug("Image generation raw output type: {}", rawOutput.getClass());
            String imageBase64 = extractImageBase64(rawOutput);
            if (imageBase64 == null || imageBase64.isBlank()) {
                log.warn("Unsupported image payload format for entityId={}, displayName={}, outputType={}",
                        request.getEntityId(), displayName, rawOutput.getClass());
                return BusinessImageGenerateResponse.builder()
                        .success(false)
                        .error("Image provider returned unsupported payload format")
                        .revisedPrompt(revisedPrompt)
                        .entityId(request.getEntityId())
                        .displayName(displayName)
                        .overlays(overlaySpecs)
                        .build();
            }

            return BusinessImageGenerateResponse.builder()
                    .success(true)
                    .mimeType("image/png")
                    .imageBase64(imageBase64)
                    .revisedPrompt(revisedPrompt)
                    .entityId(request.getEntityId())
                    .displayName(displayName)
                    .overlays(overlaySpecs)
                    .build();
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid image size requested for entityId={}: {}", request.getEntityId(), ex.getMessage());
            return BusinessImageGenerateResponse.builder()
                    .success(false)
                    .error("Invalid image size. Please use WIDTHxHEIGHT, e.g. 1024x1024.")
                    .revisedPrompt(revisedPrompt)
                    .entityId(request.getEntityId())
                    .displayName(displayName)
                    .overlays(overlaySpecs)
                    .build();
        } catch (Exception ex) {
            log.error("Image generation failed for entityId={}: {}", request.getEntityId(), ex.getMessage(), ex);
            return BusinessImageGenerateResponse.builder()
                    .success(false)
                    .error("Failed to generate image: " + ex.getMessage())
                    .revisedPrompt(revisedPrompt)
                    .entityId(request.getEntityId())
                    .displayName(displayName)
                    .overlays(overlaySpecs)
                    .build();
        }
    }

    private String normalizeSize(String size) {
        if (size == null || size.isBlank()) {
            return "1024x1024";
        }
        String normalized = size.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("\\d+x\\d+")) {
            return normalized;
        }
        log.warn("Unrecognized size format '{}', defaulting to 1024x1024", size);
        return "1024x1024";
    }

    private int parseDimension(String size, int index) {
        if (size == null || !size.contains("x")) {
            throw new IllegalArgumentException("size must be in WIDTHxHEIGHT format");
        }
        String[] parts = size.split("x");
        if (index < 0 || index >= parts.length) {
            throw new IllegalArgumentException("Invalid dimension index: " + index);
        }
        try {
            int value = Integer.parseInt(parts[index]);
            if (value <= 0) {
                throw new IllegalArgumentException("dimension must be positive, was: " + value);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric dimension in size: " + size, ex);
        }
    }

    private List<OverlaySpec> buildOverlaySpecs(String displayName,
                                                IntentSummary summary,
                                                List<String> overlayPhrases) {
        List<OverlaySpec> specs = new ArrayList<>();
        if (overlayPhrases == null || overlayPhrases.isEmpty()) {
            return specs;
        }
        int slot = 1;
        for (String phrase : overlayPhrases) {
            if (phrase == null || phrase.isBlank()) {
                continue;
            }
            String text = trimOverlayLength(normalizePriceInPhrase(phrase.trim()));
            if (text.isEmpty()) {
                continue;
            }

            boolean isBrand = displayName != null && text.equalsIgnoreCase(displayName.trim());
            boolean isOffer = summary != null && summary.offer != null && text.equalsIgnoreCase(summary.offer);
            boolean isOccasion = summary != null && summary.occasion != null &&
                    text.equalsIgnoreCase(("Celebrate " + summary.occasion).trim());

            String role;
            String zone;
            String size;
            String category;
            int priority;

            if (isBrand) {
                role = "brand";
                zone = "top-center";
                size = "large";
                category = "entityName";
                priority = 1;
            } else if (isOffer) {
                role = "offer";
                zone = "bottom-bar";
                size = "large";
                category = "offer";
                priority = 2;
            } else if (isOccasion) {
                role = "headline";
                zone = "upper-middle";
                size = "medium"; // avoid small fonts
                category = "event";
                priority = 2;
            } else {
                role = (slot == 1) ? "headline" : "details";
                zone = (slot == 1) ? "top-banner" : "bottom-right";
                // Previously non-primary overlays used "small"; to avoid
                // unreadable small fonts, keep everything at medium or larger.
                size = (slot == 1) ? "large" : "medium";
                // Heuristically classify remaining phrases using summary fields
                if (summary != null && summary.occasion != null && text.toLowerCase(Locale.ROOT).contains(summary.occasion.toLowerCase(Locale.ROOT))) {
                    category = "event";
                    priority = 2;
                } else if (CTA_PATTERN.matcher(text).find()) {
                    category = "cta";
                    priority = 3;
                } else {
                    category = "details";
                    priority = 4;
                }
            }

            specs.add(OverlaySpec.builder()
                    .slot(slot)
                    .role(role)
                    .text(text)
                    .zone(zone)
                    .size(size)
                    .category(category)
                    .priority(priority)
                    .build());
            slot++;
            if (slot > MAX_OVERLAY_PHRASES) {
                break;
            }
        }
        return specs;
    }


    private List<String> getContextChunks(Long entityId, String displayName, String query) {
        try {
            float[] embedding = embeddingCacheService.embed(query);
            if (embedding == null || embedding.length == 0) {
                log.warn("Embedding service returned empty vector for query '{}'.", query);
                return List.of();
            }
            String embeddingLiteral = formatEmbeddingForPostgres(embedding);

            List<Object[]> rows = businessDocumentChunkRepository.findSimilarChunksByEntity(
                    entityId, displayName, embeddingLiteral, DEFAULT_TOP_K);

            List<String> chunks = new ArrayList<>();
            for (Object[] row : rows) {
                if (row.length > 4 && row[4] != null) {
                    chunks.add((String) row[4]);
                }
            }
            return chunks;
        } catch (Exception ex) {
            log.warn("Could not fetch business document context for image generation: {}", ex.getMessage());
            return List.of();
        }
    }

    private String buildImagePrompt(String userPrompt, String displayName, String contextText,
                                    IntentSummary summary, List<String> overlayPhrases) {
        String system = "You are a world-class senior visual designer and art director for brand posters and campaigns. " +
                "You specialize in marketing visuals for small businesses, doctors, lawyers, politicians, business leaders, and celebrities. " +
                "Your job is to design launch and event posters, festival greetings, happy hour offers, and release announcements at the quality of top global brands. " +
                "You must produce a SINGLE, COMPACT image description plus a STRICT TEXT_OVERLAYS spec. " +
                "Define a single cohesive visual style for the entire poster, including both the main image and all text overlays. " +
                "Text overlays must visually belong to the same design as the main image: they must share the same overall palette family, tone, and shape language while remaining highly readable. " +
                "Use palette-aware background panels for text (sampled from or harmonizing with the main image colors) while preserving very high contrast between text and background. " +
                "Avoid generic flat black or white rectangles for text unless the entire design is intentionally minimalist or monochrome; instead, derive panel colors from the main palette (for Holi or festival scenes, use vibrant, playful tones with soft, rounded shapes). " +
                "Use the provided business or personal context as the primary reference for tone, audience, and details. " +
                "Do NOT draw or spell out overlay phrases anywhere except inside the TEXT_OVERLAYS block. " +
                "Keep copy concise, perfectly spelled, and ensure overlays never collide or overlap. " +
                "Never invent slogans, prices, offers, names, or titles beyond what the user or business already supplied. " +
                "All readable text in the final image must come only from the TEXT_OVERLAYS section; do not add any other readable words, labels, or slogans in the scene.\n" +
                "- Any additional signs, labels, or packaging must be non-readable shapes or abstract marks only, not real words.\n" +
                "- Each overlay must sit on a solid or softly gradient, opaque background panel with very high contrast between text and background, using simple, clean sans-serif typography that matches the overall poster mood (festive, formal, clinical, luxury, etc.).\n" +
                "- Respect the reputation of professionals, celebrities, and public figures; keep visuals tasteful and brand-safe.";

        String intentSummaryBlock = describeIntentSummary(summary, displayName);

        // Normalize overlay phrases and cap list size/length here as a first guardrail
        List<String> safeOverlays = new ArrayList<>();
        if (overlayPhrases != null) {
            for (String phrase : overlayPhrases) {
                if (phrase == null) {
                    continue;
                }
                String normalized = normalizePriceInPhrase(phrase.trim());
                String trimmed = trimOverlayLength(normalized);
                if (!trimmed.isEmpty()) {
                    safeOverlays.add(trimmed);
                }
                if (safeOverlays.size() >= MAX_OVERLAY_PHRASES) {
                    break;
                }
            }
        }

        StringBuilder overlayBlock = new StringBuilder();
        if (safeOverlays.isEmpty()) {
            overlayBlock.append("- slot: 1 | role: headline | text: (infer a 2–4 word headline from the occasion/intent) | zone: top banner | size: large | style: bold sans-serif, simple clean font, solid or softly gradient background band in a color harmonizing with the main scene but adjusted for very high contrast, generous padding, rounded or softly rounded corners that match the overall poster shape language, absolutely avoid pure black bars unless the entire design is minimalist, no overlap with other text or key visuals\n");
        } else {
            int slot = 1;
            for (String phrase : safeOverlays) {
                boolean isBrand = phrase.equalsIgnoreCase(displayName);
                boolean isOffer = summary != null && summary.offer != null && phrase.equalsIgnoreCase(summary.offer);
                boolean isOccasion = summary != null && summary.occasion != null &&
                        phrase.equalsIgnoreCase(("Celebrate " + summary.occasion).trim());

                String role;
                String zone;
                String size;
                String style;

                if (isBrand) {
                    role = "brand";
                    zone = "top center";
                    size = "large";
                    style = "bold sans-serif, clean logo-style wordmark on a solid or softly gradient high-contrast band that matches the poster’s overall color palette (avoid plain black unless the whole design is minimalist), generous padding, visually integrated with the main scene, no overlap with other text";
                } else if (isOffer) {
                    role = "offer";
                    zone = "bottom bar";
                    size = "large";
                    style = "bold sans-serif, simple clean font, solid or softly gradient background band in a color harmonizing with the main scene but darkened or lightened for very high contrast, generous padding, no overlap, keep entire phrase on a single line, do not split numbers from the product name or currency, shapes (rounded, pill-shaped, or angular) must match the overall poster style and occasion mood";
                } else if (isOccasion) {
                    role = "headline";
                    zone = "upper middle";
                    size = "medium"; // avoid small fonts for occasion
                    style = "bold sans-serif, simple clean font, solid or softly gradient background band over the color splashes using a tone derived from the scene palette, very high contrast, generous padding, shapes consistent with the overall festive, playful design (avoid rigid black strips), no overlap";
                } else {
                    role = (slot == 1) ? "headline" : "details";
                    zone = (slot == 1) ? "top banner" : "bottom right";
                    // Do not allow small fonts; keep supporting details at least medium.
                    size = (slot == 1) ? "large" : "medium";
                    style = "clean sans-serif, solid or softly gradient background band in a color sampled from or harmonizing with the main image palette (not pure black unless the design is minimalist), high contrast, generous padding, shapes that follow the same rounded or angular language as the poster, no overlap";
                }

                overlayBlock.append("- slot: ")
                        .append(slot)
                        .append(" | role: ")
                        .append(role)
                        .append(" | text: \"")
                        .append(phrase.replace("\"", "'"))
                        .append("\" | zone: ")
                        .append(zone)
                        .append(" | size: ")
                        .append(size)
                        .append(" | style: ")
                        .append(style)
                        .append('\n');
                slot++;
            }
        }

        String user = "Business or personality display name: " + displayName + "\n\n" +
                "Who this poster is for (examples: small business, doctor, lawyer, politician, business leader, celebrity):\n" +
                "Infer from the context and occasion and design in a respectful, professional way.\n\n" +
                "Intent summary (what this image is for):\n" + intentSummaryBlock + "\n\n" +
                "Business / personal context from uploaded documents (this describes their brand, services, style, or profile; align with it carefully):\n" + contextText + "\n\n" +
                "User prompt (for reference only, do not restate word-for-word):\n" + userPrompt + "\n\n" +
                "You must respond in two clearly separated parts:\n" +
                "1) IMAGE_DESCRIPTION: A single paragraph (maximum 80 words) describing the visual scene only. " +
                "Design this as a best-in-class poster or campaign visual for the given person or business. " +
                "Do NOT write any overlay phrases or other long copy into this section. Focus on composition, mood, colors, lighting, and layout regions that will remain visually clean around text zones. " +
                "The color palette, mood, and overall shape language you choose here must also guide the look of the text overlays so that everything feels like one cohesive design.\n" +
                "2) " + TEXT_OVERLAY_HEADER + ": A markdown-style list where each line describes exactly ONE overlay phrase, its layout zone, and styling, " +
                "using the following schema: '- slot: <number> | role: <headline/offer/details> | text: \"<exact phrase>\" | zone: <location> | size: <small/medium/large> | style: <short styling hints>'.\n\n" +
                "Overlay phrases to render exactly (no extra slogans, prices, offers, wishes, or event details beyond this list):\n" +
                overlayBlock +
                "\nCRITICAL RULES:\n" +
                "- The background panels, text colors, and shapes of overlays must match the overall poster mood and color palette from IMAGE_DESCRIPTION (festive vs formal vs clinical vs luxury). For festive occasions like Holi, use vibrant, harmonious colors and softer, playful shapes; avoid harsh, generic black boxes.\n" +
                "- Avoid generic black or white bars for text unless the entire poster style is intentionally minimalist or monochrome; instead, derive overlay panel colors from the main image palette while still ensuring very high legibility.\n" +
                "- Never repeat an overlay phrase more than once in the entire response.\n" +
                "- Do NOT embed overlay phrases inside IMAGE_DESCRIPTION; only describe the scene.\n" +
                "- Ensure each overlay has its own negative space and does not touch or overlap another.\n" +
                "- Reserve enough clear, uncluttered background behind every text overlay for strong legibility.\n" +
                "- Avoid handwritten/cursive unless explicitly requested; use clean, professional type.\n" +
                "- No lorem ipsum, random characters, or misspellings. All words must be correctly spelled in clear English.\n" +
                "- Do not invent new discounts, prices, taglines, names, or titles.\n" +
                "- All readable text in the final image must come only from the TEXT_OVERLAYS section; do not add any other readable words, labels, or slogans in the scene.\n" +
                "- Any additional signs, labels, or packaging must be non-readable shapes or abstract marks only, not real words.\n" +
                "- Each overlay must sit on a solid, opaque background panel with very high contrast between text and background, using simple, clean sans-serif typography.\n" +
                "- Respect the reputation of professionals, celebrities, and public figures; keep visuals tasteful and brand-safe.";

        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(system),
                    new UserMessage(user)
            ));
            String generated = chatClient.prompt(prompt).call().content();
            if (generated != null && !generated.isBlank()) {
                // route through brand/offer-aware integrity helper
                return enforceTextOverlayIntegrity(generated.trim(), overlayPhrases, displayName, summary);
            }
        } catch (Exception ex) {
            log.warn("Failed to refine image prompt with LLM, using fallback prompt: {}", ex.getMessage());
        }

        // Fallback: keep the same structure but build it locally to avoid duplicate phrases
        StringBuilder fallback = new StringBuilder();
        fallback.append("IMAGE_DESCRIPTION: Create a premium, world-class marketing image for ")
                .append(displayName)
                .append(" featuring ")
                .append(defaultText(summary != null ? summary.occasion : null, "the event"))
                .append(". Keep visuals modern, brand-safe, and professional, with clear negative space reserved for text at the top banner and bottom bar, both on solid-color panels for maximum readability. Use a single cohesive color palette and shape language for both the main scene and the text overlays so they feel designed together.\n\n");
        fallback.append(TEXT_OVERLAY_HEADER).append(":\n");
        if (safeOverlays.isEmpty()) {
            fallback.append("- slot: 1 | role: headline | text: \"Celebrate Together\" | zone: top banner | size: large | style: bold sans-serif, simple clean font, solid background band in a color harmonizing with the main image palette but adjusted for very high contrast, generous padding, shapes that match the overall poster style, no overlap with other text or key visuals\n");
        } else {
            int slot = 1;
            for (String phrase : safeOverlays) {
                String role = slot == 1 ? "headline" : "offer";
                String zone = slot == 1 ? "top banner" : "bottom bar";
                String style = slot == 1
                        ? "bold sans-serif, simple clean font, solid background band in a color sampled from or harmonizing with the main scene, very high contrast, generous padding, shapes consistent with the poster’s overall design, no overlap"
                        : "bold sans-serif, simple clean font, solid background band in a color harmonizing with the main scene but darkened or lightened for high contrast, generous padding, shapes matching the poster’s style, no overlap, keep entire phrase on a single line, do not split numbers from the product name or currency";
                fallback.append("- slot: ")
                        .append(slot)
                        .append(" | role: ")
                        .append(role)
                        .append(" | text: \"")
                        .append(phrase.replace("\"", "'"))
                        .append("\" | zone: ")
                        .append(zone)
                        .append(" | size: ")
                        .append("large")
                        .append(" | style: ")
                        .append(style)
                        .append('\n');
                slot++;
                if (slot > MAX_OVERLAY_PHRASES) {
                    break;
                }
            }
        }
        return fallback.toString().trim();
    }

    /**
     * Ensure the final image prompt we send to the provider is safely below the
     * known maximum length. The earlier implementation tried to trim a
     * non-existent "BUSINESS / DOCUMENT CONTEXT" marker, which meant some
     * prompts were never shortened and could exceed the provider's 4000-char
     * hard limit when business document context was long.
     *
     * We now treat the prompt as two conceptual segments: a short "system"
     * preamble and a longer "user/context" body. We split on the first blank
     * line and trim only the trailing body portion, preserving the critical
     * system instructions while guaranteeing that the final string is always
     * <= MAX_IMAGE_PROMPT_CHARS.
     */
    private String ensurePromptWithinLimit(String prompt) {
        if (prompt == null) {
            return null;
        }
        if (prompt.length() <= MAX_IMAGE_PROMPT_CHARS) {
            return prompt;
        }

        // Try to preserve the initial system instructions block (before first
        // double newline) and trim only the rest.
        String separator = "\n\n";
        int sepIndex = prompt.indexOf(separator);
        if (sepIndex <= 0) {
            // No clear split between system and user, just trim at a safe
            // boundary and avoid cutting mid-line.
            String shortened = prompt.substring(0, MAX_IMAGE_PROMPT_CHARS);
            int lastNewline = shortened.lastIndexOf('\n');
            if (lastNewline > 0) {
                shortened = shortened.substring(0, lastNewline);
            }
            return shortened.trim();
        }

        String systemPart = prompt.substring(0, sepIndex + separator.length());
        String bodyPart = prompt.substring(sepIndex + separator.length());

        if (systemPart.length() >= MAX_IMAGE_PROMPT_CHARS) {
            // Extremely unlikely – fall back to hard trim of the whole prompt.
            String shortened = systemPart.substring(0, MAX_IMAGE_PROMPT_CHARS);
            int lastNewline = shortened.lastIndexOf('\n');
            if (lastNewline > 0) {
                shortened = shortened.substring(0, lastNewline);
            }
            return shortened.trim();
        }

        int remaining = MAX_IMAGE_PROMPT_CHARS - systemPart.length();
        if (bodyPart.length() <= remaining) {
            return (systemPart + bodyPart).trim();
        }

        String shortenedBody = bodyPart.substring(0, remaining);
        int lastNewline = shortenedBody.lastIndexOf('\n');
        if (lastNewline > 0) {
            shortenedBody = shortenedBody.substring(0, lastNewline);
        }
        return (systemPart + shortenedBody).trim();
    }

    private String buildTextFreeImagePrompt(String userPrompt,
                                            String displayName,
                                            String contextText,
                                            IntentSummary summary) {
        String system = "You are a world-class senior visual designer and art director who creates campaign and event posters for all kinds of entities: " +
                "restaurants, doctors, lawyers, business leaders, politicians, celebrities, content creators and small businesses. " +
                "You design top-tier, brand-safe visuals for launches, releases, awareness drives, offers, seasonal greetings, " +
                "upcoming events, rallies, concerts, movie or album launches, product announcements and professional campaigns.\n" +
                "YOUR OUTPUT WILL BE USED ONLY AS AN IMAGE BACKGROUND. THE FRONTEND WILL RENDER ALL TEXT AS OVERLAYS.\n" +
                "Therefore, the generated image MUST NOT contain any readable text, lettering, numbers, signs, logos or labels.\n" +
                "- Do NOT draw brand names, offers, dates, prices, slogans, or any other readable words in the scene.\n" +
                "- Any boards, banners, packaging, shop signs, posters, or documents in the image must be blank, abstract, or contain only non-readable squiggles and shapes.\n" +
                "- Focus entirely on composition, lighting, subject placement, colors, and atmosphere so that text can be cleanly added later on top.\n" +
                "- Leave generous negative space where text overlays can later sit (top banner, center focus, bottom bar, side panels), but keep that space visually integrated (subtle gradients, texture, bokeh, or soft shapes).\n" +
                "- Respect the tone and profession: clinical and trustworthy for doctors, authoritative yet approachable for lawyers and politicians, vibrant and appetizing for restaurants, aspirational and stylish for celebrities and product launches, etc.\n" +
                "- Always design at the quality level of global brand campaigns.\n" +
                "- Keep faces, key dishes, venues, or hero objects clearly visible and not covered by imaginary text.\n" +
                "- Never place fake UI elements, chat bubbles, or phone screenshots unless explicitly requested by the user.\n" +
                "- The image should be self-explanatory about the theme (festival, sale, health camp, rally, concert, launch, etc.) even without any text.\n";

        String intentSummaryBlock = describeIntentSummary(summary, displayName);

        StringBuilder user = new StringBuilder();
        user.append("USER PROMPT (raw request): \n").append(userPrompt).append("\n\n");
        user.append("ENTITY / BRAND DISPLAY NAME: ").append(displayName).append("\n\n");
        user.append("INTENT SUMMARY (interpreted goal of the poster): \n")
                .append(intentSummaryBlock).append("\n\n");
        user.append("BUSINESS / DOCUMENT CONTEXT (for visual tone and authenticity only, NOT for direct text):\n")
                .append(contextText).append("\n\n");
        user.append("IMAGE REQUIREMENTS (IMPORTANT):\n");
        user.append("- Generate a single, high-quality poster-style image.\n");
        user.append("- Absolutely no readable text, numbers, or logos anywhere in the image.\n");
        user.append("- Clearly communicate the kind of event, campaign, or message visually (e.g., restaurant Holi offer, doctor health camp, politician rally, product launch).\n");
        user.append("- Include clear focal areas where future text overlays from the frontend can be placed without covering critical faces or hero objects.\n");
        user.append("- Use lighting, depth of field, props, and color palette to match the intent and profession.\n");
        user.append("- Design as if you are a senior poster designer working for the best brands in the world.\n");

        // We only need to return a single combined textual prompt string for the ImageModel.
        // The Spring AI ImageModel will take this description and generate a text-free image.
        return (system + "\n\n" + user).trim();
    }

    private List<String> buildOverlayPhrases(String userPrompt, String displayName, IntentSummary summary) {
        java.util.Set<String> overlays = new java.util.LinkedHashSet<>();

        if (displayName != null && !displayName.trim().isEmpty()) {
            overlays.add(displayName.trim());
        }

        if (summary != null) {
            if (summary.offer != null) {
                overlays.add(summary.offer);
            }
            if (summary.occasion != null) {
                overlays.add("Celebrate " + summary.occasion);
            }
            if (summary.cta != null) {
                overlays.add(summary.cta);
            }
        }

        if (overlays.isEmpty()) {
            overlays.addAll(extractCopyPhrases(userPrompt));
        }

        List<String> result = new ArrayList<>();
        for (String phrase : overlays) {
            if (phrase == null || phrase.isBlank()) {
                continue;
            }
            String normalizedPhrase = normalizePriceInPhrase(phrase.trim());
            String trimmed = trimOverlayLength(normalizedPhrase);
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
            if (result.size() >= MAX_OVERLAY_PHRASES) {
                break;
            }
        }
        return result;
    }

    // Simple normalizer that reuses currency tidy logic; keeps signature local to this class
    private String normalizePriceInPhrase(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return phrase;
        }
        Matcher m = PRICE_VALUE_PATTERN.matcher(phrase);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            // Use group(0), the entire match, to avoid IndexOutOfBounds when
            // the pattern does not define additional capturing groups.
            String matched = m.group(0);
            String tidy = tidyCurrency(matched);
            // Quote the replacement to avoid treating $ and \\ as special
            // replacement characters.
            m.appendReplacement(sb, Matcher.quoteReplacement(tidy));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String formatEmbeddingForPostgres(float[] embedding) {
        // Format embedding as Postgres vector literal: '[v1,v2,...]'
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private List<String> extractCopyPhrases(String userPrompt) {
        List<String> phrases = new ArrayList<>();
        if (userPrompt == null || userPrompt.isBlank()) {
            return phrases;
        }
        Matcher m = QUOTED_TEXT_PATTERN.matcher(userPrompt);
        while (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String val = g1 != null ? g1 : g2;
            if (val != null && !val.isBlank()) {
                phrases.add(val.trim());
            }
        }
        if (phrases.isEmpty()) {
            phrases.add(userPrompt.trim());
        }
        return phrases;
    }

    private boolean containsIgnoreCase(String text, String search) {
        if (text == null || search == null || search.isBlank()) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private String enforceTextOverlayIntegrity(String prompt,
                                               List<String> copyPhrases,
                                               String displayName,
                                               IntentSummary summary) {
        if (prompt == null || prompt.isBlank()) {
            return prompt;
        }

        final List<String> phrases = (copyPhrases == null) ? List.of() : copyPhrases;
        String normalized = prompt.trim();

        // Ensure we have a TEXT_OVERLAYS section
        boolean hasOverlaySection = normalized.toUpperCase(Locale.ROOT).contains(TEXT_OVERLAY_HEADER);

        // Remove accidental duplicate raw phrases from IMAGE_DESCRIPTION by lightly de-duplicating
        for (String phrase : phrases) {
            if (phrase == null || phrase.isBlank()) {
                continue;
            }
            String trimmed = trimOverlayLength(phrase.trim());
            if (trimmed.isEmpty()) {
                continue;
            }
            int firstIndex = indexOfIgnoreCase(normalized, trimmed);
            if (firstIndex >= 0) {
                int nextIndex = indexOfIgnoreCase(normalized, trimmed, firstIndex + trimmed.length());
                while (nextIndex >= 0) {
                    normalized = replaceRangeIgnoreCase(normalized, trimmed, nextIndex, "the main offer");
                    nextIndex = indexOfIgnoreCase(normalized, trimmed, nextIndex + "the main offer".length());
                }
            }
        }

        final String finalNormalized = normalized;
        boolean missingCopy = phrases.stream()
                .anyMatch(phrase -> phrase != null && !phrase.isBlank() && !containsIgnoreCase(finalNormalized, phrase));

        if (!hasOverlaySection || missingCopy) {
            StringBuilder builder = new StringBuilder(finalNormalized);
            if (!hasOverlaySection) {
                builder.append("\n\n").append(TEXT_OVERLAY_HEADER).append(":\n");
            } else {
                builder.append("\n");
            }

            int slot = 1;
            for (String phrase : phrases) {
                if (phrase == null || phrase.isBlank()) {
                    continue;
                }
                String normalizedPhrase = normalizePriceInPhrase(phrase.trim());
                String trimmed = trimOverlayLength(normalizedPhrase);
                if (trimmed.isEmpty()) {
                    continue;
                }

                boolean isBrand = displayName != null && trimmed.equalsIgnoreCase(displayName.trim());
                boolean isOffer = summary != null && summary.offer != null && trimmed.equalsIgnoreCase(summary.offer);
                boolean isOccasion = summary != null && summary.occasion != null &&
                        trimmed.equalsIgnoreCase(("Celebrate " + summary.occasion).trim());

                String role;
                String zone;
                String size = "large";
                String style;

                if (isBrand) {
                    role = "brand";
                    zone = "top banner";
                    style = "bold sans-serif, clean logo-style wordmark on a solid or softly gradient high-contrast band that matches the poster’s overall color palette (avoid plain black unless the whole design is minimalist), generous padding, visually integrated with the main scene, no overlap with other text";
                } else if (isOffer) {
                    role = "offer";
                    zone = "bottom bar";
                    style = "bold sans-serif, simple clean font, solid or softly gradient background band in a color harmonizing with the main scene but darkened or lightened for very high contrast, generous padding, no overlap, keep entire phrase on a single line, do not split numbers from the product name or currency, shapes (rounded, pill-shaped, or angular) must match the overall poster style";
                } else if (isOccasion) {
                    role = "headline";
                    zone = "upper middle";
                    size = "medium"; // ensure occasion is not too small
                    style = "bold sans-serif, simple clean font, solid or softly gradient background band over the color splashes using a tone derived from the scene palette, very high contrast, generous padding, shapes consistent with the overall festive, playful design (avoid rigid black strips), no overlap";
                } else {
                    role = (slot == 1) ? "headline" : "details";
                    zone = (slot == 1) ? "top banner" : "bottom right";
                    // Avoid small fonts for any auto-inserted overlay.
                    if (!"large".equals(size)) {
                        size = "medium";
                    }
                    style = "clean sans-serif, solid or softly gradient background band in a color sampled from or harmonizing with the main image palette (not pure black unless the design is minimalist), high contrast, generous padding, shapes that follow the same rounded or angular language as the poster, no overlap";
                }

                builder.append("- slot: ")
                        .append(slot)
                        .append(" | role: ")
                        .append(role)
                        .append(" | text: \"")
                        .append(trimmed.replace("\"", "'"))
                        .append("\" | zone: ")
                        .append(zone)
                        .append(" | size: ")
                        .append(size)
                        .append(" | style: ")
                        .append(style)
                        .append('\n');
                slot++;
                if (slot > MAX_OVERLAY_PHRASES) {
                    break;
                }
            }
            builder.append("Ensure each overlay has its own safe margins and readable contrast, and that overlays are the only readable text in the image; all other shapes must be non-readable graphics.");
            return builder.toString();
        }

        return finalNormalized;
    }

    // Helper to find substring ignoring case
    private int indexOfIgnoreCase(String text, String search) {
        return indexOfIgnoreCase(text, search, 0);
    }

    private int indexOfIgnoreCase(String text, String search, int fromIndex) {
        if (text == null || search == null) {
            return -1;
        }
        final int textLen = text.length();
        final int searchLen = search.length();
        if (searchLen == 0 || fromIndex >= textLen) {
            return -1;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerSearch = search.toLowerCase(Locale.ROOT);
        return lowerText.indexOf(lowerSearch, fromIndex);
    }

    // Helper to replace one occurrence of substring ignoring case at a known index
    private String replaceRangeIgnoreCase(String text, String original, int index, String replacement) {
        if (index < 0 || index + original.length() > text.length()) {
            return text;
        }
        return text.substring(0, index) + replacement + text.substring(index + original.length());
    }

    private String extractImageBase64(Object output) {
        if (output == null) {
            return null;
        }
        if (output instanceof byte[] data) {
            log.debug("Extracting base64 from byte[] payload ({} bytes)", data.length);
            return Base64.getEncoder().encodeToString(data);
        }
        if (output instanceof Image image) {
            String base64 = image.getB64Json();
            if (base64 != null && !base64.isBlank()) {
                log.debug("Extracted base64 from Image#getB64Json()");
                return base64;
            }
            String url = image.getUrl();
            if (url != null && !url.isBlank()) {
                log.debug("Attempting to download image from URL: {}", url);
                return downloadImageAsBase64(url);
            }
        }
        if (output instanceof List<?> list && !list.isEmpty()) {
            log.debug("Image output is list with {} entries, attempting recursive extraction", list.size());
            for (Object item : list) {
                String base64 = extractImageBase64(item);
                if (base64 != null) {
                    return base64;
                }
            }
        }
        log.warn("Unsupported image output type encountered: {}", output.getClass());
        return null;
    }

    private String downloadImageAsBase64(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                byte[] body = response.body();
                if (body != null && body.length > 0) {
                    log.debug("Downloaded {} bytes from image URL", body.length);
                    return Base64.getEncoder().encodeToString(body);
                }
            } else {
                log.warn("Failed to download image. Status: {} URL: {}", response.statusCode(), url);
            }
        } catch (Exception ex) {
            log.error("Error downloading image from URL {}: {}", url, ex.getMessage());
        }
        return null;
    }

    private IntentSummary analyzeIntent(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return new IntentSummary(null, null, null, null);
        }
        String lower = userPrompt.toLowerCase(Locale.ROOT);
        String occasion = KNOWN_OCCASIONS.stream()
                .filter(lower::contains)
                .findFirst()
                .map(this::capitalizeWords)
                .orElse(null);
        String offer = detectOffer(userPrompt);
        String hero = detectHeroProduct(lower);
        String cta = detectCta(userPrompt, offer != null);
        return new IntentSummary(occasion, offer, hero, cta);
    }

    // Limit overlay phrase length so overlays stay short and readable.
    private String trimOverlayLength(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= MAX_OVERLAY_LENGTH) {
            return trimmed;
        }
        int cut = trimmed.lastIndexOf(' ', MAX_OVERLAY_LENGTH);
        if (cut <= 0) {
            cut = MAX_OVERLAY_LENGTH;
        }
        return trimmed.substring(0, cut).trim();
    }

    // Normalize simple currency formats like "20$" -> "$20" and "$ 20" -> "$20".
    private String tidyCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String value = raw.trim();
        if (value.matches("(?i)\\d+\\s*\\$")) {
            String digits = value.replaceAll("[^0-9]", "");
            return "$" + digits;
        }
        if (value.matches("(?i)\\$\\s*\\d+")) {
            String digits = value.replaceAll("[^0-9]", "");
            return "$" + digits;
        }
        return value;
    }

    // Clean and title-case an offer phrase while preserving numbers and currency.
    private String tidyOffer(String rawOffer) {
        if (rawOffer == null || rawOffer.isBlank()) {
            return null;
        }
        String normalized = normalizePriceInPhrase(rawOffer.trim());
        return capitalizeWords(normalized);
    }

    // Simple word-wise capitalization helper.
    private String capitalizeWords(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        boolean startOfWord = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                startOfWord = true;
                sb.append(c);
            } else if (startOfWord) {
                sb.append(Character.toTitleCase(c));
                startOfWord = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString().trim();
    }

    // Heuristic hero-product detector using the known keyword list.
    private String detectHeroProduct(String lowerPrompt) {
        if (lowerPrompt == null || lowerPrompt.isBlank()) {
            return null;
        }
        for (String keyword : HERO_KEYWORDS) {
            if (lowerPrompt.contains(keyword)) {
                return capitalizeWords(keyword);
            }
        }
        return null;
    }

    // Helper to provide a fallback string when a value is missing.
    private String defaultText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String detectOffer(String userPrompt) {
        if (userPrompt == null) {
            return null;
        }
        // First, try to capture combined multi-part offers like
        // "buy any two biryani for 20$ get a free drink" as a single phrase.
        String lower = userPrompt.toLowerCase(Locale.ROOT);
        int buyIdx = lower.indexOf("buy any two");
        if (buyIdx >= 0) {
            // Heuristic: take from "buy any two" up to the next sentence boundary
            // or end of string, then tidy currency and capitalization.
            int endIdx = lower.indexOf('.', buyIdx);
            if (endIdx < 0) {
                endIdx = lower.indexOf('!', buyIdx);
            }
            if (endIdx < 0) {
                endIdx = lower.indexOf('?', buyIdx);
            }
            if (endIdx < 0) {
                endIdx = userPrompt.length();
            }
            String span = userPrompt.substring(buyIdx, endIdx).trim();
            if (!span.isEmpty()) {
                return tidyOffer(span);
            }
        }

        Matcher anyTwo = ANY_TWO_OFFER_PATTERN.matcher(userPrompt);
        if (anyTwo.find()) {
            return tidyOffer(anyTwo.group(1));
        }
        Matcher buyGet = BUY_GET_PATTERN.matcher(userPrompt);
        if (buyGet.find()) {
            return capitalizeWords(buyGet.group());
        }
        Matcher percent = PERCENT_OFF_PATTERN.matcher(userPrompt);
        if (percent.find()) {
            return percent.group().toUpperCase(Locale.ROOT);
        }
        Matcher price = PRICE_VALUE_PATTERN.matcher(userPrompt);
        if (price.find()) {
            return "Only " + tidyCurrency(price.group());
        }
        return null;
    }

    private String detectCta(String userPrompt, boolean hasOffer) {
        if (userPrompt != null) {
            Matcher matcher = CTA_PATTERN.matcher(userPrompt);
            if (matcher.find()) {
                return capitalizeWords(matcher.group(1));
            }
        }
        // Do not auto-inject a CTA; only return one when explicitly present in the prompt.
        return null;
    }

    private String describeIntentSummary(IntentSummary summary, String displayName) {
        if (summary == null && (displayName == null || displayName.isBlank())) {
            return "No explicit occasion or offer detected; design a general but professional poster based on the user prompt.";
        }
        StringBuilder sb = new StringBuilder();
        if (summary != null && summary.occasion != null && !summary.occasion.isBlank()) {
            sb.append("Occasion: ").append(summary.occasion).append(". ");
        }
        if (displayName != null && !displayName.isBlank()) {
            sb.append("Business name: ").append(displayName.trim()).append(". ");
        }
        if (summary != null && summary.offer != null && !summary.offer.isBlank()) {
            sb.append("Offer: ").append(summary.offer).append(". ");
        }
        if (summary != null && summary.heroProduct != null && !summary.heroProduct.isBlank()) {
            sb.append("Hero focus: ").append(summary.heroProduct).append(". ");
        }
        if (summary != null && summary.cta != null && !summary.cta.isBlank()) {
            sb.append("Call to action: ").append(summary.cta).append('.');
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            return "General promotional or greeting poster; keep layout clean, modern, and brand-safe.";
        }
        return result;
    }

    private static class IntentSummary {
        private final String occasion;
        private final String offer;
        private final String heroProduct;
        private final String cta;

        private IntentSummary(String occasion, String offer, String heroProduct, String cta) {
            this.occasion = occasion;
            this.offer = offer;
            this.heroProduct = heroProduct;
            this.cta = cta;
        }
    }
}
