/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.autofill.sync

import com.duckduckgo.autofill.impl.securestorage.SecureStorage
import com.duckduckgo.autofill.impl.securestorage.WebsiteLoginDetails
import com.duckduckgo.autofill.impl.securestorage.WebsiteLoginDetailsWithCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeSecureStorage : SecureStorage {
    private val entities = mutableListOf<WebsiteLoginDetailsWithCredentials>()

    override fun canAccessSecureStorage(): Boolean = true

    override suspend fun addWebsiteLoginDetailsWithCredentials(
        websiteLoginDetailsWithCredentials: WebsiteLoginDetailsWithCredentials,
    ): WebsiteLoginDetailsWithCredentials? {
        val newLogin = if (websiteLoginDetailsWithCredentials.details.id == null) {
            websiteLoginDetailsWithCredentials.copy(
                details = websiteLoginDetailsWithCredentials.details.copy(
                    id = entities.size.toLong() + 1,
                ),
            )
        } else {
            websiteLoginDetailsWithCredentials
        }
        entities.add(newLogin)
        return newLogin
    }

    override suspend fun websiteLoginDetailsForDomain(domain: String): Flow<List<WebsiteLoginDetails>> {
        TODO("Not yet implemented")
    }

    override suspend fun websiteLoginDetails(): Flow<List<WebsiteLoginDetails>> {
        TODO("Not yet implemented")
    }

    override suspend fun getWebsiteLoginDetailsWithCredentials(id: Long): WebsiteLoginDetailsWithCredentials? {
        return entities.find { it.details.id == id }
    }

    override suspend fun websiteLoginDetailsWithCredentialsForDomain(domain: String): Flow<List<WebsiteLoginDetailsWithCredentials>> {
        entities.find { it.details.domain?.contains(domain) == true }?.let {
            return flowOf(listOf(it))
        } ?: return flowOf(emptyList())
    }

    override suspend fun websiteLoginDetailsWithCredentials(): Flow<List<WebsiteLoginDetailsWithCredentials>> {
        return flowOf(entities)
    }

    override suspend fun updateWebsiteLoginDetailsWithCredentials(
        websiteLoginDetailsWithCredentials: WebsiteLoginDetailsWithCredentials,
    ): WebsiteLoginDetailsWithCredentials? {
        entities.find { it.details.id == websiteLoginDetailsWithCredentials.details.id }?.let {
            entities.remove(it)
            entities.add(websiteLoginDetailsWithCredentials)
            return websiteLoginDetailsWithCredentials
        } ?: return null
    }

    override suspend fun deleteWebsiteLoginDetailsWithCredentials(id: Long) {
        entities.find { it.details.id == id }?.let {
            entities.remove(it)
        }
    }
}
