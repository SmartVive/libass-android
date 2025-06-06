package io.github.peerless2012.ass.media.kt

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.ui.SubtitleView
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.extractor.AssMatroskaExtractor
import io.github.peerless2012.ass.media.factory.AssRenderersFactory
import io.github.peerless2012.ass.media.parser.AssSubtitleParserFactory
import io.github.peerless2012.ass.media.type.AssRenderType
import io.github.peerless2012.ass.media.widget.AssSubtitleView

@OptIn(UnstableApi::class)
fun ExoPlayer.Builder.buildWithAssSupport(
    context: Context,
    renderType: AssRenderType = AssRenderType.LEGACY,
    subtitleView: SubtitleView? = null,
    dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context),
    extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory(),
    renderersFactory: RenderersFactory = DefaultRenderersFactory(context)
): ExoPlayer {
    val assHandler = AssHandler(renderType)
    val assSubtitleParserFactory = AssSubtitleParserFactory(assHandler)

    val mediaSourceFactory = DefaultMediaSourceFactory(
        dataSourceFactory,
        extractorsFactory.withAssMkvSupport(assSubtitleParserFactory, assHandler)
    )

    // The external subtitle will create parser directly from SubtitleParserFactory.
    mediaSourceFactory.setSubtitleParserFactory(assSubtitleParserFactory)

    val player = this
        .setMediaSourceFactory(mediaSourceFactory)
        .setRenderersFactory(renderersFactory.withAssSupport(assHandler))
        .build()

    if (renderType === AssRenderType.OVERLAY) {
        subtitleView?.withAssSupport(assHandler)
    }

    assHandler.init(player)
    return player
}

@OptIn(UnstableApi::class)
fun ExtractorsFactory.withAssMkvSupport(
    assSubtitleParserFactory: AssSubtitleParserFactory,
    assHandler: AssHandler
): ExtractorsFactory {
    return ExtractorsFactory {
        val extractors  = createExtractors()
        extractors.forEachIndexed { index, extractor ->
            if (extractor is MatroskaExtractor) {
                // Must keep the extractor order.
                extractors[index] = AssMatroskaExtractor(assSubtitleParserFactory, assHandler)
            }
        }
        extractors
    }
}

@OptIn(UnstableApi::class)
fun RenderersFactory.withAssSupport(assHandler: AssHandler): RenderersFactory {
    return AssRenderersFactory(assHandler, this)
}

@OptIn(UnstableApi::class)
fun SubtitleView.withAssSupport(handler: AssHandler): Unit {
    val assSubtitleView = AssSubtitleView(this.context, handler)
    this.addView(assSubtitleView)
}