package dk.frankbille.iou.graphql

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import dk.frankbille.iou.graphql.generated.ViewerFamiliesQuery
import okio.Buffer

internal enum class FamilyNameSource {
    CACHE,
    NETWORK,
}

internal data class FamilyNameResult(
    val familyName: String,
    val source: FamilyNameSource,
)

internal class GraphqlSessionStore(
    initialServerUrl: String = "http://localhost:8080/graphql",
    initialJwt: String = "",
) {
    var serverUrl by mutableStateOf(initialServerUrl)
    var jwt by mutableStateOf(initialJwt)

    fun trimmedServerUrl(): String = serverUrl.trim()

    fun bearerTokenOrNull(): String? = jwt.trim().takeIf { it.isNotEmpty() }

    fun tokenHash(): Int = bearerTokenOrNull()?.hashCode() ?: 0
}

internal class FamilyNameRepository(
    private val sessionStore: GraphqlSessionStore,
) {
    private val clientFactory = ApolloClientFactory(sessionStore = sessionStore)
    private val snapshotStorage = ViewerFamiliesSnapshotStorage()

    suspend fun loadFamilyName(): FamilyNameResult {
        readCachedFamilyNameOrNull()?.let {
            return it
        }

        return refreshFamilyName()
    }

    suspend fun readCachedFamilyName(): FamilyNameResult =
        readCachedFamilyNameOrNull()
            ?: throw IllegalStateException("No cached family name is available yet.")

    suspend fun refreshFamilyName(): FamilyNameResult {
        val client = clientFactory.client()
        hydratePersistentCache(client)
        val response =
            client
                .query(ViewerFamiliesQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

        val result = response.requireFamilyName(source = FamilyNameSource.NETWORK)
        response.data?.let { data ->
            snapshotStorage.save(
                PersistedViewerFamiliesSnapshot(
                    serverUrl = sessionStore.trimmedServerUrl(),
                    tokenHash = sessionStore.tokenHash(),
                    payload = serializeViewerFamiliesData(data),
                ),
            )
        }

        return result
    }

    private suspend fun readCachedFamilyNameOrNull(): FamilyNameResult? {
        val client = clientFactory.client()
        hydratePersistentCache(client)
        val response =
            client
                .query(ViewerFamiliesQuery())
                .fetchPolicy(FetchPolicy.CacheOnly)
                .execute()

        response.exception?.let {
            return null
        }

        return response.data
            ?.viewer
            ?.families
            ?.firstOrNull()
            ?.name
            ?.let { FamilyNameResult(familyName = it, source = FamilyNameSource.CACHE) }
    }

    private fun hydratePersistentCache(client: ApolloClient) {
        val snapshot =
            snapshotStorage
                .load()
                ?.takeIf {
                    it.serverUrl == sessionStore.trimmedServerUrl() &&
                        it.tokenHash == sessionStore.tokenHash()
                } ?: return

        runCatching {
            client.apolloStore.writeOperationSync(
                operation = ViewerFamiliesQuery(),
                operationData = deserializeViewerFamiliesData(snapshot.payload),
                customScalarAdapters = CustomScalarAdapters.Empty,
            )
        }.onFailure {
            snapshotStorage.clear()
        }
    }
}

private class ApolloClientFactory(
    private val sessionStore: GraphqlSessionStore,
) {
    private var currentSessionSignature: String? = null
    private var currentClient: ApolloClient? = null

    fun client(): ApolloClient {
        val serverUrl = sessionStore.trimmedServerUrl()
        require(serverUrl.isNotEmpty()) { "GraphQL URL is required." }
        val sessionSignature = "$serverUrl#${sessionStore.tokenHash()}"

        val existing = currentClient
        if (existing != null && currentSessionSignature == sessionSignature) {
            return existing
        }

        val apolloClient =
            ApolloClient
                .Builder()
                .serverUrl(serverUrl)
                .addHttpInterceptor(BearerTokenInterceptor(sessionStore = sessionStore))
                .normalizedCache(MemoryCacheFactory(maxSizeBytes = 5 * 1024 * 1024))
                .build()

        currentSessionSignature = sessionSignature
        currentClient = apolloClient
        return apolloClient
    }
}

private class BearerTokenInterceptor(
    private val sessionStore: GraphqlSessionStore,
) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val token =
            requireNotNull(sessionStore.bearerTokenOrNull()) {
                "JWT bearer token is required."
            }

        val authenticatedRequest =
            request
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()

        return chain.proceed(authenticatedRequest)
    }
}

private fun com.apollographql.apollo.api.ApolloResponse<ViewerFamiliesQuery.Data>.requireFamilyName(
    source: FamilyNameSource,
): FamilyNameResult {
    exception?.let { throw IllegalStateException(it.message ?: "The GraphQL request failed.") }
    val graphqlError = errors?.firstOrNull()?.message
    if (graphqlError != null) {
        throw IllegalStateException(graphqlError)
    }

    val familyName =
        data
            ?.viewer
            ?.families
            ?.firstOrNull()
            ?.name
            ?: throw IllegalStateException("The authenticated viewer does not have any accessible families.")

    return FamilyNameResult(familyName = familyName, source = source)
}

private fun serializeViewerFamiliesData(data: ViewerFamiliesQuery.Data): String {
    val buffer = Buffer()
    val writer = BufferedSinkJsonWriter(buffer)
    ViewerFamiliesQuery().adapter().toJson(writer, CustomScalarAdapters.Empty, data)
    return buffer.readUtf8()
}

private fun deserializeViewerFamiliesData(payload: String): ViewerFamiliesQuery.Data {
    val buffer = Buffer().writeUtf8(payload)
    val reader = BufferedSourceJsonReader(buffer)
    return ViewerFamiliesQuery().adapter().fromJson(reader, CustomScalarAdapters.Empty)
}
