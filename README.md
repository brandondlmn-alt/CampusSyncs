# CampusSync 📱

> **Your Academic & Resource Command Center.** 
> Specifically designed for Rosebank International students in South Africa.

[![Kotlin Version](https://img.shields.io/badge/kotlin-2.2.10-blue.svg)](https://kotlinlang.org/)
[![API Level](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Material 3](https://img.shields.io/badge/Design-Material%203-purple.svg)](https://m3.material.io)

---

## 📖 Overview

**CampusSync** is a comprehensive tertiary student management platform built for the **Independent Institute of Education (IIE)** module **OPSC6312 (Open Source Coding Intermediate)**. The application centralizes academic planning, grade tracking, budgeting, and resource discovery into a single, mobile-first experience. 

Designed specifically for students in the **Diploma in IT in Software Development (DISD)** programme at **Rosebank International**, the app leverages state-of-the-art AI through the Google Gemini API to automate the most tedious parts of student life: scanning paper schedules and assessment guides.

---

## 📑 Table of Contents
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Firebase & AI Integration](#-firebase--ai-integration)
- [GitHub Actions & CI/CD](#-github-actions--cicd)
- [Release Notes](#-release-notes)
- [Team Members](#-team-members)
- [Demonstration](#-demonstration-video)
- [References](#-references)

---

## ✨ Features

### 🎓 Academic Management
*   **3-Step Registration Wizard**: Specialized onboarding for Rosebank students.
*   **Auto-Assigned Modules**: Automatically populates your module list based on your Year (1-3) and Semester (1-2).
*   **Smart Timetable**: Full CRUD support for your weekly schedule.
*   **Grade Tracker**: Automatic weighted average calculation to monitor your academic standing.
*   **Assessment Hub**: Track deadlines with color-coded urgency.

### 🤖 AI Innovation
*   **AI Student Assistant**: A context-aware chatbot that knows your grades, budget, and schedule to provide personalized advice.
*   **AI Timetable Scanner**: Upload a photo of your schedule; Gemini Vision extracts and saves the data to Firestore.
*   **AI Assessment Scanner**: Extract deadlines from a Programme Assessment Schedule (PAS) image, filtered automatically by your year of study.

### 💰 Student Life
*   **Budget Tracker**: Manage your monthly allowance with category-based spending and visual progress indicators.
*   **Resource Hub**: Searchable directory for South African bursaries and student accommodation.

---

## 🛠 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 |
| **UI Framework** | XML Layouts (Material Design 3) |
| **Minimum SDK** | 24 (Android 7.0 Nougat) |
| **Backend** | Firebase (Auth, Firestore) |
| **AI Engine** | Google Gemini API (2.0 Flash) |
| **Networking** | OkHttp 4.12.0 |
| **JSON Parsing** | Gson 2.11.0 |
| **Architecture** | Repository Pattern with Coroutines |

---

## 🏗 Architecture

CampusSync follows the **Repository Pattern**, promoting a clean separation of concerns between the UI, domain logic, and data sources.

1.  **UI Layer**: Fragments and Activities utilizing **View Binding**.
2.  **Repository Layer**: Mediator between the UI and data sources.
3.  **Data Layer**: Remote (Firestore) and AI (Gemini).

---

## 🚀 Getting Started

### Installation
1.  Clone the repository:
    ```bash
    git clone [https://github.com/brandondlmn-alt/CampusSyncs.git]
    ```
2.  Place your `google-services.json` in the `app/` directory.
3.  Create a `local.properties` file and add:
    ```properties
    GEMINI_API_KEY=your_key_here
    ```
4.  Sync and Run.

---

## 🤖 GitHub Actions

CampusSync uses a CI/CD pipeline defined in `.github/workflows/build.yml`.
*   **Build Automation**: Triggers on every push to the `master` branch.
*   **Automated Testing**: Runs unit tests for grade calculations and date normalization.
*   **APK Generation**: Automatically builds a debug APK available in the Actions tab.

---

## 📝 Release Notes

### VERSION 1.5.0 (Part 2)
*   Integrated analytical AI Student Assistant.
*   Released Gemini Vision Timetable and PAS scanners.
*   Added 3-step Rosebank registration wizard.
*   Implemented color-coded deadline urgency.

---

## 👥 Team Members
*   Rendani Muthathe — ST10449327
*   Manelisi Vatsha — ST10447923
*   Brandon Dlamini — ST10442379
*   Ofentse Rakosa — ST10443425
*   Rashokeng Molokomme — ST10265955

---

## 🎥 Demonstration Video

[Watch the CampusSync demo on Google drive]([https://drive.google.com/file/d/1NIMpmc7HYnRkz94DwP7KAzPpuO9VSvjJ/view?usp=drive_link])

---

## ⚖️ License
This project is submitted for academic evaluation as part of IIE OPSC6312.

**Disclaimer**: *CampusSync is not officially affiliated with or endorsed by Rosebank International or the IIE.*
