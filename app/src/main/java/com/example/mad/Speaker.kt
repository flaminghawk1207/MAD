
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import java.util.LinkedList
import java.util.Locale

class Speaker(context: Context) : OnInitListener, TextToSpeech.OnUtteranceCompletedListener {
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private val messageQueue = LinkedList<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            tts.setSpeechRate(2.0f)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language is not supported!")
            }
        }
    }

    public fun speakOut(text: String) {
        if (messageQueue.isEmpty()) {
            // If the queue is empty, speak immediately
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        } else {
            // Add to the queue
            messageQueue.add(text)
        }
    }

    override fun onUtteranceCompleted(utteranceId: String?) {
        // Called when the current message is completed
        if (!messageQueue.isEmpty()) {
            val nextMessage = messageQueue.poll()
            tts.speak(nextMessage, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
