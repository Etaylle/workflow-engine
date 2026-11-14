package engine.shared;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What the domain "execution" may read of the confirmed contracts of the domain "workflows". */
public interface ContractPort {

    /** One software task of the contract. */
    record TaskLine(String title, String lane, String description) {
    }

    /**
     * The latest confirmed contract.
     *
     * @param version contract version
     * @param costLimitUsd the limit chosen for the contract, or empty when none was chosen
     * @param markdown the whole contract as text (wish, answers, requirements, workflow)
     * @param softwareTasks the software tasks in the order of the workflow
     */
    record ContractInfo(int version, Optional<BigDecimal> costLimitUsd, String markdown,
            List<TaskLine> softwareTasks) {
    }

    /** The latest confirmed contract of the project, or empty when there is none. */
    Optional<ContractInfo> latestContract(UUID projectId);
}
