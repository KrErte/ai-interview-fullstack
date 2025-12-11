package ee.kerrete.ainterview.softskills.catalog.service;

import ee.kerrete.ainterview.softskills.catalog.entity.SoftSkillDimension;

import java.util.List;

final class SoftSkillDimensionSeedData {

    private SoftSkillDimensionSeedData() {
    }

    static List<SoftSkillDimension> all() {
        return List.of(
            dim("communication", "Communication", "Clearly conveys ideas to different audiences.",
                list("Structures messages logically", "Tailors depth to audience", "Seeks confirmation of understanding"),
                list("Overloads with detail", "Assumes shared context", "Avoids hard conversations"),
                list("Ask them to explain a complex topic simply", "Probe how they handled miscommunications"),
                list("Practice concise summaries", "Check for understanding", "Invite feedback on clarity")),
            dim("collaboration", "Collaboration", "Works effectively with others to achieve shared outcomes.",
                list("Shares credit", "Seeks diverse input", "Supports teammates under pressure"),
                list("Works in silos", "Blames others publicly", "Withholds info"),
                list("Describe a time they unblocked a peer", "How they handled conflicting priorities across teams"),
                list("Set clear shared goals", "Do regular syncs", "Use blameless retros")),
            dim("ownership", "Ownership", "Takes responsibility end-to-end and follows through.",
                list("Anticipates risks", "Surfaces issues early", "Finishes strong"),
                list("Passes buck", "Lets blockers linger", "Leaves loose ends"),
                list("Example of owning a messy problem", "How they handled failure in their area"),
                list("Define clear owners", "Use decision logs", "Celebrate follow-through")),
            dim("problem_solving", "Problem Solving", "Analyzes and resolves issues effectively.",
                list("Clarifies the problem before solutioning", "Uses data and experiments", "Considers trade-offs"),
                list("Jumps to solutions", "Ignores constraints", "Fails to validate"),
                list("Walk through a recent tough problem", "How they chose among options"),
                list("Write down problem statements", "Compare options explicitly", "Do small experiments")),
            dim("learning_agility", "Learning Agility", "Learns quickly and adapts to new contexts.",
                list("Seeks feedback", "Experiments and iterates", "Transfers learnings across domains"),
                list("Resists new tools", "Repeats past mistakes", "Avoids feedback"),
                list("Example of quickly picking up a new domain", "How they respond to feedback"),
                list("Set personal learning goals", "Do regular retros", "Shadow experts")),
            dim("stress_management", "Stress Management", "Stays composed and prioritizes under pressure.",
                list("Communicates early", "Re-prioritizes ruthlessly", "Protects team focus"),
                list("Goes silent", "Spreads anxiety", "Burns out teammates"),
                list("Describe a high-pressure incident", "How they set boundaries under load"),
                list("Use checklists", "Limit WIP", "Schedule recovery time")),
            dim("adaptability", "Adaptability", "Adjusts approach based on changing conditions.",
                list("Reframes plans quickly", "Re-evaluates assumptions", "Keeps stakeholders aligned"),
                list("Digs into sunk costs", "Denies new info", "Overcommits to old plan"),
                list("Time they pivoted", "How they handled shifting priorities"),
                list("Run pre-mortems", "Keep option space open", "Use lightweight plans")),
            dim("leadership", "Leadership", "Inspires, aligns, and elevates others.",
                list("Provides clear direction", "Recognizes others", "Protects team health"),
                list("Leads via authority only", "Takes credit", "Micromanages"),
                list("Story of leading without authority", "How they grow others"),
                list("Clarify vision regularly", "Delegate outcomes", "Coach 1:1s")),
            dim("stakeholder_management", "Stakeholder Management", "Manages expectations and alignment across parties.",
                list("Maps stakeholders", "Sets clear expectations", "Shares timely updates"),
                list("Surprises stakeholders", "Ignores dissent", "Overpromises"),
                list("Example of navigating conflicting asks", "How they handle bad news"),
                list("RACI for key work", "Regular check-ins", "Share risks early")),
            dim("decision_making", "Decision Making", "Makes timely, sound decisions with clear rationale.",
                list("Clarifies decision type/owner", "Uses principles and data", "Calls decisions at the right time"),
                list("Endless analysis", "Opaque rationale", "Defers decisions excessively"),
                list("Walk through a recent decision", "How they handled limited data"),
                list("Set decision principles", "Timebox options", "Record decisions and revisit")),
            dim("conflict_management", "Conflict Management", "Navigates disagreements constructively.",
                list("Separates people from problems", "Finds shared goals", "Escalates appropriately"),
                list("Avoids conflict", "Makes it personal", "Escalates too late"),
                list("Example of resolving conflict", "How they respond to criticism"),
                list("Use mediators early", "Focus on interests", "Document agreements")),
            dim("inclusiveness", "Inclusiveness", "Creates space where all voices contribute.",
                list("Invites diverse viewpoints", "Interrupts interruptions", "Credits ideas properly"),
                list("Talks over others", "Assumes intent negatively", "Excludes quieter members"),
                list("How they ensure inclusivity in meetings", "Handling biased behavior"),
                list("Rotate facilitation", "Establish norms", "Solicit written input")),
            dim("proactivity", "Proactivity", "Anticipates needs and acts without being asked.",
                list("Spots and tackles issues early", "Brings options, not just problems", "Seeks impact autonomously"),
                list("Waits for direction", "Flags problems without options", "Ignores low-hanging wins"),
                list("Example of proactive improvement", "How they choose where to act"),
                list("Keep a risks/opps log", "Share proposals early", "Align on impact metrics")),
            dim("resilience", "Resilience", "Bounces back from setbacks and keeps momentum.",
                list("Reframes setbacks", "Seeks support", "Finds next step quickly"),
                list("Dwells on failure", "Withdraws", "Spreads pessimism"),
                list("Story of a setback and recovery", "How they manage energy"),
                list("Use recovery rituals", "Reflect on learnings", "Set small wins")),
            dim("attention_to_detail", "Attention to Detail", "Produces accurate, high-quality work.",
                list("Double-checks critical work", "Uses checklists", "Catches edge cases"),
                list("Frequent avoidable errors", "Skips reviews", "Ignores specs"),
                list("Describe QA habits", "Example of preventing a defect"),
                list("Use templates", "Peer reviews", "Automate checks")),
            dim("ownership_boundary_setting", "Ownership & Boundaries", "Balances accountability with healthy limits.",
                list("Clarifies scope", "Says no when needed", "Escalates capacity risks"),
                list("Accepts everything", "Scope creeps silently", "Burns out to deliver"),
                list("How they set boundaries", "Example of renegotiating scope"),
                list("Define service levels", "Negotiate trade-offs", "Share capacity openly"))
        );
    }

    private static SoftSkillDimension dim(String key, String label, String def,
                                          List<String> high, List<String> low, List<String> interview, List<String> coaching) {
        return SoftSkillDimension.builder()
            .dimensionKey(key)
            .label(label)
            .definition(def)
            .highSignals(toJson(high))
            .lowSignals(toJson(low))
            .interviewSignals(toJson(interview))
            .coachingIdeas(toJson(coaching))
            .build();
    }

    private static List<String> list(String... v) {
        return List.of(v);
    }

    private static String toJson(List<String> values) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }
}

