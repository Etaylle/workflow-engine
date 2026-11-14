package engine;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Domains stay apart: a domain package uses another domain only through {@code engine.shared}. */
@AnalyzeClasses(packages = "engine", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainsDoNotDependOnEachOther =
            slices().matching("engine.(*)..")
                    .should()
                    .notDependOnEachOther()
                    .ignoreDependency(
                            com.tngtech.archunit.base.DescribedPredicate.alwaysTrue(),
                            com.tngtech.archunit.core.domain.JavaClass.Predicates
                                    .resideInAPackage("engine.shared.."));
}
