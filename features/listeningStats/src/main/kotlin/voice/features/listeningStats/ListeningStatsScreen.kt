package voice.features.listeningStats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import coil.compose.AsyncImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@ContributesTo(AppScope::class)
interface ListeningStatsGraph {
  val listeningStatsViewModel: ListeningStatsViewModel
}

@ContributesTo(AppScope::class)
interface ListeningStatsProvider {

  @Provides
  @IntoSet
  fun listeningStatsNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.ListeningStatistics> { key ->
    NavEntry(key) {
      ListeningStatsScreen()
    }
  }
}

@Composable
fun ListeningStatsScreen() {
  val viewModel = retain { rootGraphAs<ListeningStatsGraph>().listeningStatsViewModel }
  // Null until the first aggregation lands: rendering Empty here would flash "No listening data yet"
  // at a user who has years of it.
  val viewState = viewModel.viewState() ?: return
  ListeningStatsScreen(
    viewState = viewState,
    onClose = viewModel::onClose,
    onBookClick = viewModel::onBookClick,
  )
}

@Composable
internal fun ListeningStatsScreen(
  viewState: ListeningStatsViewState,
  onClose: () -> Unit,
  onBookClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(StringsR.string.listening_stats)) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(StringsR.string.close))
          }
        },
      )
    },
  ) { paddingValues ->
    if (viewState.totalLifetimeMs == 0L && viewState.booksInLibrary == 0) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(StringsR.string.listening_stats_no_data),
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Scaffold
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (viewState.totalLifetimeMs > 0L) {
          TotalListening(viewState)
          Recently(viewState)
          LastTwelveMonths(viewState)
          Records(viewState)
        }

        if (viewState.finishedBooks.isNotEmpty()) {
          FinishedBooks(
            books = viewState.finishedBooks,
            onBookClick = onBookClick,
          )
        }

        if (viewState.booksInLibrary > 0) {
          LibraryProgress(viewState)
        }

        if (viewState.totalLifetimeMs > 0L) {
          viewState.firstListeningDateLabel?.let { date ->
            Text(
              text = stringResource(StringsR.string.listening_stats_since, date),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.Center,
            )
          }
        }

        Spacer(modifier = Modifier.size(12.dp))
      }
    }
  }
}

