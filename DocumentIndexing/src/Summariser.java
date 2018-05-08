import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Summariser {
    private static final Pattern HEADLINE_TAG_REGEX = Pattern.compile("<HEADLINE>(.+?)</HEADLINE>");
    private static final Pattern TEXT_TAG_REGEX = Pattern.compile("<TEXT>(.+?)</TEXT>");
    private static final Pattern PARAGRAPH_REGEX = Pattern.compile("<P>|</P>");
    private static final String DOC_END_TAG = "</DOC>";

    private Scanner scanner;

    public Summariser(String filename) {
        scanner = new Scanner(filename);
    }

    public ArrayList<String> generateSummaries(ArrayList<String> docNumbers) {
        ArrayList<String> summaries = new ArrayList<>();
        for (String docNo :
                docNumbers) {
            String document = readDocument(docNo);
            ArrayList<String> sentences = makeSentences(document);
            SentenceSimilarity similar = new SentenceSimilarity(sentences);
            String summary = similar.generateSummary();
            summaries.add(summary);
        }
        return summaries;
    }

    private String readDocument(String docNo) {
        StringBuilder builder = new StringBuilder();
        while (scanner.hasNext()) {
            String line = scanner.next();
            if (line.contains(docNo)) {
                while (scanner.hasNext() && !line.contains(DOC_END_TAG)) {
                    line = scanner.next();
                    builder.append(line);
                }
            }
        }
        return builder.toString();
    }

    private ArrayList<String> makeSentences(String document) {
        ArrayList<String> sentences = new ArrayList<>();

        String headline = getHeadline(document);
        sentences.add(headline);

        String text = getText(document);

        BreakIterator boundary = BreakIterator.getSentenceInstance();
        boundary.setText(text);
        int start = boundary.first();
        int end = boundary.next();

        do {
            sentences.add(text.substring(start, end).trim());
            start = end;
            end = boundary.next();
        } while (end != BreakIterator.DONE);

        return sentences;
    }

    private String getText(String document) {
        Matcher matcher = TEXT_TAG_REGEX.matcher(document);
        return (PARAGRAPH_REGEX.matcher(matcher.group(1)).replaceAll(" "));
    }

    private String getHeadline(String document) {
        Matcher matcher = HEADLINE_TAG_REGEX.matcher(document);
        return (PARAGRAPH_REGEX.matcher(matcher.group(1)).replaceAll(" "));
    }

}
