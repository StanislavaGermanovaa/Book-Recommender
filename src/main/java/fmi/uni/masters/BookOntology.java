package fmi.uni.masters;

import fmi.uni.masters.db.DatabaseManager;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import openllet.jena.PelletReasonerFactory;
import org.apache.jena.reasoner.Reasoner;


public class BookOntology {

    private static final String FILE_PATH = "books.owl";
    private static final String BASE = "http://www.semanticweb.org/ontologies/books";
    private static final String NS = BASE + "#";

    private OntModel baseModel;
    private OntModel model;

    private DatabaseManager dbManager;

    public BookOntology() {
        this.dbManager = new DatabaseManager();
        loadOntology();
    }

    private void loadOntology() {
        try (InputStream in = new FileInputStream(FILE_PATH)) {
            this.baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            this.baseModel.read(in, BASE);

            if (this.dbManager != null) {
                this.dbManager.syncLikesToOntology(this.baseModel, NS);
            }
            refreshReasoner();

            System.out.println("Book ontology loaded successfully");
        } catch (Exception e) {
            System.err.println("Error loading book ontology: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveOntology() {
        try (OutputStream out = new FileOutputStream(FILE_PATH)) {
            baseModel.write(out, "RDF/XML", BASE);
            System.out.println("Ontology saved successfully");
        } catch (Exception e) {
            System.err.println("Error saving ontology: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addBook(String name, String bookClassType) {
        OntClass bookClass = baseModel.getOntClass(NS + bookClassType);

        if (bookClass == null) {
            bookClass = baseModel.getOntClass(NS + "Book");
        }

        if (bookClass != null) {
            baseModel.createIndividual(NS + name, bookClass);
            saveOntology();
            refreshReasoner();
        }
    }

    public void deleteBook(String name) {
        Individual ind = baseModel.getIndividual(NS + name);
        if (ind == null) {
            ind = baseModel.getIndividual("#" + name);
        }
        if (ind != null) {
            ind.remove();
            saveOntology();
            refreshReasoner();
        }
    }

    public void addPropertyToBook(String bookName, String valueName, String propName) {
        Individual ind = baseModel.getIndividual(NS + bookName);
        if (ind == null) ind = baseModel.getIndividual("#" + bookName);
        if (ind == null) return;

        ObjectProperty prop = baseModel.getObjectProperty(NS + propName);
        if (prop == null) {
            prop = baseModel.getObjectProperty("#" + propName);
        }
        if (prop == null) {
            prop = baseModel.createObjectProperty(NS + propName);
        }

        Resource value = baseModel.getResource(NS + valueName);
        if (value == null) {
            value = baseModel.getResource("#" + valueName);
        }

        if (!baseModel.contains(value, null, (RDFNode) null) && propName.equals("writtenBy")) {
            OntClass authorClass = baseModel.getOntClass(NS + "Author");
            if (authorClass == null) authorClass = baseModel.getOntClass("#" + "Author");
            if (authorClass != null) {
                value = baseModel.createIndividual(NS + valueName, authorClass);
            }
        }

        ind.setPropertyValue(prop, value);
        saveOntology();
        refreshReasoner();
    }

    public List<String> getAllBooksNames() {
        List<String> result = new ArrayList<>();

        if (this.model == null) {
            return result;
        }
        OntClass bookClass = model.getOntClass(NS + "Book");
        if (bookClass == null) return result;

        ExtendedIterator<? extends OntResource> instances = bookClass.listInstances();
        while (instances.hasNext()) {
            OntResource r = instances.next();
            if (r.getURI() != null && r.getURI().startsWith(NS)) {
                result.add(localName(r));
            }
        }
        return result;
    }

    public BookInfo getBook(String bookName) {
        Individual ind = model.getIndividual(NS + bookName);
        if (ind == null) ind = model.getIndividual("#" + bookName);
        if (ind == null) return null;

        BookInfo info = new BookInfo();
        info.setName(bookName);

        Property writtenBy = model.getProperty(NS + "writtenBy");
        if (writtenBy == null) writtenBy = model.getProperty("#" + "writtenBy");
        if (writtenBy != null) {
            NodeIterator authorIt = model.listObjectsOfProperty(ind, writtenBy);
            while (authorIt.hasNext()) {
                info.getAuthors().add(localName(authorIt.next().asResource()));
            }
        }

        Property hasGenre = model.getProperty(NS + "hasGenre");
        if (hasGenre == null) hasGenre = model.getProperty("#" + "hasGenre");
        NodeIterator genreIt = model.listObjectsOfProperty(ind, hasGenre);
        while (genreIt.hasNext()) {
            info.getGenres().add(localName(genreIt.next().asResource()));
        }

        Property hasRating = model.getProperty(NS + "hasRating");
        if (hasRating == null) hasRating = model.getProperty("#" + "hasRating");
        NodeIterator ratingIt = model.listObjectsOfProperty(ind, hasRating);
        while (ratingIt.hasNext()) {
            info.getRatings().add(localName(ratingIt.next().asResource()));
        }

        DatatypeProperty pubYearProp = model.getDatatypeProperty(NS + "hasPublicationYear");
        if (pubYearProp == null) pubYearProp = model.getDatatypeProperty("#" + "hasPublicationYear");
        if (pubYearProp != null && ind.hasProperty(pubYearProp)) {
            info.setPublicationYear(ind.getPropertyValue(pubYearProp).asLiteral().getInt());
        }

        Property publishedBy = model.getProperty(NS + "publishedBy");
        if (publishedBy == null) publishedBy = model.getProperty("#" + "publishedBy");
        if (publishedBy != null) {
            NodeIterator pubIt = model.listObjectsOfProperty(ind, publishedBy);
            while (pubIt.hasNext()) {
                info.getPublishers().add(localName(pubIt.next().asResource()));
            }
        }

        Property belongsToSeries = model.getProperty(NS + "belongsToSeries");
        if (belongsToSeries == null) belongsToSeries = model.getProperty("#" + "belongsToSeries");
        if (belongsToSeries != null && ind.hasProperty(belongsToSeries)) {
            RDFNode seriesNode = ind.getPropertyValue(belongsToSeries);
            if (seriesNode != null && seriesNode.isResource()) info.setSeriesName(localName(seriesNode.asResource()));
        }

        DatatypeProperty seriesOrderProp = model.getDatatypeProperty(NS + "hasSeriesOrder");
        if (seriesOrderProp == null) seriesOrderProp = model.getDatatypeProperty("#" + "hasSeriesOrder");
        if (seriesOrderProp != null && ind.hasProperty(seriesOrderProp)) {
            info.setSeriesOrder(ind.getPropertyValue(seriesOrderProp).asLiteral().getInt());
        }

        Property isSimilarTo = model.getProperty(NS + "isSimilarTo");
        if (isSimilarTo == null) isSimilarTo = model.getProperty("#" + "isSimilarTo");
        NodeIterator simIt = model.listObjectsOfProperty(ind, isSimilarTo);
        while (simIt.hasNext()) {
            info.getSimilarBooks().add(localName(simIt.next().asResource()));
        }

        if (!info.getAuthors().isEmpty()) {
            OntClass bookClass = model.getOntClass(NS + "Book");
            if (bookClass != null) {
                ExtendedIterator<? extends OntResource> instances = bookClass.listInstances();
                while (instances.hasNext()) {
                    OntResource r = instances.next();
                    String otherBookName = localName(r);
                    if (otherBookName.equals(bookName)) continue; // Прескачаме себе си

                    NodeIterator otherAuthorsIt = model.listObjectsOfProperty(r, writtenBy);
                    while (otherAuthorsIt.hasNext()) {
                        String author = localName(otherAuthorsIt.next().asResource());
                        if (info.getAuthors().contains(author)) {
                            info.getSameAuthorBooks().add(otherBookName);
                            break;
                        }
                    }
                }
            }
        }

        ExtendedIterator<Resource> typesIt = ind.listRDFTypes(true);
        while (typesIt.hasNext()) {
            Resource type = typesIt.next();
            String typeName = localName(type);
            if (!typeName.equals("NamedIndividual") && !typeName.equals("Thing")) {
                info.getInferredTypes().add(typeName);
            }
        }

        try {
            Property hasLiked = model.getProperty(NS + "hasLiked");
            Property hasRecommendedBook = model.getProperty(NS + "hasRecommendedBook");

            if (hasLiked != null) {
                ResIterator it = model.listSubjectsWithProperty(hasLiked, ind);
                while (it.hasNext()) info.getInferredTypes().add("LikedBy_" + it.nextResource().getLocalName());
            }
            if (hasRecommendedBook != null) {
                ResIterator it = model.listSubjectsWithProperty(hasRecommendedBook, ind);
                while (it.hasNext()) info.getInferredTypes().add("RecommendedTo_" + it.nextResource().getLocalName());
            }
        } catch (Exception e) { e.printStackTrace(); }

        return info;
    }

    public void addBookPublicationYear(String bookName, int year) {
        Individual bookInd = baseModel.getIndividual(NS + bookName);
        if (bookInd == null) bookInd = baseModel.getIndividual("#" + bookName);

        DatatypeProperty yearProp = baseModel.getDatatypeProperty(NS + "hasPublicationYear");
        if (yearProp == null) yearProp = baseModel.getDatatypeProperty("#" + "hasPublicationYear");

        if (bookInd != null && yearProp != null) {
            bookInd.removeAll(yearProp);
            bookInd.addProperty(yearProp, baseModel.createTypedLiteral(year));
            saveOntology();
            refreshReasoner();
        } else {
            System.err.println("Error: Book or property 'hasPublicationYear' not found!");
        }
    }

    public void addBookSeriesOrder(String bookName, int order) {
        Individual bookInd = baseModel.getIndividual(NS + bookName);
        if (bookInd == null) bookInd = baseModel.getIndividual("#" + bookName);

        DatatypeProperty orderProp = baseModel.getDatatypeProperty(NS + "hasSeriesOrder");
        if (orderProp == null) orderProp = baseModel.getDatatypeProperty("#" + "hasSeriesOrder");

        if (bookInd != null && orderProp != null) {
            bookInd.removeAll(orderProp);
            bookInd.addProperty(orderProp, baseModel.createTypedLiteral(order));
            saveOntology();
            refreshReasoner();
        } else {
            System.err.println("Error: Book or property 'hasSeriesOrder' not found!");
        }
    }

    public boolean hasIndividual(String id) {
        return model.getIndividual(NS + id) != null || model.getIndividual("#" + id) != null;
    }

    public boolean validateUserLogin(String username, String password) {
        boolean isValid = dbManager.validateUser(username, password);

        if (isValid) {
            OntClass userClass = baseModel.getOntClass(NS + "User");
            if (userClass != null && baseModel.getIndividual(NS + username) == null) {
                baseModel.createIndividual(NS + username, userClass);
                refreshReasoner();
            }
        }
        return isValid;
    }

    public boolean registerNewUser(String username, String password) {
        boolean dbSuccess = dbManager.registerUser(username, password);

        if (!dbSuccess) {
            return false;
        }
        try {
            OntClass userClass = baseModel.getOntClass(NS + "User");
            if (userClass != null) {
                Individual userInd = baseModel.createIndividual(NS + username, userClass);
                refreshReasoner();
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean interactWithBook(String username, String bookTitle, String action) {
        try {
            Individual userBase = baseModel.getIndividual(NS + username);
            Individual bookBase = baseModel.getIndividual(NS + bookTitle);
            if (userBase == null) userBase = baseModel.getIndividual("#" + username);
            if (bookBase == null) bookBase = baseModel.getIndividual("#" + bookTitle);

            Individual userInf = model.getIndividual(NS + username);
            Individual bookInf = model.getIndividual(NS + bookTitle);
            if (userInf == null) userInf = model.getIndividual("#" + username);
            if (bookInf == null) bookInf = model.getIndividual("#" + bookTitle);

            ObjectProperty hasLikedBase = baseModel.getObjectProperty(NS + "hasLiked");
            if (hasLikedBase == null) hasLikedBase = baseModel.getObjectProperty("#" + "hasLiked");

            ObjectProperty hasLikedInf = model.getObjectProperty(NS + "hasLiked");
            if (hasLikedInf == null) hasLikedInf = model.getObjectProperty("#" + "hasLiked");

            if (userBase == null || bookBase == null) {
                return false;
            }

            if ("LIKE".equals(action)) {
                dbManager.insertLike(username, bookTitle);
                if (!userBase.hasProperty(hasLikedBase, bookBase)) {
                    userBase.addProperty(hasLikedBase, bookBase);
                }

            } else if ("UNLIKE".equals(action)) {
                dbManager.removeLike(username, bookTitle);

                if (userBase.hasProperty(hasLikedBase, bookBase)) {
                    userBase.removeProperty(hasLikedBase, bookBase);
                }
            }

            refreshReasoner();

            return true;
        } catch (Exception e) {
            System.err.println("Error " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String localName(Resource r) {
        if (r == null) return "";
        String uri = r.getURI();
        if (uri != null && uri.contains("#")) {
            return uri.substring(uri.indexOf("#") + 1);
        }
        return r.getLocalName() != null ? r.getLocalName() : "";
    }

    private void refreshReasoner() {
        try {
            Reasoner reasoner = PelletReasonerFactory.theInstance().create();
            InfModel infModel = ModelFactory.createInfModel(reasoner, this.baseModel);

            this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, infModel);

        } catch (Exception e) {
            System.err.println("Error refreshing the Reasoner: " + e.getMessage());
        }
    }
}