package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.EndsRecurrenceOption
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.MonthlyRecurrenceOption
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Data class defining the custom recurrence state
 *
 * @property newRules                           [ChatScheduledRules].
 * @property isWeekdaysSelected                 True if weekday option is selected. False, if not.
 * @property monthDayOption                     Month day.
 * @property monthWeekDayListOption             List of [MonthWeekDayItem].
 * @property monthlyRadioButtonOptionSelected   [MonthlyRecurrenceOption].
 * @property endsRadioButtonOptionSelected      [EndsRecurrenceOption].
 * @property endDateOccurrenceOption            [ZonedDateTime]
 * @property isValidRecurrence                  True if the custom recurrence is valid. False, if not.
 * @property showMonthlyRecurrenceWarning       True, if the text on the monthly recurrence warning should be displayed. False, if not.
 */
data class CustomRecurrenceState constructor(
    val newRules: ChatScheduledRules = ChatScheduledRules(),
    val isWeekdaysSelected: Boolean = false,
    val monthDayOption: Int = 1,
    val monthWeekDayListOption: List<MonthWeekDayItem> = listOf(
        MonthWeekDayItem(
            WeekOfMonth.First,
            listOf(Weekday.Monday)
        )
    ),
    val monthlyRadioButtonOptionSelected: MonthlyRecurrenceOption = MonthlyRecurrenceOption.MonthDay,
    val endsRadioButtonOptionSelected: EndsRecurrenceOption = EndsRecurrenceOption.Never,
    val endDateOccurrenceOption: ZonedDateTime = Instant.now().atZone(ZoneId.systemDefault()),
    val isValidRecurrence: Boolean = true,
    val showMonthlyRecurrenceWarning: Boolean = false,
)
