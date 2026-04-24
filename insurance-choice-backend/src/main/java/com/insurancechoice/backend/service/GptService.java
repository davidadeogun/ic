package com.insurancechoice.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurancechoice.backend.entity.GptCache;
import com.insurancechoice.backend.entity.InsuranceProfile;
import com.insurancechoice.backend.repository.GptCacheRepository;
import com.insurancechoice.backend.repository.InsuranceProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates: InsuranceProfile → cache check → (GPT call if needed) → JSON parse → cache store.
 * Cache key: (userObjectId, problemType).
 * Cache validity: SHA-256 hash of all 8 profile fields — any change invalidates the cache.
 */
@Service
public class GptService {

    private static final Logger log = LoggerFactory.getLogger(GptService.class);

    @Autowired private InsuranceProfileRepository profileRepository;
    @Autowired private GptCacheRepository         cacheRepository;
    @Autowired private DomainTemplateService       domainTemplateService;
    @Autowired private PromptBuilderService        promptBuilderService;
    @Autowired(required = false) private ChatModel chatModel;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── JSON output schema appended to every prompt ───────────────────────────

    private static final String DM_JSON_SCHEMA = """

            RESPOND ONLY WITH VALID JSON — no markdown fences, no explanation text, no preamble.
            Use exactly this structure (three alternatives in order: Basic, Standard, Comprehensive):
            {
              "alternatives": [
                {
                  "name": "<short alternative name>",
                  "br": <number: baseline risk %>,
                  "rrr": <number: relative risk reduction %>,
                  "rr": <number: residual risk %>,
                  "clRrr": "<confidence level>",
                  "clRr": "<confidence level>",
                  "advantages": [
                    {"rank": 1, "text": "<RRR advantage text>", "cl": "<confidence level>"},
                    {"rank": 2, "text": "<advantage text>", "cl": "<confidence level>"},
                    {"rank": 3, "text": "<advantage text>", "cl": "<confidence level>"},
                    {"rank": 4, "text": "<advantage text>", "cl": "<confidence level>"},
                    {"rank": 5, "text": "<advantage text>", "cl": "<confidence level>"}
                  ],
                  "disadvantages": [
                    {"rank": 1, "text": "<RR disadvantage text>", "cl": "<confidence level>"},
                    {"rank": 2, "text": "<disadvantage text>", "cl": "<confidence level>"},
                    {"rank": 3, "text": "<disadvantage text>", "cl": "<confidence level>"},
                    {"rank": 4, "text": "<disadvantage text>", "cl": "<confidence level>"},
                    {"rank": 5, "text": "<disadvantage text>", "cl": "<confidence level>"}
                  ]
                }
              ]
            }
            """;

    private static final String PS_JSON_SCHEMA = """

            RESPOND ONLY WITH VALID JSON — no markdown fences, no explanation text, no preamble.
            Use exactly this structure (three alternatives in order: Basic, Standard, Comprehensive):
            {
              "alternatives": [
                {
                  "name": "<short alternative name>",
                  "hypothesis1": {
                    "br": <number: baseline risk %>,
                    "rrr": <number: relative risk reduction %>,
                    "clRrr": "<confidence level for RRR>",
                    "rr": <number: residual risk % = br * (1 - rrr/100)>,
                    "clRr": "<confidence level for RR>",
                    "difficulties": [
                      {"category": "material",       "description": "<text>", "cl": "<confidence level>", "solution": "<text>"},
                      {"category": "physical",       "description": "<text>", "cl": "<confidence level>", "solution": "<text>"},
                      {"category": "psychological",  "description": "<text>", "cl": "<confidence level>", "solution": "<text>"},
                      {"category": "social",         "description": "<text>", "cl": "<confidence level>", "solution": "<text>"},
                      {"category": "spiritual",      "description": "<text>", "cl": "<confidence level>", "solution": "<text>"}
                    ]
                  },
                  "hypothesis2": {
                    "br": <number: same baseline risk % as hypothesis1.br>,
                    "rrr": <number: same relative risk reduction % as hypothesis1.rrr>,
                    "clRrr": "<same confidence level as hypothesis1.clRrr>",
                    "rr": <number: same residual risk % as hypothesis1.rr>,
                    "clRr": "<same confidence level as hypothesis1.clRr>",
                    "significance": [
                      {"category": "material",       "description": "<text>", "strength": "<scale value>", "el": "<evidence level>", "strategy": "<text>"},
                      {"category": "physical",       "description": "<text>", "strength": "<scale value>", "el": "<evidence level>", "strategy": "<text>"},
                      {"category": "psychological",  "description": "<text>", "strength": "<scale value>", "el": "<evidence level>", "strategy": "<text>"},
                      {"category": "social",         "description": "<text>", "strength": "<scale value>", "el": "<evidence level>", "strategy": "<text>"},
                      {"category": "spiritual",      "description": "<text>", "strength": "<scale value>", "el": "<evidence level>", "strategy": "<text>"}
                    ]
                  }
                }
              ]
            }
            """;

    // ── Public entry point ────────────────────────────────────────────────────

