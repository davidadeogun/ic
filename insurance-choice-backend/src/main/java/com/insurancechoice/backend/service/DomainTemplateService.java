package com.insurancechoice.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides hardcoded insurance domain template data for the 7 supported domains.
 * No database persistence — this is static reference/config data.
 */
@Service
public class DomainTemplateService {

    private static final Map<String, Map<String, Object>> DOMAINS = new LinkedHashMap<>();

    static {
        add("Auto",
            "Accident, theft, weather damage, or vandalism",
            "Liability-Only",
            "Liability-Only (State minimum BI/PD liability; no physical damage)",
            "Liability + Extended Coverage",
            "Liability + Extended Coverage (adds Collision and Other-Than-Collision protection for the vehicle, with moderate limits/deductibles; optional rental/roadside)",
            "Full Coverage",
            "Full Coverage (Higher limits; lower deductibles; add-ons like GAP, OEM parts, new-car replacement)");

        add("Home/Property",
            "Fire, storm, theft, water damage, or vandalism",
            "Basic Coverage",
            "Basic Coverage (Dwelling + liability; ACV contents; $1k deductible)",
            "Expanded Coverage",
            "Expanded Coverage (RCV dwelling/contents; higher liability; extended replacement; water backup)",
            "Comprehensive Coverage",
            "Comprehensive Coverage (Open-perils; higher liability; broad add-ons (service line repairs, umbrella liability))");

        add("Health",
            "Illness, injury, chronic conditions, hospitalization, or high-cost treatment",
            "High-Deductible Plan",
            "High-Deductible Plan (Low premium; high deductible; HSA option)",
            "Standard Plan",
            "Standard Plan (Moderate deductible; copays + coinsurance; balanced coverage)",
            "Comprehensive Plan",
            "Comprehensive Plan (Low/zero deductible; low OOP max; strong copay structure)");

        add("Life",
            "Premature death causing income loss, immediate costs (funeral, debts), and ongoing dependent support needs",
            "Term Life",
            "Term Life (10–20 years; fixed benefit; no cash value)",
            "Extended Term Life",
            "Extended Term Life (Laddered or 30-year coverage; tailored to obligations)",
            "Permanent Life",
            "Permanent Life (Lifetime coverage; cash value; optional policy add-ons)");

        add("Business",
            "Property loss, liability claims, workplace injuries, legal actions, or business interruption",
            "Basic Policy",
            "Basic Policy (Property + liability + limited income; basic endorsements)",
            "Expanded Policy",
            "Expanded Policy (Broader property/liability; stronger income protection; optional cyber/EPLI)",
            "Comprehensive Package",
            "Comprehensive Package (Tailored coverages; higher limits; umbrella, cyber, or E&O)");

        add("Cyber",
            "Cyberattack (including BEC/phishing), data breach, ransomware, or system outage causing data loss, business interruption, and financial/legal exposure",
            "Basic Protection",
            "Basic Protection (Incident response, forensics, data restoration; modest limits)",
            "Expanded Protection",
            "Expanded Protection (First- and third-party; liability + regulatory defense; ransomware)",
            "Comprehensive Protection",
            "Comprehensive Protection (Broad first/third-party; system failure BI; high limits; pre-loss services)");

        add("Reputation",
            "Defamation, misinformation, online attacks, negative media, social-media crises, or scandals causing loss of trust, revenue, or brand value",
            "Basic Coverage",
            "Basic Coverage (Crisis communications; media monitoring; $250k–$500k limits)",
            "Expanded Coverage",
            "Expanded Coverage (Crisis response + lost income + liability defense; $1M+ limits)",
            "Comprehensive Coverage",
            "Comprehensive Coverage (Global PR + income protection + cyber-linked harm; $5M+ limits)");
    }

    private static void add(String domain, String futureRiskEvent,
                             String basicShort,        String basicFull,
                             String standardShort,     String standardFull,
                             String comprehensiveShort, String comprehensiveFull) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("domainName",                 domain);
        m.put("futureRiskEvent",            futureRiskEvent);
        m.put("basicShortName",             basicShort);
        m.put("basicFullDescription",       basicFull);
        m.put("standardShortName",          standardShort);
        m.put("standardFullDescription",    standardFull);
        m.put("comprehensiveShortName",     comprehensiveShort);
        m.put("comprehensiveFullDescription", comprehensiveFull);
        DOMAINS.put(domain, m);
    }

    /**
     * Returns the domain template for the given assetAtRisk label (e.g. "Auto").
     * Throws 404 if not found.
     */
    public Map<String, Object> getByAssetAtRisk(String assetAtRisk) {
        Map<String, Object> domain = DOMAINS.get(assetAtRisk);
        if (domain == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Domain template not found for: " + assetAtRisk);
        }
        return domain;
    }

    /**
     * Returns null (no exception) when assetAtRisk is not recognised — used for
     * optional enrichment in template building.
     */
    public Map<String, Object> findByAssetAtRisk(String assetAtRisk) {
        return DOMAINS.get(assetAtRisk);
    }
}
