package org.simple.clinic.summary

import android.annotation.SuppressLint
import android.support.v4.content.res.ResourcesCompat
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.reactivex.subjects.Subject
import kotterknife.bindView
import org.simple.clinic.R
import org.simple.clinic.bp.BloodPressureMeasurement
import org.simple.clinic.util.Just
import org.simple.clinic.util.None
import org.simple.clinic.util.Truss
import org.simple.clinic.util.Unicode
import org.simple.clinic.widgets.UiEvent
import org.simple.clinic.widgets.setPaddingBottom
import org.simple.clinic.widgets.setPaddingTop
import org.simple.clinic.widgets.setTextAppearanceCompat

data class SummaryBloodPressureListItem(
    val measurement: BloodPressureMeasurement,
    private val displayDaysTimestamp: RelativeTimestamp,
    val isEditable: Boolean = false,
    val showDivider: Boolean,
    val displayTime: String?,
    val firstInGroup: Boolean
) : GroupieItemWithUiEvents<SummaryBloodPressureListItem.BpViewHolder>(measurement.uuid.hashCode().toLong()) {

  override lateinit var uiEvents: Subject<UiEvent>

  override fun getLayout() = R.layout.list_patientsummary_bp_measurement

  override fun createViewHolder(itemView: View): BpViewHolder {
    return BpViewHolder(itemView)
  }

  @SuppressLint("SetTextI18n")
  override fun bind(holder: BpViewHolder, position: Int) {
    val context = holder.itemView.context
    val resources = context.resources

    val level = measurement.level

    holder.levelTextView.text = when (level.displayTextRes) {
      is Just -> context.getString(level.displayTextRes.value)
      is None -> ""
    }

    val bpReading = context.resources.getString(R.string.patientsummary_bp_reading, measurement.systolic, measurement.diastolic)
    holder.readingsTextView.text = bpReading

    val readingsTextAppearanceResId = when {
      level.isUrgent() -> R.style.Clinic_V2_TextAppearance_PatientSummary_BloodPressure_High
      else -> R.style.Clinic_V2_TextAppearance_PatientSummary_BloodPressure_Normal
    }
    holder.readingsTextView.setTextAppearanceCompat(readingsTextAppearanceResId)

    holder.daysAgoTextView.text = if (isEditable) {
      val colorSpanForEditLabel = ForegroundColorSpan(ResourcesCompat.getColor(resources, R.color.patientsummary_edit_label_color, context.theme))
      Truss()
          .pushSpan(colorSpanForEditLabel)
          .append(resources.getString(R.string.patientsummary_bp_edit))
          .popSpan()
          .append(" ${Unicode.bullet} ")
          .append(displayDaysTimestamp.displayText(context))
          .build()

    } else {
      displayDaysTimestamp.displayText(context)
    }

    val measurementImageTint = when {
      level.isUrgent() -> R.color.patientsummary_bp_reading_high
      else -> R.color.patientsummary_bp_reading_normal
    }
    holder.heartImageView.imageTintList = ResourcesCompat.getColorStateList(resources, measurementImageTint, null)

    holder.itemView.setOnClickListener { uiEvents.onNext(PatientSummaryBpClicked(measurement)) }
    holder.itemView.isClickable = isEditable

    holder.divider.visibility = if (showDivider) VISIBLE else GONE

    if (displayTime != null) {
      holder.timeTextView.visibility = VISIBLE
      holder.timeTextView.text = displayTime
    } else {
      holder.timeTextView.visibility = GONE
    }

    val multipleItemsInThisGroup = displayTime != null

    if (multipleItemsInThisGroup) {
      if (firstInGroup) holder.itemLayout.setPaddingTop(R.dimen.patientsummary_bp_list_item_first_in_group_top_padding) else holder.itemLayout.setPaddingTop(R.dimen.patientsummary_bp_list_item_multiple_in_group_bp_top_padding)
      if (showDivider) holder.itemLayout.setPaddingBottom(R.dimen.patientsummary_bp_list_item_last_in_group_bottom_padding) else holder.itemLayout.setPaddingBottom(R.dimen.patientsummary_bp_list_item_multiple_in_group_bp_bottom_padding)
    } else {
      holder.itemLayout.setPaddingBottom(R.dimen.patientsummary_bp_list_item_single_padding)
      holder.itemLayout.setPaddingTop(R.dimen.patientsummary_bp_list_item_single_padding)
    }

  }

  override fun isSameAs(other: Item<*>?): Boolean {
    return this == other
  }

  class BpViewHolder(rootView: View) : ViewHolder(rootView) {
    val readingsTextView by bindView<TextView>(R.id.patientsummary_item_bp_readings)
    val heartImageView by bindView<ImageView>(R.id.patientsummary_bp_reading_heart)
    val levelTextView by bindView<TextView>(R.id.patientsummary_item_bp_level)
    val daysAgoTextView by bindView<TextView>(R.id.patientsummary_item_bp_date)
    val divider by bindView<View>(R.id.patientsummary_item_bp_divider)
    val timeTextView by bindView<TextView>(R.id.patientsummary_item_bp_time)
    val itemLayout by bindView<LinearLayout>(R.id.patientsummary_item_layout)
  }
}
