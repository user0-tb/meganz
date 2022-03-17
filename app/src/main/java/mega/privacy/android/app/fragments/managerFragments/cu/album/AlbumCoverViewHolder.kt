package mega.privacy.android.app.fragments.managerFragments.cu.album

import android.net.Uri
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemAlbumCoverBinding
import mega.privacy.android.app.utils.Util

/**
 *  AlbumCoverViewHolder work with  AlbumCoverAdapter
 */
class AlbumCoverViewHolder(
    private val binding: ItemAlbumCoverBinding,
    coverWidth: Int,
    coverMargin: Int
) : RecyclerView.ViewHolder(binding.root) {

    init {
        initCoverParams(coverWidth, coverMargin)
    }

    /**
     * init cover params
     */
    private fun initCoverParams(coverWidth: Int, coverMargin: Int) {
        val params = binding.root.layoutParams as GridLayoutManager.LayoutParams
        params.width = coverWidth
        params.marginStart = coverMargin
        params.marginEnd = coverMargin
        params.bottomMargin = coverMargin * 4

        // Image cover.
        val coverParams = binding.cover.layoutParams as ConstraintLayout.LayoutParams
        coverParams.width = coverWidth
        coverParams.height = coverWidth
    }

    /**
     * Handler Album Cover UI logic and listener
     */
    fun bind(albumCover: AlbumCover, listener: AlbumCoverAdapter.Listener) {
        itemView.setOnClickListener {
            listener.onCoverClicked(albumCover)
        }

        if(albumCover.thumbnail == null) {
            if (Util.isDarkMode(itemView.context)) {
                binding.cover.setActualImageResource(R.drawable.ic_album_cover_d)
            } else {
                binding.cover.setActualImageResource(R.drawable.ic_album_cover)
            }
        } else {
            val cover = albumCover.thumbnail

            val request = ImageRequestBuilder.newBuilderWithSource(Uri.fromFile(cover)).build()
            val controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setOldController(binding.cover.controller)
                .build()
            binding.cover.controller = controller
        }

        binding.number.text = albumCover.count.toString()
        binding.title.text = albumCover.title
    }
}