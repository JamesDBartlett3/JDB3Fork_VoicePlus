package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningSession
import java.time.Instant

@Dao
public interface ListeningSessionDao {

  @Insert
  public suspend fun insert(session: ListeningSession)

  @Query("SELECT * FROM listening_session WHERE bookId = :bookId ORDER BY startedAt DESC LIMIT 500")
  public fun sessionsForBook(bookId: BookId): Flow<List<ListeningSession>>

  @Query("DELETE FROM listening_session WHERE bookId = :bookId")
  public suspend fun deleteAllForBook(bookId: BookId)

  // Restore cleanup: removes a book's rows for the given start instants so a snapshot's canonical
  // re-keyed copies can replace them. Scoped on purpose — rows a snapshot does not contain are
  // organic listening recorded since the backup and must never be deleted wholesale.
  @Query("DELETE FROM listening_session WHERE bookId = :bookId AND startedAt IN (:startedAt)")
  public suspend fun deleteForBookAt(
    bookId: BookId,
    startedAt: List<Instant>,
  )

  @Query("SELECT * FROM listening_session ORDER BY startedAt ASC")
  public fun allSessions(): Flow<List<ListeningSession>>

  @Query("SELECT * FROM listening_session")
  public suspend fun all(): List<ListeningSession>

  // A lightweight change trigger for the snapshot writer (Room re-emits on any insert/update/delete);
  // cheaper than loading the whole table via allSessions() just to act as a signal.
  @Query("SELECT COUNT(*) FROM listening_session")
  public fun count(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun upsert(session: ListeningSession)

  // Dedup probe for the interrupted-session finalizer: was this session already recorded normally?
  @Query("SELECT COUNT(*) FROM listening_session WHERE bookId = :bookId AND startedAt = :startedAt")
  public suspend fun countAt(
    bookId: BookId,
    startedAt: Instant,
  ): Int
}
