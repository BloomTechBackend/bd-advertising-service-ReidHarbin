package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;
    private boolean allTruePredicatesTracker = true;


    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();
        ExecutorService executorService = Executors.newCachedThreadPool();

        targetingPredicates.forEach(targetingPredicate -> executorService.submit(() -> {
            if (!targetingPredicate.evaluate(this.requestContext).isTrue()) {
                this.allTruePredicatesTracker = false;
            }
        }));

        executorService.shutdown();
        // Shuts down the ExecutorService by calling shutDownNow() to cancel any lingering tasks if necessary
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Executor Service did not terminate");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        boolean allTruePredicates = this.allTruePredicatesTracker;
        this.allTruePredicatesTracker = resetAllTruePredicatesTracker();

        return allTruePredicates ? TargetingPredicateResult.TRUE : TargetingPredicateResult.FALSE;
    }

    // Needed to reset the tracker for future calls to this evaluator
    private boolean resetAllTruePredicatesTracker() {
        return true;
    }
}
