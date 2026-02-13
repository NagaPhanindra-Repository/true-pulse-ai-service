package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.BusinessDocument;
import com.codmer.turepulseai.entity.BusinessDocumentChunk;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.DocumentSearchRequest;
import com.codmer.turepulseai.model.DocumentSearchResponse;
import com.codmer.turepulseai.model.DocumentUploadResponse;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import com.codmer.turepulseai.repository.BusinessDocumentRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.BusinessDocumentService;
import com.codmer.turepulseai.service.EmbeddingCacheService;
import com.codmer.turepulseai.util.DocumentChunker;
import com.codmer.turepulseai.util.DocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BusinessDocumentServiceImpl implements BusinessDocumentService {

    private static final int DEFAULT_TOP_K = 5;

    private final BusinessDocumentRepository businessDocumentRepository;
    private final BusinessDocumentChunkRepository businessDocumentChunkRepository;
    private final UserRepository userRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;
    private final EmbeddingCacheService embeddingCacheService;
    private final EmbeddingService embeddingService;
    private final ChatClient chatClient;

    @Override
    @CacheEvict(value = "ragAnswers", allEntries = true, beforeInvocation = false)
    public DocumentUploadResponse uploadDocument(MultipartFile file, Long entityId, String displayName) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "document file is required");
        }
        if (entityId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }

        User user = fetchUser();

        // Use username as businessId
        String businessId = user.getUserName();

        BusinessDocument document = new BusinessDocument();
        document.setBusinessId(businessId);
        document.setEntityId(entityId);
        document.setDisplayName(displayName.trim());
        document.setUserId(user.getId());
        document.setTitle(file.getOriginalFilename());
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus("PROCESSING");

        BusinessDocument saved = businessDocumentRepository.save(document);

        String text = documentTextExtractor.extractText(file);
        List<DocumentChunker.Chunk> chunks = documentChunker.chunk(text);

        if (chunks.isEmpty()) {
            saved.setStatus("EMPTY");
            businessDocumentRepository.save(saved);
            return DocumentUploadResponse.builder()
                    .documentId(saved.getId())
                    .businessId(saved.getBusinessId())
                    .entityId(saved.getEntityId())
                    .displayName(saved.getDisplayName())
                    .title(saved.getTitle())
                    .status(saved.getStatus())
                    .message("No text content found in document")
                    .build();
        }

        int dimension = embeddingCacheService.dimensions();

        final int finalDimension = dimension;
        for (DocumentChunker.Chunk chunk : chunks) {
            BusinessDocumentChunk entity = new BusinessDocumentChunk();
            entity.setDocumentId(saved.getId());
            entity.setBusinessId(saved.getBusinessId());
            entity.setEntityId(saved.getEntityId());
            entity.setDisplayName(saved.getDisplayName());
            entity.setChunkIndex(chunk.getIndex());
            entity.setContent(chunk.getContent());
            entity.setPrevContent(chunk.getPrevContent());
            entity.setNextContent(chunk.getNextContent());
            entity.setEmbeddingDimension(finalDimension);

            // Use EmbeddingService to generate embedding and store chunk
            try {
                embeddingService.embedAndStoreChunk(entity);
            } catch (Exception e) {
                log.warn("Failed to generate embedding for chunk " + chunk.getIndex() +
                        ". Check your OpenAI API quota and key. Saving chunk without embedding...", e);
                // Save chunk without embedding
                businessDocumentChunkRepository.save(entity);
            }
        }

        saved.setStatus("READY");
        businessDocumentRepository.save(saved);

        return DocumentUploadResponse.builder()
                .documentId(saved.getId())
                .businessId(saved.getBusinessId())
                .entityId(saved.getEntityId())
                .displayName(saved.getDisplayName())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .message("Document processed and indexed")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "ragAnswers",
               key = "#request.entityId + '_' + #request.displayName + '_' + #request.query.hashCode()")
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        if (request == null || request.getEntityId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId is required");
        }
        if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }

        User user = fetchUser();
        String businessId = user.getUserName();

        // Step 1: Check if this is an order placement query
        boolean isOrderQuery = isPlaceOrderQuery(request.getQuery());

        String answer;
        if (isOrderQuery) {
            // Step 2: Handle order placement flow
            answer = handleOrderPlacement(businessId, request.getEntityId(),
                                         request.getDisplayName().trim(), request.getQuery());
        } else {
            // Step 3: Handle general question flow (existing logic)
            int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : DEFAULT_TOP_K;
            float[] embedding = embeddingCacheService.embed(request.getQuery());
            String embeddingLiteral = formatEmbeddingForPostgres(embedding);

            List<Object[]> rows = businessDocumentChunkRepository.findSimilarChunksByEntityAndBusiness(
                    businessId, request.getEntityId(), request.getDisplayName().trim(), embeddingLiteral, topK);

            List<String> contextChunks = new ArrayList<>();
            for (Object[] row : rows) {
                if (row.length > 4 && row[4] != null) {
                    contextChunks.add((String) row[4]);
                }
            }

            answer = generateRagAnswer(request.getQuery(), contextChunks);
        }

        return DocumentSearchResponse.builder()
                .businessId(businessId)
                .entityId(request.getEntityId())
                .displayName(request.getDisplayName().trim())
                .query(request.getQuery())
                .answer(answer)
                .build();
    }

    /**
     * Detect if the query is about placing an order
     */
    private boolean isPlaceOrderQuery(String query) {
        String systemPrompt = """
                Classify whether the customer request is about placing a food order.
                Respond with only 'true' or 'false'.
                """;

        String userPrompt = """
                Request: %s
                """.formatted(query);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));

        String response = chatClient.prompt(prompt).call().content();
        return parseBooleanResponse(response);
    }

    /**
     * Handle order placement flow - STRICT MODE: Don't place order if ANY items are missing
     */
    private String handleOrderPlacement(String businessId, Long entityId,
                                       String displayName, String query) {
        log.info("=" + "=".repeat(79));
        log.info("🛒 ORDER PLACEMENT REQUEST: {}", query);
        log.info("=" + "=".repeat(79));

        // Step 1: Retrieve menu chunks from vector DB
        List<String> menuChunks = retrieveMenuChunks(businessId, entityId, displayName);

        if (menuChunks.isEmpty()) {
            log.warn("❌ No menu chunks found for entity: {}", entityId);
            return "Sorry, menu information is not available at the moment. Please try again later.";
        }

        String menuText = String.join("\n\n", menuChunks);
        log.info("📋 Menu text retrieved: {} characters", menuText.length());

        // Step 2: Extract menu items from menu text
        List<String> menuItems = extractMenuItems(menuText);
        if (menuItems.isEmpty()) {
            log.error("❌ Failed to extract any menu items from menu text!");
            return "Sorry, I couldn't understand the menu. Please try again later.";
        }
        log.info("✓ Extracted {} unique menu items: {}", menuItems.size(), menuItems);

        // Step 3: Extract requested items from query
        List<String> requestedItems = extractRequestedItems(query);
        log.info("🛍️  Extracted {} requested items: {}", requestedItems.size(), requestedItems);

        if (requestedItems.isEmpty()) {
            log.warn("⚠️  No items identified in query");
            return "I couldn't identify the items you want to order. Please specify the items clearly.";
        }

        // Step 4: Check availability - STRICT: If ANY items are missing, reject the order
        List<String> missingItems = checkAvailability(menuItems, requestedItems);
        log.info("🔍 Availability check complete: {} missing out of {}", missingItems.size(), requestedItems.size());

        // Step 5: If ANY items are missing, inform user and DON'T place order
        if (!missingItems.isEmpty()) {
            List<String> availableItems = new ArrayList<>(requestedItems);
            availableItems.removeAll(missingItems);

            String missingList = String.join(", ", missingItems);

            if (availableItems.isEmpty()) {
                // CASE 1: None of the requested items are available
                log.error("❌ ORDER REJECTED: NONE of the requested items are available!");
                log.error("   Requested: {}", requestedItems);
                log.error("   Menu has: {}", menuItems);
                return String.format(
                    "I'm sorry, none of the items you requested are available. " +
                    "The items you asked for: %s\n\n" +
                    "These are not on our menu. Please check our menu and place a new order with available items.",
                    missingList);
            } else {
                // CASE 2: Some items are missing - reject order and ask user to update
                String availableList = String.join(", ", availableItems);
                log.error("❌ ORDER REJECTED: {} missing items out of {}", missingItems.size(), requestedItems.size());
                log.error("   Requested: {}", requestedItems);
                log.error("   Available from request: {}", availableItems);
                log.error("   Missing: {}", missingItems);
                return String.format(
                    "I'm sorry, I cannot place your order because the following items are not available: %s\n\n" +
                    "Available items from your request: %s\n\n" +
                    "Please update your order to include only available items and try again.",
                    missingList, availableList);
            }
        }

        // Step 6: All items are available - place order
        String itemsList = String.join(", ", requestedItems);
        log.info("✅ ORDER PLACED: All {} items confirmed available", requestedItems.size());
        log.info("   Items: {}", itemsList);
        log.info("=" + "=".repeat(79));
        return String.format("Great! All items are available. Order placed for %s. You can pick up in 30 minutes.", itemsList);
    }

    /**
     * Retrieve menu chunks from vector DB
     */
    private List<String> retrieveMenuChunks(String businessId, Long entityId, String displayName) {
        // Embed "menu" query to find menu-related chunks
        float[] menuEmbedding = embeddingCacheService.embed("menu items prices food available");
        String embeddingLiteral = formatEmbeddingForPostgres(menuEmbedding);

        List<Object[]> rows = businessDocumentChunkRepository.findSimilarChunksByEntityAndBusiness(
                businessId, entityId, displayName, embeddingLiteral, 10);

        List<String> menuChunks = new ArrayList<>();
        for (Object[] row : rows) {
            if (row.length > 4 && row[4] != null) {
                menuChunks.add((String) row[4]);
            }
        }
        return menuChunks;
    }

    /**
     * Extract menu items from menu text using LLM
     */
    private List<String> extractMenuItems(String menuText) {
        String systemPrompt = """
                Extract all distinct menu items from the menu text.
                Include all variations and item names mentioned.
                Return ONLY a JSON array of item names.
                Example: ["Pizza", "Burger", "Salad", "Sambar Fish", "Fish Fry"]
                """;

        String userPrompt = """
                Menu Text:
                %s
                
                Extract ALL menu items. Be comprehensive and include all food items mentioned.
                """.formatted(menuText);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));

        String response = chatClient.prompt(prompt).call().content();
        List<String> items = parseJsonList(response);
        log.debug("Extracted menu items: {}", items);
        return items;
    }

    /**
     * Extract requested items from customer query using LLM
     */
    private List<String> extractRequestedItems(String query) {
        String systemPrompt = """
                From the customer request, extract ALL the ordered items mentioned by the customer.
                Be thorough and extract every item mentioned, including typos or variations.
                Return ONLY a JSON array of item names as mentioned by the customer.
                Example: ["Pizza", "Coke", "sambar fished"]
                """;

        String userPrompt = """
                Customer Request:
                %s
                
                Extract ALL items the customer mentioned. Do not miss any items even if they have typos.
                """.formatted(query);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));

        String response = chatClient.prompt(prompt).call().content();
        List<String> items = parseJsonList(response);
        log.debug("Extracted requested items: {}", items);
        return items;
    }

    /**
     * Check which requested items are missing from menu using fuzzy matching
     * STRICT MODE: If ANY items are missing, they are reported
     */
    private List<String> checkAvailability(List<String> menuItems, List<String> requestedItems) {
        log.debug("Menu items: {}", menuItems);
        log.debug("Requested items: {}", requestedItems);

        // Normalize menu items for comparison
        List<String> normalizedMenuItems = menuItems.stream()
                .map(this::normalizeItem)
                .toList();
        log.info("Normalized menu items: {}", normalizedMenuItems);

        List<String> missingItems = new ArrayList<>();

        for (String requestedItem : requestedItems) {
            String normalizedRequested = normalizeItem(requestedItem);
            boolean found = false;
            String matchedWith = null;

            // Step 1: Try exact match first (strict)
            for (String normalizedMenu : normalizedMenuItems) {
                if (normalizedMenu.equals(normalizedRequested)) {
                    found = true;
                    matchedWith = normalizedMenu;
                    log.info("✓ EXACT match found for: '{}' in menu item: '{}'", requestedItem, normalizedMenu);
                    break;
                }
            }

            // Step 2: If no exact match, try fuzzy matching (typo tolerance only)
            if (!found) {
                for (String normalizedMenu : normalizedMenuItems) {
                    if (isSimilarItem(normalizedRequested, normalizedMenu)) {
                        found = true;
                        matchedWith = normalizedMenu;
                        log.info("✓ FUZZY match found for: '{}' in menu item: '{}'", requestedItem, normalizedMenu);
                        break;
                    }
                }
            }

            // Step 3: Log missing items with context
            if (!found) {
                log.warn("✗ Item NOT found in menu: '{}' (normalized: '{}')", requestedItem, normalizedRequested);
                log.debug("Searched against menu items: {}", normalizedMenuItems);
                missingItems.add(requestedItem);
            } else {
                log.info("Item '{}' CONFIRMED available (matched: '{}')", requestedItem, matchedWith);
            }
        }

        log.info("Final missing items count: {} out of {}", missingItems.size(), requestedItems.size());
        if (!missingItems.isEmpty()) {
            log.warn("Missing items list: {}", missingItems);
        }
        return missingItems;
    }

    /**
     * Fuzzy match two items - STRICT MODE
     * Only matches if items are substantially similar (same word composition)
     * Handles typos in individual words but NOT word addition/removal
     */
    private boolean isSimilarItem(String requested, String menuItem) {
        // STRICT: Exact match takes priority
        if (requested.equals(menuItem)) {
            return true;
        }

        String[] requestedWords = requested.split("\\s+");
        String[] menuWords = menuItem.split("\\s+");

        // STRICT: Must have same number of words or very close
        // Allow ±1 word difference for typos, but not complete word removal
        if (Math.abs(requestedWords.length - menuWords.length) > 1) {
            log.debug("Word count mismatch: '{}' ({} words) vs '{}' ({} words)",
                    requested, requestedWords.length, menuItem, menuWords.length);
            return false;
        }

        // Match words one-by-one (accounting for typos)
        int exactMatches = 0;
        int typoMatches = 0;
        int totalWords = Math.max(requestedWords.length, menuWords.length);

        for (int i = 0; i < Math.min(requestedWords.length, menuWords.length); i++) {
            String rWord = requestedWords[i];
            String mWord = menuWords[i];

            if (rWord.equals(mWord)) {
                exactMatches++;
            } else if (areWordsSimilar(rWord, mWord)) {
                typoMatches++;
                log.debug("Typo match: '{}' vs '{}'", rWord, mWord);
            } else {
                log.debug("Word mismatch: '{}' vs '{}'", rWord, mWord);
                return false; // Word mismatch means items are different
            }
        }

        // STRICT: Require at least 80% match rate
        double matchRate = (double) (exactMatches + typoMatches) / totalWords;
        boolean isMatch = matchRate >= 0.8 && exactMatches >= Math.ceil(totalWords * 0.5);
        log.debug("Match rate for '{}' vs '{}': {:.2f} (exact: {}, typo: {}, total: {})",
                requested, menuItem, matchRate, exactMatches, typoMatches, totalWords);
        return isMatch;
    }

    /**
     * Check if two words are similar (handling typos)
     * Levenshtein-like approach but simplified
     */
    private boolean areWordsSimilar(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();

        // Words must be at least 80% similar in length
        double lengthRatio = (double) Math.min(len1, len2) / Math.max(len1, len2);
        if (lengthRatio < 0.8) {
            log.debug("Length ratio too low for '{}' vs '{}': {}", word1, word2, lengthRatio);
            return false;
        }

        // Count character matches (simple Levenshtein approximation)
        int matches = 0;
        for (int i = 0; i < Math.min(len1, len2); i++) {
            if (word1.charAt(i) == word2.charAt(i)) {
                matches++;
            }
        }

        // Require at least 75% character match
        double charMatchRate = (double) matches / Math.max(len1, len2);
        log.debug("Character match for '{}' vs '{}': {:.2f}", word1, word2, charMatchRate);
        return charMatchRate >= 0.75;
    }

    /**
     * Normalize item name for comparison
     */
    private String normalizeItem(String item) {
        return item.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Parse boolean response from LLM
     */
    private boolean parseBooleanResponse(String response) {
        if (response == null) {
            return false;
        }
        String normalized = response.toLowerCase().trim();
        return normalized.contains("true") || normalized.contains("yes");
    }

    /**
     * Parse JSON list from LLM response
     */
    private List<String> parseJsonList(String response) {
        try {
            // Clean response to extract JSON array
            String cleaned = response.trim();
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']');

            if (start == -1 || end == -1 || start >= end) {
                log.warn("No valid JSON array found in response: {}", response);
                return List.of();
            }

            String jsonArray = cleaned.substring(start, end + 1);
            log.debug("Extracted JSON array: {}", jsonArray);

            // Simple parsing - extract items between quotes
            List<String> items = new ArrayList<>();
            String[] parts = jsonArray.split("\"");
            for (int i = 1; i < parts.length; i += 2) {
                String item = parts[i].trim();
                if (!item.isEmpty()) {
                    items.add(item);
                    log.debug("Parsed item: '{}'", item);
                }
            }

            log.info("Successfully parsed {} items from response", items.size());
            return items;
        } catch (Exception e) {
            log.warn("Failed to parse JSON list from LLM response: {}", response, e);
            return List.of();
        }
    }

    private String generateRagAnswer(String query, List<String> contextChunks) {
        String contextText = contextChunks.isEmpty()
                ? "No relevant context found."
                : String.join("\n\n", contextChunks);

        String systemPrompt = """
                You are a customer care representative for small businesses. Answer questions based on the provided context. If you don't know the answer, respond politely that you don't have the information.
                """;

        String userPrompt = """
                Question: %s

                Context:
                %s

                Provide a clear answer based only on the context above.
                """.formatted(query, contextText);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));

        return chatClient.prompt(prompt).call().content();
    }

    /**
     * Format embedding array as PostgreSQL vector literal
     * Format: [1.0, 2.0, 3.0, ...]
     */
    private String formatEmbeddingForPostgres(float[] embedding) {
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
}
