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

package com.duckduckgo.autofill.impl

import com.duckduckgo.autofill.api.AutofillCapabilityChecker
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.api.passwordgeneration.AutomaticSavedLoginsMonitor
import com.duckduckgo.autofill.api.store.AutofillStore
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

class RealDuckAddressLoginCreatorTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val autofillStore: AutofillStore = mock()
    private val automaticSavedLoginsMonitor: AutomaticSavedLoginsMonitor = mock()
    private val autofillCapabilityChecker: AutofillCapabilityChecker = mock()

    private val testee = RealDuckAddressLoginCreator(
        autofillStore = autofillStore,
        autoSavedLoginsMonitor = automaticSavedLoginsMonitor,
        autofillCapabilityChecker = autofillCapabilityChecker,
        dispatchers = coroutineTestRule.testDispatcherProvider,
        appCoroutineScope = coroutineTestRule.testScope,
    )

    @Test
    fun whenAutofillCapabilitiesRestrictSavingThenNoLoginCreated() = runTest {
        whenever(autofillCapabilityChecker.canSaveCredentialsFromWebView(URL)).thenReturn(false)
        testee.createLoginForPrivateDuckAddress(DUCK_ADDRESS, TAB_ID, URL)
        verifyNotSavedOrUpdated()
    }

    @Test
    fun whenNoAutoSavedLoginIdThenNewLoginSaved() = runTest {
        whenever(autofillCapabilityChecker.canSaveCredentialsFromWebView(URL)).thenReturn(true)
        whenever(automaticSavedLoginsMonitor.getAutoSavedLoginId(TAB_ID)).thenReturn(null)
        testee.createLoginForPrivateDuckAddress(DUCK_ADDRESS, TAB_ID, URL)
        verifyLoginSaved()
    }

    @Test
    fun whenAutoSavedLoginIdSetButNoMatchingLoginFoundThenNewLoginSaved() = runTest {
        whenever(autofillCapabilityChecker.canSaveCredentialsFromWebView(URL)).thenReturn(true)
        whenever(automaticSavedLoginsMonitor.getAutoSavedLoginId(TAB_ID)).thenReturn(1)
        whenever(autofillStore.getCredentialsWithId(1)).thenReturn(null)
        testee.createLoginForPrivateDuckAddress(DUCK_ADDRESS, TAB_ID, URL)
        verifyLoginSaved()
    }

    @Test
    fun whenAutoSavedLoginFoundAndDetailsAlreadyMatchThenNotSavedOrUpdated() = runTest {
        val existingLogin = aLogin(id = 1, username = DUCK_ADDRESS)
        whenever(autofillCapabilityChecker.canSaveCredentialsFromWebView(URL)).thenReturn(true)
        whenever(automaticSavedLoginsMonitor.getAutoSavedLoginId(TAB_ID)).thenReturn(1)
        whenever(autofillStore.getCredentialsWithId(1)).thenReturn(existingLogin)

        testee.createLoginForPrivateDuckAddress(DUCK_ADDRESS, TAB_ID, URL)
        verifyNotSavedOrUpdated()
    }

    @Test
    fun whenAutoSavedLoginFoundAndUsernameDifferentThenLoginUpdated() = runTest {
        val existingLogin = aLogin(id = 1, username = "different-username")
        whenever(autofillCapabilityChecker.canSaveCredentialsFromWebView(URL)).thenReturn(true)
        whenever(automaticSavedLoginsMonitor.getAutoSavedLoginId(TAB_ID)).thenReturn(1)
        whenever(autofillStore.getCredentialsWithId(1)).thenReturn(existingLogin)

        testee.createLoginForPrivateDuckAddress(DUCK_ADDRESS, TAB_ID, URL)
        verifyLoginUpdated()
    }

    private suspend fun verifyLoginSaved() = verify(autofillStore).saveCredentials(eq(URL), any())
    private suspend fun verifyLoginUpdated() = verify(autofillStore).updateCredentials(any())

    private suspend fun verifyNotSavedOrUpdated() {
        verify(autofillStore, never()).saveCredentials(any(), any())
        verify(autofillStore, never()).updateCredentials(any())
    }

    private fun aLogin(id: Long? = null, username: String? = null, password: String? = null): LoginCredentials {
        return LoginCredentials(id = id, domain = "example.com", username = username, password = password)
    }

    companion object {
        private const val TAB_ID = "tab-id-123"
        private const val URL = "example.com"
        private const val DUCK_ADDRESS = "foo@duck.com"
    }
}
