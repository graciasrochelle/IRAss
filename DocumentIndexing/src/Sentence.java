import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Pattern;

import static java.lang.Math.sqrt;

public class Sentence implements Comparable<Sentence> {
    private String fullText;
    private HashMap<String, Integer> termFrequency;
    private int id;
    private double norm = -1;
    private ArrayList<Sentence> similarSentences = new ArrayList<>();
    private static final Pattern FILTER_REGEX = Pattern.compile("[\\p{Punct}]");
    private double rank = 0.0;

    public Sentence(String fullText, int id) {
        this.fullText = fullText;
        this.termFrequency = calculateTermFrequency(fullText);
        this.id = id;
    }

    private HashMap<String, Integer> calculateTermFrequency(String fullText) {
        String text = removePunctuation(fullText);
        ArrayList<String> terms = tokenise(text);
        return countTerms(terms);
    }

    private HashMap<String, Integer> countTerms(ArrayList<String> terms) {
        HashMap<String, Integer> termFrequencyList = new HashMap<>();

        for (String term : terms) {
            if (!term.equals(" ") && !term.equals("")) {
                if (termFrequencyList.containsKey(term)) {
                    termFrequencyList.put(term, termFrequencyList.get((term)) + 1);
                } else {
                    termFrequencyList.put(term, 1);
                }
            }
        }
        return termFrequencyList;
    }

    private ArrayList<String> tokenise(String text) {
        String[] splitText = text.split("\\s+");
        return new ArrayList<>(Arrays.asList(splitText));
    }

    private String removePunctuation(String text) {
        return FILTER_REGEX.matcher(text).replaceAll(" ");
    }

    public void addSimilarSentence(Sentence newSentence) {
        similarSentences.add(newSentence);
        rank = similarSentences.size();
    }

    public double getNorm() {
        if (norm > 0) {
            return norm;
        } else {
            norm = 0;
            for (Integer i : termFrequency.values()) {
                norm += i ^ 2;
            }
            norm = sqrt(norm);
            return norm;
        }
    }

    public double dot(Sentence other) {
        double result = 0.0;
        for (String term : termFrequency.keySet()) {
            Integer otherValue = other.getTermFrequency().get(term);
            if (otherValue != null) {
                result += otherValue * termFrequency.get(term);
            }
        }
        return result;
    }

    public double cosineSimilarity(Sentence other) {
        return this.dot(other) / (this.getNorm() * other.getNorm());
    }

    public double queryBiasedRank(ArrayList<String> queryTerms) {
        double count = 0.0;
        for (String term :
                queryTerms) {
            if (termFrequency.get(term) != null) {
                count++;
            }
        }
        rank = count * count / queryTerms.size();
        return rank;
    }

    public int getSizeSimilarSentences() {
        return similarSentences.size();
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public int getNumTerms() {
        return termFrequency.size();
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public HashMap<String, Integer> getTermFrequency() {
        return termFrequency;
    }

    public void setTermFrequency(HashMap<String, Integer> termFrequency) {
        this.termFrequency = termFrequency;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int compareTo(Sentence o) {
        return (int) Math.signum(o.getRank() - rank);
    }

    @Override
    public String toString() {
        return fullText;
    }
}