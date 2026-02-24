package org.example.transliterationpersiantoengilish.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data

@AllArgsConstructor

public class TranslatedWord {

    private String persianWord;
    private String pronunciation;
    private boolean found;
}

