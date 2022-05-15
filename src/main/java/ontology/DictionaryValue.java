package ontology;

import commons.StringUtils;
import org.apache.solr.client.solrj.beans.Field;

import java.util.List;
import java.util.stream.Collectors;

public class DictionaryValue {
    @Field public String id;
    @Field public String label;
    @Field public List<String> senseExample;
    @Field public String lemma;
    @Field public List<String> definition;
    @Field public String label_unaccented;
    @Field public List<String> senseExample_unaccented;
    @Field public String lemma_unaccented;
    @Field public List<String> definition_unaccented;

    public DictionaryValue(
            String id,
            String label,
            List<String> senseExample,
            String lemma,
            List<String> definition
    ) {
        this.id = id;
        this.label = label;
        this.senseExample = senseExample;
        this.lemma = lemma;
        this.definition = definition;
        this.label_unaccented = StringUtils.stripAccents(label);
        this.senseExample_unaccented = senseExample.stream().map(StringUtils::stripAccents).collect(Collectors.toList());
        this.lemma_unaccented = StringUtils.stripAccents(lemma);
        this.definition_unaccented = definition.stream().map(StringUtils::stripAccents).collect(Collectors.toList());
    }

    public DictionaryValue(){}
}
