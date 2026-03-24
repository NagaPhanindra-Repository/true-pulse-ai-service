package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.BusinessImageGenerateRequest;
import com.codmer.turepulseai.model.BusinessImageGenerateResponse;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import com.codmer.turepulseai.repository.UserRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessImageServiceImpl implements BusinessImageService {

    private static final int DEFAULT_TOP_K = 6;
    private static final String DEFAULT_IMAGE_MODEL = "gpt-image-1";

    private final BusinessDocumentChunkRepository businessDocumentChunkRepository;
    private final UserRepository userRepository;
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

        User user = fetchUser();
        if (imageModel == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Image provider is not configured. Please set up the OpenAI image model.");
        }
        String businessId = user.getUserName();
        String displayName = request.getDisplayName().trim();
        String prompt = request.getPrompt().trim();

        List<String> contextChunks = getContextChunks(businessId, request.getEntityId(), displayName, prompt);
        String contextText = contextChunks.isEmpty()
                ? "No relevant business document context found."
                : String.join("\n\n", contextChunks);

        String revisedPrompt = buildImagePrompt(prompt, displayName, contextText);

        try {
            String normalizedSize = normalizeSize(request.getSize());
            int width = parseWidth(normalizedSize);
            int height = parseHeight(normalizedSize);

            ImagePrompt imagePrompt = new ImagePrompt(
                    revisedPrompt,
                    ImageOptionsBuilder.builder()
                            .width(width)
                            .height(height)
                            .build()
            );

            ImageResponse imageResponse = imageModel.call(imagePrompt);
            List<ImageGeneration> generations = (imageResponse != null) ? imageResponse.getResults() : null;
            if (generations == null || generations.isEmpty()) {
                return BusinessImageGenerateResponse.builder()
                        .success(false)
                        .error("Image provider returned empty response")
                        .revisedPrompt(revisedPrompt)
                        .businessId(businessId)
                        .entityId(request.getEntityId())
                        .displayName(displayName)
                        .build();
            }

            ImageGeneration generation = generations.get(0);
            Object rawOutput = generation.getOutput();
            log.debug("Image generation raw output type: {}", rawOutput != null ? rawOutput.getClass() : "null");
            String imageBase64 = extractImageBase64(rawOutput);
            if (imageBase64 == null || imageBase64.isBlank()) {
                log.warn("Unsupported image payload format for businessId={}, entityId={}, displayName={}, outputType={}",
                        businessId, request.getEntityId(), displayName,
                        rawOutput != null ? rawOutput.getClass() : "null");
                return BusinessImageGenerateResponse.builder()
                        .success(false)
                        .error("Image provider returned unsupported payload format")
                        .revisedPrompt(revisedPrompt)
                        .businessId(businessId)
                        .entityId(request.getEntityId())
                        .displayName(displayName)
                        .build();
            }

            return BusinessImageGenerateResponse.builder()
                    .success(true)
                    .mimeType("image/png")
                    .imageBase64(imageBase64)
                    .revisedPrompt(revisedPrompt)
                    .businessId(businessId)
                    .entityId(request.getEntityId())
                    .displayName(displayName)
                    .build();
        } catch (Exception ex) {
            log.error("Image generation failed: {}", ex.getMessage(), ex);
            return BusinessImageGenerateResponse.builder()
                    .success(false)
                    .error("Failed to generate image: " + ex.getMessage())
                    .revisedPrompt(revisedPrompt)
                    .businessId(businessId)
                    .entityId(request.getEntityId())
                    .displayName(displayName)
                    .build();
        }
    }

    private String normalizeSize(String size) {
        if (size == null || size.isBlank()) {
            return "1024x1024";
        }
        String normalized = size.trim().toLowerCase();
        if (normalized.matches("\\d+x\\d+")) {
            return normalized;
        }
        return "1024x1024";
    }

    private int parseWidth(String size) {
        try {
            return Integer.parseInt(size.split("x")[0]);
        } catch (Exception ignored) {
            return 1024;
        }
    }

    private int parseHeight(String size) {
        try {
            return Integer.parseInt(size.split("x")[1]);
        } catch (Exception ignored) {
            return 1024;
        }
    }

    private List<String> getContextChunks(String businessId, Long entityId, String displayName, String query) {
        try {
            float[] embedding = embeddingCacheService.embed(query);
            if (embedding == null || embedding.length == 0) {
                log.warn("Embedding service returned empty vector for query '{}'.", query);
                return List.of();
            }
            String embeddingLiteral = formatEmbeddingForPostgres(embedding);

            List<Object[]> rows = businessDocumentChunkRepository.findSimilarChunksByEntityAndBusiness(
                    businessId, entityId, displayName, embeddingLiteral, DEFAULT_TOP_K);

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

    private String buildImagePrompt(String userPrompt, String displayName, String contextText) {
        String system = "You are a world-class creative director for small business brand visuals. " +
                "Create a single high-quality image prompt using business context. Return only the final prompt text.";

        String user = "Business display name: " + displayName + "\n\n" +
                "Business context from documents:\n" + contextText + "\n\n" +
                "User intent:\n" + userPrompt + "\n\n" +
                "Generate one production-ready prompt for a photorealistic, brand-aligned image. " +
                "Include composition, lighting, color, style, and quality cues.";

        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(system),
                    new UserMessage(user)
            ));
            String generated = chatClient.prompt(prompt).call().content();
            if (generated != null && !generated.isBlank()) {
                return generated.trim();
            }
        } catch (Exception ex) {
            log.warn("Failed to refine image prompt with LLM, using fallback prompt: {}", ex.getMessage());
        }

        return "Create a premium marketing image for " + displayName + ": " + userPrompt;
    }

    private String formatEmbeddingForPostgres(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(embedding[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    private User fetchUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
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
}
