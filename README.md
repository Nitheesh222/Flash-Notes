# FlashNotes ⚡📚

FlashNotes is an intelligent, AI-powered Android application designed to supercharge your study sessions. By leveraging the power of Google's Gemini API, FlashNotes automatically generates flashcards, creates summaries, and acts as a personalized AI tutor for your notes.

## 🌟 Features

- **🤖 AI Flashcard Generation:** Automatically convert your study notes into comprehensive, interactive flashcards in seconds.
- **📝 Smart Summarization:** Instantly generate concise summaries of lengthy documents and study materials.
- **💬 Interactive AI Chat:** Talk directly to your notes! Ask questions, clarify concepts, and get detailed explanations through a built-in AI chat interface.
- **📁 Notebook Organization:** Keep your subjects and topics perfectly organized with isolated notebooks to prevent data leakage between different study sets.
- **🃏 Flashcard Player:** Test your knowledge with a sleek, distraction-free flashcard player. 

## 🛠️ Technology Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **Architecture:** MVVM (Model-View-ViewModel)
- **UI Toolkit:** XML Layouts with ViewBinding
- **Concurrency:** Kotlin Coroutines
- **Networking:** OkHttp
- **AI Integration:** Google Gemini 2.0 Flash API

## 🚀 Getting Started

### Prerequisites
- Android Studio (Jellyfish or newer recommended)
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Nitheesh222/Flash-Notes.git
   ```
2. Open the project in Android Studio.
3. Obtain a **Google Gemini API Key** from Google AI Studio.
4. Navigate to `app/src/main/java/com/flashnotes/app/data/llm/LlmManager.kt`.
5. Replace `"YOUR_API_KEY_HERE"` with your actual API key:
   ```kotlin
   private const val GEMINI_API_KEY = "your_actual_api_key_here"
   ```
   *(Note: Never commit your API key to public repositories!)*
6. Build and run the app on your emulator or physical device.

## 📱 How It Works

### 1. Organize with Notebooks
Create and manage dedicated notebooks for different subjects or projects (e.g., *Numerical NLP Methods* or *Cloud Computing*). This keeps your study materials perfectly isolated, ensuring the AI only focuses on the specific context you need at the moment.
<p align="center">
  <img src="screenshots/home.png" width="300" alt="Home Screen showing notebooks">
</p>

### 2. Curate Your Sources
Upload PDFs, text snippets, or web links directly to the **Curation Hub**. The AI instantly processes and analyzes these documents to build the core intelligence of your notebook, making them ready for deep analysis.
<p align="center">
  <img src="screenshots/sources.png" width="300" alt="Sources Tab showing uploaded PDFs">
</p>

### 3. Chat with Your Notes
Say goodbye to manual searching! Use the interactive AI Chat to ask complex questions, request specific summaries, and extract key concepts directly from the sources you uploaded. The AI cites your notes for every answer.
<p align="center">
  <img src="screenshots/chat.png" width="300" alt="Chat interface interacting with document">
</p>

### 4. Master Topics with Flashcards
Transform your documents into an active recall session instantly. The app generates targeted flashcards based on your materials, complete with a sleek, distraction-free player to test your knowledge.
<p align="center">
  <img src="screenshots/flashcards.png" width="300" alt="Interactive Flashcard Player">
</p>

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

## 📝 License
This project is open-source and available under the [MIT License](LICENSE).