@Composable
private fun TotalListening(viewState: ListeningStatsViewState) {
  val totalMs = viewState.totalLifetimeMs
  val days = totalMs / 86_400_000
  val hours = (totalMs % 86_400_000) / 3_600_000
  val minutes = (totalMs % 3_600_000) / 60_000

  Column {
    Text(
      text = stringResource(StringsR.string.listening_stats_total_lifetime),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      UnitTile(days.toString(), stringResource(StringsR.string.listening_stats_unit_days), Modifier.weight(1f))
      UnitTile(hours.toString(), stringResource(StringsR.string.listening_stats_unit_hours), Modifier.weight(1f))
      UnitTile(minutes.toString(), stringResource(StringsR.string.listening_stats_unit_minutes), Modifier.weight(1f))
    }
    Text(
      text = formatDuration(totalMs),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp),
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun UnitTile(
  value: String,
  unit: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = unit,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}

@Composable
private fun Recently(viewState: ListeningStatsViewState) {
  Column {
    SectionTitle(stringResource(StringsR.string.listening_stats_recently))
    StatsCard {
      StatRow(stringResource(StringsR.string.listening_stats_today), formatDuration(viewState.todayMs))
      RowDivider()
      StatRow(
        label = stringResource(StringsR.string.listening_stats_this_week),
        value = formatDuration(viewState.thisWeekMs),
        supporting = viewState.weekChangePercent?.let { change ->
          when {
            change > 0 -> stringResource(StringsR.string.listening_stats_week_delta_more, change)
            change < 0 -> stringResource(StringsR.string.listening_stats_week_delta_less, -change)
            else -> null
          }
        },
      )
      RowDivider()
      StatRow(stringResource(StringsR.string.listening_stats_this_month), formatDuration(viewState.thisMonthMs))
    }
  }
}

@Composable
private fun LastTwelveMonths(viewState: ListeningStatsViewState) {
  val data = viewState.monthlyData
  if (data.isEmpty()) return
  // Not keyed on data: a session closing while the screen is open changes the current month's value
  // and must not snap the selection away from the bar the user is inspecting.
  var selectedIndex by remember { mutableIntStateOf(data.lastIndex) }
  val selectedPoint = data[selectedIndex.coerceIn(0, data.lastIndex)]

  Column {
    SectionTitle(stringResource(StringsR.string.listening_stats_last_12_months))
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
      shape = MaterialTheme.shapes.extraLarge,
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(StringsR.string.listening_stats_activity),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = stringResource(
              StringsR.string.listening_stats_selected_period,
              selectedPoint.label,
              formatDuration(selectedPoint.valueMs),
            ),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp),
          )
        }
        InteractiveBarChart(
          data = data,
          selectedIndex = selectedIndex,
          onSelect = { selectedIndex = it },
          modifier = Modifier.padding(top = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun InteractiveBarChart(
  data: List<ChartDataPoint>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val maxValue = data.maxOfOrNull { it.valueMs }?.coerceAtLeast(1L) ?: 1L

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(160.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    data.forEachIndexed { index, point ->
      val selected = index == selectedIndex
      val heightFraction by animateFloatAsState(
        targetValue = (point.valueMs.toFloat() / maxValue).coerceAtLeast(0.04f),
        animationSpec = spring(),
        label = "Listening bar height",
      )
      val description = stringResource(
        StringsR.string.listening_stats_selected_period,
        point.label,
        formatDuration(point.valueMs),
      )

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clickable(
            role = Role.Button,
            onClickLabel = stringResource(StringsR.string.listening_stats_select_period),
            onClick = { onSelect(index) },
          )
          .semantics(mergeDescendants = true) {
            contentDescription = description
            this.selected = selected
          },
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.BottomCenter,
        ) {
          if (point.valueMs == 0L) {
            // A month without listening is a dot, not a stub bar pretending to be data.
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)),
            )
          } else {
            Box(
              modifier = Modifier
                .fillMaxWidth(0.58f)
                .fillMaxHeight(heightFraction)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                  if (selected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                  },
                ),
            )
          }
        }
        Text(
          text = point.label,
          style = MaterialTheme.typography.labelSmall,
          color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun Records(viewState: ListeningStatsViewState) {
  Column {
    SectionTitle(stringResource(StringsR.string.listening_stats_records))
    StatsCard {
      StatRow(
        label = stringResource(StringsR.string.listening_stats_longest_streak),
        value = pluralStringResource(
          StringsR.plurals.listening_stats_streak_days,
          viewState.longestStreak,
          viewState.longestStreak,
        ),
        badge = stringResource(StringsR.string.listening_stats_streak_now)
          .takeIf { viewState.currentStreak == viewState.longestStreak && viewState.currentStreak > 0 },
      )
      if (viewState.currentStreak != viewState.longestStreak) {
        RowDivider()
        StatRow(
          label = stringResource(StringsR.string.listening_stats_current_streak),
          value = pluralStringResource(
            StringsR.plurals.listening_stats_streak_days,
            viewState.currentStreak,
            viewState.currentStreak,
          ),
        )
      }
      RowDivider()
      StatRow(
        label = stringResource(StringsR.string.listening_stats_longest_day),
        value = if (viewState.longestDayMs > 0 && viewState.longestDayLabel != null) {
          stringResource(
            StringsR.string.listening_stats_selected_period,
            formatDuration(viewState.longestDayMs),
            viewState.longestDayLabel,
          )
        } else {
          "—"
        },
      )
      RowDivider()
      StatRow(
        label = stringResource(StringsR.string.listening_stats_biggest_month),
        value = if (viewState.biggestMonthMs > 0 && viewState.biggestMonthLabel != null) {
          stringResource(
            StringsR.string.listening_stats_selected_period,
            formatDuration(viewState.biggestMonthMs),
            viewState.biggestMonthLabel,
          )
        } else {
          "—"
        },
      )
      RowDivider()
      StatRow(
        label = stringResource(StringsR.string.listening_stats_best_day_of_week),
        value = viewState.bestDayOfWeek ?: "—",
      )
      RowDivider()
      StatRow(
        label = stringResource(StringsR.string.listening_stats_avg_session),
        value = formatDuration(viewState.avgSessionMs),
      )
    }
  }
}

@Composable
private fun FinishedBooks(
  books: List<FinishedBookStats>,
  onBookClick: (BookId) -> Unit,
) {
  Column {
    SectionTitle(stringResource(StringsR.string.listening_stats_finished))
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      shape = MaterialTheme.shapes.large,
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
        books.forEachIndexed { index, book ->
          if (index > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
          FinishedBookRow(
            book = book,
            onClick = { onBookClick(book.bookId) },
          )
        }
      }
    }
  }
}

@Composable
private fun FinishedBookRow(
  book: FinishedBookStats,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AsyncImage(
      modifier = Modifier
        .size(width = 42.dp, height = 58.dp)
        .clip(MaterialTheme.shapes.small),
      model = book.cover?.file,
      placeholder = painterResource(id = UiR.drawable.album_art),
      error = painterResource(id = UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
    ) {
      Text(
        text = book.name,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      val meta = listOfNotNull(
        formatDuration(book.listenedMs).takeIf { book.listenedMs > 0 },
        book.finishedDateLabel,
      ).joinToString(" · ")
      if (meta.isNotEmpty()) {
        Text(
          text = meta,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
    Icon(
      Icons.Outlined.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun LibraryProgress(viewState: ListeningStatsViewState) {
  val progress = (viewState.booksCompleted.toFloat() / viewState.booksInLibrary).coerceIn(0f, 1f)

  Column {
    SectionTitle(stringResource(StringsR.string.listening_stats_library))
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      shape = MaterialTheme.shapes.large,
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(StringsR.string.listening_stats_library_completed),
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            text = pluralStringResource(
              StringsR.plurals.listening_stats_library_progress,
              viewState.booksInLibrary,
              viewState.booksCompleted,
              viewState.booksInLibrary,
            ),
            style = MaterialTheme.typography.labelLarge,
          )
        }
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(8.dp)
            .clip(CircleShape),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
      }
    }
  }
}

@Composable
private fun StatsCard(content: @Composable () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      content()
    }
  }
}

@Composable
private fun RowDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = 8.dp),
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}

@Composable
private fun StatRow(
  label: String,
  value: String,
  supporting: String? = null,
  badge: String? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Column(
      modifier = Modifier.padding(start = 12.dp),
      horizontalAlignment = Alignment.End,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = value,
          style = MaterialTheme.typography.labelLarge,
          textAlign = TextAlign.End,
        )
        badge?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(start = 6.dp),
          )
        }
      }
      supporting?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
  )
}

fun formatDuration(ms: Long): String {
  val hours = ms / 3_600_000
  val minutes = (ms % 3_600_000) / 60_000
  val seconds = (ms % 60_000) / 1_000
  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m ${seconds}s"
    seconds > 0 -> "${seconds}s"
    else -> "0m"
  }
}
