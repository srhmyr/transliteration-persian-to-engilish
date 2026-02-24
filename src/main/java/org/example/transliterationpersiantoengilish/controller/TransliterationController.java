package org.example.transliterationpersiantoengilish.controller;

import lombok.RequiredArgsConstructor;
import org.example.transliterationpersiantoengilish.service.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transliteration")
@RequiredArgsConstructor
public class TransliterationController {
    private final TranslationService service;

    @GetMapping("/persian-to-english")
    public ResponseEntity<String> transliterate(@RequestParam(value = "persian", required = true)
                                                String persian) {
        return ResponseEntity.ok(service.translateWithPronunciation(persian).getFullPronunciation());
    }
}
