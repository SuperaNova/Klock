package cit.edu.KlockApp.ui.main.alarm

import java.sql.Date
import java.sql.Time

data class Alarm(
    var time: Time,
    var isEnabled: Boolean,
    var date: Date
)
