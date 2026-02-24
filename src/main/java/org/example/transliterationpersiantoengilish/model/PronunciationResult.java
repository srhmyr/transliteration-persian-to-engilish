package org.example.transliterationpersiantoengilish.model;

import lombok.Data;

@Data

public class PronunciationResult {

    private String originalWord;
    private PronunciationMatch exactMatch;
    private PronunciationMatch normalizedMatch;
    private PronunciationMatch substringMatch;
    private PronunciationMatch compositionMatch;
    private PronunciationMatch bestMatch;



    public void determineBestMatch() {

        if (exactMatch != null) {
            bestMatch = exactMatch;
        } else if (normalizedMatch != null) {
            bestMatch = normalizedMatch;
        } else if (compositionMatch != null) {
            bestMatch = compositionMatch;
        } else if (substringMatch != null) {
            bestMatch = substringMatch;
        }
    }

    public String getBestPronunciation() {
        return bestMatch != null ? bestMatch.getPronunciation() : null;
    }

}

