package voice.core.playback.session

import android.os.Bundle
import android.view.InputDevice
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.playback.history.PlaybackIntentHolder
import voice.core.playback.player.VoicePlayer

@RunWith(RobolectricTestRunner::class)
class LibrarySessionCallbackTest {

  @Test
  fun `lockscreen timed skip commands seek back and forward`() {
    val player = mockk<VoicePlayer>(relaxed = true)
    val callback = LibrarySessionCallback(
      mediaItemProvider = mockk(),
      scope = mockk(),
      player = player,
      bookSearchParser = mockk(),
      bookSearchHandler = mockk(),
      currentBookStoreId = mockk(),
      bookRepository = mockk(),
      doubleClickHandlerStore = mockk(),
      tripleClickHandlerStore = mockk(),
      bookmarkRepo = mockk(),
      intentHolder = mockk(),
      positionUpdater = mockk(),
      context = mockk(),
    )

    val result = callback.onCustomCommand(
      session = mockk<MediaSession>(),
      controller = mockk(),
      customCommand = CustomCommand.SeekBack.toSessionCommand(),
      args = Bundle.EMPTY,
    ).get()

    result.resultCode shouldBe SessionResult.RESULT_SUCCESS
    verify(exactly = 1) { player.seekBack() }

    val forwardResult = callback.onCustomCommand(
      session = mockk<MediaSession>(),
      controller = mockk(),
      customCommand = CustomCommand.SeekForward.toSessionCommand(),
      args = Bundle.EMPTY,
    ).get()

    forwardResult.resultCode shouldBe SessionResult.RESULT_SUCCESS
    verify(exactly = 1) { player.seekForward() }
  }

  @Test
  fun `only physical alphabetic devices bypass configured headset actions`() {
    fun device(
      isVirtual: Boolean,
      keyboardType: Int,
    ) = mockk<InputDevice> {
      every { this@mockk.isVirtual } returns isVirtual
      every { this@mockk.keyboardType } returns keyboardType
    }

    device(isVirtual = true, InputDevice.KEYBOARD_TYPE_ALPHABETIC)
      .isPhysicalAlphabeticKeyboard() shouldBe false
    device(isVirtual = false, InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC)
      .isPhysicalAlphabeticKeyboard() shouldBe false
    device(isVirtual = false, InputDevice.KEYBOARD_TYPE_ALPHABETIC)
      .isPhysicalAlphabeticKeyboard() shouldBe true
  }

  @Test
  fun `pause with rewind marks sleep pauses then seeks the underlying position`() {
    val player = mockk<VoicePlayer>(relaxed = true)
    every { player.pause() } just Runs
    every { player.currentPosition } returns 50_000L
    every { player.seekTo(any<Long>()) } just Runs
    val intentHolder = PlaybackIntentHolder()

    pauseWithRewind(player, intentHolder, rewindMs = 7_500L)

    intentHolder.stoppedBySleepTimer shouldBe true
    verifySequence {
      player.pause()
      player.currentPosition
      player.seekTo(42_500L)
    }
  }
}
