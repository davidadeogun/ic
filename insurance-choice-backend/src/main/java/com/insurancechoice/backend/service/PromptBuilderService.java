package com.insurancechoice.backend.service;

import com.insurancechoice.backend.entity.InsuranceProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Builds completed AI prompt strings from an InsuranceProfile + DomainTemplate.
 * Does NOT call any AI API — outputs are logged for verification.
 */
@Service
public class PromptBuilderService {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilderService.class);

    private static final String INSTRUCTIONS_PREFIX =
        "INSTRUCTIONS: Use ONLY the User Profile, coverage alternatives, and established " +
        "actuarial/industry data. Do not fabricate values. If a value cannot be determined, " +
        "state insufficient data. For each calculated value (BR, RRR, RR), cite whether it " +
        "came from User Profile, actuarial data, or guidelines. Use the same sources across " +
        "all three alternatives and across both modes. Temperature is set to 0.\n\n" +
        "CRITICAL: Baseline Risk (BR) represents the annual probability of out-of-pocket financial " +
        "loss if uninsured. Use realistic actuarial estimates, NOT rounded or inflated numbers. " +
        "For Auto insurance: high-risk zone BR is approximately 7-10%, low-risk zone BR is " +
        "approximately 4-6%. For Home insurance: high-risk zone BR is approximately 5-8%, low-risk " +
        "zone BR is approximately 2-4%. For other domains, scale proportionally based on industry " +
        "actuarial data. BR must differ meaningfully between risk zones. Do NOT default to 25% — " +
        "that is unrealistically high for most insurance domains.\n\n" +
        "CRITICAL: RRR should reflect the actual risk reduction each coverage level provides. " +
        "Basic coverage still provides meaningful risk reduction (typically 25-40%), Standard " +
        "provides substantial reduction (typically 60-80%), and Comprehensive provides the highest " +
        "reduction (typically 80-90%). Do NOT use artificially low RRR for Basic or artificially " +
        "spread values like 10/40/70.\n\n" +
        "CRITICAL: Confidence Levels for risk calculations (CL(RRR) and CL(RR)) should reflect " +
        "the quality of the underlying actuarial evidence, NOT the magnitude of the risk reduction. " +
        "Since all three alternatives rely on the same actuarial data sources for the same user " +
        "profile, CL(RRR) and CL(RR) should typically be the same level across all three " +
        "alternatives and must be identical between Decision-Making and Problem-Solving modes. " +
        "Do NOT scale CL with coverage level — a higher RRR does not mean higher confidence.\n\n";

    // ── Prompt templates (verbatim from professor) ────────────────────────────

    private static final String PROMPT_1_DM_TEMPLATE =
        "For the decision-making Problem of selecting the appropriate level of [Asset-at-Risk] " +
        "Insurance coverage \u2013 Basic, Standard, or Comprehensive, aimed at sufficiently reducing " +
        "the risk of financial loss, Use the following sources: User Profile, Levels of Coverage " +
        "(Alternatives), Statistical Data (evidence-based estimates of baseline risk, expected risk " +
        "reduction resulting from each alternative, and outcome probabilities, derived from experimental " +
        "studies, observational data, actuarial analyses, and validated risk prediction models), and " +
        "Guideline-Based Recommendations (professional standards and regulatory guidance for appropriate " +
        "risk-reduction strategies), to Generate the following personalized advantages and disadvantages " +
        "for each level of coverage:\n\n" +
        "1. Advantages. 1.1. List five advantages relevant to the user's attributes and decision context. " +
        "For each advantage, indicate the Confidence Level (CL), using the scale: extremely low, very low, " +
        "low, moderate, very high, extremely high. 1.2. The first advantage must be Relative Risk Reduction " +
        "(RRR). Provide the following: a) Calculate RRR (%), using the user's Baseline Risk (BR, %), defined " +
        "as the annual probability of out-of-pocket financial loss if uninsured, and indicate CL for RRR. " +
        "b) Report the result in the following format: BR = __%, RRR = __%, CL(RRR): ___.\n" +
        "2. Disadvantages. 2.1. List five disadvantages reflecting the user's challenges or barriers. For " +
        "each disadvantage, indicate the Confidence Level (CL), using the same scale. 2.2. The first " +
        "disadvantage must be Residual Risk (RR). Provide the following: a) Calculate RR as " +
        "RR = BR \u00d7 (1 \u2013 RRR) and indicate CL for RR. b) Report the result in the following " +
        "format: RR = __%, CL(RR): ___.\n\n" +
        "User Profile: [User Profile]\n\n" +
        "Levels of Coverage (Alternatives): Basic: [Basic-2] Standard: [Standard-2] Comprehensive: [Comprehensive-2]\n";

    private static final String PROMPT_2_PS_TEMPLATE =
        "For the decision-making Problem of selecting the appropriate level of [Asset-at-Risk] " +
        "Insurance coverage \u2013 Basic, Standard, or Comprehensive; " +
        "Problem Goal (PG): sufficiently reducing the risk of financial loss resulting from the " +
        "[Future risk event]; " +
        "Activity Goal (AG): receiving benefits and support after a covered loss;\n\n" +
        "Use the following sources: User Profile, Future risk event, Levels of Coverage (Alternatives), " +
        "Statistical Data (evidence-based estimates of baseline risk, expected risk reduction resulting " +
        "from each alternative, and outcome probabilities, derived from experimental studies, " +
        "observational data, actuarial analyses, and validated risk prediction models), and " +
        "Guideline-Based Recommendations (professional standards and regulatory guidance for appropriate " +
        "risk-reduction strategies), to Generate for each level of coverage the following personalized " +
        "advantages and disadvantages under both hypotheses:\n\n" +
        "Hypothesis 1: [Future risk event] happens.\n\n" +
        "1. Advantages: Relative Risk Reduction (RRR). a) Calculate RRR (%), using the user's Baseline " +
        "Risk (BR, %), defined as the annual probability of out-of-pocket financial loss if uninsured. " +
        "b) Indicate \u201cCL for RRR\u201d, using the scale: extremely low, very low, low, moderate, " +
        "very high, extremely high. c) Report the result in the following format: " +
        "BR = __%, RRR = __%, CL(RRR): ___.\n" +
        "2. Disadvantages: Difficulty in achieving AG \u2013 receiving benefits and support after a " +
        "covered loss. 2.1. Categorize the difficulty as material, physical, psychological, social, or " +
        "spiritual. 2.2. For each category (bullet formatting), provide: a) Description (brief); " +
        "b) Confidence Level (CL), using the scale: extremely low, very low, low, moderate, very high, " +
        "extremely high; c) Solution to address the difficulty (add justification based on the User " +
        "Profile where appropriate).\n\n" +
        "Hypothesis 2: [Future risk event] doesn't happen.\n\n" +
        "1. Advantages: Significance of achieving AG from the perspective of achieving PG. " +
        "1.1. Categorize the significance as material, physical, psychological, social, or spiritual. " +
        "1.2. For each category (bullet formatting), provide: a) Description (brief); b) Strength, using " +
        "the scale: extremely weak, very weak, weak, moderate, strong, very strong, extremely strong; " +
        "c) Evidence Level (EL), using the scale: extremely low, very low, low, moderate, high, very " +
        "high, extremely high; d) Strategy for risk reduction (add justification based on the User " +
        "Profile where appropriate).\n" +
        "2. Disadvantages: Residual Risk (RR). a) Calculate RR (%) as RR = BR \u00d7 (1 \u2013 RRR); " +
        "b) Indicate CL using the scale: extremely low, very low, low, moderate, high, very high, " +
        "extremely high. c) Report the result in the following format: RR = __%, CL(RR): ___.\n\n" +
        "User Profile: [User Profile]\n\n" +
        "Levels of Coverage (Alternatives): Basic: [Basic-2] Standard: [Standard-2] Comprehensive: [Comprehensive-2]\n\n" +
        "Future risk event: [Future risk event]\n";

    // ── Public build methods ──────────────────────────────────────────────────

    public String buildDmPrompt(InsuranceProfile profile, Map<String, Object> domain) {
        String completed = INSTRUCTIONS_PREFIX + PROMPT_1_DM_TEMPLATE
                .replace("[Asset-at-Risk]",    str(domain.get("domainName")))
                .replace("[User Profile]",     formatUserProfile(profile))
                .replace("[Basic-2]",          str(domain.get("basicFullDescription")))
                .replace("[Standard-2]",       str(domain.get("standardFullDescription")))
                .replace("[Comprehensive-2]",  str(domain.get("comprehensiveFullDescription")));

        verifyNoPlaceholders(completed, "DM");
        log.info("=== BUILT DM PROMPT ===\n{}", completed);
        return completed;
    }

    public String buildPsPrompt(InsuranceProfile profile, Map<String, Object> domain) {
        String riskEvent = str(domain.get("futureRiskEvent"));
        String completed = INSTRUCTIONS_PREFIX + PROMPT_2_PS_TEMPLATE
                .replace("[Asset-at-Risk]",     str(domain.get("domainName")))
                .replace("[Future risk event]", riskEvent)
                .replace("[User Profile]",      formatUserProfile(profile))
                .replace("[Basic-2]",           str(domain.get("basicFullDescription")))
                .replace("[Standard-2]",        str(domain.get("standardFullDescription")))
                .replace("[Comprehensive-2]",   str(domain.get("comprehensiveFullDescription")));

        verifyNoPlaceholders(completed, "PS");
        log.info("=== BUILT PS PROMPT ===\n{}", completed);
        return completed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Formats the 7-field InsuranceProfile into the [User Profile] block.
     * Field labels match the professor's placeholder specification exactly.
     */
    String formatUserProfile(InsuranceProfile p) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Asset-at-Risk: ").append(orUnknown(p.getAssetAtRisk())).append('\n');
        sb.append("2. Estimated Value of Asset / Exposure: ").append(orUnknown(p.getEstimatedValue())).append('\n');
        sb.append("3. Financial Capacity: ").append(orUnknown(p.getFinancialCapacity())).append('\n');
        sb.append("4. Geographic/Environmental/Situational Risk Factors: ").append(orUnknown(p.getGeographicRisk())).append('\n');
        String desc = p.getGeographicRiskDescription();
        if (desc != null && !desc.isBlank()) {
            sb.append("   Description: ").append(desc).append('\n');
        }
        sb.append("5. Risk Tolerance: ").append(orUnknown(p.getRiskTolerance())).append('\n');
        sb.append("6. Decision Preferences: ").append(orUnknown(p.getDecisionPreferences())).append('\n');
        sb.append("7. Engagement Style: ").append(orUnknown(p.getEngagementStyle()));
        return sb.toString();
    }

    private void verifyNoPlaceholders(String prompt, String mode) {
        boolean hasPlaceholder = prompt.contains("[Asset-at-Risk]")
                || prompt.contains("[User Profile]")
                || prompt.contains("[Basic-2]")
                || prompt.contains("[Standard-2]")
                || prompt.contains("[Comprehensive-2]")
                || prompt.contains("[Future risk event]");
        if (hasPlaceholder) {
            log.warn("PROMPT VERIFICATION FAILED ({}): remaining placeholders detected!", mode);
        } else {
            log.info("PROMPT VERIFICATION PASSED ({}): no remaining placeholders.", mode);
        }
    }

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private String orUnknown(String val) {
        return (val != null && !val.isBlank()) ? val : "Not specified";
    }
}
