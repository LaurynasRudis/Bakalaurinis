package commons;

import java.util.List;

public class SearchResult {
    private String id;
    private String label;
    private String lemma;
    private String score;
    private List<String> definition;
    private List<String> senseExample;

    public SearchResult(String id, String label, String lemma, String score, List<String> definition, List<String> senseExample){
        this.id = id;
        this.label = label;
        this.lemma = lemma;
        this.score = score;
        this.definition = definition;
        this.senseExample = senseExample;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLemma() {
        return lemma;
    }

    public String getScore() {
        return score;
    }

    public List<String> getDefinition() {
        return definition;
    }

    public List<String> getSenseExample() {
        return senseExample;
    }

}
