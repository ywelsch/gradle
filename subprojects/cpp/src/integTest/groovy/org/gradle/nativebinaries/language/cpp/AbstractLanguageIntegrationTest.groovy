/*
 * Copyright 2013 the original author or authors.
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


package org.gradle.nativebinaries.language.cpp
import org.gradle.nativebinaries.language.cpp.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativebinaries.language.cpp.fixtures.app.HelloWorldApp
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

abstract class AbstractLanguageIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {

    abstract HelloWorldApp getHelloWorldApp()

    def "setup"() {
        // TODO:DAZ Only apply the required language plugins
        buildFile << """
            apply plugin: 'assembler'
            apply plugin: 'c'
            apply plugin: 'cpp'
"""
    }

    def "compile and link executable"() {
        given:
        buildFile << """
            executables {
                main {}
            }
            binaries.all {
                $helloWorldApp.customArgs
            }
        """

        and:
        helloWorldApp.writeSources(file("src/main"))

        when:
        run "mainExecutable"

        then:
        def mainExecutable = executable("build/binaries/mainExecutable/main")
        mainExecutable.assertExists()
        mainExecutable.exec().out == helloWorldApp.englishOutput
    }

    def "build executable with custom compiler arg"() {
        given:
        buildFile << """
            executables {
                main {
                    binaries.all {
                        cppCompiler.args "-DFRENCH"
                        cCompiler.args "-DFRENCH"
                    }
                }
            }
            binaries.all {
                $helloWorldApp.customArgs
            }
        """

        and:
        helloWorldApp.writeSources(file("src/main"))

        when:
        run "mainExecutable"

        then:
        def mainExecutable = executable("build/binaries/mainExecutable/main")
        mainExecutable.assertExists()
        mainExecutable.exec().out == helloWorldApp.frenchOutput
    }

    def "build executable with macro defined"() {
        given:
        buildFile << """
            executables {
                main {
                    binaries.all {
                        define "FRENCH"
                    }
                }
            }
            binaries.all {
                $helloWorldApp.customArgs
            }
        """

        and:
        helloWorldApp.writeSources(file("src/main"))

        when:
        run "mainExecutable"

        then:
        def mainExecutable = executable("build/binaries/mainExecutable/main")
        mainExecutable.assertExists()
        mainExecutable.exec().out == helloWorldApp.frenchOutput
    }

    @Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
    def "build shared library and link into executable"() {
        given:
        buildFile << """
            executables {
                main {}
            }
            libraries {
                hello {}
            }
            sources.main.c.lib libraries.hello
            binaries.all {
                $helloWorldApp.customArgs
            }
        """

        and:
        helloWorldApp.writeSources(file("src/main"), file("src/hello"))

        when:
        run "installMainExecutable"

        then:
        sharedLibrary("build/binaries/helloSharedLibrary/hello").assertExists()
        executable("build/binaries/mainExecutable/main").assertExists()

        def install = installation("build/install/mainExecutable")
        install.assertInstalled()
        install.assertIncludesLibraries("hello")
        install.exec().out == helloWorldApp.englishOutput
    }

    @Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
    def "build static library and link into executable"() {
        given:
        buildFile << """
            executables {
                main {}
            }
            libraries {
                hello {
                    binaries.withType(StaticLibraryBinary) {
                        define "FRENCH"
                    }
                }
            }
            sources.main.c.lib libraries.hello.static
            binaries.all {
                $helloWorldApp.customArgs
            }
        """

        and:
        helloWorldApp.writeSources(file("src/main"), file("src/hello"))

        when:
        run "installMainExecutable"

        then:
        staticLibrary("build/binaries/helloStaticLibrary/hello").assertExists()
        executable("build/binaries/mainExecutable/main").assertExists()

        and:
        def install = installation("build/install/mainExecutable")
        install.assertInstalled()
        install.exec().out == helloWorldApp.frenchOutput
    }
}

