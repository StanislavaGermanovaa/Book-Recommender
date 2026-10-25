package fmi.uni.masters.behaviours;

import fmi.uni.masters.agent.BookRecommenderAgent;
import fmi.uni.masters.BookOntology;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class WaitForRequests extends CyclicBehaviour {

    private BookRecommenderAgent myRecommenderAgent;
    private BookOntology ontology;

    public WaitForRequests(BookRecommenderAgent agent, BookOntology ontology) {
        this.myRecommenderAgent = agent;
        this.ontology = ontology;
        this.myAgent = agent;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            String content = msg.getContent();
            if (content == null || content.isEmpty()) {
                block();
                return;
            }

            System.out.println("Received REQUEST -> " + content);
            String[] parts = content.split(":");
            boolean success = false;

            if (parts.length == 3) {
                String action = parts[0];

                if ("LOGIN".equals(action) || "REGISTER".equals(action)) {
                    String username = parts[1];
                    String password = parts[2];

                    if ("LOGIN".equals(action)) {
                        success = ontology.validateUserLogin(username, password);
                    } else {
                        success = ontology.registerNewUser(username, password);
                    }
                }

                else if ("LIKE".equals(action) || "UNLIKE".equals(action)) {
                    String username = parts[1];
                    String bookTitle = parts[2];

                    success = myRecommenderAgent.handleBookInteraction(username, bookTitle, action);
                }
            }

            ACLMessage reply = msg.createReply();
            reply.setPerformative(success ? ACLMessage.INFORM : ACLMessage.FAILURE);
            myAgent.send(reply);

        } else {
            block();
        }
    }
}
