package com.academy.common;

/**
 * Published whenever the learner does something that counts toward today's training session
 * (review, quiz, lesson completed, explanation practised). Decouples the services that record
 * activity from the Today session that evaluates it.
 */
public record ProgressEvent(Long userId) {
}
