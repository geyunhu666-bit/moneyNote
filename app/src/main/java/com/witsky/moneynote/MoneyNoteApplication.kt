package com.witsky.moneynote

import android.app.Application

class MoneyNoteApplication : Application() {
  val container: AppContainer by lazy { AppContainer(this) }
}
