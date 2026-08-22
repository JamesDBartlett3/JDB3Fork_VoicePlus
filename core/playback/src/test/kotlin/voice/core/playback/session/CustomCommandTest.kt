package voice.core.playback.session

import android.os.Bundle
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomCommandTest {

  @Test
  fun `session command carries its action`() {
    val command = CustomCommand.ForceSeekToNext

    CustomCommand.parse(command.toSessionCommand(), Bundle.EMPTY) shouldBe command
  }
}
