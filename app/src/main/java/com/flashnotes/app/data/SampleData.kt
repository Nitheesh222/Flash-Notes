package com.flashnotes.app.data

import com.flashnotes.app.R
import com.flashnotes.app.data.model.Flashcard
import com.flashnotes.app.data.model.Notebook
import com.flashnotes.app.data.model.Source
import com.flashnotes.app.data.model.SourceType
import com.flashnotes.app.data.model.StudioOption

object SampleData {
    val notebooks = listOf(
        Notebook(1, "Numerical NLP Methods: St...", 3, "March 10, 2026", R.drawable.ic_infographic),
        Notebook(2, "Cloud and Distributed Com...", 1, "March 10, 2026", R.drawable.ic_image), // Cloud placeholder
        Notebook(3, "Machine Learning Concept...", 1, "March 9, 2026", R.drawable.ic_quiz), // Robot placeholder
        Notebook(4, "CS50P: Harvard's Introduct...", 1, "February 21, 2026", R.drawable.ic_quiz) // Snake placeholder
    )

    val currentSources = listOf(
        Source(1, "BCA_CA2-Updated.pdf", SourceType.PDF, isLoading = true)
    )

    val studioOptions = listOf(
        StudioOption(R.string.flashcards, R.drawable.ic_flashcards, R.color.studioFlashcards)
    )

    val flashcards = listOf(
        Flashcard(
            1,
            "According to the BCA Semester 2 schedule, what is the course name for MAT1202?",
            "Discrete Mathematics"
        ),
        Flashcard(
            2,
            "What is the total number of credits for the Database Management Systems course?",
            "4 Credits"
        ),
        Flashcard(
            3,
            "Which room is assigned for the Object-Oriented Programming lab on Tuesdays?",
            "Lab 3, Computer Science Block"
        )
    )
}
