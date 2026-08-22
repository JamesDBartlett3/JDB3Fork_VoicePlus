package voice.features.bookmark

import voice.core.data.Bookmark
import java.time.Instant

data class BookmarkItemViewState(
  val title: String,
  val chapterPosition: String,
  val bookPosition: String,
  val addedAt: Instant,
  val type: BookmarkType,
  val id: Bookmark.Id,
)

enum class BookmarkType {
  Manual,
  QuickBookmark,
  SleepTimer,
}

data class BookmarkViewState(
  val bookmarks: List<BookmarkItemViewState>,
  val shouldScrollTo: Bookmark.Id?,
  val dialogViewState: BookmarkDialogViewState,
)

sealed interface BookmarkDialogViewState {
  data object None : BookmarkDialogViewState
  data object AddBookmark : BookmarkDialogViewState
  data class EditBookmark(
    val id: Bookmark.Id,
    val title: String?,
  ) : BookmarkDialogViewState
}
