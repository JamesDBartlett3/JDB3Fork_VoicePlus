package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.logging.api.Logger
import voice.core.ui.ImmutableFile
import voice.core.ui.formatTime

@Immutable
data class BookOverviewItemViewState(
  val name: String,
  val author: String?,
  val cover: ImmutableFile?,
  val progress: Float,
  val id: BookId,
  val remainingTime: String,
)

internal fun Book.toItemViewState() = BookOverviewItemViewState(
  name = content.name,
  author = content.author,
  cover = content.cover?.let(::ImmutableFile),
  id = id,
  progress = progress(),
  remainingTime = formatTime(realTimeRemainingMs()),
)

/**
 * Wall-clock listening time left at this book's playback speed, not raw audio time —
 * at 2x, a 2h remainder is one real hour (GitHub issue #6).
 */
internal fun Book.realTimeRemainingMs(): Long {
  val remaining = (duration - position).coerceAtLeast(0)
  val speed = content.playbackSpeed
  return if (speed > 0f) (remaining / speed.toDouble()).toLong() else remaining
}

private fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Couldn't determine progress for book=$this")
  }
  return progress.coerceIn(0F, 1F)
}
