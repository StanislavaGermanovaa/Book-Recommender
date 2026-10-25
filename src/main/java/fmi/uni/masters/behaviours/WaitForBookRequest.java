package fmi.uni.masters.behaviours;

import com.fasterxml.jackson.databind.ObjectMapper;
import fmi.uni.masters.BookInfo;
import fmi.uni.masters.BookOntology;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class WaitForBookRequest extends CyclicBehaviour {

    private BookOntology ontology;

    public WaitForBookRequest(BookOntology ontology, Agent agent) {
        this.ontology = ontology;
        this.myAgent = agent;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            System.out.println("Received REQUEST");

            ACLMessage reply = msg.createReply();
            List<BookInfo> allBooksData = new ArrayList<>();

            List<String> bookNames = ontology.getAllBooksNames();
            for (String bookName : bookNames) {
                BookInfo info = ontology.getBook(bookName);
                if (info != null) {
                    allBooksData.add(info);
                }
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                String jsonResponse = mapper.writeValueAsString(allBooksData);

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(jsonResponse);
                reply.setLanguage("JSON");
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("Грешка при сериализацията на книгите.");
                e.printStackTrace();
            }

            myAgent.send(reply);
        } else {
            block();
        }
    }
}