package org.example.transliterationpersiantoengilish.model;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;


@Data

@NoArgsConstructor

@AllArgsConstructor

public class DictionaryWord {

    private String persianWord;
    private String pronunciation;

    public String[] toCsvRow() {
        return new String[]{persianWord, pronunciation};
    }
}

