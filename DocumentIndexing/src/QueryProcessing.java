/**
 * Query processing Algorithm
 * To retrieve the top r relevant documents for a query Q:
 * 1) Initialize an empty hash table, H
 * 2) For each term t query Q
 * a) Search the lexicon for t
 * b) Record ft and address of the inverted list It
 * c) Fetch inverted list It
 * d) For each posting <d, fd,t> in It
 * i) If no accumulator for d exists in hash table H
 * Create an accumulator Ad for d
 * Update value for new entry, using similarity metric
 * ii) Else
 * Update value for existing accumulator Ad, using similarity metric
 * 3) For 1<d<=r
 * Select d such that Ad=max(Ad of entries in H)
 * Retrieve d and present to the user
 * Can sort the set of accumulators, Ad, and present the top r answers
 **/

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

class QueryProcessing {

    private static final String READ_MODE = "r";
    private static final boolean VERBOSE = false;
    private final String invlistFile;
    private final Integer numResults;
    private final Integer queryLabel;

    private StopwordRemover stopwordRemover = null;
    private ArrayList<String> queryTerms = null;
    private final HashMap<Integer, Document> documentIDMap;
    private final HashMap<String, IndexEntry> lexicon;
    private final HashMap<Integer, Double> accumulativeScore = new HashMap<>();

    QueryProcessing(Integer queryLabel, String queryString, Integer numResults,
                    HashMap<String, IndexEntry> lexicon, HashMap<Integer, Document> documentIDMap,
                    String invlistFile, String stoplist) {

        this.queryLabel = queryLabel;
        this.numResults = numResults;
        this.lexicon = lexicon;
        this.documentIDMap = documentIDMap;
        this.invlistFile = invlistFile;
        if (stoplist != null && !stoplist.trim().equals("")) {
            stopwordRemover = new StopwordRemover(stoplist);
        }
        processingQueryString(queryString);
        processingEachQueryTerm();
    }

    private void processingQueryString(String queryString) {
        //Filtering Query
        queryString = queryString.replaceAll("[\\p{Punct}]", " ").replaceAll("\\d+", "");
        queryString = queryString.toLowerCase();
        //Break query into tokens
        String[] splitQueryString = queryString.split("\\s+");
        this.queryTerms = new ArrayList<>(Arrays.asList(splitQueryString));
        //Removing Stoplist words
        if (stopwordRemover != null) {
            queryTerms = stopwordRemover.removeStopwords(queryTerms);
        }
    }

    private void processingEachQueryTerm() {
        for (String term : queryTerms) {
            if (!term.equals("") && lexicon.containsKey(term)) {
                IndexEntry entry = lexicon.get(term);
                int docFrequency = entry.getDocumentFrequency();
                long byteOffset = entry.getByteOffset();
                HashMap<Integer, Integer> invlist = readInvlistFile(docFrequency, byteOffset);
                for (Integer key : invlist.keySet()) {
                    double score = Document.calcSimilarityScore(documentIDMap.get(key).getkValue(), invlist.get(key), docFrequency);
                    if (accumulativeScore.containsKey(key)) {
                        score = accumulativeScore.get(key) + score;
                    }
                    accumulativeScore.put(key, score);
                    if (VERBOSE) {
                        System.out.print("Query Label: " + queryLabel);
                        System.out.print("|| DocNum:" + documentIDMap.get(key).getDocNo());
                        System.out.print("|| Rank:?");
                        System.out.print("|| kValue:" + documentIDMap.get(key).getkValue());
                        System.out.print("|| Score:" + accumulativeScore.get(key));
                        System.out.println();
                    }
                }
            }
        }
    }

    private HashMap<Integer, Integer> readInvlistFile(int docFrequency, long byteOffset) {
        HashMap<Integer, Integer> invlist = new HashMap<>();
        try (RandomAccessFile indexFile = new RandomAccessFile(invlistFile, READ_MODE)) {

            indexFile.seek(byteOffset);
            for (int i = 0; i < docFrequency; i++) {
                int docID = indexFile.readInt();
                int termFrequency = indexFile.readInt();
                String docNo = documentIDMap.get(docID).getDocNo();
                invlist.put(docID, termFrequency);
                if (VERBOSE) {
                    System.out.print("DocID: " + docID);
                    System.out.print("||DocNo:" + docNo);
                    System.out.print("||termFrequency:" + termFrequency);
                    System.out.println();
                }
            }
        } catch (IOException e) {
            System.err.println("Problem with reading from index file!");
        }
        return invlist;
    }

    void displayResults(Summariser summariser) {
        TopNResults topNResults = new TopNResults(numResults, accumulativeScore);
        HashMap<Integer, Integer> topResults = topNResults.getTopNResults();
        System.out.println();

        for (Integer rank : topResults.keySet()) {
            Integer key = topResults.get(rank);
            System.out.print(queryLabel + " ");
            System.out.print(documentIDMap.get(key).getDocNo() + " ");
            System.out.print(rank + " ");
            System.out.print(accumulativeScore.get(key) + " ");
            System.out.println();
            if (summariser != null) {
                System.out.println(summariser.generateSummary(queryTerms, documentIDMap.get(key).getDocNo()));
                System.out.println();
            }
        }
        System.out.println();
    }
}
