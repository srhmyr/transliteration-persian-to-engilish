package org.example.transliterationpersiantoengilish.model;

import lombok.Data;

import java.util.List;

@Data

public class TranslationResult {

    private String originalText;
    private List<TranslatedWord> words;
    private String fullPronunciation;

}