    public Map<String, Object> generate(String userObjectId, int problemType) {
        if (chatModel == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Azure OpenAI is not configured. Check api-key and endpoint in application.yml.");
        }
        if (problemType != 1 && problemType != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "problemType must be 1 (DM) or 3 (PS)");
        }

        InsuranceProfile profile = profileRepository.findByUserObjectId(userObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Insurance profile not found for user: " + userObjectId));

        if (profile.getAssetAtRisk() == null || profile.getAssetAtRisk().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insurance profile has no Asset-at-Risk set");
        }

        String currentSnapshot = snapshotProfile(profile);
        String mode = (problemType == 1) ? "DM" : "PS";
        boolean isDm = (problemType == 1);

        // ── 1. Same-mode cache hit check ───────────────────────────────────
        Optional<GptCache> sameMode = cacheRepository
                .findByUserObjectIdAndProblemType(userObjectId, problemType);

        if (sameMode.isPresent()) {
            String cachedSnapshot = sameMode.get().getProfileSnapshot();
            if (cachedSnapshot.equals(currentSnapshot)) {
                GptCache hit = sameMode.get();
                log.info("CACHE HIT: Returning cached values for {} {} — BR={}%, RRR={}%, RR={}%",
                        profile.getAssetAtRisk(), userObjectId, hit.getBr(), hit.getRrr(), hit.getRr());
                Map<String, Object> cached = parseJson(hit.getGptResponse(), mode);
                cached.put("mode",        mode);
                cached.put("assetAtRisk", profile.getAssetAtRisk());
                cached.put("fromCache",   true);
                return cached;
            }
            log.info("CACHE STALE: Profile changed for {} {} — will call GPT",
                    profile.getAssetAtRisk(), userObjectId);
        } else {
            log.info("CACHE MISS: Calling GPT for {} {}", profile.getAssetAtRisk(), userObjectId);
        }

        // ── 2. Look for other-mode cache with same snapshot (cross-mode consistency) ──
        //       DM and PS must produce identical BR/RRR/RR for the same user + domain.
        //       If the other mode was already generated with this profile, reuse its values.
        int otherType = isDm ? 3 : 1;
        Optional<GptCache> otherMode = cacheRepository
                .findByUserObjectIdAndProblemType(userObjectId, otherType);

        // ── 3. Build prompt ────────────────────────────────────────────────
        Map<String, Object> domain = domainTemplateService.getByAssetAtRisk(profile.getAssetAtRisk());

        String basePrompt = isDm
                ? promptBuilderService.buildDmPrompt(profile, domain)
                : promptBuilderService.buildPsPrompt(profile, domain);

        // Inject pinned BR/RRR/RR from the other mode (same snapshot) OR from a stale same-mode cache
        String contextPrefix = buildConsistencyPrefix(otherMode, sameMode, currentSnapshot);
        String fullPrompt = contextPrefix + basePrompt + (isDm ? DM_JSON_SCHEMA : PS_JSON_SCHEMA);

        log.info("Sending {} prompt to Azure OpenAI ({} chars, contextPrefix={} chars)",
                mode, fullPrompt.length(), contextPrefix.length());

        // ── 4. GPT call ────────────────────────────────────────────────────
        String rawResponse = chatModel.call(new Prompt(fullPrompt))
                .getResult()
                .getOutput()
                .getText();

        log.info("Raw GPT {} response ({} chars):\n{}", mode, rawResponse.length(), rawResponse);

        // ── 5. Parse ───────────────────────────────────────────────────────
        Map<String, Object> parsed = parseJson(rawResponse, mode);

        // ── 6. Store / update cache ────────────────────────────────────────
        saveCache(sameMode, userObjectId, problemType, currentSnapshot, rawResponse, parsed, isDm);

        parsed.put("mode",        mode);
        parsed.put("assetAtRisk", profile.getAssetAtRisk());
        parsed.put("fromCache",   false);
        return parsed;
    }

    /**
     * Purge all cached GPT responses for a user.
     * Called by InsuranceProfileController when the profile is updated.
     */
    public void purgeCache(String userObjectId) {
        cacheRepository.deleteAllByUserObjectId(userObjectId);
        log.info("GPT cache purged for user={}", userObjectId);
    }

    // ── Cache helpers ─────────────────────────────────────────────────────────

