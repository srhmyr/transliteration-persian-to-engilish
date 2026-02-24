package org.example.transliterationpersiantoengilish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transliterationpersiantoengilish.model.PronunciationMatch;
import org.example.transliterationpersiantoengilish.model.PronunciationResult;
import org.example.transliterationpersiantoengilish.model.TranslatedWord;
import org.example.transliterationpersiantoengilish.model.TranslationResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor

public class TranslationService {

    private final PronunciationLoaderService pronunciationService;
    private final PronunciationLoaderService pronunciationLoaderService;
    private Map<String, String> pronunciationMap;

    private Map<String, String> getPronunciationMap() {
        try {
            pronunciationMap = pronunciationService.getPronunciationMap();
        } catch (IOException e) {
            log.error("error to read pronunciation file", e);
        }
        return pronunciationMap;
    }

    public TranslationResult translateWithPronunciation(String persianText) {
        getPronunciationMap();
        persianText = normatilzedToFarsi(persianText);
        persianText = Normalizer.normalize(persianText, Normalizer.Form.NFC);
        TranslationResult result = new TranslationResult();
        result.setOriginalText(persianText);
        String[] words = persianText.split("[\\s\\p{Punct}]+");
        List<TranslatedWord> translatedWords = new ArrayList<>();
        StringBuilder fullPronunciation = new StringBuilder();

        for (String word : words) {
            if (word.trim().isEmpty()) {
                continue;
            }

            String pronunciation = getHighHeuristicPronunciation(word);
            boolean found = pronunciation != null;
            if (!found) {
                log.debug("Word not found in dictionary: {}", word);
                pronunciation = "[UNKNOWN]";
            }
            TranslatedWord tw = new TranslatedWord(word, pronunciation, found);
            translatedWords.add(tw);
            fullPronunciation.append(pronunciation).append(" ");
        }
        result.setWords(translatedWords);
        String pingilishWord = fullPronunciation.toString().trim();
        pingilishWord = pingilishWord.toLowerCase();
        result.setFullPronunciation(pingilishWord);
        return result;
    }

    private String normatilzedToFarsi(String name) {
        name.replace("ي", "ی");
        name.replace("ك", "ک");
        name.replace("ئ", "ی");
        return name;
    }

    public String getHighHeuristicPronunciation(String dictionaryWord) {
        log.debug("Heuristic lookup for: '{}'", dictionaryWord);
        PronunciationResult result = new PronunciationResult();
        result.setOriginalWord(dictionaryWord);
        result.setExactMatch(tryExactMatch(dictionaryWord));
        if (result.getExactMatch() == null) {
            result.setNormalizedMatch(tryNormalizedMatch(dictionaryWord));
        }

        if (result.getExactMatch() == null && result.getNormalizedMatch() == null) {
            result.setSubstringMatch(trySubstringMatch(dictionaryWord));
        }

        if (result.getExactMatch() == null && result.getNormalizedMatch() == null) {
            result.setCompositionMatch(tryMergeSubstringMatch(dictionaryWord));
        }

        result.determineBestMatch();
        return result.getBestPronunciation();
    }

    private PronunciationMatch tryExactMatch(String word) {
        String pronunciation = pronunciationMap.get(word);
        if (pronunciation != null) {
            return new PronunciationMatch(
                    word, pronunciation, "EXACT", 100.0, word
            );
        }
        return null;
    }

    private PronunciationMatch tryNormalizedMatch(String word) {
        List<String> normalizedVariations = generateNormalizations(word);
        for (String normalized : normalizedVariations) {
            String pronunciation = pronunciationMap.get(normalized);
            if (pronunciation != null) {
                return new PronunciationMatch(
                        normalized, pronunciation, "NORMALIZED", 95.0, word
                );
            }
        }
        return null;
    }

    private List<String> generateNormalizations(String word) {
        List<String> variations = new ArrayList<>();
        variations.add(Normalizer.normalize(word, Normalizer.Form.NFC));
        variations.add(Normalizer.normalize(word, Normalizer.Form.NFD));
        variations.add(Normalizer.normalize(word, Normalizer.Form.NFKC));
        variations.add(Normalizer.normalize(word, Normalizer.Form.NFKD));
        variations.add(word.replace('ي', 'ی').replace('ك', 'ک'));
        variations.add(word.replace('ي', 'ی'));
        variations.add(word.replace('ك', 'ک'));
        variations.add(word.replaceAll("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]", ""));
        variations.add(word.replaceAll("[\\u200C\\u200D\\u200E\\u200F]", ""));
        variations.add(word.replaceAll("\\s+", ""));
        variations.add(word.replace("ـ", ""));
        return variations.stream()
                .filter(v -> !v.isEmpty() && !v.equals(word))
                .distinct()
                .collect(Collectors.toList());
    }

