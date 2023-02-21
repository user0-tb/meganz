package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default CloudExplorerNode search Nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getSearchFromMegaNodeParent [GetSearchFromMegaNodeParent]
 */
class DefaultCloudExplorerSearchNodeUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParent,
) : GetCloudExplorerSearchNodeUseCase {
    override suspend fun invoke(
        query: String?,
        parentHandle: Long,
        parentHandleSearch: Long,
        megaCancelToken: MegaCancelToken,
    ): List<MegaNode>? {
        return query?.let {
            val parentNode = megaNodeRepository.getNodeByHandle(parentHandle)
            getSearchFromMegaNodeParent(
                query = query,
                parentHandleSearch = parentHandleSearch,
                megaCancelToken = megaCancelToken,
                parent = parentNode
            )
        } ?: run {
            emptyList()
        }
    }
}