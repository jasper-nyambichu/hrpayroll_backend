// audit/Auditable.java
package com.company.hrpayroll.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Usage: @Auditable(action = "AGENT_PAYMENTS_CALCULATED", entityType = "AgentSettlement")
// placed directly on a service method — e.g. AgentSettlementController.calculate.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
    String entityType();
}