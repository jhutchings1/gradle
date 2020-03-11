/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.instantexecution

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.BuildOperationsFixture
import org.gradle.integtests.fixtures.DefaultTestExecutionResult
import org.gradle.integtests.fixtures.instantexecution.InstantExecutionBuildOperationsFixture
import org.gradle.internal.logging.ConsoleRenderer
import org.intellij.lang.annotations.Language

import javax.annotation.Nullable
import java.nio.file.Paths
import java.util.regex.Pattern

import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.CoreMatchers.notNullValue
import static org.hamcrest.CoreMatchers.nullValue
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class AbstractInstantExecutionIntegrationTest extends AbstractIntegrationSpec {

    void buildKotlinFile(@Language("kotlin") String script) {
        buildKotlinFile << script
    }

    void instantRun(String... args) {
        run(INSTANT_EXECUTION_PROPERTY, *args)
    }

    void instantFails(String... args) {
        fails(INSTANT_EXECUTION_PROPERTY, *args)
    }

    public static final String INSTANT_EXECUTION_PROPERTY = "-Dorg.gradle.unsafe.instant-execution=true"

    protected InstantExecutionBuildOperationsFixture newInstantExecutionFixture() {
        return new InstantExecutionBuildOperationsFixture(new BuildOperationsFixture(executer, temporaryFolder))
    }

    protected void withDoNotFailOnProblems() {
        executer.withArgument("-D${SystemProperties.failOnProblems}=false")
    }

    protected void withFailOnProblems() {
        executer.withArgument("-D${SystemProperties.failOnProblems}=true")
    }

    protected void expectNoInstantExecutionProblem() {
        verifyDeprecationWarnings(executer.workingDir, 0, [])
    }

    protected void expectInstantExecutionProblems(
        int count = problems.length,
        String... problems
    ) {
        if (problems.length == 0) {
            throw new IllegalArgumentException("Use expectNoInstantExecutionProblem() when no deprecation warnings are to be expected")
        }
        verifyDeprecationWarnings(executer.workingDir, count, problems as List)
    }

    private void verifyDeprecationWarnings(
        File rootDir = executer.workingDir,
        int totalProblemsCount,
        List<String> uniqueProblems
    ) {
        assertProblemsConsoleSummaryHeaderFor(totalProblemsCount, uniqueProblems.size())
        assertProblemsConsoleReport(uniqueProblems)
        assertProblemReportGeneration(totalProblemsCount, uniqueProblems)
    }

    private void assertProblemsConsoleSummaryHeaderFor(int totalProblems, int uniqueProblems) {
        if (totalProblems > 0 || uniqueProblems > 0) {
            def header = "${totalProblems} instant execution problem${totalProblems >= 2 ? 's were' : ' was'} found, " +
                "${uniqueProblems} of which seem${uniqueProblems >= 2 ? '' : 's'} unique:"
            assertThat(output, containsNormalizedString(header))
        } else {
            assertThat(output, not(containsNormalizedString("instant execution problem")))
        }
    }

    private void assertProblemsConsoleReport(List<String> uniqueProblems) {
        def uniqueProblemsCount = uniqueProblems.size()
        def found = 0
        def output = resultOrFailureOutput()
        output.readLines().eachWithIndex { String line, int idx ->
            if (uniqueProblems.remove(line.trim())) {
                found++
                return
            }
        }
        assert uniqueProblems.empty, "Expected ${uniqueProblemsCount} unique problems, found ${found} unique problems, remaining:\n${uniqueProblems.collect { " - $it" }.join("\n")}"
    }

    private void assertProblemReportGeneration(int totalProblemCount, List<String> uniqueProblems) {
        def expectReport = totalProblemCount > 0 || uniqueProblems.size() > 0
        def reportDir = resolveInstantExecutionReportDirectory()
        if (expectReport) {
            assertThat("HTML report URI not found", reportDir, notNullValue())
            assertTrue("HTML report directory not found '$reportDir'", reportDir.isDirectory())
            assertTrue("HTML report HTML file not found in '$reportDir'", new File(reportDir, 'instant-execution-report.html').isFile())
            assertTrue("HTML report JS model not found in '$reportDir'", new File(reportDir, 'instant-execution-report-data.js').isFile())
        } else {
            assertThat("Unexpected HTML report URI found", reportDir, nullValue())
        }
    }

    @Nullable
    private File resolveInstantExecutionReportDirectory() {
        def baseDirUri = new ConsoleRenderer().asClickableFileUrl(new File(executer.workingDir, "build/reports/instant-execution"))
        def pattern = Pattern.compile("See the complete report at (${baseDirUri}.*)instant-execution-report.html")
        def reportDirUri = resultOrFailureOutput().readLines().findResult { line ->
            def matcher = pattern.matcher(line)
            matcher.matches() ? matcher.group(1) : null
        }
        return reportDirUri ? Paths.get(URI.create(reportDirUri)).toFile() : null
    }

    private String resultOrFailureOutput() {
        return result?.output ?: failure?.output ?: ''
    }

    protected static String clickableUrlFor(File file) {
        new ConsoleRenderer().asClickableFileUrl(file)
    }

    protected void assertTestsExecuted(String testClass, String... testNames) {
        new DefaultTestExecutionResult(testDirectory)
            .testClass(testClass)
            .assertTestsExecuted(testNames)
    }
}