    private PronunciationMatch trySubstringMatch(String word) {
        word = pronunciationService.getNormalizations(word);
        List<PronunciationMatch> matches = new ArrayList<>();
        for (String key : pronunciationMap.keySet()) {
            if (word.contains(key) && key.length() >= 2) {
                double confidence = (key.length() * 100.0) / word.length();
                matches.add(new PronunciationMatch(
                        key, pronunciationMap.get(key), "SUBSET_CONTAINS", confidence, word
                ));
            }
            if (key.contains(word) && word.length() >= 2) {
                double confidence = (word.length() * 100.0) / key.length();
                matches.add(new PronunciationMatch(
                        key, pronunciationMap.get(key), "SUPERSET_CONTAINS", confidence, word
                ));
            }
        }
        if (!matches.isEmpty()) {
            return matches.stream()
                    .max(Comparator.comparingDouble(PronunciationMatch::getConfidence))
                    .orElse(null);
        }
        return tryPrefixSuffixMatch(word);
    }

    private PronunciationMatch tryPrefixSuffixMatch(String word) {
        List<PronunciationMatch> matches = new ArrayList<>();
        for (String key : pronunciationMap.keySet()) {
            int prefixLength = getCommonPrefixLength(word, key);
            if (prefixLength >= 3) {
                double confidence = (prefixLength * 100.0) / Math.min(word.length(), key.length());
                matches.add(new PronunciationMatch(
                        key, pronunciationMap.get(key), "PREFIX_MATCH", confidence, word
                ));
            }
            int suffixLength = getCommonSuffixLength(word, key);
            if (suffixLength >= 3) {
                double confidence = (suffixLength * 100.0) / Math.min(word.length(), key.length());
                matches.add(new PronunciationMatch(
                        key, pronunciationMap.get(key), "SUFFIX_MATCH", confidence, word
                ));
            }
        }
        if (!matches.isEmpty()) {
            return matches.stream()
                    .max(Comparator.comparingDouble(PronunciationMatch::getConfidence))
                    .orElse(null);
        }
        return null;
    }

    private int getCommonPrefixLength(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return minLength;
    }

    private int getCommonSuffixLength(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 1; i <= minLength; i++) {
            if (s1.charAt(s1.length() - i) != s2.charAt(s2.length() - i)) {
                return i - 1;
            }
        }
        return minLength;
    }

    public List<String> findSubstringMatches(String word) {
        List<String> matches = new ArrayList<>();
        for (String key : pronunciationMap.keySet()) {
            if (key.length() <= word.length() && word.contains(key)) {
                matches.add(key);
                if(key.length() < 2) {
                    log.info(key +":"+ pronunciationMap.get(key));
                }
            }
        }
        matches.sort((a, b) -> Integer.compare(b.length(), a.length()));
        return matches;
    }

    public PronunciationMatch tryMergeSubstringMatch(String word) {
        word = pronunciationService.getNormalizations(word);
        List<String> segmentedWords = segmentWord(word);
        if (!segmentedWords.isEmpty()) {
            StringBuilder pronunciation = new StringBuilder();
            for (String segment : segmentedWords) {
                pronunciation.append(pronunciationMap.get(segment));
            }
            return new PronunciationMatch(
                    word,
                    pronunciation.toString(),
                    "SEGMENTED_COMPOSITION",
                    100.0,
                    word
            );
        }
        return findBestSingleMatch(word);
    }

    public List<String> segmentWord(String word) {
        List<String> candidates = findSubstringMatches(word);
        List<List<String>> results = new ArrayList<>();
        backtrack(word, candidates, new ArrayList<>(), results, 0);
        return results.isEmpty() ? new ArrayList<>() : results.get(0);
    }

    private void backtrack(String word, List<String> candidates,
                           List<String> current, List<List<String>> results, int start) {
        if (start == word.length()) {
            results.add(new ArrayList<>(current));
            return;
        }
        for (String candidate : candidates) {
            if (start + candidate.length() <= word.length() &&
                    word.substring(start, start + candidate.length()).equals(candidate)) {
                current.add(candidate);
                backtrack(word, candidates, current, results, start + candidate.length());
                current.remove(current.size() - 1);
                if (!results.isEmpty()) {
                    return;
                }
            }
        }
    }

    private PronunciationMatch findBestSingleMatch(String word) {
        List<PronunciationMatch> matches = new ArrayList<>();
        for (String key : pronunciationMap.keySet()) {
            if (word.contains(key) && key.length() >= 2) {
                if (key.length() == word.length()) {
                    matches.add(new PronunciationMatch(
                            key, pronunciationMap.get(key), "EXACT_MATCH", 100.0, word
                    ));
                } else {
                    matches.add(new PronunciationMatch(
                            key, pronunciationMap.get(key), "SUBSET_CONTAINS", 60.0, word
                    ));
                }
            }
            if (key.contains(word) && word.length() >= 2) {
                matches.add(new PronunciationMatch(
                        key, pronunciationMap.get(key), "SUPERSET_CONTAINS", 60.0, word
                ));
            }
        }
        if (!matches.isEmpty()) {
            return matches.stream()
                    .max(Comparator.comparingDouble(PronunciationMatch::getConfidence))
                    .orElse(null);
        }
        return null;
    }
}
