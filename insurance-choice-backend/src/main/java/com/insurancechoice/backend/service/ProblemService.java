package com.insurancechoice.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurancechoice.backend.entity.Problem;
import com.insurancechoice.backend.repository.ProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ProblemService {

    @Autowired private ProblemRepository problemRepository;
    @Autowired private DomainTemplateService domainTemplateService;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");

    // ── Problem CRUD ──────────────────────────────────────────────────────────

    public List<Map<String, Object>> listProblems(Long userId) {
        return problemRepository
                .findByUserIdAndIsTemplateFalseOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    public Map<String, Object> saveProblem(Map<String, Object> req, Long userId) {
        Object rawId = req.get("id");
        Long id = rawId != null ? toLong(rawId) : null;

        Problem problem = (id != null && id > 0)
                ? problemRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"))
                : new Problem();

        problem.setName((String) req.getOrDefault("name", ""));
        problem.setNotes((String) req.getOrDefault("notes", ""));
        problem.setProblemType(toInt(req.getOrDefault("problemType", 1)));
        problem.setUserId(userId);
        problem.setIsTemplate(false);

        // Accept both "metaData" (frontend send) and "metadata" (normalised)
        Object meta = req.containsKey("metaData") ? req.get("metaData") : req.get("metadata");
        problem.setMetaDataJson(toJson(meta));
        problem.setProblemAlternativesJson(toJson(req.get("problemAlternatives")));

        problem = problemRepository.save(problem);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", problem.getId());
        return result;
    }

    public void deleteProblem(Long id, Long userId) {
        Problem p = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
        if (!p.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your problem");
        }
        problemRepository.delete(p);
    }

    public Map<String, Object> getProblemById(Long id) {
        Problem p = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
        return toFullResponse(p);
    }

    // ── Templates (hardcoded — no DB row needed) ──────────────────────────────

    /**
     * Returns a summary list for the mode dropdown in the frontend.
     * The frontend iterates this and displays template.name.
     */
    public List<Map<String, Object>> listTemplates() {
        String ts = "2022-05-27T17:40:39.273+00:00";
        return List.of(
                templateListItem(1, "Decision Making", ts),
                templateListItem(3, "Problem Solving", ts)
        );
    }

    /**
     * Returns the full template tree consumed by the Angular tree component.
     * When assetAtRisk is provided, alternative titles and hypothesis text are
     * populated with domain-specific names from DomainTemplateService.
     */
    public Map<String, Object> getTemplateById(Long id, String assetAtRisk) {
        Map<String, Object> domain = (assetAtRisk != null && !assetAtRisk.isBlank())
                ? domainTemplateService.findByAssetAtRisk(assetAtRisk)
                : null;
        return switch (id.intValue()) {
            case 1 -> domain != null ? template1WithDomain(domain) : template1();
            case 2 -> template2();
            case 3 -> domain != null ? template3WithDomain(domain) : template3();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        };
    }

    // ── Private template builders ─────────────────────────────────────────────

    /** Decision Making */
    private Map<String, Object> template1() {
        String ts = "2022-05-27T17:40:39.273+00:00";
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", 1);
        t.put("name", "Decision Making");
        t.put("notes", null);
        t.put("problemType", 1);
        t.put("createdDateTime", ts);
        t.put("lastUpdatedDateTime", ts);
        t.put("problemAlternatives", List.of(
                alt("Problem: ", "Basic vs Standard vs Comprehensive", 1, true, true, false, 0, List.of(
                        staticLabel("Risk: ", "risk of financial loss", 0),
                        alt("Basic: ",         "Basic",         1, false, true, true, 0, List.of()),
                        alt("Standard: ",      "Standard",      2, false, true, true, 0, List.of()),
                        alt("Comprehensive: ", "Comprehensive", 3, false, true, true, 0, List.of())
                ))
        ));
        t.put("metadata", metadata(dmSubAlternatives(), dmFrame()));
        return t;
    }

    /** Problem Solving – Reactive */
    private Map<String, Object> template2() {
        String ts = "2022-05-27T17:40:49.805+00:00";
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", 2);
        t.put("name", "Problem Solving - Reactive");
        t.put("notes", null);
        t.put("problemType", 2);
        t.put("createdDateTime", ts);
        t.put("lastUpdatedDateTime", ts);
        t.put("problemAlternatives", List.of(
                alt("Problem: ",    "Alternative 1 vs Alternative 2", 1, true,  true, false, 2, List.of()),
                alt("STG: ",       "STG",                             2, false, true, false, 0, List.of()),
                alt("LTG: ",       "LTG",                             3, false, true, false, 0, List.of())
        ));
        t.put("metadata", metadata(
                List.of(subAlt("helps ",        1),
                        subAlt("doesn't help ", 2)),
                dmFrame()
        ));
        return t;
    }

    /** Problem Solving */
    private Map<String, Object> template3() {
        String ts = "2022-05-27T17:40:49.805+00:00";
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", 3);
        t.put("name", "Problem Solving");
        t.put("notes", null);
        t.put("problemType", 3);
        t.put("createdDateTime", ts);
        t.put("lastUpdatedDateTime", ts);
        t.put("problemAlternatives", List.of(
                alt("Problem: ", "Basic vs Standard vs Comprehensive", 1, true, true, false, 0, List.of(
                        staticLabel("Future risk event: ", "[Future risk event]",                              1),
                        staticLabel("Risk: ",              "risk of financial loss",                          2),
                        staticLabel("Activity goal: ",     "receiving financial support after a covered loss", 3),
                        staticLabel("Problem goal: ",      "sufficiently reducing the risk of financial loss", 4),
                        alt("Basic: ",         "Basic", 5, false, true, true, 0, List.of(
                                hyp1("[Future risk event] happens"),
                                hyp2("[Future risk event] doesn't happen")
                        )),
                        alt("Standard: ",      "Standard", 6, false, true, true, 0, List.of(
                                hyp1("[Future risk event] happens"),
                                hyp2("[Future risk event] doesn't happen")
                        )),
                        alt("Comprehensive: ", "Comprehensive", 7, false, true, true, 0, List.of(
                                hyp1("[Future risk event] happens"),
                                hyp2("[Future risk event] doesn't happen")
                        ))
                ))
        ));
        t.put("metadata", metadata(psSubAlternatives(), psFrame()));
        return t;
    }

    /** Decision Making with domain-specific alternative names and full descriptions. */
    private Map<String, Object> template1WithDomain(Map<String, Object> domain) {
        String domainName        = (String) domain.get("domainName");
        String basic             = (String) domain.get("basicShortName");
        String standard          = (String) domain.get("standardShortName");
        String comprehensive     = (String) domain.get("comprehensiveShortName");
        String basicFull         = (String) domain.get("basicFullDescription");
        String standardFull      = (String) domain.get("standardFullDescription");
        String comprehensiveFull = (String) domain.get("comprehensiveFullDescription");
        String problemTitle      = domainName + ": Basic vs Standard vs Comprehensive";
        String ts = "2022-05-27T17:40:39.273+00:00";
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", 1);
        t.put("name", "Decision Making");
        t.put("notes", null);
        t.put("problemType", 1);
        t.put("createdDateTime", ts);
        t.put("lastUpdatedDateTime", ts);
        t.put("problemAlternatives", List.of(
                alt("Problem: ", problemTitle, 1, true, true, false, 0, List.of(
                        staticLabel("Risk: ", "risk of financial loss", 0),
                        alt("Basic: ",         basicFull,         1, false, true, true, 0, List.of()),
                        alt("Standard: ",      standardFull,      2, false, true, true, 0, List.of()),
                        alt("Comprehensive: ", comprehensiveFull, 3, false, true, true, 0, List.of())
                ))
        ));
        t.put("metadata", metadata(dmSubAlternatives(), dmFrame()));
        return t;
    }

    /** Problem Solving with domain-specific names, full descriptions, and pre-filled goals. */
    private Map<String, Object> template3WithDomain(Map<String, Object> domain) {
        String domainName        = (String) domain.get("domainName");
        String basic             = (String) domain.get("basicShortName");
        String standard          = (String) domain.get("standardShortName");
        String comprehensive     = (String) domain.get("comprehensiveShortName");
        String basicFull         = (String) domain.get("basicFullDescription");
        String standardFull      = (String) domain.get("standardFullDescription");
        String comprehensiveFull = (String) domain.get("comprehensiveFullDescription");
        String riskEvent         = (String) domain.get("futureRiskEvent");
        String problemTitle      = domainName + ": Basic vs Standard vs Comprehensive";
        String ts = "2022-05-27T17:40:49.805+00:00";
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", 3);
        t.put("name", "Problem Solving");
        t.put("notes", "risk of financial loss");
        t.put("problemType", 3);
        t.put("createdDateTime", ts);
        t.put("lastUpdatedDateTime", ts);
        t.put("problemAlternatives", List.of(
                alt("Problem: ", problemTitle, 1, true, true, false, 0, List.of(
                        staticLabel("Future risk event: ", riskEvent,                                              1),
                        staticLabel("Risk: ",              "risk of financial loss",                              2),
                        staticLabel("Activity goal: ",     "receiving financial support after a covered loss",    3),
                        staticLabel("Problem goal: ",      "sufficiently reducing the risk of financial loss",    4),
                        alt("Basic: ",         basicFull,         5, false, true, true, 0, List.of(
                                hyp1(riskEvent + " happens"),
                                hyp2(riskEvent + " doesn't happen")
                        )),
                        alt("Standard: ",      standardFull,      6, false, true, true, 0, List.of(
                                hyp1(riskEvent + " happens"),
                                hyp2(riskEvent + " doesn't happen")
                        )),
                        alt("Comprehensive: ", comprehensiveFull, 7, false, true, true, 0, List.of(
                                hyp1(riskEvent + " happens"),
                                hyp2(riskEvent + " doesn't happen")
                        ))
                ))
        ));
        t.put("metadata", metadata(psSubAlternatives(), psFrame()));
        return t;
    }

    // ── Template node / metadata helpers ─────────────────────────────────────

    /** Static, read-only label row — no expand button, no edit/add/delete icons. */
    private Map<String, Object> staticLabel(String label, String title, int order) {
        Map<String, Object> m = alt(label, title, order, false, false, false, 0, List.of());
        m.put("hideDetailView", true);
        return m;
    }

    private Map<String, Object> alt(String label, String title, int order,
                                    boolean canAdd, boolean canEdit, boolean canDelete,
                                    int maxSub, List<Map<String, Object>> children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("title", title);
        m.put("disabled", false);
        m.put("evaluated", false);
        m.put("order", order);
        m.put("negativeOutcomeIntensity", 0);
        m.put("negativeOutcomeLikelihood", 0);
        m.put("postiveOutcomeIntensity", 0);    // preserved frontend typo
        m.put("postiveOutcomeLikelihood", 0);   // preserved frontend typo
        m.put("children", children);
        m.put("canAddAlternatives", canAdd);
        m.put("canEditAlternatives", canEdit);
        m.put("canDeleteAlternatives", canDelete);
        m.put("maxAllowedSubAlternatives", maxSub);
        m.put("hideDetailView", !children.isEmpty());
        m.put("isNew", false);
        m.put("pros", 0);
        m.put("cons", 0);
        m.put("percentage", 0);
        return m;
    }

    /** Hypothesis 1 — "event happens": advantages title + disadvantage category fields (difficulties). */
    private Map<String, Object> hyp1(String title) {
        Map<String, Object> m = alt("Hypothesis 1: ", title, 1, false, true, false, 0, List.of());
        m.put("showDisdvantageSubContent", true);
        return m;
    }

    /** Hypothesis 2 — "event doesn't happen": disadvantages title + advantage category fields (significance). */
    private Map<String, Object> hyp2(String title) {
        Map<String, Object> m = alt("Hypothesis 2: ", title, 2, false, true, false, 0, List.of());
        m.put("showAdvantageSubContent", true);
        return m;
    }

    private Map<String, Object> subAlt(String label, int order) {
        return Map.of("label", label, "order", order);
    }

    private Map<String, Object> metadata(Object subAlternatives, Map<String, Object> frame) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subAlternatives", subAlternatives);
        m.put("frame", frame);
        return m;
    }

    /** subAlternatives structure for DM mode: one level of alternatives, no hypotheses. */
    private Map<String, Object> dmSubAlternatives() {
        Map<String, Object> level1Item = new LinkedHashMap<>();
        level1Item.put("label", "Alternative: ");
        level1Item.put("title", "New alternative");
        level1Item.put("order", 1);
        level1Item.put("children", List.of());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level1", List.of(level1Item));
        m.put("level2", null);
        m.put("level3", null);
        return m;
    }

    /** subAlternatives structure for PS mode: alternatives with hyp1/hyp2 children. */
    private Map<String, Object> psSubAlternatives() {
        Map<String, Object> hyp1 = new LinkedHashMap<>();
        hyp1.put("label", "Hypothesis 1: ");
        hyp1.put("showDisdvantageSubContent", true);

        Map<String, Object> hyp2 = new LinkedHashMap<>();
        hyp2.put("label", "Hypothesis 2: ");
        hyp2.put("showAdvantageSubContent", true);

        Map<String, Object> level1Item = new LinkedHashMap<>();
        level1Item.put("label", "Alternative: ");
        level1Item.put("title", "New alternative");
        level1Item.put("order", 1);
        level1Item.put("children", List.of(hyp1, hyp2));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level1", List.of(level1Item));
        m.put("level2", null);
        m.put("level3", null);
        return m;
    }

    /** Frame labels for Decision Making mode. */
    private Map<String, Object> dmFrame() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("positiveOutcomeLabel",               "Advantages");
        f.put("positiveOutcomeIntensityLabel",       "Magnitude");
        f.put("positiveOutcomeIntensityQuestion",    "How appealing are advantages to you?");
        f.put("positiveOutcomeLikelyhoodLabel",      "Likelihood");
        f.put("positiveOutcomeLikelyhoodQuestion",   "How likely are you to experience these advantages?");
        f.put("positiveOutcomeTitleLabel",           "Advantage title");
        f.put("positiveOutcomeTitlePH",              "Describe the advantages");
        f.put("positiveOutcomePhysicalLabel",        "Physical");
        f.put("positiveOutcomeSpiritualLabel",       "Spiritual");
        f.put("positiveOutcomeSocialLabel",          "Social");
        f.put("positiveOutcomeMaterialLabel",        "Material");
        f.put("positiveOutcomePsychologicalLabel",   "Psychological");
        f.put("negativeOutcomeLabel",                "Disadvantages");
        f.put("negativeOutcomeIntensityLabel",       "Magnitude");
        f.put("negativeOutcomeIntensityQuestion",    "How unappealing are disadvantages to you?");
        f.put("negativeOutcomeLikelyhoodLabel",      "Likelihood");
        f.put("negativeOutcomeLikelyhoodQuestion",   "How likely are you to experience these disadvantages?");
        f.put("negativeOutcomeTitleLabel",           "Disadvantage title");
        f.put("negativeOutcomeTitlePH",              "Describe the disadvantages");
        f.put("negativeOutcomePhysicalLabel",        "Physical");
        f.put("negativeOutcomeSpiritualLabel",       "Spiritual");
        f.put("negativeOutcomeSocialLabel",          "Social");
        f.put("negativeOutcomeMaterialLabel",        "Material");
        f.put("negativeOutcomePsychologicalLabel",   "Psychological");
        return f;
    }

    /** Frame labels for Problem Solving mode. */
    private Map<String, Object> psFrame() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("positiveOutcomeLabel",               "Advantages");
        f.put("positiveOutcomeIntensityLabel",       "Magnitude");
        f.put("positiveOutcomeIntensityQuestion",    "How are the advantages appealing to you from the perspective of sufficiently reducing the risk of financial loss?");
        f.put("positiveOutcomeLikelyhoodLabel",      "Likelihood");
        f.put("positiveOutcomeLikelyhoodQuestion",   "How likely are you to experience these advantages?");
        f.put("positiveOutcomeTitleLabel",           "Advantage title");
        f.put("positiveOutcomeTitlePH",              "Describe the advantages");
        f.put("positiveOutcomePhysicalLabel",        "Physical");
        f.put("positiveOutcomeSpiritualLabel",       "Spiritual");
        f.put("positiveOutcomeSocialLabel",          "Social");
        f.put("positiveOutcomeMaterialLabel",        "Material");
        f.put("positiveOutcomePsychologicalLabel",   "Psychological");
        f.put("negativeOutcomeLabel",                "Disadvantages");
        f.put("negativeOutcomeIntensityLabel",       "Magnitude");
        f.put("negativeOutcomeIntensityQuestion",    "How are the disadvantages unappealing to you from the perspective of sufficiently reducing the risk of financial loss?");
        f.put("negativeOutcomeLikelyhoodLabel",      "Likelihood");
        f.put("negativeOutcomeLikelyhoodQuestion",   "How likely are you to experience these disadvantages?");
        f.put("negativeOutcomeTitleLabel",           "Disadvantage title");
        f.put("negativeOutcomeTitlePH",              "Describe the disadvantages");
        f.put("negativeOutcomePhysicalLabel",        "Physical");
        f.put("negativeOutcomeSpiritualLabel",       "Spiritual");
        f.put("negativeOutcomeSocialLabel",          "Social");
        f.put("negativeOutcomeMaterialLabel",        "Material");
        f.put("negativeOutcomePsychologicalLabel",   "Psychological");
        return f;
    }

    // ── Response mappers ──────────────────────────────────────────────────────

    private Map<String, Object> toListItem(Problem p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("notes", p.getNotes());
        m.put("problemType", p.getProblemType());
        m.put("createdDateTime", p.getCreatedAt() != null ? p.getCreatedAt().format(ISO) : null);
        m.put("lastUpdatedDateTime", p.getUpdatedAt() != null ? p.getUpdatedAt().format(ISO) : null);
        return m;
    }

    private Map<String, Object> toFullResponse(Problem p) {
        Map<String, Object> m = toListItem(p);
        m.put("metadata", fromJson(p.getMetaDataJson()));
        m.put("problemAlternatives", fromJsonList(p.getProblemAlternativesJson()));
        return m;
    }

    private Map<String, Object> templateListItem(int id, String name, String ts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("notes", null);
        m.put("problemType", id);
        m.put("createdDateTime", ts);
        m.put("lastUpdatedDateTime", ts);
        return m;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { return null; }
    }

    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try { return mapper.readValue(json, new TypeReference<Map<String, Object>>() {}); }
        catch (Exception e) { return null; }
    }

    private List<Object> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return mapper.readValue(json, new TypeReference<List<Object>>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private Long toLong(Object val) {
        if (val instanceof Integer i) return i.longValue();
        if (val instanceof Long l) return l;
        if (val instanceof String s) return Long.parseLong(s);
        return 0L;
    }

    private Integer toInt(Object val) {
        if (val instanceof Integer i) return i;
        if (val instanceof Long l) return l.intValue();
        if (val instanceof String s) return Integer.parseInt(s);
        return 1;
    }
}
