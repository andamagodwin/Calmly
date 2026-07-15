package app.andama.calmly.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import app.andama.calmly.data.CalMood
import app.andama.calmly.ui.faceRes

/**
 * Renders one of Cal's faces as a notification large-icon.
 *
 * The system crops large icons to a circle, and the faces are square PNGs whose
 * gills run right out to the edges — dropped in raw, they'd come back with the
 * corners sheared off. So the face is composited onto a filled circle at 80%
 * scale: it survives the crop intact and reads as an avatar, which is the point.
 * Every push in the app is *from Cal*, and his face on the lockscreen is what
 * makes that land.
 */
object CalIcon {

    private const val SIZE_PX = 256
    private const val BACKDROP = 0xFF2A2046.toInt() // ElevatedBackground

    fun face(context: Context, mood: CalMood): Bitmap? = face(context, mood.faceRes)

    /**
     * Same treatment, but for one of Cal's expanded expressions that live outside
     * the 5-face mood engine (anxious, sleepy, excited…). Notifications reach for
     * these directly when the event has a sharper feeling than the mood alone.
     */
    fun face(context: Context, @DrawableRes faceRes: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, faceRes) ?: return null
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawCircle(
            SIZE_PX / 2f, SIZE_PX / 2f, SIZE_PX / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BACKDROP }
        )

        val inset = (SIZE_PX * 0.10f).toInt()
        drawable.setBounds(inset, inset, SIZE_PX - inset, SIZE_PX - inset)
        drawable.draw(canvas)

        return bitmap
    }
}
