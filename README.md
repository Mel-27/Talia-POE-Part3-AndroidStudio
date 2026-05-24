#  Budget Bee — Personal Budget Tracker App

**PROG7313 Programming 3C | POE Part 2 | Android Studio (Kotlin)**

---

##  Table of Contents
- [Group Members](#-group-members)
- [App Overview](#-app-overview)
- [Features](#-features)
- [Feature-Based Code Structure](#-feature-based-code-structure)
- [Technology Stack](#-technology-stack)
- [How to Run](#-how-to-run)
- [Testing](#-testing)
- [Demonstration Video](#-demonstration-video)
- [Database Overview](#-database-overview)
- [References](#-references)

---

##  Group Members

| Name | Student Number |
|------|---------------|
| Tiara Naidoo | ST10453072 |
| Anelisa Mkhize | ST10288433 |
| Ayushi Jagganath | ST10452981 |
| Kivishnee Mel Subramoney | ST10438899 |
| Gia Jenica Gounden | ST10247357 |

---

##  App Overview

**Budget Bee** is a personal budgeting Android application designed to help users manage daily expenses and achieve financial goals.

Users can:
- Track spending  
- Organise expenses into categories  
- Set budget limits  
- Monitor financial progress  

To enhance engagement, the app includes gamification features such as **Honey Points ** and **achievement badges **.

**Built with:**
- Kotlin + Android Studio  
- Room Database (SQLite) for local storage  

---

##  Features

###  Login & Registration
- User registration with full name, email, username, password  
- Secure login with validation  
- Password hashing before storage  
- Session persistence across app restarts  

###  Expense Categories
- Create custom categories  
- Emoji icon picker (Food, Travel, Home, etc.)  
- Optional monthly spending limit  
- Stored in Room Database  

###  Add Expense Entry
- Description, amount, date, start & end time  
- Category selection  
- Optional receipt photo  
- Stored locally in RoomDB  

###  Receipt Photo Upload
- Capture via camera (FileProvider supported)  
- Select from gallery  
- Instant preview  
- Stored as file path  
- Viewable from expense list  

###  Expense List & Tracking
- Filter by:
  - This Month  
  - Last 7 Days  
  - Last 3 Months  
  - Custom Range  

- Sort:
  - Newest / Oldest  

- Displays:
  - Category  
  - Amount  
  - Date  
  - Receipt link  

- Summary:
  - Total spending  
  - Number of entries  
  - Average per day  

###  Budget Goals
- Set minimum & maximum monthly goals  
- Dynamic progress bars  
- Category-level tracking  
- Visual warnings when limits exceeded  

###  Badges & Honey Points

| Badge | Requirement |
|------|------------|
|  Worker Bee | 100 points |
|  Honey Collector | 250 points |
|  Honey Hoarder | 500 points |
|  Queen Bee | 1000 points |

- Progress dashboard for locked/unlocked badges  

###  Light & Dark Mode
- Toggle in Account screen  
- Saved with SharedPreferences  
- Applied globally using AppCompatDelegate  

###  Reminders
- Set reminders for bills/subscriptions  
- Notification options:
  - On the day  
  - 1, 3, or 5 days before  
- Displayed in reminders list  

---

##  Feature-Based Code Structure

###  Authentication
- `LoginActivity.kt`  
- `RegisterActivity.kt`  
- `SessionManager.kt`  

###  Expenses
- `AddExpenseActivity.kt`  
- `ExpensesListActivity.kt`  
- `Expense.kt`  
- `ExpenseDao.kt`  

###  Categories
- `AddCategoryActivity.kt`  
- `Category.kt`  
- `CategoryDao.kt`  
- `CategorySeeder.kt`  

###  Budget Goals
- `BudgetGoalsActivity.kt`  

###  Gamification
- `BadgesActivity.kt`  

###  Reminders
- `RemindersActivity.kt`  
- `Reminder.kt`  
- `ReminderDao.kt`  

###  Core & UI
- `MainActivity.kt` (Dashboard)  
- `AccountActivity.kt`  
- XML layouts → `res/layout`  
- Themes → `values/`, `values-night/`  

###  Data Layer
- `AppDatabase.kt`  
- Room entities & DAOs  

---

##  Technology Stack

| Technology | Purpose |
|----------|--------|
| Kotlin | Main language |
| Android Studio | IDE |
| Room (SQLite) | Local database |
| KSP | Annotation processing |
| ViewBinding | Safe UI binding |
| Material Design 3 | UI components |
| SharedPreferences | Local settings |
| FileProvider | Secure image handling |
| Coroutines | Background tasks |
| LiveData | UI updates |
| ActivityResultContracts | Camera/Gallery |

---

##  How to Run

### Prerequisites
- Android Studio (Meerkat or newer)  
- Android SDK 35  
- Emulator or physical device (API 24+)  

### Steps
```bash
- git clone https://github.com/YOUR-REPO-LINK-HERE.git
- Open project in Android Studio
- Sync Gradle
- Run on emulator/device
- Testing
- GitHub Actions CI configured
- Runs on every push
```
## YouTube Link: 
Includes full walkthrough and explanation of all features.

## Database Overview
###Categories Table
- id (PK)
- name
- iconEmoji
- monthlyLimit
- createdAt
### Expenses Table
- id (PK)
- categoryId (FK)
- description
- amount
- date
- startTime
- endTime
- receiptPhotoPath

---

## References
Android Developers. (2024). Access data using Room DAOs.
Available at: https://developer.android.com/training/data-storage/room/accessing-data

[Accessed 27 April 2026].

Android Developers. (2024). Activity - finish.
Available at: https://developer.android.com/reference/android/app/Activity#finish()

[Accessed 27 April 2026].

Android Developers. (2024). Activity Lifecycle.
Available at: https://developer.android.com/guide/components/activities/activity-lifecycle

[Accessed 27 April 2026].

Android Developers. (2024). AlertDialog.
Available at: https://developer.android.com/guide/topics/ui/dialogs

[Accessed 27 April 2026].

Android Developers. (2024). AppCompatActivity.
Available at: https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity

[Accessed 18 April 2026].

Android Developers. (2024). Database (Room).
Available at: https://developer.android.com/reference/androidx/room/Database

[Accessed 20 April 2026].

Android Developers. (2024). Define data using Room entities.
Available at: https://developer.android.com/training/data-storage/room/defining-data

[Accessed 27 April 2026].

Android Developers. (2024). Entity.
Available at: https://developer.android.com/reference/androidx/room/Entity

[Accessed 27 April 2026].

Android Developers. (2024). Insert.
Available at: https://developer.android.com/reference/androidx/room/Insert

[Accessed 27 April 2026].

Android Developers. (2024). Intents and Intent Filters.
Available at: https://developer.android.com/guide/components/intents-filters

[Accessed 27 April 2026].

Android Developers. (2024). Kotlin coroutines on Android.
Available at: https://developer.android.com/kotlin/coroutines

[Accessed 27 April 2026].

Android Developers. (2024). Log.
Available at: https://developer.android.com/reference/android/util/Log

[Accessed 27 April 2026].

Android Developers. (2024). MessageDigest.
Available at: https://developer.android.com/reference/java/security/MessageDigest

[Accessed 27 April 2026].

Android Developers. (2024). PrimaryKey.
Available at: https://developer.android.com/reference/androidx/room/PrimaryKey

[Accessed 27 April 2026].

Android Developers. (2024). Query.
Available at: https://developer.android.com/reference/androidx/room/Query

[Accessed 27 April 2026].

Android Developers. (2024). Room Database overview.
Available at: https://developer.android.com/training/data-storage/room

[Accessed 27 April 2026].

Android Developers. (2024). Room.databaseBuilder.
Available at: https://developer.android.com/reference/androidx/room/Room#databaseBuilder

[Accessed 20 April 2026].

Android Developers. (2024). RoomDatabase.
Available at: https://developer.android.com/reference/androidx/room/RoomDatabase

[Accessed 20 April 2026].

Android Developers. (2024). RoomDatabase.Builder - fallbackToDestructiveMigration.
Available at: https://developer.android.com/reference/androidx/room/RoomDatabase.Builder#fallbackToDestructiveMigration()

[Accessed 20 April 2026].

Android Developers. (2024). Save key-value data with SharedPreferences.
Available at: https://developer.android.com/training/data-storage/shared-preferences

[Accessed 27 April 2026].

Android Developers. (2024). SuppressLint.
Available at: https://developer.android.com/reference/android/annotation/SuppressLint

[Accessed 27 April 2026].

Android Developers. (2024). Tasks and Back Stack.
Available at: https://developer.android.com/guide/components/activities/tasks-and-back-stack

[Accessed 27 April 2026].

Android Developers. (2024). Use Kotlin coroutines with lifecycle-aware components.
Available at: https://developer.android.com/topic/libraries/architecture/coroutines

[Accessed 27 April 2026].

Android Developers. (2024). View - findViewById.
Available at: https://developer.android.com/reference/android/view/View#findViewById(int)

[Accessed 27 April 2026].

Android Developers. (2024). View - setOnClickListener.
Available at: https://developer.android.com/reference/android/view/View#setOnClickListener

[Accessed 18 April 2026].

Android Developers. (2026). ProgressBar.
Available at: https://developer.android.com/reference/android/widget/ProgressBar

[Accessed 27 April 2026].

GeeksforGeeks. (2022). How to check if an app is in dark mode and change it to light mode in Android.
Available at: https://www.geeksforgeeks.org/android/how-to-check-if-an-app-is-in-dark-mode-and-change-it-to-light-mode-in-android/

[Accessed 27 April 2026].

GeeksforGeeks. (2022). Material Design Date Picker in Android using Kotlin.
Available at: https://www.geeksforgeeks.org/kotlin/material-design-date-picker-in-android-using-kotlin/

[Accessed 27 April 2026].

JetBrains (Kotlin). (2024). Coroutines guide.
Available at: https://kotlinlang.org/docs/coroutines-guide.html

[Accessed 28 April 2026].

JetBrains (Kotlin). (2024). Data classes.
Available at: https://kotlinlang.org/docs/data-classes.html

[Accessed 27 April 2026].

JetBrains (Kotlin). (2024). Regex.
Available at: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/

[Accessed 27 April 2026].

JetBrains (Kotlin). (2024). Volatile.
Available at: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/

[Accessed 20 April 2026].

JetBrains (Kotlin). (2024). Shared mutable state and concurrency.
Available at: https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html

[Accessed 20 April 2026].

Oracle. (2024). MessageDigest (Java SE 11).
Available at: https://docs.oracle.com/en/java/api/java.base/java/security/MessageDigest.html

[Accessed 24 April 2026].

Medium (Android Ideas). (2018). findViewById in Kotlin.
Available at: https://medium.com/android-ideas/findviewbyid-in-kotlin-ce4d22193c79

[Accessed 27 April 2026].

Medium. (2019). Logging in Kotlin — the right way.
Available at: https://muthuraj57.medium.com/logging-in-kotlin-the-right-way-d7a357bb0343

[Accessed 27 April 2026].

Stack Overflow. (2017). Log.e with Kotlin.
Available at: https://stackoverflow.com/questions/44158802/log-e-with-kotlin

[Accessed 27 April 2026].

Independent Institute of Education (IIE). (2026). PROG7313 Programming 3C Module Manual.
Durban: The Independent Institute of Education.

---

## GitHub Repository

https://github.com/EMKNDW/prog7313-g1-2026-poe-group-7.git
