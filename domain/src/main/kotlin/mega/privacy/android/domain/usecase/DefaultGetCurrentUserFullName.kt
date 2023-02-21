package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Implementation of [GetCurrentUserFullName]
 */
@Deprecated(
    message = "Default display values should be handled in the presentation layer, see [MyCodeFragment] for an example",
    replaceWith = ReplaceWith("GetUserFullName")
)
class DefaultGetCurrentUserFullName @Inject constructor(
    private val getUserFullName: GetUserFullName,
) : GetCurrentUserFullName {

    override suspend fun invoke(
        forceRefresh: Boolean,
        defaultFirstName: String,
        defaultLastName: String,
    ): String =
        getUserFullName(forceRefresh) ?: "$defaultFirstName $defaultLastName"
}