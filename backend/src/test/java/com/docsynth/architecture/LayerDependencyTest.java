package com.docsynth.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
    packages = "com.docsynth",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application =
        classes().that().resideInAPackage("..domain..")
                 .should().onlyAccessClassesThat().resideOutsideOfPackage("..application..");

    @ArchTest
    static final ArchRule application_may_depend_on_domain_only =
        classes().that().resideInAPackage("..application..")
                 .should().onlyAccessClassesThat().resideInAnyPackage("..domain..", "..application..");

    @ArchTest
    static final ArchRule interfaces_depend_on_application_only =
        classes().that().resideInAPackage("..interfaces..")
                 .should().onlyAccessClassesThat().resideInAnyPackage("..application..", "..domain..");

    @ArchTest
    static final ArchRule no_cycles_between_bounded_contexts =
        slices().matching("com.docsynth.domain.(*)..")
                .should().beFreeOfCycles();
}
