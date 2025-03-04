// Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.credentials.pinning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.replaceService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import software.aws.toolkits.jetbrains.core.credentials.AwsBearerTokenConnection
import software.aws.toolkits.jetbrains.core.credentials.ToolkitAuthManager
import software.aws.toolkits.jetbrains.core.credentials.ToolkitConnection

class ConnectionPinningManagerTest {
    @Rule
    @JvmField
    val projectRule = ProjectRule()

    @Rule
    @JvmField
    val disposableRule = DisposableRule()

    private lateinit var sut: DefaultConnectionPinningManager

    @Before
    fun setUp() {
        sut = spy(DefaultConnectionPinningManager(projectRule.project))
    }

    @Test
    fun `switching connection to unsupported feature pins connection to initial if user allows`() {
        sut.stub {
            onGeneric { it.showDialogIfNeeded(any(), any(), any()) } doReturn true
        }

        val feature = object : FeatureWithPinnedConnection {
            override val featureId = "mockId"
            override val featureName = "mockFeature"
            override fun supportsConnectionType(connection: ToolkitConnection) = true
        }

        val oldConnection = mock<AwsBearerTokenConnection>() {
            on { id } doReturn "connId"
        }
        val mockAuthManager = mock<ToolkitAuthManager> {
            whenever(it.getConnection(any())).thenReturn(oldConnection)
        }
        ApplicationManager.getApplication().replaceService(ToolkitAuthManager::class.java, mockAuthManager, disposableRule.disposable)

        sut.maybePinFeatures(oldConnection, mock<AwsBearerTokenConnection>(), listOf(feature))

        assertThat(sut.getPinnedConnection(feature)).isEqualTo(oldConnection)
    }

    @Test
    fun `switching connection to unsupported feature does not pin connection to initial if user declines`() {
        sut.stub {
            onGeneric { it.showDialogIfNeeded(any(), any(), any()) } doReturn false
        }

        val feature = object : FeatureWithPinnedConnection {
            override val featureId = "mockId"
            override val featureName = "mockFeature"
            override fun supportsConnectionType(connection: ToolkitConnection) = true
        }

        val oldConnection = mock<AwsBearerTokenConnection>() {
            on { id } doReturn "connId"
        }
        val mockAuthManager = mock<ToolkitAuthManager> {
            whenever(it.getConnection(any())).thenReturn(oldConnection)
        }
        ApplicationManager.getApplication().replaceService(ToolkitAuthManager::class.java, mockAuthManager, disposableRule.disposable)

        sut.maybePinFeatures(oldConnection, mock<AwsBearerTokenConnection>(), listOf(feature))

        assertThat(sut.getPinnedConnection(feature)).isNull()
    }

    @Test
    fun `pinned connection returns null if connection no longer exists in auth manager`() {
        val feature = object : FeatureWithPinnedConnection {
            override val featureId = "mockId"
            override val featureName = "mockFeature"
            override fun supportsConnectionType(connection: ToolkitConnection) = true
        }
        val connection = mock<AwsBearerTokenConnection>() {
            on { id } doReturn "connId"
        }

        sut.setPinnedConnection(feature, connection)

        assertThat(sut.getPinnedConnection(feature)).isNull()
    }

    @Test
    fun `respects pinned feature`() {
        val feature = object : FeatureWithPinnedConnection {
            override val featureId = "mockId"
            override val featureName = "mockFeature"
            override fun supportsConnectionType(connection: ToolkitConnection) = true
        }
        val connection = mock<AwsBearerTokenConnection>() {
            on { id } doReturn "connId"
        }
        val mockAuthManager = mock<ToolkitAuthManager> {
            whenever(it.getConnection(any())).thenReturn(connection)
        }
        ApplicationManager.getApplication().replaceService(ToolkitAuthManager::class.java, mockAuthManager, disposableRule.disposable)

        sut.setPinnedConnection(feature, connection)

        assertThat(sut.getPinnedConnection(feature)).isEqualTo(connection)
    }
}
