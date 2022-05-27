package ontology;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OntologyModel {
    private String ontologyUri = "http://www.lexinfo.net/lmf#";
    private OntModel model;
    private String lexicalEntryUri = ontologyUri + "LexicalEntry";
    private OntClass lexicalEntryClass;
    private Property hasLemma;
    private Property hasSense;
    private Property hasDefinition;
    private Property hasTextRepresentation;
    private Property hasSenseExample;
    private Property writtenForm;
    private Property text;
    private String graph ;


    public OntologyModel(String inputFileName) {
        try {
            InputStream input = new FileInputStream(inputFileName);
            OntModel model = ModelFactory.createOntologyModel();
            model.read(input, "");
            this.lexicalEntryClass = model.getOntClass(lexicalEntryUri);
            this.model = model;
            this.hasLemma = model.createProperty(ontologyUri, "hasLemma");
            this.hasSense = model.createProperty(ontologyUri, "hasSense");
            this.hasDefinition = model.createProperty(ontologyUri, "hasDefinition");
            this.hasTextRepresentation = model.createProperty(ontologyUri, "hasTextRepresentation");
            this.hasSenseExample = model.createProperty(ontologyUri, "hasSenseExample");
            this.writtenForm = model.createProperty(ontologyUri, "writtenForm");
            this.text = model.createProperty(ontologyUri, "text");
            this.graph = "";
        }
        catch (Exception e) {
            System.out.println("Caught exception: " + e.getMessage());
        }
    }

    public OntologyModel(Model model, String graphName) {
        OntModel ontologyModel = ModelFactory.createOntologyModel();
        ontologyModel.add(model);
        this.lexicalEntryClass = ontologyModel.getOntClass(lexicalEntryUri);
        this.model = ontologyModel;
        this.hasLemma = ontologyModel.createProperty(ontologyUri, "hasLemma");
        this.hasSense = ontologyModel.createProperty(ontologyUri, "hasSense");
        this.hasDefinition = ontologyModel.createProperty(ontologyUri, "hasDefinition");
        this.hasTextRepresentation = ontologyModel.createProperty(ontologyUri, "hasTextRepresentation");
        this.hasSenseExample = ontologyModel.createProperty(ontologyUri, "hasSenseExample");
        this.writtenForm = ontologyModel.createProperty(ontologyUri, "writtenForm");
        this.text = ontologyModel.createProperty(ontologyUri, "text");
        this.graph = graphName;
    }

    public OntModel getModel() { return model; }

    public ExtendedIterator<Individual> getLexicalEntryIndividualList() {
        return model.listIndividuals(lexicalEntryClass);
    }

    private StmtIterator findSenses(Individual individual) { return individual.listProperties(hasSense); }

    public List<String> findSenseExamples(Individual individual) {
        List<String> senseList = new ArrayList<>();
        StmtIterator senses = findSenses(individual);
        while(senses.hasNext()) {
            Resource sense = senses.next().getResource();
            try {
                senseList.add(sense
                        .getPropertyResourceValue(hasSenseExample)
                        .getProperty(text)
                        .getObject()
                        .toString());
            } catch (NullPointerException np) {
                senseList.add("null");
            }
        }
        return senseList;
    }

    public List<String> findDefinitions(Individual individual) {
        List<String> definitions = new ArrayList<>();
        StmtIterator senses = findSenses(individual);
        while (senses.hasNext()){
            Resource sense = senses.next().getResource();
            try {
                definitions.add(sense
                        .getPropertyResourceValue(hasDefinition)
                        .getPropertyResourceValue(hasTextRepresentation)
                        .getProperty(writtenForm)
                        .getObject().toString());
            } catch (NullPointerException np) {
                definitions.add("null");
            }
        }
        return definitions;
    }

    public String findLemma(Individual individual) {
        String lemma = "";
        try {
            lemma = individual
                    .getPropertyResourceValue(hasLemma)
                    .getProperty(writtenForm)
                    .getObject().toString();
        } catch (NullPointerException np) {
            lemma = "";
        }
        return lemma;
    }

    public List<DictionaryValue> getDictionaryValues() {
        ExtendedIterator<Individual> lexicalEntryListIterator = getLexicalEntryIndividualList();
        List<DictionaryValue> dictionaryValues = new ArrayList<>();
        while (lexicalEntryListIterator.hasNext()) {
            Individual individual = lexicalEntryListIterator.next();
            String id = individual.getURI();
            String label = individual.getLabel(null);
            List<String> senseExamples = findSenseExamples(individual);
            String lemma = findLemma(individual);
            List<String> definitions = findDefinitions(individual);
            dictionaryValues.add(new DictionaryValue(id, label, senseExamples, lemma, definitions));
        }
        return dictionaryValues;
    }

    public List<RDFIndexValue> getRDFIndexValues() {
        ExtendedIterator<Individual> lexicalEntryListIterator = getLexicalEntryIndividualList();
        List<RDFIndexValue> rdfIndexValues = new ArrayList<>();
        while (lexicalEntryListIterator.hasNext()) {
            Individual individual = lexicalEntryListIterator.next();
            String uri = individual.getURI();
            String label = individual.getLabel(null);
            List<String> senseExamples = findSenseExamples(individual);
            String lemma = findLemma(individual);
            List<String> definitions = findDefinitions(individual);
            String graph = this.graph;
            rdfIndexValues.add(new RDFIndexValue(uri, label, senseExamples, lemma, definitions, graph));
        }
        return rdfIndexValues;
    }
}
