package fmi.uni.masters.agent;

import fmi.uni.masters.BookOntology;
import fmi.uni.masters.BookInfo;
import fmi.uni.masters.behaviours.WaitForRequests;
import fmi.uni.masters.gui.AdminWebServer;
import fmi.uni.masters.behaviours.WaitForBookRequest;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.List;

public class BookRecommenderAgent extends Agent {

    private BookOntology ontology;

    @Override
    protected void setup() {
        System.out.println("BookRecommenderAgent - started");
        ontology = new BookOntology();

        DFAgentDescription ad = new DFAgentDescription();
        ad.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-recommender");
        sd.setName("FMI Book Recommender Service");
        ad.addServices(sd);

        try {
            DFService.register(this, ad);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        AdminWebServer.start(this);

        addBehaviour(new WaitForBookRequest(ontology, this));
        addBehaviour(new WaitForRequests(this, ontology));
    }

    public List<String> getAllBooksNames() {
        return ontology.getAllBooksNames();
    }

    public BookInfo getBookDetails(String bookName) {
        return ontology.getBook(bookName);
    }

    public void addBook(String id, String format) {
        ontology.addBook(id, format);
    }

    public void addPropertyToBook(String bookName, String valueName, String propName) {
        ontology.addPropertyToBook(bookName, valueName, propName);
    }

    public void deleteBookFromSystem(String bookName) {
        ontology.deleteBook(bookName);
    }

    public void addBookPublicationYear(String bookName, int year) {
        this.ontology.addBookPublicationYear(bookName, year);
    }

    public void addBookSeriesOrder(String bookName, int order) {
        this.ontology.addBookSeriesOrder(bookName, order);
    }

    public boolean checkBookExists(String id) {
        return ontology.hasIndividual(id);
    }

    public boolean handleBookInteraction(String username, String bookTitle, String action) {
        return ontology.interactWithBook(username, bookTitle, action);
    }
}