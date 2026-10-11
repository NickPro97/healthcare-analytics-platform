package com.nikhil.analytics.scoring;

/** NEWS2 clinical risk bands (Royal College of Physicians, 2017). */
public enum RiskLevel {
    /** Aggregate 0-4 and no single parameter scoring 3. */
    LOW,
    /** Aggregate 0-4 but at least one parameter scored 3 ("red score"). */
    LOW_MEDIUM,
    /** Aggregate 5-6. */
    MEDIUM,
    /** Aggregate 7 or more. */
    HIGH
}
