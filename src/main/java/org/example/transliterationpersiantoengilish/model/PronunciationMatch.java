package org.example.transliterationpersiantoengilish.model;

import lombok.AllArgsConstructor;

import lombok.Data;

@Data

@AllArgsConstructor

public class PronunciationMatch {

    private String matchedKey;
    private String pronunciation;
    private String matchType;
    private double confidence;
    private String originalWord;

}