    /**
     * Builds a context prefix that pins BR/RRR/RR/CL values so GPT reuses them.
     * Priority:
     *   1. Other-mode cache with matching snapshot (cross-mode consistency — Issue 1)
     *   2. Same-mode stale cache (profile just changed but values still useful as anchor)
     * If neither has numeric values, returns empty string (GPT calculates fresh).
     */
    private String buildConsistencyPrefix(Optional<GptCache> otherMode,
                                          Optional<GptCache> sameMode,
                                          String currentSnapshot) {
        // 1. Prefer other-mode cache with same snapshot — guarantees cross-mode consistency
        if (otherMode.isPresent() && currentSnapshot.equals(otherMode.get().getProfileSnapshot())) {
            GptCache c = otherMode.get();
            if (c.getBr() != null && c.getRrr() != null && c.getRr() != null) {
                String otherLabel = (c.getProblemType() == 1) ? "Decision-Making" : "Problem-Solving";
                log.info("Pinning BR/RRR/RR from {} mode cache for cross-mode consistency: BR={}%, RRR={}%, RR={}%",
                        otherLabel, c.getBr(), c.getRrr(), c.getRr());
                return buildPinPrefix(c);
            }
        }
        // 2. Fall back to stale same-mode cache as an anchor
        if (sameMode.isPresent()) {
            GptCache c = sameMode.get();
            if (c.getBr() != null && c.getRrr() != null && c.getRr() != null) {
                log.info("Using stale same-mode cache as anchor: BR={}%, RRR={}%, RR={}%",
                        c.getBr(), c.getRrr(), c.getRr());
                return buildPinPrefix(c);
            }
        }
        log.info("No prior values available — GPT will calculate BR/RRR/RR from scratch");
        return "";
    }

    private String buildPinPrefix(GptCache c) {
        String clRrr = c.getClRrr() != null ? c.getClRrr() : "moderate";
        String clRr  = c.getClRr()  != null ? c.getClRr()  : "moderate";
        return String.format(
            "IMPORTANT — CONSISTENCY CONSTRAINT: The following risk values were previously " +
            "calculated for this exact user profile and domain. You MUST use them exactly as " +
            "provided across all three alternatives (Basic, Standard, Comprehensive) and must " +
            "not recalculate or deviate from them: " +
            "BR = %.1f%%, RRR = %.1f%%, RR = %.1f%%, CL(RRR) = %s, CL(RR) = %s. " +
            "Only the advantages, disadvantages, and category descriptions may differ between alternatives.\n\n",
            c.getBr(), c.getRrr(), c.getRr(), clRrr, clRr);
    }

    @SuppressWarnings("unchecked")
    private void saveCache(Optional<GptCache> existing, String userObjectId, int problemType,
                           String snapshot, String rawResponse,
                           Map<String, Object> parsed, boolean isDm) {
        GptCache cache = existing.orElseGet(GptCache::new);
        cache.setUserObjectId(userObjectId);
        cache.setProblemType(problemType);
        cache.setProfileSnapshot(snapshot);
        cache.setGptResponse(rawResponse);

        // Extract BR/RRR/RR from first alternative for context prepend
        try {
            List<Map<String, Object>> alts =
                    (List<Map<String, Object>>) parsed.get("alternatives");
            if (alts != null && !alts.isEmpty()) {
                Map<String, Object> first = alts.get(0);
                if (isDm) {
                    cache.setBr(toDouble(first.get("br")));
                    cache.setRrr(toDouble(first.get("rrr")));
                    cache.setRr(toDouble(first.get("rr")));
                    cache.setClRrr(str(first.get("clRrr")));
                    cache.setClRr(str(first.get("clRr")));
                } else {
                    // PS: both hypotheses carry identical BR/RRR/RR — read all from h1
                    Map<String, Object> h1 = (Map<String, Object>) first.get("hypothesis1");
                    if (h1 != null) {
                        cache.setBr(toDouble(h1.get("br")));
                        cache.setRrr(toDouble(h1.get("rrr")));
                        cache.setRr(toDouble(h1.get("rr")));
                        cache.setClRrr(str(h1.get("clRrr")));
                        cache.setClRr(str(h1.get("clRr")));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract BR/RRR/RR from parsed response for cache: {}", e.getMessage());
        }

        cacheRepository.save(cache);
        log.info("GPT cache saved for user={} type={}", userObjectId, problemType);
    }

    // ── Profile snapshot (SHA-256 of all 8 fields) ────────────────────────────

    private String snapshotProfile(InsuranceProfile p) {
        String raw = join(
            p.getAssetAtRisk(),
            p.getEstimatedValue(),
            p.getFinancialCapacity(),
            p.getGeographicRisk(),
            p.getGeographicRiskDescription(),
            p.getRiskTolerance(),
            p.getDecisionPreferences(),
            p.getEngagementStyle()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Fallback: use raw string (won't break functionality)
            return raw;
        }
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part != null ? part : "").append('|');
        }
        return sb.toString();
    }

    // ── JSON parsing ──────────────────────────────────────────────────────────

    private Map<String, Object> parseJson(String raw, String mode) {
        String json = stripMarkdownFences(raw.trim());
        try {
            Map<String, Object> result = mapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            validateResponse(result, mode);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse GPT {} response as JSON: {}", mode, e.getMessage());
            log.error("Raw text was:\n{}", raw);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "GPT returned an unparseable response. See server logs.");
        }
    }

    private String stripMarkdownFences(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence    = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private void validateResponse(Map<String, Object> parsed, String mode) {
        if (!parsed.containsKey("alternatives")) {
            log.warn("GPT {} response missing 'alternatives' key", mode);
        } else {
            log.info("GPT {} response parsed successfully — {} alternatives returned",
                    mode, ((List<?>) parsed.get("alternatives")).size());
        }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }
}
