/*
 * Copyright 2014-2016 the original author or authors.
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
package com.mooregreatsoftware.gradle.defaults.config

import com.mooregreatsoftware.gradle.defaults.Ternary
import com.mooregreatsoftware.gradle.defaults.postEvalCreate
import org.gradle.api.Project
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.Future


/**
 * Returns a handle to the configuration in the given Project.
 *
 * @param project the project containing the configuration
 */
inline fun <reified T> ofFuture(project: Project, creator: (Project) -> Future<T?>): Future<T?> {
    val className = T::class.qualifiedName
    @Suppress("UNCHECKED_CAST")
    val configFuture = project.extensions.findByName(className) as Future<T?>?
    return when (configFuture) {
        null -> {
            val future = creator(project)
            project.extensions.add(className, future)
            future
        }
        else -> configFuture
    }
}


/**
 * Returns a handle to the configuration in the given Project.
 *
 * @param project the project containing the configuration
 */
inline fun <reified T> ofInstance(project: Project, creator: (Project) -> T): Future<T?> {
    return ofFuture(project, { completedFuture(creator(project)) })
}


fun <T> confFuture(project: Project,
                   confName: String,
                   enabledEval: () -> Ternary,
                   doDetect: () -> Boolean,
                   creator: () -> T,
                   notFlaggedWarnMsg: String?): Future<T?> {
    return when (enabledEval()) {
        Ternary.FALSE -> completedFuture(project.noConf(confName))
        Ternary.TRUE -> completedFuture(project.withConf(confName, creator))
        Ternary.MAYBE -> {
            project.postEvalShouldUseConfig(confName,
                enabledEval,
                doDetect, creator,
                notFlaggedWarnMsg
            )
        }
    }
}

private fun <T> Project.noConf(confName: String): T? {
    project.logger.info("Not configuring {} for {}", confName, name)
    return null
}

private fun <T> Project.withConf(confName: String, creator: () -> T): T {
    logger.info("Configuring {} for {}", confName, name)
    return creator()
}

/**
 * Returns a Future that returns a value after the Project has been fully evaluated. This gives a chance for
 * any flags to have been set before giving a "definitive" answer on whether or not the Config should be used.
 *
 * @return a Future that will be evaluated after the project has been evaluated; will contain a `null` if
 * the Config should not be used.
 */
private fun <T> Project.postEvalShouldUseConfig(confName: String, enabledEval: () -> Ternary,
                                                doDetect: () -> Boolean, creator: () -> T,
                                                notFlaggedWarnMsg: String?): Future<T?> {
    return postEvalCreate { postEvalDetect(confName, enabledEval, doDetect, creator, notFlaggedWarnMsg) }
}


private fun <T> Project.postEvalDetect(confName: String, enabledEval: () -> Ternary,
                                       doDetect: () -> Boolean, creator: () -> T,
                                       notFlaggedWarnMsg: String?): T? {
    return when (enabledEval()) {
        Ternary.FALSE -> noConf(confName)
        Ternary.TRUE -> withConf(confName, creator)
        Ternary.MAYBE -> postEvalDetectMaybe(confName, doDetect, creator, notFlaggedWarnMsg)
    }
}

private fun <T> Project.postEvalDetectMaybe(confName: String, doDetect: () -> Boolean, creator: () -> T,
                                            notFlaggedWarnMsg: String?): T? {
    if (doDetect()) {
        if (notFlaggedWarnMsg != null)
            logger.warn(notFlaggedWarnMsg)
        return withConf(confName, creator)
    }
    else return noConf(confName)
}
