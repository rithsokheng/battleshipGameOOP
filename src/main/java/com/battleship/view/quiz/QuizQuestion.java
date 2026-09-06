package com.battleship.view.quiz;

/**
 * One multiple-choice question shown by the NuclearLaunchDialog "mysterious box"
 * before a Nuclear launcher shot is allowed to fire. Pure UI data — not a domain
 * model, so it lives under view rather than model.
 */
public record QuizQuestion(String prompt, String[] options, int correctIndex) {
}
